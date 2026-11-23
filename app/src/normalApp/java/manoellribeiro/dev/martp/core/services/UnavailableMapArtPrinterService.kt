package manoellribeiro.dev.martp.core.services

import android.graphics.Bitmap

class UnavailableMapArtPrinterService : MapArtPrinterService {
    override val isAvailable = false
    override val printableWidthPixels = 0
    override fun isConnected() = false
    override suspend fun connect() = false
    override suspend fun printBitmap(mapBitmap: Bitmap) = Unit
}
