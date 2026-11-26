package manoellribeiro.dev.martp.core.services

import android.location.Location
import android.location.LocationManager
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.models.failures.LocationDisabledFailure
import manoellribeiro.dev.martp.core.models.failures.LocationPermissionNotGrantedFailure
import manoellribeiro.dev.martp.core.models.failures.UnknownErrorFailure
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class PlatformLocationServiceTest {

    private lateinit var locationService: PlatformLocationService
    private lateinit var locationManager: LocationManager
    private lateinit var errorLogger: ErrorLogger
    private lateinit var listenerSlot: CapturingSlot<LocationListenerCompat>

    @BeforeEach
    fun setup() {
        mockkStatic(LocationManagerCompat::class)
        locationManager = mockk()
        errorLogger = mockk()
        listenerSlot = slot()
        every { LocationManagerCompat.removeUpdates(locationManager, any()) } just runs

        locationService = PlatformLocationService(
            locationManager = locationManager,
            errorLogger = errorLogger
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(LocationManagerCompat::class)
    }

    private fun givenLocationEnabledWithProviders(vararg providers: String) {
        every { LocationManagerCompat.isLocationEnabled(locationManager) } returns true
        every { locationManager.getProviders(true) } returns providers.toList()
    }

    // Accepts the location requests and keeps the listener, so the test decides when a location arrives
    private fun givenLocationRequestsAreAccepted() {
        every {
            LocationManagerCompat.requestLocationUpdates(
                locationManager, any<String>(), any<LocationRequestCompat>(), any(), capture(listenerSlot)
            )
        } just runs
    }

    // Answers the first location request with the given location as soon as it is made
    private fun givenLocationRequestAnswers(location: Location) {
        every {
            LocationManagerCompat.requestLocationUpdates(
                locationManager, any<String>(), any<LocationRequestCompat>(), any(), capture(listenerSlot)
            )
        } answers {
            listenerSlot.captured.onLocationChanged(location)
        }
    }

    @Test
    fun `if location is disabled should throw a LocationDisabledFailure without requesting or logging`() = runTest {
        // Given
        every { LocationManagerCompat.isLocationEnabled(locationManager) } returns false

        // When
        val failure = assertThrows<LocationDisabledFailure> {
            locationService.getCurrentLocation()
        }

        // Then
        assertEquals(R.string.location_permission_not_granted_failure_error, failure.messageToBeDisplayedToUserId)
        verify(exactly = 0) { locationManager.getProviders(any<Boolean>()) }
        verify(exactly = 0) {
            LocationManagerCompat.requestLocationUpdates(any(), any<String>(), any<LocationRequestCompat>(), any(), any<LocationListenerCompat>())
        }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    @Test
    fun `if no usable provider is enabled should log and throw a LocationDisabledFailure`() = runTest {
        // Given
        givenLocationEnabledWithProviders("passive")
        every {
            errorLogger.logNonFatal(ofType<LocationDisabledFailure>(), "PlatformLocationService.getCurrentLocation")
        } just runs

        // When
        val failure = assertThrows<LocationDisabledFailure> {
            locationService.getCurrentLocation()
        }

        // Then
        assertEquals("No usable location provider is enabled", failure.message)
        verify(exactly = 0) {
            LocationManagerCompat.requestLocationUpdates(any(), any<String>(), any<LocationRequestCompat>(), any(), any<LocationListenerCompat>())
        }
        verify(exactly = 1) { errorLogger.logNonFatal(failure, "PlatformLocationService.getCurrentLocation") }
    }

    @Test
    fun `if fused, network and gps are enabled should request updates from all of them and not from passive`() = runTest {
        // Given
        val fakeLocation = mockk<Location>()
        givenLocationEnabledWithProviders("passive", "gps", "network", "fused")
        givenLocationRequestAnswers(fakeLocation)

        // When
        locationService.getCurrentLocation()

        // Then
        listOf("fused", "network", "gps").forEach { provider ->
            verify(exactly = 1) {
                LocationManagerCompat.requestLocationUpdates(locationManager, provider, any<LocationRequestCompat>(), any(), any<LocationListenerCompat>())
            }
        }
        verify(exactly = 0) {
            LocationManagerCompat.requestLocationUpdates(locationManager, "passive", any<LocationRequestCompat>(), any(), any<LocationListenerCompat>())
        }
    }

    @Test
    fun `if a provider returns a location should return it and stop the location updates`() = runTest {
        // Given
        val fakeLocation = mockk<Location>()
        givenLocationEnabledWithProviders("network", "gps")
        givenLocationRequestAnswers(fakeLocation)

        // When
        val location = locationService.getCurrentLocation()

        // Then
        assertSame(fakeLocation, location)
        verify(exactly = 1) { LocationManagerCompat.removeUpdates(locationManager, listenerSlot.captured) }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    @Test
    fun `if two providers return locations should return the first one`() = runTest {
        // Given
        val fakeFirstLocation = mockk<Location>()
        val fakeSecondLocation = mockk<Location>()
        givenLocationEnabledWithProviders("network", "gps")
        givenLocationRequestsAreAccepted()

        // When
        val result = async { locationService.getCurrentLocation() }
        runCurrent()
        listenerSlot.captured.onLocationChanged(fakeFirstLocation)
        listenerSlot.captured.onLocationChanged(fakeSecondLocation)

        // Then
        assertSame(fakeFirstLocation, result.await())
    }

    @Test
    fun `if no provider returns a location before the timeout should log, throw a LocationDisabledFailure and stop the location updates`() = runTest {
        // Given
        givenLocationEnabledWithProviders("network", "gps")
        givenLocationRequestsAreAccepted()
        every {
            errorLogger.logNonFatal(ofType<IllegalStateException>(), "PlatformLocationService.getCurrentLocation")
        } just runs

        // When
        val failure = assertThrows<LocationDisabledFailure> {
            locationService.getCurrentLocation()
        }

        // Then
        assertEquals(R.string.location_permission_not_granted_failure_error, failure.messageToBeDisplayedToUserId)
        verify(exactly = 1) {
            errorLogger.logNonFatal(ofType<IllegalStateException>(), "PlatformLocationService.getCurrentLocation")
        }
        verify(exactly = 1) { LocationManagerCompat.removeUpdates(locationManager, listenerSlot.captured) }
    }

    @Test
    fun `if location request throws a SecurityException should log and throw a LocationPermissionNotGrantedFailure with its message`() = runTest {
        // Given
        val fakeErrorMessage = "fake permission error"
        val fakeSecurityException = SecurityException(fakeErrorMessage)
        givenLocationEnabledWithProviders("network")
        every {
            LocationManagerCompat.requestLocationUpdates(
                locationManager, "network", any<LocationRequestCompat>(), any(), any<LocationListenerCompat>()
            )
        } throws fakeSecurityException
        every {
            errorLogger.logNonFatal(fakeSecurityException, "PlatformLocationService.getCurrentLocation")
        } just runs

        // When
        val failure = assertThrows<LocationPermissionNotGrantedFailure> {
            locationService.getCurrentLocation()
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        verify(exactly = 1) { errorLogger.logNonFatal(fakeSecurityException, "PlatformLocationService.getCurrentLocation") }
    }

    @Test
    fun `if location request throws an unexpected exception should log and throw an UnknownErrorFailure with its message`() = runTest {
        // Given
        val fakeErrorMessage = "fake unexpected error"
        val fakeException = IllegalStateException(fakeErrorMessage)
        givenLocationEnabledWithProviders("network")
        every {
            LocationManagerCompat.requestLocationUpdates(
                locationManager, "network", any<LocationRequestCompat>(), any(), any<LocationListenerCompat>()
            )
        } throws fakeException
        every {
            errorLogger.logNonFatal(fakeException, "PlatformLocationService.getCurrentLocation")
        } just runs

        // When
        val failure = assertThrows<UnknownErrorFailure> {
            locationService.getCurrentLocation()
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        assertEquals(R.string.unknown_error_failure, failure.messageToBeDisplayedToUserId)
        verify(exactly = 1) { errorLogger.logNonFatal(fakeException, "PlatformLocationService.getCurrentLocation") }
    }

    @Test
    fun `if the coroutine is cancelled while waiting for a location should stop the location updates`() = runTest {
        // Given
        givenLocationEnabledWithProviders("gps")
        givenLocationRequestsAreAccepted()
        val job = launch { locationService.getCurrentLocation() }
        runCurrent()

        // When
        job.cancel()
        runCurrent()

        // Then
        assertTrue(job.isCancelled)
        verify(exactly = 1) { LocationManagerCompat.removeUpdates(locationManager, listenerSlot.captured) }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }
}
