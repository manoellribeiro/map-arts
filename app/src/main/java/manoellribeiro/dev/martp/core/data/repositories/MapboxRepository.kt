package manoellribeiro.dev.martp.core.data.repositories

import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.data.network.mapbox.models.MapboxMapStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

class MapboxRepository(
    private val mapboxApiService: MapboxApiService,
) {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun fetchStaticMapImageUrlAsync(
        latitude: Double,
        longitude: Double,
        mapWidth: Int,
        mapHeight: Int,
        dir: File
    ): Deferred<String> = ioScope.async {
        try {
            val response: ResponseBody = mapboxApiService.getStaticMapImageAsync(
                styleId = MapboxMapStyle.DarkV11.id,
                latitude = latitude,
                longitude = longitude,
                mapWidth = mapWidth,
                mapHeight = mapHeight
            )

            val imageFile = File(dir,"image.png")

            imageFile.createNewFile()

            val fileOutputStream = FileOutputStream(imageFile)
            fileOutputStream.write(response.bytes())
            fileOutputStream.close()

            return@async imageFile.path
        } catch (e: Exception) {
            throw e
        }
    }

}