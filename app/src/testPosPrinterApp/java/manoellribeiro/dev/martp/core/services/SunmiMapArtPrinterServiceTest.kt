package manoellribeiro.dev.martp.core.services

import android.graphics.Bitmap
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.printerhardware.models.failures.SmartPosPrinterOutOfPaperFailure
import manoellribeiro.dev.martp.printerhardware.services.SmartPosPrinterService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SunmiMapArtPrinterServiceTest {

    private lateinit var mapArtPrinterService: SunmiMapArtPrinterService
    private lateinit var smartPosPrinterService: SmartPosPrinterService

    @BeforeEach
    fun setup() {
        smartPosPrinterService = mockk()
        mapArtPrinterService = SunmiMapArtPrinterService(smartPosPrinterService)
    }

    @Test
    fun `isAvailable should be true`() {
        // Then
        assertTrue(mapArtPrinterService.isAvailable)
    }

    @Test
    fun `printableWidthPixels should return the paper width of the smart pos printer service`() {
        // Given
        every { smartPosPrinterService.paperWidthPixels } returns 384

        // When
        val printableWidthPixels = mapArtPrinterService.printableWidthPixels

        // Then
        assertEquals(384, printableWidthPixels)
        verify(exactly = 1) { smartPosPrinterService.paperWidthPixels }
    }

    @Test
    fun `isConnected should return the value of the smart pos printer service`() {
        // Given
        every { smartPosPrinterService.isConnected() } returns false

        // When
        val isConnected = mapArtPrinterService.isConnected()

        // Then
        assertFalse(isConnected)
        verify(exactly = 1) { smartPosPrinterService.isConnected() }
    }

    @Test
    fun `connect should return the result of the smart pos printer service`() = runTest {
        // Given
        coEvery { smartPosPrinterService.connect() } returns true

        // When
        val isConnected = mapArtPrinterService.connect()

        // Then
        assertTrue(isConnected)
        coVerify(exactly = 1) { smartPosPrinterService.connect() }
    }

    @Test
    fun `printBitmap should print the same bitmap with the smart pos printer service`() = runTest {
        // Given
        val fakeBitmap = mockk<Bitmap>()
        coEvery { smartPosPrinterService.printBitmap(fakeBitmap) } just runs

        // When
        mapArtPrinterService.printBitmap(fakeBitmap)

        // Then
        coVerify(exactly = 1) { smartPosPrinterService.printBitmap(fakeBitmap) }
    }

    @Test
    fun `if the smart pos printer service throws a failure printBitmap should rethrow it unchanged`() = runTest {
        // Given
        val fakeBitmap = mockk<Bitmap>()
        val fakeFailure = SmartPosPrinterOutOfPaperFailure(originalExceptionMessage = "fake out of paper")
        coEvery { smartPosPrinterService.printBitmap(fakeBitmap) } throws fakeFailure

        // When
        val failure = assertThrows<SmartPosPrinterOutOfPaperFailure> {
            mapArtPrinterService.printBitmap(fakeBitmap)
        }

        // Then
        assertSame(fakeFailure, failure)
    }
}
