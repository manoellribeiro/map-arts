package manoellribeiro.dev.martp.core.services

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConnectivityServiceTest {

    private lateinit var connectivityService: ConnectivityService
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var fakeActiveNetwork: Network
    private lateinit var fakeNetworkCapabilities: NetworkCapabilities

    @BeforeEach
    fun setup() {
        connectivityManager = mockk()
        fakeActiveNetwork = mockk()
        fakeNetworkCapabilities = mockk()

        connectivityService = ConnectivityService(connectivityManager = connectivityManager)
    }

    @Test
    fun `if there is no active network should return false without asking for the network capabilities`() {
        // Given
        every { connectivityManager.activeNetwork } returns null

        // When
        val isInternetConnected = connectivityService.isInternetConnected()

        // Then
        assertFalse(isInternetConnected)
        verify(exactly = 1) { connectivityManager.activeNetwork }
        verify(exactly = 0) { connectivityManager.getNetworkCapabilities(any()) }
    }

    @Test
    fun `if the active network has no capabilities should return false`() {
        // Given
        every { connectivityManager.activeNetwork } returns fakeActiveNetwork
        every { connectivityManager.getNetworkCapabilities(fakeActiveNetwork) } returns null

        // When
        val isInternetConnected = connectivityService.isInternetConnected()

        // Then
        assertFalse(isInternetConnected)
        verify(exactly = 1) { connectivityManager.activeNetwork }
        verify(exactly = 1) { connectivityManager.getNetworkCapabilities(fakeActiveNetwork) }
        verify(exactly = 0) { fakeNetworkCapabilities.hasTransport(any()) }
    }

    @Test
    fun `if the active network has the wifi transport should return true`() {
        // Given
        every { connectivityManager.activeNetwork } returns fakeActiveNetwork
        every {
            connectivityManager.getNetworkCapabilities(fakeActiveNetwork)
        } returns fakeNetworkCapabilities
        every {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } returns true

        // When
        val isInternetConnected = connectivityService.isInternetConnected()

        // Then
        assertTrue(isInternetConnected)
        verify(exactly = 1) { connectivityManager.getNetworkCapabilities(fakeActiveNetwork) }
        verify(exactly = 1) {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    }

    @Test
    fun `if the active network has the cellular transport should return true`() {
        // Given
        every { connectivityManager.activeNetwork } returns fakeActiveNetwork
        every {
            connectivityManager.getNetworkCapabilities(fakeActiveNetwork)
        } returns fakeNetworkCapabilities
        every {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } returns false
        every {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } returns true

        // When
        val isInternetConnected = connectivityService.isInternetConnected()

        // Then
        assertTrue(isInternetConnected)
        verify(exactly = 1) { connectivityManager.getNetworkCapabilities(fakeActiveNetwork) }
        verify(exactly = 1) {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }
    }

    @Test
    fun `if the active network has the ethernet transport should return true`() {
        // Given
        every { connectivityManager.activeNetwork } returns fakeActiveNetwork
        every {
            connectivityManager.getNetworkCapabilities(fakeActiveNetwork)
        } returns fakeNetworkCapabilities
        every {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } returns false
        every {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } returns false
        every {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } returns true

        // When
        val isInternetConnected = connectivityService.isInternetConnected()

        // Then
        assertTrue(isInternetConnected)
        verify(exactly = 1) { connectivityManager.getNetworkCapabilities(fakeActiveNetwork) }
        verify(exactly = 1) {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
    }

    @Test
    fun `if the active network has none of the wifi, cellular and ethernet transports should return false`() {
        // Given
        every { connectivityManager.activeNetwork } returns fakeActiveNetwork
        every {
            connectivityManager.getNetworkCapabilities(fakeActiveNetwork)
        } returns fakeNetworkCapabilities
        every {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } returns false
        every {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } returns false
        every {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } returns false

        // When
        val isInternetConnected = connectivityService.isInternetConnected()

        // Then
        assertFalse(isInternetConnected)
        verify(exactly = 1) { connectivityManager.getNetworkCapabilities(fakeActiveNetwork) }
        verify(exactly = 1) {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
        verify(exactly = 1) {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }
        verify(exactly = 1) {
            fakeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
    }
}
