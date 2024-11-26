package manoellribeiro.dev.martp.core.data.repositories

import androidx.lifecycle.LiveData
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.data.network.mapbox.models.MapboxMapStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

class MartpRepository(
    private val mapboxApiService: MapboxApiService,
    private val mapArtDao: MapArtsDao
) {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun fetchUserMapArts(): Deferred<List<MapArtEntity>> = ioScope.async {
        try {
            return@async mapArtDao.getAll()
        } catch (e: Exception) {
            throw LocalStorageErrorFailure(e.message)
        }
    }

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