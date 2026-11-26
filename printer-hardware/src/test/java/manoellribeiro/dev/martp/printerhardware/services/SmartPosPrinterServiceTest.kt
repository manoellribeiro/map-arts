package manoellribeiro.dev.martp.printerhardware.services

import android.content.Context
import android.graphics.Bitmap
import android.os.RemoteException
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.InnerResultCallback
import com.sunmi.peripheral.printer.SunmiPrinterService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.printerhardware.models.failures.SmartPosPrinterNotConnectedFailure
import manoellribeiro.dev.martp.printerhardware.models.failures.SmartPosPrinterOutOfPaperFailure
import manoellribeiro.dev.martp.printerhardware.models.failures.SmartPosPrintingFailure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class SmartPosPrinterServiceTest {

    private lateinit var context: Context
    private lateinit var errorLogger: ErrorLogger
    private lateinit var printerManager: InnerPrinterManager
    private lateinit var fakeSunmiPrinterService: SunmiPrinterService
    private lateinit var fakeMapBitmap: Bitmap

    @BeforeEach
    fun setup() {
        context = mockk()
        errorLogger = mockk()
        printerManager = mockk()
        fakeSunmiPrinterService = mockk()
        fakeMapBitmap = mockk()
    }

    private fun TestScope.createService(): SmartPosPrinterService {
        return SmartPosPrinterService(
            context = context,
            errorLogger = errorLogger,
            printerManager = printerManager,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )
    }

    private fun connectServiceWithPrinter(service: SmartPosPrinterService) {
        every { printerManager.hasPrinter(fakeSunmiPrinterService) } returns true
        service.onPrinterConnected(fakeSunmiPrinterService)
    }

    // Records the print commands and answers the commit with the given callback call
    private fun givenPrinterReadyAndCommitAnswers(answer: (InnerResultCallback) -> Unit) {
        val callbackSlot = slot<InnerResultCallback>()
        every { fakeSunmiPrinterService.updatePrinterState() } returns 1
        every { fakeSunmiPrinterService.enterPrinterBuffer(true) } just runs
        every { fakeSunmiPrinterService.printBitmap(fakeMapBitmap, null) } just runs
        every { fakeSunmiPrinterService.lineWrap(3, null) } just runs
        every { fakeSunmiPrinterService.exitPrinterBufferWithCallback(true, capture(callbackSlot)) } answers {
            answer(callbackSlot.captured)
        }
    }

    @Test
    fun `if printer is already connected should return true without binding`() = runTest {
        // Given
        val service = createService()
        connectServiceWithPrinter(service)

        // When
        val isConnected = service.connect()

        // Then
        assertTrue(isConnected)
        verify(exactly = 0) { printerManager.bindService(any(), any()) }
    }

    @Test
    fun `if bindService returns false should return false`() = runTest {
        // Given
        val service = createService()
        every { printerManager.bindService(context, any()) } returns false

        // When
        val isConnected = service.connect()

        // Then
        assertFalse(isConnected)
        verify(exactly = 1) { printerManager.bindService(context, any()) }
    }

    @Test
    fun `if bindService throws InnerPrinterException should log it and return false`() = runTest {
        // Given
        val service = createService()
        val fakeException = InnerPrinterException("fake bind error")
        every { printerManager.bindService(context, any()) } throws fakeException
        every { errorLogger.logNonFatal(fakeException, "SmartPosPrinterService.connect") } just runs

        // When
        val isConnected = service.connect()

        // Then
        assertFalse(isConnected)
        verify(exactly = 1) { errorLogger.logNonFatal(fakeException, "SmartPosPrinterService.connect") }
    }

    @Test
    fun `if connected service has a printer should return true`() = runTest {
        // Given
        val service = createService()
        every { printerManager.bindService(context, any()) } returns true
        every { printerManager.hasPrinter(fakeSunmiPrinterService) } returns true

        // When
        val connection = async { service.connect() }
        runCurrent()
        service.onPrinterConnected(fakeSunmiPrinterService)

        // Then
        assertTrue(connection.await())
        assertTrue(service.isConnected())
    }

    @Test
    fun `if connected service has no printer should return false`() = runTest {
        // Given
        val service = createService()
        every { printerManager.bindService(context, any()) } returns true
        every { printerManager.hasPrinter(fakeSunmiPrinterService) } returns false

        // When
        val connection = async { service.connect() }
        runCurrent()
        service.onPrinterConnected(fakeSunmiPrinterService)

        // Then
        assertFalse(connection.await())
        assertFalse(service.isConnected())
    }

    @Test
    fun `if connected service is null should return false`() = runTest {
        // Given
        val service = createService()
        every { printerManager.bindService(context, any()) } returns true

        // When
        val connection = async { service.connect() }
        runCurrent()
        service.onPrinterConnected(null)

        // Then
        assertFalse(connection.await())
        assertFalse(service.isConnected())
        verify(exactly = 0) { printerManager.hasPrinter(any()) }
    }

    @Test
    fun `if checking for a printer throws InnerPrinterException should log it and return false`() = runTest {
        // Given
        val service = createService()
        val fakeException = InnerPrinterException("fake hasPrinter error")
        every { printerManager.bindService(context, any()) } returns true
        every { printerManager.hasPrinter(fakeSunmiPrinterService) } throws fakeException
        every { errorLogger.logNonFatal(fakeException, "SmartPosPrinterService.onPrinterConnected") } just runs

        // When
        val connection = async { service.connect() }
        runCurrent()
        service.onPrinterConnected(fakeSunmiPrinterService)

        // Then
        assertFalse(connection.await())
        verify(exactly = 1) { errorLogger.logNonFatal(fakeException, "SmartPosPrinterService.onPrinterConnected") }
    }

    @Test
    fun `if connect is called while another connection is pending should bind only once and both receive the result`() = runTest {
        // Given
        val service = createService()
        every { printerManager.bindService(context, any()) } returns true
        every { printerManager.hasPrinter(fakeSunmiPrinterService) } returns true

        // When
        val firstConnection = async { service.connect() }
        val secondConnection = async { service.connect() }
        runCurrent()
        service.onPrinterConnected(fakeSunmiPrinterService)

        // Then
        assertTrue(firstConnection.await())
        assertTrue(secondConnection.await())
        verify(exactly = 1) { printerManager.bindService(context, any()) }
    }

    @Test
    fun `if the service never connects should return false after the timeout`() = runTest {
        // Given
        val service = createService()
        every { printerManager.bindService(context, any()) } returns true

        // When
        val isConnected = service.connect()

        // Then
        assertFalse(isConnected)
        assertFalse(service.isConnected())
    }

    @Test
    fun `if printer disconnects should report not connected`() = runTest {
        // Given
        val service = createService()
        connectServiceWithPrinter(service)

        // When
        service.onPrinterDisconnected()

        // Then
        assertFalse(service.isConnected())
    }

    @Test
    fun `if printer is not connected should throw SmartPosPrinterNotConnectedFailure and not call the printer`() = runTest {
        // Given
        val service = createService()

        // When
        // Then
        assertThrows<SmartPosPrinterNotConnectedFailure> {
            service.printBitmap(fakeMapBitmap)
        }
        verify(exactly = 0) { fakeSunmiPrinterService.updatePrinterState() }
        verify(exactly = 0) { fakeSunmiPrinterService.printBitmap(any(), any()) }
    }

    @Test
    fun `if printer is ready and print result is success should enter buffer, print bitmap, wrap lines and commit in order`() = runTest {
        // Given
        val service = createService()
        connectServiceWithPrinter(service)
        givenPrinterReadyAndCommitAnswers { callback -> callback.onPrintResult(0, null) }

        // When
        service.printBitmap(fakeMapBitmap)

        // Then
        verifyOrder {
            fakeSunmiPrinterService.updatePrinterState()
            fakeSunmiPrinterService.enterPrinterBuffer(true)
            fakeSunmiPrinterService.printBitmap(fakeMapBitmap, null)
            fakeSunmiPrinterService.lineWrap(3, null)
            fakeSunmiPrinterService.exitPrinterBufferWithCallback(true, any())
        }
        verify(exactly = 1) { fakeSunmiPrinterService.printBitmap(fakeMapBitmap, null) }
    }

    @Test
    fun `if printer is out of paper should throw SmartPosPrinterOutOfPaperFailure, not print and not log`() = runTest {
        // Given
        val service = createService()
        connectServiceWithPrinter(service)
        every { fakeSunmiPrinterService.updatePrinterState() } returns 4

        // When
        // Then
        assertThrows<SmartPosPrinterOutOfPaperFailure> {
            service.printBitmap(fakeMapBitmap)
        }
        verify(exactly = 0) { fakeSunmiPrinterService.printBitmap(any(), any()) }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    @Test
    fun `if printer state is not normal should throw SmartPosPrintingFailure and log it`() = runTest {
        // Given
        val service = createService()
        connectServiceWithPrinter(service)
        every { fakeSunmiPrinterService.updatePrinterState() } returns 6
        every { errorLogger.logNonFatal(any<SmartPosPrintingFailure>(), "SmartPosPrinterService.printBitmap") } just runs

        // When
        // Then
        assertThrows<SmartPosPrintingFailure> {
            service.printBitmap(fakeMapBitmap)
        }
        verify(exactly = 0) { fakeSunmiPrinterService.printBitmap(any(), any()) }
        verify(exactly = 1) { errorLogger.logNonFatal(any<SmartPosPrintingFailure>(), "SmartPosPrinterService.printBitmap") }
    }

    @Test
    fun `if print result is not success should throw SmartPosPrintingFailure and log it`() = runTest {
        // Given
        val service = createService()
        connectServiceWithPrinter(service)
        givenPrinterReadyAndCommitAnswers { callback -> callback.onPrintResult(1, "fake print error") }
        every { errorLogger.logNonFatal(any<SmartPosPrintingFailure>(), "SmartPosPrinterService.printBitmap") } just runs

        // When
        // Then
        assertThrows<SmartPosPrintingFailure> {
            service.printBitmap(fakeMapBitmap)
        }
        verify(exactly = 1) { errorLogger.logNonFatal(any<SmartPosPrintingFailure>(), "SmartPosPrinterService.printBitmap") }
    }

    @Test
    fun `if printer raises an exception should throw SmartPosPrintingFailure and log it`() = runTest {
        // Given
        val service = createService()
        connectServiceWithPrinter(service)
        givenPrinterReadyAndCommitAnswers { callback -> callback.onRaiseException(-1, "fake raised error") }
        every { errorLogger.logNonFatal(any<SmartPosPrintingFailure>(), "SmartPosPrinterService.printBitmap") } just runs

        // When
        // Then
        assertThrows<SmartPosPrintingFailure> {
            service.printBitmap(fakeMapBitmap)
        }
        verify(exactly = 1) { errorLogger.logNonFatal(any<SmartPosPrintingFailure>(), "SmartPosPrinterService.printBitmap") }
    }

    @Test
    fun `if printer call throws RemoteException should log it and throw SmartPosPrintingFailure`() = runTest {
        // Given
        val service = createService()
        connectServiceWithPrinter(service)
        val fakeException = RemoteException()
        every { fakeSunmiPrinterService.updatePrinterState() } throws fakeException
        // Coroutines may recover the stack trace by copying the exception when it leaves withContext, so match by type
        every { errorLogger.logNonFatal(any<RemoteException>(), "SmartPosPrinterService.printBitmap") } just runs

        // When
        // Then
        assertThrows<SmartPosPrintingFailure> {
            service.printBitmap(fakeMapBitmap)
        }
        verify(exactly = 1) { errorLogger.logNonFatal(any<RemoteException>(), "SmartPosPrinterService.printBitmap") }
    }

    @Test
    fun `if printer never answers should throw SmartPosPrintingFailure after the timeout and log it`() = runTest {
        // Given
        val service = createService()
        connectServiceWithPrinter(service)
        givenPrinterReadyAndCommitAnswers { }
        every { errorLogger.logNonFatal(any<SmartPosPrintingFailure>(), "SmartPosPrinterService.printBitmap") } just runs

        // When
        // Then
        assertThrows<SmartPosPrintingFailure> {
            service.printBitmap(fakeMapBitmap)
        }
        verify(exactly = 1) { errorLogger.logNonFatal(any<SmartPosPrintingFailure>(), "SmartPosPrinterService.printBitmap") }
    }

    @Test
    fun `paperWidthPixels should be the 384 dots of the 58 mm paper`() = runTest {
        // Given
        val service = createService()

        // Then
        assertEquals(384, service.paperWidthPixels)
    }
}
