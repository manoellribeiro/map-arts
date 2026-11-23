package manoellribeiro.dev.martp.core.services

import android.graphics.Bitmap
import manoellribeiro.dev.martp.printerhardware.services.SmartPosPrinterService

class SunmiMapArtPrinterService(
    private val smartPosPrinterService: SmartPosPrinterService
) : MapArtPrinterService {
    override val isAvailable = true
    override val printableWidthPixels get() = smartPosPrinterService.paperWidthPixels
    override fun isConnected() = smartPosPrinterService.isConnected()
    override suspend fun connect() = smartPosPrinterService.connect()
    override suspend fun printBitmap(mapBitmap: Bitmap) = smartPosPrinterService.printBitmap(mapBitmap)
}
