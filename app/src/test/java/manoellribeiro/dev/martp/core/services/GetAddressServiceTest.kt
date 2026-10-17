package manoellribeiro.dev.martp.core.services

import android.location.Address
import android.location.Geocoder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.infra.MainDispatcherExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException

/**
 * `GetAddressService` catches every `Exception` and returns null, and that includes the exception
 * a strict mock throws for a call that was not configured. So a null result alone proves nothing:
 * every test also verifies which `getFromLocation` overload was called and which was not.
 */
// The blocking `getFromLocation(latitude, longitude, maxResults)` used below Tiramisu is deprecated
@Suppress("DEPRECATION")
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class GetAddressServiceTest {

    private lateinit var getAddressService: GetAddressService
    private lateinit var geocoder: Geocoder
    private lateinit var sdkVersionService: SdkVersionService
    private lateinit var connectivityService: ConnectivityService

    @BeforeEach
    fun setup() {
        geocoder = mockk()
        connectivityService = mockk()
        sdkVersionService = mockk()
        // Geocoder.isPresent() is static: its behavior is configured inside each test
        mockkStatic(Geocoder::class)

        getAddressService = GetAddressService(
            geocoder = geocoder,
            ioDispatcher = UnconfinedTestDispatcher(),
            connectivityService = connectivityService,
            sdkVersionService = sdkVersionService
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // region Geocoder presence

    @Test
    fun `if geocoder is not present should return null without asking for the address`() = runTest {
        // Given
        val fakeLatitude = -12.97
        val fakeLongitude = -38.5
        every { Geocoder.isPresent() } returns false

        // When
        val address = getAddressService.getAddress(fakeLatitude, fakeLongitude)

        // Then
        assertNull(address)
        verify(exactly = 1) { Geocoder.isPresent() }
        verify(exactly = 0) { sdkVersionService.isAtLeastTiramisu() }
        verify(exactly = 0) { geocoder.getFromLocation(any(), any(), any()) }
        verify(exactly = 0) {
            geocoder.getFromLocation(any(), any(), any(), any<Geocoder.GeocodeListener>())
        }
    }

    @Test
    fun `if checking the geocoder presence throws an exception should return null`() = runTest {
        // Given
        val fakeLatitude = -13.06
        val fakeLongitude = -38.56
        every { Geocoder.isPresent() } throws IllegalStateException("fake geocoder presence error")

        // When
        val address = getAddressService.getAddress(fakeLatitude, fakeLongitude)

        // Then
        assertNull(address)
        verify(exactly = 1) { Geocoder.isPresent() }
        verify(exactly = 0) { sdkVersionService.isAtLeastTiramisu() }
        verify(exactly = 0) { geocoder.getFromLocation(any(), any(), any()) }
        verify(exactly = 0) {
            geocoder.getFromLocation(any(), any(), any(), any<Geocoder.GeocodeListener>())
        }
    }

    // endregion

    // region Below Tiramisu: blocking getFromLocation inside the io dispatcher

    @Test
    fun `if sdk version service reports below Tiramisu and geocoder returns one address should return it`() = runTest {
        // Given
        val fakeLatitude = -13.01
        val fakeLongitude = -38.51
        val fakeAddress = mockk<Address>()
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns false
        every { geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1) } returns listOf(fakeAddress)

        // When
        val address = getAddressService.getAddress(fakeLatitude, fakeLongitude)

        // Then
        assertSame(fakeAddress, address)
        verify(exactly = 1) { sdkVersionService.isAtLeastTiramisu() }
        verify(exactly = 1) { geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1) }
        verify(exactly = 0) {
            geocoder.getFromLocation(any(), any(), any(), any<Geocoder.GeocodeListener>())
        }
    }

    @Test
    fun `if sdk version service reports below Tiramisu and geocoder returns many addresses should return the first one`() = runTest {
        // Given
        val fakeLatitude = -13.02
        val fakeLongitude = -38.52
        val fakeFirstAddress = mockk<Address>()
        val fakeSecondAddress = mockk<Address>()
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns false
        every {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1)
        } returns listOf(fakeFirstAddress, fakeSecondAddress)

        // When
        val address = getAddressService.getAddress(fakeLatitude, fakeLongitude)

        // Then
        assertSame(fakeFirstAddress, address)
        verify(exactly = 1) { geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1) }
        verify(exactly = 0) {
            geocoder.getFromLocation(any(), any(), any(), any<Geocoder.GeocodeListener>())
        }
    }

    @Test
    fun `if sdk version service reports below Tiramisu and geocoder returns an empty list should return null`() = runTest {
        // Given
        val fakeLatitude = -13.03
        val fakeLongitude = -38.53
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns false
        every { geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1) } returns emptyList()

        // When
        val address = getAddressService.getAddress(fakeLatitude, fakeLongitude)

        // Then
        assertNull(address)
        verify(exactly = 1) { geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1) }
        verify(exactly = 0) {
            geocoder.getFromLocation(any(), any(), any(), any<Geocoder.GeocodeListener>())
        }
    }

    @Test
    fun `if sdk version service reports below Tiramisu and geocoder returns null should return null`() = runTest {
        // Given
        val fakeLatitude = -13.04
        val fakeLongitude = -38.54
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns false
        every { geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1) } returns null

        // When
        val address = getAddressService.getAddress(fakeLatitude, fakeLongitude)

        // Then
        assertNull(address)
        verify(exactly = 1) { geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1) }
        verify(exactly = 0) {
            geocoder.getFromLocation(any(), any(), any(), any<Geocoder.GeocodeListener>())
        }
    }

    @Test
    fun `if sdk version service reports below Tiramisu and geocoder throws an IOException should return null`() = runTest {
        // Given
        val fakeLatitude = -13.05
        val fakeLongitude = -38.55
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns false
        every {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1)
        } throws IOException("fake geocoder network error")

        // When
        val address = getAddressService.getAddress(fakeLatitude, fakeLongitude)

        // Then
        assertNull(address)
        verify(exactly = 1) { geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1) }
        verify(exactly = 0) {
            geocoder.getFromLocation(any(), any(), any(), any<Geocoder.GeocodeListener>())
        }
    }

    @Test
    fun `if sdk version service reports below Tiramisu and geocoder throws a CancellationException should rethrow the same exception`() = runTest {
        // Given
        val fakeLatitude = -13.07
        val fakeLongitude = -38.57
        val fakeCancellationException = CancellationException("fake cancellation")
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns false
        every {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1)
        } throws fakeCancellationException

        // When
        val thrownException = assertThrows<CancellationException> {
            getAddressService.getAddress(fakeLatitude, fakeLongitude)
        }

        // Then
        assertSame(fakeCancellationException, thrownException)
        verify(exactly = 1) { geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1) }
        verify(exactly = 0) {
            geocoder.getFromLocation(any(), any(), any(), any<Geocoder.GeocodeListener>())
        }
    }

    // endregion

    // region Tiramisu or above: getFromLocation with a GeocodeListener

    @Test
    fun `if sdk version service reports Tiramisu or above and the listener receives one address should return it`() = runTest {
        // Given
        val fakeLatitude = -14.01
        val fakeLongitude = -39.51
        val fakeAddress = mockk<Address>()
        val fakeListenerSlot = slot<Geocoder.GeocodeListener>()
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns true
        every {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, capture(fakeListenerSlot))
        } answers {
            fakeListenerSlot.captured.onGeocode(listOf(fakeAddress))
        }

        // When
        val address = getAddressService.getAddress(fakeLatitude, fakeLongitude)

        // Then
        assertSame(fakeAddress, address)
        verify(exactly = 1) { sdkVersionService.isAtLeastTiramisu() }
        verify(exactly = 1) {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, any<Geocoder.GeocodeListener>())
        }
        verify(exactly = 0) { geocoder.getFromLocation(any(), any(), any()) }
    }

    @Test
    fun `if sdk version service reports Tiramisu or above and the listener receives many addresses should return the first one`() = runTest {
        // Given
        val fakeLatitude = -14.02
        val fakeLongitude = -39.52
        val fakeFirstAddress = mockk<Address>()
        val fakeSecondAddress = mockk<Address>()
        val fakeListenerSlot = slot<Geocoder.GeocodeListener>()
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns true
        every {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, capture(fakeListenerSlot))
        } answers {
            fakeListenerSlot.captured.onGeocode(listOf(fakeFirstAddress, fakeSecondAddress))
        }

        // When
        val address = getAddressService.getAddress(fakeLatitude, fakeLongitude)

        // Then
        assertSame(fakeFirstAddress, address)
        verify(exactly = 1) {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, any<Geocoder.GeocodeListener>())
        }
        verify(exactly = 0) { geocoder.getFromLocation(any(), any(), any()) }
    }

    @Test
    fun `if sdk version service reports Tiramisu or above and the listener receives an empty list should return null`() = runTest {
        // Given
        val fakeLatitude = -14.03
        val fakeLongitude = -39.53
        val fakeListenerSlot = slot<Geocoder.GeocodeListener>()
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns true
        every {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, capture(fakeListenerSlot))
        } answers {
            fakeListenerSlot.captured.onGeocode(emptyList())
        }

        // When
        val address = getAddressService.getAddress(fakeLatitude, fakeLongitude)

        // Then
        assertNull(address)
        assertTrue(fakeListenerSlot.isCaptured)
        verify(exactly = 1) {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, any<Geocoder.GeocodeListener>())
        }
        verify(exactly = 0) { geocoder.getFromLocation(any(), any(), any()) }
    }

    @Test
    fun `if sdk version service reports Tiramisu or above and the listener receives an error should return null`() = runTest {
        // Given
        val fakeLatitude = -14.04
        val fakeLongitude = -39.54
        val fakeListenerSlot = slot<Geocoder.GeocodeListener>()
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns true
        every {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, capture(fakeListenerSlot))
        } answers {
            fakeListenerSlot.captured.onError("fake geocoder error message")
        }

        // When
        val address = getAddressService.getAddress(fakeLatitude, fakeLongitude)

        // Then
        assertNull(address)
        assertTrue(fakeListenerSlot.isCaptured)
        verify(exactly = 1) {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, any<Geocoder.GeocodeListener>())
        }
        verify(exactly = 0) { geocoder.getFromLocation(any(), any(), any()) }
    }

    @Test
    fun `if sdk version service reports Tiramisu or above and the listener receives the address after the call suspended should return it`() = runTest {
        // Given
        val fakeLatitude = -14.05
        val fakeLongitude = -39.55
        val fakeAddress = mockk<Address>()
        val fakeListenerSlot = slot<Geocoder.GeocodeListener>()
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns true
        every {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, capture(fakeListenerSlot))
        } just runs

        // When
        val deferredAddress = async { getAddressService.getAddress(fakeLatitude, fakeLongitude) }
        advanceUntilIdle()
        val wasCompletedBeforeTheListenerAnswered = deferredAddress.isCompleted
        fakeListenerSlot.captured.onGeocode(listOf(fakeAddress))
        val address = deferredAddress.await()

        // Then
        assertFalse(wasCompletedBeforeTheListenerAnswered)
        assertSame(fakeAddress, address)
        verify(exactly = 1) {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, any<Geocoder.GeocodeListener>())
        }
        verify(exactly = 0) { geocoder.getFromLocation(any(), any(), any()) }
    }

    @Test
    fun `if sdk version service reports Tiramisu or above and geocoder throws an exception should return null`() = runTest {
        // Given
        val fakeLatitude = -14.06
        val fakeLongitude = -39.56
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns true
        every {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, any<Geocoder.GeocodeListener>())
        } throws IllegalArgumentException("fake invalid coordinates error")

        // When
        val address = getAddressService.getAddress(fakeLatitude, fakeLongitude)

        // Then
        assertNull(address)
        verify(exactly = 1) {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, any<Geocoder.GeocodeListener>())
        }
        verify(exactly = 0) { geocoder.getFromLocation(any(), any(), any()) }
    }

    @Test
    fun `if sdk version service reports Tiramisu or above and geocoder throws a CancellationException should rethrow the same exception`() = runTest {
        // Given
        val fakeLatitude = -14.07
        val fakeLongitude = -39.57
        val fakeCancellationException = CancellationException("fake cancellation")
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns true
        every {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, any<Geocoder.GeocodeListener>())
        } throws fakeCancellationException

        // When
        val thrownException = assertThrows<CancellationException> {
            getAddressService.getAddress(fakeLatitude, fakeLongitude)
        }

        // Then
        assertSame(fakeCancellationException, thrownException)
        verify(exactly = 1) {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, any<Geocoder.GeocodeListener>())
        }
        verify(exactly = 0) { geocoder.getFromLocation(any(), any(), any()) }
    }

    @Test
    fun `if sdk version service reports Tiramisu or above and the caller is cancelled before the listener answers should not return a value`() = runTest {
        // Given
        val fakeLatitude = -14.08
        val fakeLongitude = -39.58
        val fakeListenerSlot = slot<Geocoder.GeocodeListener>()
        var hasGetAddressReturned = false
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns true
        every {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, capture(fakeListenerSlot))
        } just runs
        val job = launch {
            getAddressService.getAddress(fakeLatitude, fakeLongitude)
            // Only reached if the cancellation is swallowed and turned into a null result
            hasGetAddressReturned = true
        }
        advanceUntilIdle()

        // When
        job.cancel()
        advanceUntilIdle()

        // Then
        assertTrue(fakeListenerSlot.isCaptured)
        assertTrue(job.isCancelled)
        assertFalse(hasGetAddressReturned)
        verify(exactly = 1) {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, any<Geocoder.GeocodeListener>())
        }
        verify(exactly = 0) { geocoder.getFromLocation(any(), any(), any()) }
    }

    @Test
    fun `if sdk version service reports Tiramisu or above and the listener answers after the caller was cancelled should not throw`() = runTest {
        // Given
        val fakeLatitude = -14.09
        val fakeLongitude = -39.59
        val fakeAddress = mockk<Address>()
        val fakeListenerSlot = slot<Geocoder.GeocodeListener>()
        var hasGetAddressReturned = false
        every { Geocoder.isPresent() } returns true
        every { sdkVersionService.isAtLeastTiramisu() } returns true
        every {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, capture(fakeListenerSlot))
        } just runs
        val job = launch {
            getAddressService.getAddress(fakeLatitude, fakeLongitude)
            hasGetAddressReturned = true
        }
        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()

        // When / Then
        assertDoesNotThrow { fakeListenerSlot.captured.onGeocode(listOf(fakeAddress)) }
        advanceUntilIdle()
        assertFalse(hasGetAddressReturned)
        verify(exactly = 1) {
            geocoder.getFromLocation(fakeLatitude, fakeLongitude, 1, any<Geocoder.GeocodeListener>())
        }
        verify(exactly = 0) { geocoder.getFromLocation(any(), any(), any()) }
    }

    // endregion
}
