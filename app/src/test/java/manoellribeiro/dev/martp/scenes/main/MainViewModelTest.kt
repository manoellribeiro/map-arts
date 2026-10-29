package manoellribeiro.dev.martp.scenes.main

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import manoellribeiro.dev.martp.core.models.SketchArtType
import manoellribeiro.dev.martp.infra.MainDispatcherExtension
import manoellribeiro.dev.martp.scenes.artStyleSettings.ArtStyleSettingsUiState
import manoellribeiro.dev.martp.scenes.gallery.GalleryUiState
import manoellribeiro.dev.martp.scenes.userInfo.UserInfoUiState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var repository: MartpRepository
    private lateinit var fakeUserMapArtsChannel: Channel<List<MapArtEntity>>

    @BeforeEach
    fun setup() {
        repository = mockk()

        // Only behavior configured in setup: MainViewModel calls observeUserMapArts() when it is
        // created, so it has to answer before the constructor runs. Each test decides what the
        // flow emits (or throws) through the channel.
        fakeUserMapArtsChannel = Channel(Channel.UNLIMITED)
        every { repository.observeUserMapArts() } returns fakeUserMapArtsChannel.receiveAsFlow()

        viewModel = MainViewModel(repository = repository)
    }

    private fun <T> TestScope.collectStates(stateFlow: StateFlow<T>): List<T> {
        val states = mutableListOf<T>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            stateFlow.toList(states)
        }
        return states
    }

    private fun createFakeUserInfo(
        username: String? = "Fake User",
        email: String? = "fake.user@example.com"
    ): UserInfoEntity {
        return UserInfoEntity(
            id = "fake-user-id",
            username = username,
            email = email
        )
    }

    private fun createFakeMapArt(): MapArtEntity {
        return MapArtEntity(
            id = "fake-map-art-id",
            title = "Fake Map Art",
            description = "Fake map art description",
            latitude = 12.5f,
            longitude = -38.5f,
            dateInMillis = 123456789L,
            imagePathLocation = "fake/path/image.png"
        )
    }

    // region galleryState

    @Test
    fun `if repository returns an empty list should emit EmptyList ui state`() = runTest {
        // Given
        fakeUserMapArtsChannel.trySend(emptyList())

        // When
        val states = collectStates(viewModel.galleryState)
        advanceUntilIdle()

        // Then
        assertEquals(listOf(GalleryUiState.Loading, GalleryUiState.EmptyList), states)
        verify(exactly = 1) { repository.observeUserMapArts() }
    }

    @Test
    fun `if repository returns a not empty list should emit NotEmptyList ui state`() = runTest {
        // Given
        val fakeMapArts = listOf(createFakeMapArt())
        fakeUserMapArtsChannel.trySend(fakeMapArts)

        // When
        val states = collectStates(viewModel.galleryState)
        advanceUntilIdle()

        // Then
        assertEquals(listOf(GalleryUiState.Loading, GalleryUiState.NotEmptyList(fakeMapArts)), states)
    }

    @Test
    fun `if repository throws a Failure should emit Error ui state`() = runTest {
        // Given
        val fakeErrorMessage = "fake local storage error"
        fakeUserMapArtsChannel.close(LocalStorageErrorFailure(fakeErrorMessage))

        // When
        val states = collectStates(viewModel.galleryState)
        advanceUntilIdle()

        // Then
        assertEquals(2, states.size)
        assertEquals(GalleryUiState.Loading, states[0])
        val errorState = assertInstanceOf(GalleryUiState.Error::class.java, states[1])
        assertInstanceOf(LocalStorageErrorFailure::class.java, errorState.failure)
    }

    @Test
    fun `if repository throws an exception that is not a Failure should rethrow it without emitting Error ui state`() {
        // Given
        val fakeErrorMessage = "fake unexpected error"
        fakeUserMapArtsChannel.close(IllegalStateException(fakeErrorMessage))
        var states = emptyList<GalleryUiState>()

        // When
        val thrownException = assertThrows<IllegalStateException> {
            runTest {
                states = collectStates(viewModel.galleryState)
                advanceUntilIdle()
            }
        }

        // Then
        // The message is compared because coroutines may rethrow a copy of the original exception
        assertEquals(fakeErrorMessage, thrownException.message)
        assertEquals(listOf<GalleryUiState>(GalleryUiState.Loading), states)
    }

    // endregion

    // region userInfoState / tryToGetUserInfo

    @Test
    fun `if the view model was just created should have Loading as the user info ui state`() = runTest {
        // Given
        // The view model created in setup, with no method called

        // When
        advanceUntilIdle()

        // Then
        assertEquals(UserInfoUiState.Loading, viewModel.userInfoState.value)
        coVerify(exactly = 0) { repository.fetchCurrentUserInfo() }
    }

    @Test
    fun `if repository returns the user info should emit UserFound ui state`() = runTest {
        // Given
        val fakeUserInfo = createFakeUserInfo()
        coEvery { repository.fetchCurrentUserInfo() } returns fakeUserInfo
        val states = collectStates(viewModel.userInfoState)

        // When
        viewModel.tryToGetUserInfo()
        advanceUntilIdle()

        // Then
        assertEquals(
            listOf(UserInfoUiState.Loading, UserInfoUiState.UserFound(fakeUserInfo)),
            states
        )
        coVerify(exactly = 1) { repository.fetchCurrentUserInfo() }
    }

    @Test
    fun `if tryToGetUserInfo is called again after a user was found should emit Loading before the new UserFound ui state`() = runTest {
        // Given
        val fakeFirstUserInfo = createFakeUserInfo(username = "Fake First User")
        val fakeSecondUserInfo = createFakeUserInfo(username = "Fake Second User")
        coEvery { repository.fetchCurrentUserInfo() } returnsMany listOf(fakeFirstUserInfo, fakeSecondUserInfo)
        val states = collectStates(viewModel.userInfoState)
        viewModel.tryToGetUserInfo()
        advanceUntilIdle()

        // When
        viewModel.tryToGetUserInfo()
        advanceUntilIdle()

        // Then
        assertEquals(
            listOf(
                UserInfoUiState.Loading,
                UserInfoUiState.UserFound(fakeFirstUserInfo),
                UserInfoUiState.Loading,
                UserInfoUiState.UserFound(fakeSecondUserInfo)
            ),
            states
        )
        coVerify(exactly = 2) { repository.fetchCurrentUserInfo() }
    }

    // endregion

    // region artStyleSettingsState / getArtStyleSettings

    @Test
    fun `if the view model was just created should have Loading as the art style settings ui state`() = runTest {
        // Given
        // The view model created in setup, with no method called

        // When
        advanceUntilIdle()

        // Then
        assertEquals(ArtStyleSettingsUiState.Loading, viewModel.artStyleSettingsState.value)
        coVerify(exactly = 0) { repository.getMapZoomPreference() }
        coVerify(exactly = 0) { repository.getArtStylePreference() }
        coVerify(exactly = 0) { repository.getColorPalettePreference() }
    }

    @Test
    fun `if repository returns the art style preferences should emit SettingsLoaded ui state`() = runTest {
        // Given
        val fakeMapZoom = 14.5f
        val fakeArtStyle = SketchArtType.POINTILLISM
        val fakeColorPaletteName = "Fake Settings Palette"
        coEvery { repository.getMapZoomPreference() } returns fakeMapZoom
        coEvery { repository.getArtStylePreference() } returns fakeArtStyle
        coEvery { repository.getColorPalettePreference() } returns fakeColorPaletteName
        val states = collectStates(viewModel.artStyleSettingsState)

        // When
        viewModel.getArtStyleSettings()
        advanceUntilIdle()

        // Then
        assertEquals(
            listOf(
                ArtStyleSettingsUiState.Loading,
                ArtStyleSettingsUiState.SettingsLoaded(
                    mapZoom = fakeMapZoom,
                    mapArtStyle = fakeArtStyle,
                    colorPaletteSelected = fakeColorPaletteName
                )
            ),
            states
        )
        coVerify(exactly = 1) { repository.getMapZoomPreference() }
        coVerify(exactly = 1) { repository.getArtStylePreference() }
        coVerify(exactly = 1) { repository.getColorPalettePreference() }
    }

    @Test
    fun `if getArtStyleSettings is called again after the settings were loaded should emit Loading before the new SettingsLoaded ui state`() = runTest {
        // Given
        val fakeFirstMapZoom = 12.5f
        val fakeSecondMapZoom = 17.5f
        val fakeFirstArtStyle = SketchArtType.DEFAULT
        val fakeSecondArtStyle = SketchArtType.POINTILLISM
        val fakeFirstColorPaletteName = "Fake First Palette"
        val fakeSecondColorPaletteName = "Fake Second Palette"
        coEvery { repository.getMapZoomPreference() } returnsMany listOf(fakeFirstMapZoom, fakeSecondMapZoom)
        coEvery { repository.getArtStylePreference() } returnsMany listOf(fakeFirstArtStyle, fakeSecondArtStyle)
        coEvery { repository.getColorPalettePreference() } returnsMany listOf(fakeFirstColorPaletteName, fakeSecondColorPaletteName)
        val states = collectStates(viewModel.artStyleSettingsState)
        viewModel.getArtStyleSettings()
        advanceUntilIdle()

        // When
        viewModel.getArtStyleSettings()
        advanceUntilIdle()

        // Then
        assertEquals(
            listOf(
                ArtStyleSettingsUiState.Loading,
                ArtStyleSettingsUiState.SettingsLoaded(
                    mapZoom = fakeFirstMapZoom,
                    mapArtStyle = fakeFirstArtStyle,
                    colorPaletteSelected = fakeFirstColorPaletteName
                ),
                ArtStyleSettingsUiState.Loading,
                ArtStyleSettingsUiState.SettingsLoaded(
                    mapZoom = fakeSecondMapZoom,
                    mapArtStyle = fakeSecondArtStyle,
                    colorPaletteSelected = fakeSecondColorPaletteName
                )
            ),
            states
        )
        coVerify(exactly = 2) { repository.getMapZoomPreference() }
        coVerify(exactly = 2) { repository.getArtStylePreference() }
        coVerify(exactly = 2) { repository.getColorPalettePreference() }
    }

    // endregion

    // region setUserName

    @Test
    fun `if the username debounce has not ended should not save the username`() = runTest {
        // Given
        val fakeUserId = "fake-user-id-1"
        val fakeUsername = "Fake Pending User"
        coEvery { repository.setUserName(any(), any()) } just runs

        // When
        viewModel.setUserName(fakeUsername, fakeUserId)
        advanceTimeBy(1999)
        runCurrent()

        // Then
        coVerify(exactly = 0) { repository.setUserName(any(), any()) }
    }

    @Test
    fun `if the username debounce of 2000 milliseconds has ended should save the username`() = runTest {
        // Given
        val fakeUserId = "fake-user-id-2"
        val fakeUsername = "Fake Saved User"
        coEvery { repository.setUserName(any(), any()) } just runs

        // When
        viewModel.setUserName(fakeUsername, fakeUserId)
        advanceTimeBy(2000)
        runCurrent()

        // Then
        coVerify(exactly = 1) { repository.setUserName(fakeUsername, fakeUserId) }
        coVerify(exactly = 0) { repository.setUserEmail(any(), any()) }
    }

    @Test
    fun `if setUserName is called twice before the debounce ends should save only the last username`() = runTest {
        // Given
        val fakeUserId = "fake-user-id-3"
        val fakeFirstUsername = "fake first username"
        val fakeLastUsername = "fake last username"
        coEvery { repository.setUserName(any(), any()) } just runs

        // When
        viewModel.setUserName(fakeFirstUsername, fakeUserId)
        advanceTimeBy(1999)
        viewModel.setUserName(fakeLastUsername, fakeUserId)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { repository.setUserName(fakeFirstUsername, fakeUserId) }
        coVerify(exactly = 1) { repository.setUserName(fakeLastUsername, fakeUserId) }
    }

    @Test
    fun `if setUserName is called again after the debounce ended should save both usernames`() = runTest {
        // Given
        val fakeUserId = "fake-user-id-4"
        val fakeFirstUsername = "fake earlier username"
        val fakeLastUsername = "fake later username"
        coEvery { repository.setUserName(any(), any()) } just runs

        // When
        viewModel.setUserName(fakeFirstUsername, fakeUserId)
        advanceUntilIdle()
        viewModel.setUserName(fakeLastUsername, fakeUserId)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.setUserName(fakeFirstUsername, fakeUserId) }
        coVerify(exactly = 1) { repository.setUserName(fakeLastUsername, fakeUserId) }
    }

    // endregion

    // region setUserEmail

    @Test
    fun `if the email debounce has not ended should not save the email`() = runTest {
        // Given
        val fakeUserId = "fake-user-id-5"
        val fakeEmail = "fake.pending@example.com"
        coEvery { repository.setUserEmail(any(), any()) } just runs

        // When
        viewModel.setUserEmail(fakeEmail, fakeUserId)
        advanceTimeBy(1999)
        runCurrent()

        // Then
        coVerify(exactly = 0) { repository.setUserEmail(any(), any()) }
    }

    @Test
    fun `if the email debounce of 2000 milliseconds has ended should save the email`() = runTest {
        // Given
        val fakeUserId = "fake-user-id-6"
        val fakeEmail = "fake.saved@example.com"
        coEvery { repository.setUserEmail(any(), any()) } just runs

        // When
        viewModel.setUserEmail(fakeEmail, fakeUserId)
        advanceTimeBy(2000)
        runCurrent()

        // Then
        coVerify(exactly = 1) { repository.setUserEmail(fakeEmail, fakeUserId) }
        coVerify(exactly = 0) { repository.setUserName(any(), any()) }
    }

    @Test
    fun `if setUserEmail is called twice before the debounce ends should save only the last email`() = runTest {
        // Given
        val fakeUserId = "fake-user-id-7"
        val fakeFirstEmail = "fake.first@example.com"
        val fakeLastEmail = "fake.last@example.com"
        coEvery { repository.setUserEmail(any(), any()) } just runs

        // When
        viewModel.setUserEmail(fakeFirstEmail, fakeUserId)
        advanceTimeBy(1999)
        viewModel.setUserEmail(fakeLastEmail, fakeUserId)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { repository.setUserEmail(fakeFirstEmail, fakeUserId) }
        coVerify(exactly = 1) { repository.setUserEmail(fakeLastEmail, fakeUserId) }
    }

    @Test
    fun `if setUserEmail is called again after the debounce ended should save both emails`() = runTest {
        // Given
        val fakeUserId = "fake-user-id-8"
        val fakeFirstEmail = "fake.earlier@example.com"
        val fakeLastEmail = "fake.later@example.com"
        coEvery { repository.setUserEmail(any(), any()) } just runs

        // When
        viewModel.setUserEmail(fakeFirstEmail, fakeUserId)
        advanceUntilIdle()
        viewModel.setUserEmail(fakeLastEmail, fakeUserId)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.setUserEmail(fakeFirstEmail, fakeUserId) }
        coVerify(exactly = 1) { repository.setUserEmail(fakeLastEmail, fakeUserId) }
    }

    @Test
    fun `if setUserName and setUserEmail are called before the debounce ends should save both the username and the email`() = runTest {
        // Given
        val fakeUserId = "fake-user-id-9"
        val fakeUsername = "Fake Independent User"
        val fakeEmail = "fake.independent@example.com"
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs

        // When
        viewModel.setUserName(fakeUsername, fakeUserId)
        viewModel.setUserEmail(fakeEmail, fakeUserId)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.setUserName(fakeUsername, fakeUserId) }
        coVerify(exactly = 1) { repository.setUserEmail(fakeEmail, fakeUserId) }
    }

    // endregion

    // region setMapZoom

    @Test
    fun `if the map zoom debounce has not ended should not save the map zoom`() = runTest {
        // Given
        val fakeMapZoom = 11.5f
        coEvery { repository.setMapZoomPreference(any()) } just runs

        // When
        viewModel.setMapZoom(fakeMapZoom)
        advanceTimeBy(1499)
        runCurrent()

        // Then
        coVerify(exactly = 0) { repository.setMapZoomPreference(any()) }
    }

    @Test
    fun `if the map zoom debounce of 1500 milliseconds has ended should save the map zoom`() = runTest {
        // Given
        val fakeMapZoom = 13.5f
        coEvery { repository.setMapZoomPreference(any()) } just runs

        // When
        viewModel.setMapZoom(fakeMapZoom)
        advanceTimeBy(1500)
        runCurrent()

        // Then
        coVerify(exactly = 1) { repository.setMapZoomPreference(fakeMapZoom) }
        coVerify(exactly = 0) { repository.setMapArtStylePreference(any()) }
    }

    @Test
    fun `if setMapZoom is called twice before the debounce ends should save only the last map zoom`() = runTest {
        // Given
        val fakeFirstMapZoom = 10.5f
        val fakeLastMapZoom = 16.5f
        coEvery { repository.setMapZoomPreference(any()) } just runs

        // When
        viewModel.setMapZoom(fakeFirstMapZoom)
        advanceTimeBy(1499)
        viewModel.setMapZoom(fakeLastMapZoom)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { repository.setMapZoomPreference(fakeFirstMapZoom) }
        coVerify(exactly = 1) { repository.setMapZoomPreference(fakeLastMapZoom) }
    }

    @Test
    fun `if setMapZoom is called again after the debounce ended should save both map zooms`() = runTest {
        // Given
        val fakeFirstMapZoom = 9.5f
        val fakeLastMapZoom = 18.5f
        coEvery { repository.setMapZoomPreference(any()) } just runs

        // When
        viewModel.setMapZoom(fakeFirstMapZoom)
        advanceUntilIdle()
        viewModel.setMapZoom(fakeLastMapZoom)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.setMapZoomPreference(fakeFirstMapZoom) }
        coVerify(exactly = 1) { repository.setMapZoomPreference(fakeLastMapZoom) }
    }

    // endregion

    // region setMapStyle

    @Test
    fun `if the map style debounce has not ended should not save the map style`() = runTest {
        // Given
        val fakeArtStyle = SketchArtType.POINTILLISM
        coEvery { repository.setMapArtStylePreference(any()) } just runs

        // When
        viewModel.setMapStyle(fakeArtStyle)
        advanceTimeBy(1499)
        runCurrent()

        // Then
        coVerify(exactly = 0) { repository.setMapArtStylePreference(any()) }
    }

    @Test
    fun `if the map style debounce of 1500 milliseconds has ended should save the map style`() = runTest {
        // Given
        val fakeArtStyle = SketchArtType.POINTILLISM
        coEvery { repository.setMapArtStylePreference(any()) } just runs

        // When
        viewModel.setMapStyle(fakeArtStyle)
        advanceTimeBy(1500)
        runCurrent()

        // Then
        coVerify(exactly = 1) { repository.setMapArtStylePreference(fakeArtStyle) }
        coVerify(exactly = 0) { repository.setMapZoomPreference(any()) }
    }

    @Test
    fun `if setMapStyle is called twice before the debounce ends should save only the last map style`() = runTest {
        // Given
        val fakeFirstArtStyle = SketchArtType.DEFAULT
        val fakeLastArtStyle = SketchArtType.POINTILLISM
        coEvery { repository.setMapArtStylePreference(any()) } just runs

        // When
        viewModel.setMapStyle(fakeFirstArtStyle)
        advanceTimeBy(1499)
        viewModel.setMapStyle(fakeLastArtStyle)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { repository.setMapArtStylePreference(fakeFirstArtStyle) }
        coVerify(exactly = 1) { repository.setMapArtStylePreference(fakeLastArtStyle) }
    }

    @Test
    fun `if setMapStyle is called again after the debounce ended should save both map styles`() = runTest {
        // Given
        val fakeFirstArtStyle = SketchArtType.POINTILLISM
        val fakeLastArtStyle = SketchArtType.DEFAULT
        coEvery { repository.setMapArtStylePreference(any()) } just runs

        // When
        viewModel.setMapStyle(fakeFirstArtStyle)
        advanceUntilIdle()
        viewModel.setMapStyle(fakeLastArtStyle)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.setMapArtStylePreference(fakeFirstArtStyle) }
        coVerify(exactly = 1) { repository.setMapArtStylePreference(fakeLastArtStyle) }
    }

    @Test
    fun `if setMapZoom and setMapStyle are called before the debounce ends should save both the map zoom and the map style`() = runTest {
        // Given
        val fakeMapZoom = 15.5f
        val fakeArtStyle = SketchArtType.POINTILLISM
        coEvery { repository.setMapZoomPreference(any()) } just runs
        coEvery { repository.setMapArtStylePreference(any()) } just runs

        // When
        viewModel.setMapZoom(fakeMapZoom)
        viewModel.setMapStyle(fakeArtStyle)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.setMapZoomPreference(fakeMapZoom) }
        coVerify(exactly = 1) { repository.setMapArtStylePreference(fakeArtStyle) }
    }

    // endregion

    // region setColorPalette

    @Test
    fun `if the color palette debounce has not ended should not save the color palette`() = runTest {
        // Given
        val fakeColorPaletteName = "Fake Pending Palette"
        coEvery { repository.setColorPalettePreference(any()) } just runs

        // When
        viewModel.setColorPalette(fakeColorPaletteName)
        advanceTimeBy(1499)
        runCurrent()

        // Then
        coVerify(exactly = 0) { repository.setColorPalettePreference(any()) }
    }

    @Test
    fun `if the color palette debounce of 1500 milliseconds has ended should save the color palette`() = runTest {
        // Given
        val fakeColorPaletteName = "Fake Saved Palette"
        coEvery { repository.setColorPalettePreference(any()) } just runs

        // When
        viewModel.setColorPalette(fakeColorPaletteName)
        advanceTimeBy(1500)
        runCurrent()

        // Then
        coVerify(exactly = 1) { repository.setColorPalettePreference(fakeColorPaletteName) }
        coVerify(exactly = 0) { repository.setMapArtStylePreference(any()) }
        coVerify(exactly = 0) { repository.setMapZoomPreference(any()) }
    }

    @Test
    fun `if setColorPalette is called twice before the debounce ends should save only the last color palette`() = runTest {
        // Given
        val fakeFirstColorPaletteName = "Fake First Debounced Palette"
        val fakeLastColorPaletteName = "Fake Last Debounced Palette"
        coEvery { repository.setColorPalettePreference(any()) } just runs

        // When
        viewModel.setColorPalette(fakeFirstColorPaletteName)
        advanceTimeBy(1499)
        viewModel.setColorPalette(fakeLastColorPaletteName)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { repository.setColorPalettePreference(fakeFirstColorPaletteName) }
        coVerify(exactly = 1) { repository.setColorPalettePreference(fakeLastColorPaletteName) }
    }

    @Test
    fun `if setColorPalette is called again after the debounce ended should save both color palettes`() = runTest {
        // Given
        val fakeFirstColorPaletteName = "Fake Earlier Palette"
        val fakeLastColorPaletteName = "Fake Later Palette"
        coEvery { repository.setColorPalettePreference(any()) } just runs

        // When
        viewModel.setColorPalette(fakeFirstColorPaletteName)
        advanceUntilIdle()
        viewModel.setColorPalette(fakeLastColorPaletteName)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.setColorPalettePreference(fakeFirstColorPaletteName) }
        coVerify(exactly = 1) { repository.setColorPalettePreference(fakeLastColorPaletteName) }
    }

    @Test
    fun `if setColorPalette and setMapStyle are called before the debounce ends should save both the color palette and the map style`() = runTest {
        // Given
        val fakeColorPaletteName = "Fake Independent Palette"
        val fakeArtStyle = SketchArtType.COLORFUL_MAP
        coEvery { repository.setColorPalettePreference(any()) } just runs
        coEvery { repository.setMapArtStylePreference(any()) } just runs

        // When
        viewModel.setColorPalette(fakeColorPaletteName)
        viewModel.setMapStyle(fakeArtStyle)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.setColorPalettePreference(fakeColorPaletteName) }
        coVerify(exactly = 1) { repository.setMapArtStylePreference(fakeArtStyle) }
    }

    // endregion

    // region isUserInfoBadgeVisible / handleBadgesVisibilities

    @Test
    fun `if the view model was just created should not show the user info badge`() = runTest {
        // Given
        // The view model created in setup, with no method called

        // When
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isUserInfoBadgeVisible.value)
        coVerify(exactly = 0) { repository.fetchCurrentUserInfo() }
    }

    @Test
    fun `if user has no username and no email should show the user info badge`() = runTest {
        // Given
        val fakeUserInfo = createFakeUserInfo(username = null, email = null)
        coEvery { repository.fetchCurrentUserInfo() } returns fakeUserInfo

        // When
        viewModel.handleBadgesVisibilities()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.isUserInfoBadgeVisible.value)
        coVerify(exactly = 1) { repository.fetchCurrentUserInfo() }
    }

    @Test
    fun `if user has an empty username and an empty email should show the user info badge`() = runTest {
        // Given
        val fakeUserInfo = createFakeUserInfo(username = "", email = "")
        coEvery { repository.fetchCurrentUserInfo() } returns fakeUserInfo

        // When
        viewModel.handleBadgesVisibilities()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.isUserInfoBadgeVisible.value)
    }

    @Test
    fun `if user has a null username and an empty email should show the user info badge`() = runTest {
        // Given
        val fakeUserInfo = createFakeUserInfo(username = null, email = "")
        coEvery { repository.fetchCurrentUserInfo() } returns fakeUserInfo

        // When
        viewModel.handleBadgesVisibilities()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.isUserInfoBadgeVisible.value)
    }

    @Test
    fun `if user has only a username should not show the user info badge`() = runTest {
        // Given
        val fakeUserInfo = createFakeUserInfo(username = "Fake Username Only", email = null)
        coEvery { repository.fetchCurrentUserInfo() } returns fakeUserInfo

        // When
        viewModel.handleBadgesVisibilities()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isUserInfoBadgeVisible.value)
        coVerify(exactly = 1) { repository.fetchCurrentUserInfo() }
    }

    @Test
    fun `if user has only an email should not show the user info badge`() = runTest {
        // Given
        val fakeUserInfo = createFakeUserInfo(username = null, email = "fake.email.only@example.com")
        coEvery { repository.fetchCurrentUserInfo() } returns fakeUserInfo

        // When
        viewModel.handleBadgesVisibilities()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isUserInfoBadgeVisible.value)
        coVerify(exactly = 1) { repository.fetchCurrentUserInfo() }
    }

    @Test
    fun `if user has a username and an email should not show the user info badge`() = runTest {
        // Given
        val fakeUserInfo = createFakeUserInfo(username = "Fake Complete User", email = "fake.complete@example.com")
        coEvery { repository.fetchCurrentUserInfo() } returns fakeUserInfo

        // When
        viewModel.handleBadgesVisibilities()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isUserInfoBadgeVisible.value)
        coVerify(exactly = 1) { repository.fetchCurrentUserInfo() }
    }

    @Test
    fun `if the badge is visible and the user fills the username should hide the user info badge`() = runTest {
        // Given
        val fakeEmptyUserInfo = createFakeUserInfo(username = null, email = null)
        val fakeFilledUserInfo = createFakeUserInfo(username = "Fake Filled User", email = null)
        coEvery { repository.fetchCurrentUserInfo() } returnsMany listOf(fakeEmptyUserInfo, fakeFilledUserInfo)
        val badgeVisibilities = collectStates(viewModel.isUserInfoBadgeVisible)
        viewModel.handleBadgesVisibilities()
        advanceUntilIdle()

        // When
        viewModel.handleBadgesVisibilities()
        advanceUntilIdle()

        // Then
        assertEquals(listOf(false, true, false), badgeVisibilities)
        coVerify(exactly = 2) { repository.fetchCurrentUserInfo() }
    }

    // endregion

}