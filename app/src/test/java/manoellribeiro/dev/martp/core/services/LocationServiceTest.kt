package manoellribeiro.dev.martp.core.services

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.models.failures.LocationDisabledFailure
import manoellribeiro.dev.martp.core.models.failures.LocationPermissionNotGrantedFailure
import manoellribeiro.dev.martp.core.models.failures.UnknownErrorFailure
import manoellribeiro.dev.martp.infra.MainDispatcherExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class LocationServiceTest {

    private lateinit var locationService: LocationService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var fakeLocationTask: Task<Location>
    private lateinit var errorLogger: ErrorLogger

    @BeforeEach
    fun setup() {
        fusedLocationClient = mockk()
        fakeLocationTask = mockk()
        errorLogger = mockk()

        locationService = LocationService(
            fusedLocationClient = fusedLocationClient,
            errorLogger = errorLogger
        )
    }

    // Makes the task call its complete listener as soon as the listener is registered
    private fun completeFakeLocationTaskWhenListenerIsAdded() {
        val listenerSlot = slot<OnCompleteListener<Location>>()
        every { fakeLocationTask.addOnCompleteListener(capture(listenerSlot)) } answers {
            listenerSlot.captured.onComplete(fakeLocationTask)
            fakeLocationTask
        }
    }

    @Test
    fun `if location task succeeds with a location should return it`() = runTest {
        // Given
        val fakeLocation = mockk<Location>()
        val cancellationTokenSlot = slot<CancellationToken>()
        every { fakeLocationTask.isSuccessful } returns true
        every { fakeLocationTask.result } returns fakeLocation
        every {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                capture(cancellationTokenSlot)
            )
        } returns fakeLocationTask
        completeFakeLocationTaskWhenListenerIsAdded()

        // When
        val location = locationService.getCurrentLocation()

        // Then
        assertSame(fakeLocation, location)
        assertFalse(cancellationTokenSlot.captured.isCancellationRequested)
        verify(exactly = 1) {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                any<CancellationToken>()
            )
        }
    }

    @Test
    fun `if location task succeeds without a location should throw a LocationDisabledFailure`() = runTest {
        // Given
        every { fakeLocationTask.isSuccessful } returns true
        every { fakeLocationTask.result } returns null
        every { fakeLocationTask.exception } returns null
        every {
            fusedLocationClient.getCurrentLocation(any<Int>(), any<CancellationToken>())
        } returns fakeLocationTask
        completeFakeLocationTaskWhenListenerIsAdded()
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<LocationDisabledFailure> {
            locationService.getCurrentLocation()
        }

        // Then
        assertNull(failure.message)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalStateException>(), "LocationService.getCurrentLocation") }
    }

    @Test
    fun `if location task fails with a SecurityException should throw a LocationPermissionNotGrantedFailure with its message`() = runTest {
        // Given
        val fakeErrorMessage = "fake missing location permission in task"
        val fakeTaskException = SecurityException(fakeErrorMessage)
        every { fakeLocationTask.isSuccessful } returns false
        every { fakeLocationTask.exception } returns fakeTaskException
        every {
            fusedLocationClient.getCurrentLocation(any<Int>(), any<CancellationToken>())
        } returns fakeLocationTask
        completeFakeLocationTaskWhenListenerIsAdded()
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<LocationPermissionNotGrantedFailure> {
            locationService.getCurrentLocation()
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        assertEquals(
            R.string.location_permission_not_granted_failure_error,
            failure.messageToBeDisplayedToUserId
        )
        verify(exactly = 1) { errorLogger.logNonFatal(fakeTaskException, "LocationService.getCurrentLocation") }
    }

    @Test
    fun `if location task fails without an exception should throw a LocationPermissionNotGrantedFailure with null message`() = runTest {
        // Given
        every { fakeLocationTask.isSuccessful } returns false
        every { fakeLocationTask.exception } returns null
        every {
            fusedLocationClient.getCurrentLocation(any<Int>(), any<CancellationToken>())
        } returns fakeLocationTask
        completeFakeLocationTaskWhenListenerIsAdded()
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<LocationPermissionNotGrantedFailure> {
            locationService.getCurrentLocation()
        }

        // Then
        assertNull(failure.message)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalStateException>(), "LocationService.getCurrentLocation") }
    }

    @Test
    fun `if location client throws a SecurityException should throw a LocationPermissionNotGrantedFailure with its message`() = runTest {
        // Given
        val fakeErrorMessage = "fake missing location permission"
        val fakeSecurityException = SecurityException(fakeErrorMessage)
        every {
            fusedLocationClient.getCurrentLocation(any<Int>(), any<CancellationToken>())
        } throws fakeSecurityException
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<LocationPermissionNotGrantedFailure> {
            locationService.getCurrentLocation()
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        assertEquals(
            R.string.location_permission_not_granted_failure_error,
            failure.messageToBeDisplayedToUserId
        )
        verify(exactly = 1) { errorLogger.logNonFatal(fakeSecurityException, "LocationService.getCurrentLocation") }
    }

    @Test
    fun `if location client throws an unexpected exception should throw an UnknownErrorFailure with its message`() = runTest {
        // Given
        val fakeErrorMessage = "fake unexpected location client error"
        every {
            fusedLocationClient.getCurrentLocation(any<Int>(), any<CancellationToken>())
        } throws IllegalStateException(fakeErrorMessage)
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<UnknownErrorFailure> {
            locationService.getCurrentLocation()
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        assertEquals(R.string.unknown_error_failure, failure.messageToBeDisplayedToUserId)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalStateException>(), "LocationService.getCurrentLocation") }
    }

    @Test
    fun `if the coroutine is cancelled while the location task is pending should cancel the location request`() = runTest {
        // Given
        val cancellationTokenSlot = slot<CancellationToken>()
        every {
            fusedLocationClient.getCurrentLocation(any<Int>(), capture(cancellationTokenSlot))
        } returns fakeLocationTask
        // The listener is registered but never called, so the task stays pending
        every {
            fakeLocationTask.addOnCompleteListener(any<OnCompleteListener<Location>>())
        } returns fakeLocationTask
        val job = launch { locationService.getCurrentLocation() }
        advanceUntilIdle()

        // When
        job.cancel()
        advanceUntilIdle()

        // Then
        assertTrue(job.isCancelled)
        assertTrue(cancellationTokenSlot.captured.isCancellationRequested)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }
}
