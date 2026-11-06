package manoellribeiro.dev.martp.core.data.network.martp

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appcheck.AppCheckToken
import com.google.firebase.appcheck.FirebaseAppCheck
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.util.concurrent.ExecutionException

class AppCheckInterceptorTest {

    private lateinit var errorLogger: ErrorLogger
    private lateinit var appCheck: FirebaseAppCheck
    private lateinit var chain: Interceptor.Chain
    private lateinit var interceptor: AppCheckInterceptor

    @BeforeEach
    fun setup() {
        errorLogger = mockk()
        appCheck = mockk()
        chain = mockk()
        // Tasks.await blocks on a real Play Services task, so each test decides what it returns
        mockkStatic(Tasks::class)

        interceptor = AppCheckInterceptor(
            errorLogger = errorLogger,
            appCheck = appCheck
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun createFakeAppCheckTask(token: String): Task<AppCheckToken> {
        val fakeTask = mockk<Task<AppCheckToken>>()
        val fakeAppCheckToken = mockk<AppCheckToken>()
        every { fakeAppCheckToken.token } returns token
        every { Tasks.await(fakeTask) } returns fakeAppCheckToken
        return fakeTask
    }

    @Test
    fun `if app check returns a token should proceed with the request with the token in the app check header`() {
        // Given
        val fakeToken = "fake-app-check-token"
        val fakeRequest = Request.Builder()
            .url("https://fake.martp.example/color-palettes")
            .header("Fake-Header", "fake-header-value")
            .build()
        val fakeResponse = mockk<Response>()
        val proceededRequestSlot = slot<Request>()
        val fakeTask = createFakeAppCheckTask(token = fakeToken)
        every { appCheck.getAppCheckToken(any()) } returns fakeTask
        every { chain.request() } returns fakeRequest
        every { chain.proceed(capture(proceededRequestSlot)) } returns fakeResponse

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertSame(fakeResponse, result)
        assertEquals(fakeToken, proceededRequestSlot.captured.header("X-Firebase-AppCheck"))
        assertEquals("fake-header-value", proceededRequestSlot.captured.header("Fake-Header"))
        assertEquals(fakeRequest.url, proceededRequestSlot.captured.url)
        assertEquals(fakeRequest.method, proceededRequestSlot.captured.method)
        verify(exactly = 1) { chain.proceed(any()) }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    @Test
    fun `if intercept is called should ask app check for the cached token without forcing a refresh`() {
        // Given
        val fakeTask = createFakeAppCheckTask(token = "fake-cached-app-check-token")
        every { appCheck.getAppCheckToken(any()) } returns fakeTask
        every { chain.request() } returns Request.Builder().url("https://fake.martp.example/map-arts").build()
        every { chain.proceed(any()) } returns mockk()

        // When
        interceptor.intercept(chain)

        // Then
        verify(exactly = 1) { appCheck.getAppCheckToken(false) }
        verify(exactly = 0) { appCheck.getAppCheckToken(true) }
    }

    @Test
    fun `if the request already has an app check header should replace it with the new token`() {
        // Given
        val fakeToken = "fake-new-app-check-token"
        val fakeRequest = Request.Builder()
            .url("https://fake.martp.example/geocoding/reverse")
            .header("X-Firebase-AppCheck", "fake-stale-app-check-token")
            .build()
        val proceededRequestSlot = slot<Request>()
        val fakeTask = createFakeAppCheckTask(token = fakeToken)
        every { appCheck.getAppCheckToken(any()) } returns fakeTask
        every { chain.request() } returns fakeRequest
        every { chain.proceed(capture(proceededRequestSlot)) } returns mockk()

        // When
        interceptor.intercept(chain)

        // Then
        assertEquals(listOf(fakeToken), proceededRequestSlot.captured.headers("X-Firebase-AppCheck"))
    }

    @Test
    fun `if waiting for the app check token fails should log it and throw IOException without proceeding`() {
        // Given
        val fakeTask = mockk<Task<AppCheckToken>>()
        val fakeException = ExecutionException("fake app check token error", IllegalStateException())
        every { appCheck.getAppCheckToken(any()) } returns fakeTask
        every { Tasks.await(fakeTask) } throws fakeException
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val thrown = assertThrows<IOException> {
            interceptor.intercept(chain)
        }

        // Then
        assertEquals("Could not get the App Check token", thrown.message)
        assertSame(fakeException, thrown.cause)
        verify(exactly = 1) { errorLogger.logNonFatal(fakeException, "AppCheckInterceptor.intercept") }
        verify(exactly = 0) { chain.proceed(any()) }
    }

    @Test
    fun `if app check throws an exception when asked for the token should log it and throw IOException without proceeding`() {
        // Given
        val fakeException = IllegalStateException("fake app check not initialized error")
        every { appCheck.getAppCheckToken(any()) } throws fakeException
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val thrown = assertThrows<IOException> {
            interceptor.intercept(chain)
        }

        // Then
        assertSame(fakeException, thrown.cause)
        verify(exactly = 1) { errorLogger.logNonFatal(fakeException, "AppCheckInterceptor.intercept") }
        verify(exactly = 0) { chain.request() }
        verify(exactly = 0) { chain.proceed(any()) }
    }
}
