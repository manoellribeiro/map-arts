package manoellribeiro.dev.martp.core.services

import android.graphics.Bitmap

interface MapArtPrinterService {
    val isAvailable: Boolean
    val printableWidthPixels: Int
    fun isConnected(): Boolean
    suspend fun connect(): Boolean
    suspend fun printBitmap(mapBitmap: Bitmap)
}
