package manoellribeiro.dev.martp.core.data.repositories

import android.util.Log
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
import manoellribeiro.dev.martp.core.models.failures.NoInternetConnectionFailure
import manoellribeiro.dev.martp.core.services.ConnectivityService
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class MartpRepository @Inject constructor(
    private val mapboxApiService: MapboxApiService,
    private val connectivityService: ConnectivityService,
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

    fun fetchStaticMapImageAsync(
        latitude: Double,
        longitude: Double,
        mapWidth: Int,
        mapHeight: Int,
        dir: File
    ): Deferred<String> = ioScope.async {
        if(connectivityService.isInternetConnected()) {
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
                Log.i("MartpRepository", e.message ?: "")
                throw e //TODO: this exception can't be thrown here
            }
        } else {
            throw NoInternetConnectionFailure(originalExceptionMessage = null)
        }
    }

}