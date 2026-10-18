package manoellribeiro.dev.martp.core.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CrashlyticsErrorLoggerTest {

    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var errorLogger: CrashlyticsErrorLogger

    @BeforeEach
    fun setup() {
        crashlytics = mockk()
        mockkStatic(Log::class)

        errorLogger = CrashlyticsErrorLogger(crashlytics = crashlytics)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `if logNonFatal is called should log a message containing the operation name`() {
        // Given
        val fakeOperation = "fakeLoadUserMapArts"
        val fakeThrowable = IllegalStateException("fake load error")
        every { crashlytics.log(any()) } just runs
        every { crashlytics.recordException(any()) } just runs
        every { Log.e(any(), any(), any()) } returns 0

        // When
        errorLogger.logNonFatal(fakeThrowable, fakeOperation)

        // Then
        verify(exactly = 1) { crashlytics.log("Non-fatal in fakeLoadUserMapArts") }
    }

    @Test
    fun `if logNonFatal is called should record the exact throwable instance`() {
        // Given
        val fakeOperation = "fakeSaveMapArt"
        val fakeThrowable = RuntimeException("fake save error")
        val fakeThrowableSlot = slot<Throwable>()
        every { crashlytics.log(any()) } just runs
        every { crashlytics.recordException(capture(fakeThrowableSlot)) } just runs
        every { Log.e(any(), any(), any()) } returns 0

        // When
        errorLogger.logNonFatal(fakeThrowable, fakeOperation)

        // Then
        verify(exactly = 1) { crashlytics.recordException(any()) }
        assertSame(fakeThrowable, fakeThrowableSlot.captured)
    }

    @Test
    fun `if logNonFatal is called in a debug build should call Log e with the tag the operation and the throwable`() {
        // Given
        val fakeOperation = "fakeFetchMapboxImage"
        val fakeThrowable = IllegalArgumentException("fake mapbox error")
        every { crashlytics.log(any()) } just runs
        every { crashlytics.recordException(any()) } just runs
        every { Log.e(any(), any(), any()) } returns 0

        // When
        errorLogger.logNonFatal(fakeThrowable, fakeOperation)

        // Then
        verify(exactly = 1) { Log.e("ErrorLogger", fakeOperation, fakeThrowable) }
    }

    @Test
    fun `if logNonFatal is called should log the message before recording the exception and before calling Log e`() {
        // Given
        val fakeOperation = "fakeObserveLocation"
        val fakeThrowable = IllegalStateException("fake location error")
        every { crashlytics.log(any()) } just runs
        every { crashlytics.recordException(any()) } just runs
        every { Log.e(any(), any(), any()) } returns 0

        // When
        errorLogger.logNonFatal(fakeThrowable, fakeOperation)

        // Then
        verifyOrder {
            crashlytics.log("Non-fatal in fakeObserveLocation")
            crashlytics.recordException(fakeThrowable)
            Log.e("ErrorLogger", fakeOperation, fakeThrowable)
        }
    }

    @Test
    fun `if the operation name is empty should log the message prefix only`() {
        // Given
        val fakeOperation = ""
        val fakeThrowable = RuntimeException("fake empty operation error")
        val fakeMessageSlot = slot<String>()
        every { crashlytics.log(capture(fakeMessageSlot)) } just runs
        every { crashlytics.recordException(any()) } just runs
        every { Log.e(any(), any(), any()) } returns 0

        // When
        errorLogger.logNonFatal(fakeThrowable, fakeOperation)

        // Then
        verify(exactly = 1) { crashlytics.log(any()) }
        assertEquals("Non-fatal in ", fakeMessageSlot.captured)
    }

    @Test
    fun `if the throwable has no message should still record the exact throwable instance`() {
        // Given
        val fakeOperation = "fakeDeleteMapArt"
        val fakeThrowable = IllegalStateException()
        every { crashlytics.log(any()) } just runs
        every { crashlytics.recordException(any()) } just runs
        every { Log.e(any(), any(), any()) } returns 0

        // When
        errorLogger.logNonFatal(fakeThrowable, fakeOperation)

        // Then
        verify(exactly = 1) { crashlytics.log("Non-fatal in fakeDeleteMapArt") }
        verify(exactly = 1) { crashlytics.recordException(fakeThrowable) }
        verify(exactly = 1) { Log.e("ErrorLogger", fakeOperation, fakeThrowable) }
    }
}
