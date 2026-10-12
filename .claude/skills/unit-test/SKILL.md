---
name: unit-test
description: "Unit test conventions for the Martp android open source project. Use when writing/reviewing JUnit 5 + MockK unit tests: configuring `MainDispatcherExtension` for coroutines, mocking with strict `mockk()`, enforcing the Given/When/Then pattern with backticked test names, applying `exactly = N` to `verify`/`coVerify`, and testing `StateFlow`s."
---

## 1. Test class configuration

### 1.1 Coroutine extensions
- For tests that involve coroutines (almost all of them), use `MainDispatcherExtension` with `@ExtendWith(MainDispatcherExtension::class)`
- The extension replaces `Dispatchers.Main` with a `StandardTestDispatcher` before each test. Every `TestDispatcher` created after that (including the one inside `runTest`) automatically shares its scheduler, so `advanceUntilIdle()` controls all of them. Without the extension, each dispatcher gets its own scheduler and the test loses control of virtual time.

```kotlin
@ExtendWith(MainDispatcherExtension::class)
class MainViewModelTest {}
```

### 1.2 Dependency injection
- You should never create a dependency injection context (Hilt) inside the unit tests. If there is a class that needs it to be tested, skip that class and report it to the user so it can be refactored first.
- You should never share dependencies between tests, all the tests should run with new instantiated mocked dependencies inside the `setup` method with `@BeforeEach` annotation.

## 2. Setup and teardown

### 2.1 `setup()` method (@BeforeEach)
- Always use `@BeforeEach` to configure the test environment
- Initialize mocks and dependencies, the mock behaviors should be created inside each unit test method never in the `setup` method (the only exception is described below).
- Always create the class under test in `setup`, never inside a test method or a helper.
- Whenever the class under test needs a `CoroutineDispatcher`, pass `UnconfinedTestDispatcher()` as a constructor parameter
- The only mock behavior allowed in `setup` is the one the class under test needs to be constructed: if it calls a dependency in its constructor or property initializers (e.g. `MainViewModel` calls `repository.observeUserMapArts()` to build `galleryState`), a strict mock throws unless that call is configured before the constructor runs. Configure only that call, right before creating the class, and make it return something each test can control (see 2.2). Everything else stays in the test methods.

```kotlin
private lateinit var mapArtsDao: MapArtsDao
private lateinit var repository: MartpRepository

@BeforeEach
fun setup() {
    // Initialize mocks
    mapArtsDao = mockk()

    // Create the class under test
    repository = MartpRepository(
        mapArtDao = mapArtsDao,
        ioDispatcher = UnconfinedTestDispatcher(),
        // other dependencies...
    )
}
```

### 2.2 Dependencies called when the class is created
- When the constructor-time call returns a `Flow`, back it with a `Channel` created in `setup`, so the class is still created in `setup` and each test decides what the flow emits or throws

```kotlin
private lateinit var viewModel: MainViewModel
private lateinit var repository: MartpRepository
private lateinit var fakeUserMapArtsChannel: Channel<List<MapArtEntity>>

@BeforeEach
fun setup() {
    repository = mockk()

    // Only behavior configured in setup: MainViewModel calls it when it is created
    fakeUserMapArtsChannel = Channel(Channel.UNLIMITED)
    every { repository.observeUserMapArts() } returns fakeUserMapArtsChannel.receiveAsFlow()

    viewModel = MainViewModel(repository = repository)
}

// In a test: emit a value
fakeUserMapArtsChannel.trySend(emptyList())

// In a test: make the flow throw
fakeUserMapArtsChannel.close(LocalStorageErrorFailure("fake error"))
```

### 2.3 `tearDown()` method (@AfterEach)
- Mocks are recreated in `setup()` for every test, so `@AfterEach` is not needed by default
- Add it only when the test class uses `mockkObject` or `mockkStatic`, because those replace global state that leaks into other test classes
- Call `unmockkAll()` in it

```kotlin
@AfterEach
fun tearDown() {
    unmockkAll()
}
```

## 3. Mocks and dependencies

### 3.1 Declaring mocks
- Declare mocks as `private lateinit var` properties of the test class and instantiate them in `setup()`
- Always use the strict `mockk()`, never `mockk(relaxed = true)` (see 9.4)

```kotlin
private lateinit var repository: MartpRepository
private lateinit var mapArtsDao: MapArtsDao

@BeforeEach
fun setup() {
    repository = mockk()
    mapArtsDao = mockk()
}
```

### 3.2 Configuring mocks
- Configure mock behavior in the test method; when reused, prefer creating it inside the test methods to isolate each behavior
- Use `every { }` for synchronous functions
- Use `coEvery { }` for suspend functions
- Use `just runs` for methods that return no value

```kotlin
every { mapArtsDao.getAll() } returns flowOf(emptyList())
coEvery { repository.getMapZoomPreference() } returns fakeMapZoom
coEvery { repository.setUserName(any(), any()) } just runs
```

## 4. Test structure

### 4.1 Test naming
- Use descriptive names with backticks, in English
- Follow the pattern: `` `if <condition> should <expected behavior>` ``
- Clearly describe the condition and the expected behavior

```kotlin
@Test
fun `if repository returns an empty list should emit EmptyList ui state`() = runTest {
    // ...
}

@Test
fun `if mapArtDao throws an exception should throw a Failure`() = runTest {
    // ...
}
```

### 4.2 Given / When / Then structure
- Organize the test into three sections, each one marked with its comment:
  - `// Given`: prepare data and configure mocks
  - `// When`: invoke the method under test
  - `// Then`: verify the results

```kotlin
@Test
fun `if repository returns the user info should emit UserFound ui state`() = runTest {
    // Given
    val fakeUserInfo = mockk<UserInfoEntity>()
    coEvery { repository.fetchCurrentUserInfo() } returns fakeUserInfo

    // When
    viewModel.tryToGetUserInfo()
    advanceUntilIdle()

    // Then
    assertEquals(UserInfoUiState.UserFound(fakeUserInfo), viewModel.userInfoState.value)
}
```

### 4.3 Coroutine tests
- Always use `runTest` for tests that involve coroutines
- Import `kotlinx.coroutines.test.runTest`
- Coroutines launched in `viewModelScope` do not run until the scheduler advances: call `advanceUntilIdle()` after the `// When` call and before asserting
- `delay()` is skipped in virtual time, so debounced methods are tested with `advanceUntilIdle()` (run everything) or `advanceTimeBy()` (stop in the middle of the delay)

```kotlin
@Test
fun `if setUserName is called twice before the debounce ends should save only the last username`() = runTest {
    // Given
    val fakeUserId = "fake-user-id"
    val fakeFirstUsername = "fake first username"
    val fakeLastUsername = "fake last username"
    coEvery { repository.setUserName(any(), any()) } just runs

    // When
    viewModel.setUserName(fakeFirstUsername, fakeUserId)
    viewModel.setUserName(fakeLastUsername, fakeUserId)
    advanceUntilIdle()

    // Then
    coVerify(exactly = 0) { repository.setUserName(fakeFirstUsername, fakeUserId) }
    coVerify(exactly = 1) { repository.setUserName(fakeLastUsername, fakeUserId) }
}
```

### 4.4 Flow tests
- For a cold `Flow` returned by a method, collect it directly with `first()` or `toList()`
- For a `StateFlow`/`SharedFlow` exposed by a ViewModel, collect every emission into a list inside `backgroundScope` with `UnconfinedTestDispatcher(testScheduler)`, then assert on the list. This is the pattern used in `MainViewModelTest`:

```kotlin
private fun <T> TestScope.collectStates(stateFlow: StateFlow<T>): List<T> {
    val states = mutableListOf<T>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        stateFlow.toList(states)
    }
    return states
}

@Test
fun `if repository returns an empty list should emit EmptyList ui state`() = runTest {
    // Given
    fakeUserMapArtsChannel.trySend(emptyList())

    // When
    val states = collectStates(viewModel.galleryState)
    advanceUntilIdle()

    // Then
    assertEquals(listOf(GalleryUiState.Loading, GalleryUiState.EmptyList), states)
}
```

## 5. Test data

### 5.1 Naming fake data
- Use the `fake` prefix for test data (e.g. `fakeUserId`, `fakeUsername`, `fakeMapZoom`)
- Use descriptive names that indicate the data's type and purpose
- Declare data at the start of the test method

```kotlin
val fakeUserId = "fake-user-id"
val fakeUsername = "Fake User"
val fakeEmail = "fake.user@example.com"
val fakeMapZoom = 14.5f
```

### 5.2 Test values
- Use realistic values that are clearly identifiable as test data
- Avoid values that could be confused with real data
- Use distinct values across different tests to make debugging easier

```kotlin
// ✅ Good
val fakeEmail = "fake.user@example.com"
val fakeUserId = "fake-user-id"

// ❌ Avoid
val email = "joao.silva@gmail.com" // could be a real email
```

## 6. Verifications and assertions

### 6.1 Assertions
- Use the JUnit 5 assertions (`org.junit.jupiter.api.Assertions`) with direct imports; `kotlin.test` is not a dependency of this project
- Use `assertEquals` for equality checks
- Use `assertTrue` / `assertFalse` for boolean checks
- Use `assertNull` / `assertNotNull` for null/non-null checks
- Use `assertInstanceOf` for type checks
- Use `assertThrows` when a call is expected to throw, never `try`/`catch` (a `try`/`catch` test passes when nothing is thrown)
- Do not use Kotlin's `assert()`: it has no failure message and depends on JVM assertions being enabled
- Verify multiple properties of the result when relevant

```kotlin
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Assertions.assertInstanceOf
//import org.junit.jupiter.api.Assertions.assertTrue
//import org.junit.jupiter.api.assertThrows

assertEquals(GalleryUiState.Loading, states[0])
assertInstanceOf(GalleryUiState.Error::class.java, states[1])
assertTrue(viewModel.isUserInfoBadgeVisible.value)

assertThrows<Failure> {
    repository.observeUserMapArts().first()
}
```

### 6.2 Call verification
- Use `coVerify` to verify calls to suspend functions
- Use `verify` to verify calls to synchronous functions
- Always specify the exact number of expected calls with `exactly = N`
- Verify passed parameters when relevant

```kotlin
coVerify(exactly = 1) {
    repository.setUserName(fakeUsername, fakeUserId)
}

verify(exactly = 1) {
    mapArtsDao.getAll()
}
```

### 6.3 Verifying non-calls
- Explicitly verify when methods should not be called using `exactly = 0` in `verify`/`coVerify`
- This makes the test's intent clearer

```kotlin
coVerify(exactly = 0) {
    repository.setUserEmail(any(), any())
}
```

## 7. Test cases

### 7.1 Scenario coverage
- Test the happy path
- Test error cases and exceptions
- Test cases with null values
- Test cases with empty/default values
- Test edge cases (boundary values)

### 7.2 Common test-case examples

```kotlin
// Success case
@Test
fun `if repository returns a not empty list should emit NotEmptyList ui state`() = runTest {
    // ...
}

// Empty value case
@Test
fun `if repository returns an empty list should emit EmptyList ui state`() = runTest {
    // ...
}

// Null value case
@Test
fun `if user has no username and no email should show the user info badge`() = runTest {
    // Given
    val fakeUserInfo = mockk<UserInfoEntity>()
    every { fakeUserInfo.username } returns null
    every { fakeUserInfo.email } returns null
    coEvery { repository.fetchCurrentUserInfo() } returns fakeUserInfo

    // When
    viewModel.handleBadgesVisibilities()
    advanceUntilIdle()

    // Then
    assertTrue(viewModel.isUserInfoBadgeVisible.value)
}

// Error case
@Test
fun `if repository throws a Failure should emit Error ui state`() = runTest {
    // Given
    fakeUserMapArtsChannel.close(LocalStorageErrorFailure("fake error"))

    // When
    val states = collectStates(viewModel.galleryState)
    advanceUntilIdle()

    // Then
    assertEquals(2, states.size)
    assertEquals(GalleryUiState.Loading, states[0])
    assertInstanceOf(GalleryUiState.Error::class.java, states[1])
}
```

## 8. Handling external dependencies

### 8.1 Android Context
- Use `mockk<Context>()` when needed

### 8.2 Dispatchers
- Never mock `CoroutineDispatcher`
- Pass `UnconfinedTestDispatcher()` to the classes that receive a dispatcher (e.g. the `@IoDispatcher` of `MartpRepository`)
- Create it with no arguments and only in a class annotated with `@ExtendWith(MainDispatcherExtension::class)`, so it shares the scheduler used by `runTest` (see 1.1)

```kotlin
repository = MartpRepository(
    ioDispatcher = UnconfinedTestDispatcher(),
    // other dependencies...
)
```

## 9. Good practices

### 9.1 Isolation
- Each test must be independent and must not rely on other tests
- Recreate mocks and the class under test for every test in `setup()`
- Do not share state across tests

### 9.2 Clarity
- Tests must be easy to read and understand
- Use descriptive names for variables and methods
- Comment complex code when necessary
- Avoid complex logic in tests

### 9.3 Maintainability
- Extract common test data into helper methods when reused
- Use helper functions to build complex test objects

```kotlin
private fun createFakeUserInfo(
    username: String? = "Fake User",
    email: String? = "fake.user@example.com"
): UserInfoEntity {
    val fakeUserInfo = mockk<UserInfoEntity>()
    every { fakeUserInfo.username } returns username
    every { fakeUserInfo.email } returns email
    return fakeUserInfo
}
```

### 9.4 Strict mocks
- Never use `relaxed = true` on mocks: it lets additions or modifications to the concrete logic go untested (it can produce a false positive — tests covering lines without exercising the new behavior)
- Add an `every`/`coEvery` for the methods and data used by the methods under test
- Configure only the behavior required by the test

### 9.5 Argument capture
- Use `slot` when you need to capture arguments passed to mocks (possible callbacks and lambdas)

```kotlin
val mapArtSlot = slot<MapArtEntity>()
coEvery { mapArtsDao.insert(capture(mapArtSlot)) } just runs

// When ...

assertEquals(fakeTitle, mapArtSlot.captured.title)
```

## 10. Review checklist

Before finalizing a test, verify:

- [ ] The class uses `@ExtendWith(MainDispatcherExtension::class)` if it involves coroutines
- [ ] There is no dependency injection context in the tests
- [ ] Mocks are `lateinit var`s created with strict `mockk()` in `@BeforeEach`, with no `relaxed = true`
- [ ] The class under test is created in `setup()`, never inside a test method
- [ ] Mock behaviors are configured inside the test methods, not in `setup()` (except the call the class under test makes when it is created)
- [ ] Has `@AfterEach` with `unmockkAll()` only if `mockkObject`/`mockkStatic` is used
- [ ] Test name follows `` `if <condition> should <expected behavior>` ``
- [ ] Test has the `// Given`, `// When`, `// Then` comments
- [ ] Test uses `runTest` if it involves coroutines, and advances the scheduler before asserting
- [ ] Test data uses the `fake` prefix and is descriptive
- [ ] Assertions come from `org.junit.jupiter.api.Assertions`, with no Kotlin `assert()` and no `try`/`catch`
- [ ] Calls are verified with `coVerify`/`verify` and `exactly = N`
- [ ] Tests success, error, null, and empty cases when applicable
- [ ] Code is clean and easy to understand

**Last updated:** 12/10/2026
