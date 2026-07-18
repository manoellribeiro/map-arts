package manoellribeiro.dev.martp.core.data.repositories

import android.graphics.Bitmap
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.data.network.mapbox.models.MapboxMapStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
import manoellribeiro.dev.martp.core.data.local.daos.UserInfoDao
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity
import manoellribeiro.dev.martp.core.data.network.geoapify.GeoapifyApiService
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import manoellribeiro.dev.martp.core.models.failures.NoInternetConnectionFailure
import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import manoellribeiro.dev.martp.core.services.ConnectivityService
import manoellribeiro.dev.martp.core.sketches.MartpSketch
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

class MartpRepository @Inject constructor(
    private val mapboxApiService: MapboxApiService,
    private val geoapifyApiService: GeoapifyApiService,
    private val connectivityService: ConnectivityService,
    private val mapArtDao: MapArtsDao,
    private val userInfoDao: UserInfoDao,
    private val artSettingsDataSore: DataStore<Preferences>
) {

    //TODO: Refactor this way of using ioScope
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val MAP_ZOOM_KEY = "MAP_ZOOM_KEY"
        private const val MAP_ART_STYLE_KEY = "MAP_ART_STYLE_KEY"
    }

    fun setMapArtStylePreference(sketchArtType: SketchArtType) = ioScope.launch {
        try {
            Log.i("MartpRepository", "set sketch type " + sketchArtType.name)
            val artStyleKey = stringPreferencesKey(MAP_ART_STYLE_KEY)
            artSettingsDataSore.edit { preferences ->
                preferences[artStyleKey] = sketchArtType.name
            }
        } catch (e: Exception) {
            throw LocalStorageErrorFailure(e.message)
        }
    }

    fun getArtStylePreference(): Deferred<SketchArtType> = ioScope.async {
        val defaultArtStyle = SketchArtType.DEFAULT
        try {
            val artStyleKey = stringPreferencesKey(MAP_ART_STYLE_KEY)
            return@async SketchArtType.valueOf(
                artSettingsDataSore.data.map { preferences ->
                    preferences[artStyleKey]
                }.first() ?: defaultArtStyle.name
            )
        } catch (e: Exception) {
            return@async defaultArtStyle
        }
    }

    fun setMapZoomPreference(mapZoom: Float) = ioScope.launch {
        try {
            val mapZoomKey = floatPreferencesKey(MAP_ZOOM_KEY)
            artSettingsDataSore.edit { preferences ->
                preferences[mapZoomKey] = mapZoom
            }
        } catch (e: Exception) {
            throw LocalStorageErrorFailure(e.message)
        }
    }

    fun getMapZoomPreference(): Deferred<Float> = ioScope.async {
        val defaultZoom = 15F
        try {
            val mapZoomKey = floatPreferencesKey(MAP_ZOOM_KEY)
            return@async artSettingsDataSore.data.map { preferences ->
                preferences[mapZoomKey]
            }.first() ?: defaultZoom
        } catch (e: Exception) {
            return@async defaultZoom
        }
    }

    fun setUserName(username: String, userId: String) = ioScope.launch {
        try {
            userInfoDao.setUserName(username = username, id = userId)
        } catch (e: Exception) {
            //Log, but don't do nothing
        }
    }

    fun setUserEmail(email: String, userId: String) = ioScope.launch {
        try {
            userInfoDao.setUserEmail(email = email, id = userId)
        } catch (e: Exception) {
            //Log, but don't do nothing
        }
    }

    fun fetchCurrentUserInfo() :Deferred<UserInfoEntity> = ioScope.async {
        try {
            var user = userInfoDao.getUser()
            return@async if(user == null) {
               user = UserInfoEntity(
                   id = UUID.randomUUID().toString(),
                   username = null,
                   email = null
               )
                userInfoDao.insert(
                    userInfoEntity = user
                )
                user
            } else user
        } catch (e: Exception) {
            return@async UserInfoEntity(
                id = UUID.randomUUID().toString(),
                username = null,
                email = null
            ) //todo: think how to handle this error
        }
    }

    fun fetchUserMapArts(): Deferred<List<MapArtEntity>> = ioScope.async {
        try {
            return@async mapArtDao.getAll()
        } catch (e: Exception) {
            throw LocalStorageErrorFailure(e.message)
        }
    }

    fun fetchStaticMapImageAsync(
        sketchArtType: SketchArtType,
        latitude: Double,
        longitude: Double,
        mapWidth: Int,
        mapHeight: Int,
        dir: File
    ): Deferred<String> = ioScope.async {
        if(connectivityService.isInternetConnected()) {
            try {
                val mapZoom = getMapZoomPreference().await()
                Log.i("MartpRepository", "mapZoom: " + mapZoom.toString())
                //TODO: this is the implementation for the geoapifyApiService, it is ready to use when I create the art style of it
//                val response: ResponseBody = geoapifyApiService.getStaticMapImageAsync(
//                    styleId = "toner",
//                    latitude = "lonlat:${longitude},${latitude}",
//                    mapWidth = mapWidth - MartpSketch.framePadding.toInt() - MartpSketch.frameThickness.toInt(),
//                    mapHeight = mapHeight - MartpSketch.framePadding.toInt() - MartpSketch.frameThickness.toInt(),
//                    mapZoom = mapZoom
//                )

                val response: ResponseBody = mapboxApiService.getStaticMapImageAsync(
                    styleId = sketchArtType.mapBoxMapStyle.id,
                    latitude = latitude,
                    longitude = longitude,
                    mapWidth = mapWidth,
                    mapHeight = mapHeight,
                    mapZoom = mapZoom
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

    fun saveMapArtEntity(mapArtEntity: MapArtEntity): Deferred<Unit> = ioScope.async {
        try {
            mapArtDao.insert(mapArtEntity)
        } catch (e: Exception) {
            throw LocalStorageErrorFailure(originalExceptionMessage = e.message)
        }
    }

}