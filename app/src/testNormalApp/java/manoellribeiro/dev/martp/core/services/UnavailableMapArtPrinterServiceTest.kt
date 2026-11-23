package manoellribeiro.dev.martp.core.services

import android.graphics.Bitmap
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class UnavailableMapArtPrinterServiceTest {

    private lateinit var mapArtPrinterService: UnavailableMapArtPrinterService

    @BeforeEach
    fun setup() {
        mapArtPrinterService = UnavailableMapArtPrinterService()
    }

    @Test
    fun `isAvailable, isConnected and printableWidthPixels should report no printer`() {
        // Then
        assertFalse(mapArtPrinterService.isAvailable)
        assertFalse(mapArtPrinterService.isConnected())
        assertEquals(0, mapArtPrinterService.printableWidthPixels)
    }

    @Test
    fun `connect should return false`() = runTest {
        // When
        val isConnected = mapArtPrinterService.connect()

        // Then
        assertFalse(isConnected)
    }

    @Test
    fun `printBitmap should complete without printing or throwing`() = runTest {
        // Given
        val fakeBitmap = mockk<Bitmap>()

        // Then
        assertDoesNotThrow {
            mapArtPrinterService.printBitmap(fakeBitmap)
        }
    }
}
