package manoellribeiro.dev.martp.printerhardware.services

import android.content.Context
import android.graphics.Bitmap
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.InnerResultCallback
import com.sunmi.peripheral.printer.SunmiPrinterService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.printerhardware.models.failures.SmartPosPrinterNotConnectedFailure
import manoellribeiro.dev.martp.printerhardware.models.failures.SmartPosPrinterOutOfPaperFailure
import manoellribeiro.dev.martp.printerhardware.models.failures.SmartPosPrintingFailure
import kotlin.time.Duration.Companion.milliseconds

class SmartPosPrinterService(
    private val context: Context,
    private val errorLogger: ErrorLogger,
    private val printerManager: InnerPrinterManager = InnerPrinterManager.getInstance(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    @Volatile
    private var printerService: SunmiPrinterService? = null

    private var pendingConnection: CompletableDeferred<Boolean>? = null

    private val connectionCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService?) = onPrinterConnected(service)
        override fun onDisconnected() = onPrinterDisconnected()
    }

    fun isConnected(): Boolean = printerService != null

    val paperWidthPixels = PAPER_WIDTH_PIXELS

    suspend fun connect(): Boolean {
        if (isConnected()) return true
        val connection = pendingOrNewConnection()
        return withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS.milliseconds) { connection.await() } ?: false
    }

    @Synchronized
    private fun pendingOrNewConnection(): CompletableDeferred<Boolean> {
        pendingConnection?.let { return it }
        val connection = CompletableDeferred<Boolean>()
        pendingConnection = connection
        try {
            if (!printerManager.bindService(context, connectionCallback)) {
                finishConnection(false)
            }
        } catch (e: InnerPrinterException) {
            errorLogger.logNonFatal(e, "SmartPosPrinterService.connect")
            finishConnection(false)
        }
        return connection
    }

    @Synchronized
    internal fun onPrinterConnected(service: SunmiPrinterService?) {
        val hasPrinter = try {
            service != null && printerManager.hasPrinter(service)
        } catch (e: InnerPrinterException) {
            errorLogger.logNonFatal(e, "SmartPosPrinterService.onPrinterConnected")
            false
        }
        printerService = if (hasPrinter) service else null
        finishConnection(hasPrinter)
    }

    @Synchronized
    internal fun onPrinterDisconnected() {
        printerService = null
        finishConnection(false)
    }

    private fun finishConnection(isConnected: Boolean) {
        pendingConnection?.complete(isConnected)
        pendingConnection = null
    }

    suspend fun printBitmap(mapBitmap: Bitmap) {
        val service = printerService ?: throw SmartPosPrinterNotConnectedFailure(
            originalExceptionMessage = "printBitmap called before the printer was connected"
        )
        try {
            withContext(ioDispatcher) {
                checkPrinterIsReady(service)
                val printResult = CompletableDeferred<Unit>()
                service.enterPrinterBuffer(true)
                service.printBitmap(mapBitmap, null)
                service.lineWrap(LINES_TO_FEED_AFTER_PRINT, null)
                service.exitPrinterBufferWithCallback(true, printResultCallback(printResult))
                withTimeoutOrNull(PRINT_TIMEOUT_MILLIS.milliseconds) { printResult.await() }
                    ?: throw SmartPosPrintingFailure(
                        originalExceptionMessage = "The printer did not answer in $PRINT_TIMEOUT_MILLIS ms"
                    )
            }
        } catch (failure: SmartPosPrintingFailure) {
            errorLogger.logNonFatal(failure, "SmartPosPrinterService.printBitmap")
            throw failure
        } catch (failure: Failure) {
            throw failure
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "SmartPosPrinterService.printBitmap")
            throw SmartPosPrintingFailure(originalExceptionMessage = e.message)
        }
    }

    private fun checkPrinterIsReady(service: SunmiPrinterService) {
        when (val printerState = service.updatePrinterState()) {
            PRINTER_STATE_NORMAL -> Unit
            PRINTER_STATE_OUT_OF_PAPER -> throw SmartPosPrinterOutOfPaperFailure(
                originalExceptionMessage = "Printer state $printerState"
            )
            else -> throw SmartPosPrintingFailure(
                originalExceptionMessage = "Printer state $printerState"
            )
        }
    }

    private fun printResultCallback(printResult: CompletableDeferred<Unit>) = object : InnerResultCallback() {
        override fun onRunResult(isSuccess: Boolean) = Unit

        override fun onReturnString(result: String?) = Unit

        override fun onRaiseException(code: Int, msg: String?) {
            printResult.completeExceptionally(
                SmartPosPrintingFailure(originalExceptionMessage = "onRaiseException code $code: $msg")
            )
        }

        override fun onPrintResult(code: Int, msg: String?) {
            if (code == PRINT_RESULT_SUCCESS) {
                printResult.complete(Unit)
            } else {
                printResult.completeExceptionally(
                    SmartPosPrintingFailure(originalExceptionMessage = "onPrintResult code $code: $msg")
                )
            }
        }
    }

    companion object {
        // 58 mm Sunmi paper
        private const val PAPER_WIDTH_PIXELS = 384
        private const val CONNECT_TIMEOUT_MILLIS = 5_000L
        private const val LINES_TO_FEED_AFTER_PRINT = 3
        private const val PRINT_TIMEOUT_MILLIS = 30_000L
        private const val PRINTER_STATE_NORMAL = 1
        private const val PRINTER_STATE_OUT_OF_PAPER = 4
        private const val PRINT_RESULT_SUCCESS = 0
    }
}
