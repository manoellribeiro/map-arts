package manoellribeiro.dev.martp.core.data.repositories

import android.graphics.Bitmap
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.data.network.mapbox.models.MapboxMapStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
import manoellribeiro.dev.martp.core.data.local.daos.UserInfoDao
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity
import manoellribeiro.dev.martp.core.data.network.geoapify.GeoapifyApiService
import manoellribeiro.dev.martp.core.di.IoDispatcher
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import manoellribeiro.dev.martp.core.models.failures.NoInternetConnectionFailure
import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import manoellribeiro.dev.martp.core.services.ConnectivityService
import manoellribeiro.dev.martp.core.sketches.MartpSketch
import manoellribeiro.dev.martp.core.utils.ColorPaletteProvider
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
    private val artSettingsDataSore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val MAP_ZOOM_KEY = "MAP_ZOOM_KEY"
        private const val MAP_ART_STYLE_KEY = "MAP_ART_STYLE_KEY"
        private const val SELECTED_COLOR_PALETTE = "SELECTED_COLOR_PALETTE"
    }

    suspend fun setColorPalettePreference(colorPaletteName: String) {
        try {
            val colorPaletteKey = stringPreferencesKey(SELECTED_COLOR_PALETTE)
            artSettingsDataSore.edit { preferences ->
                preferences[colorPaletteKey] = colorPaletteName
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw LocalStorageErrorFailure(e.message)
        }
    }

    suspend fun getColorPalettePreference(): String {
        val defaultColorPalette = "Martp"
        try {
            val colorPaletteKey = stringPreferencesKey(SELECTED_COLOR_PALETTE)
            return artSettingsDataSore.data.map { preferences ->
                preferences[colorPaletteKey]
            }.first() ?: defaultColorPalette
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return defaultColorPalette
        }
    }

    suspend fun setMapArtStylePreference(sketchArtType: SketchArtType) {
        try {
            val artStyleKey = stringPreferencesKey(MAP_ART_STYLE_KEY)
            artSettingsDataSore.edit { preferences ->
                preferences[artStyleKey] = sketchArtType.name
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw LocalStorageErrorFailure(e.message)
        }
    }

    suspend fun getArtStylePreference(): SketchArtType {
        val defaultArtStyle = SketchArtType.DEFAULT
        try {
            val artStyleKey = stringPreferencesKey(MAP_ART_STYLE_KEY)
            return SketchArtType.valueOf(
                artSettingsDataSore.data.map { preferences ->
                    preferences[artStyleKey]
                }.first() ?: defaultArtStyle.name
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return defaultArtStyle
        }
    }

    suspend fun setMapZoomPreference(mapZoom: Float) {
        try {
            val mapZoomKey = floatPreferencesKey(MAP_ZOOM_KEY)
            artSettingsDataSore.edit { preferences ->
                preferences[mapZoomKey] = mapZoom
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw LocalStorageErrorFailure(e.message)
        }
    }

    suspend fun getMapZoomPreference(): Float {
        val defaultZoom = 15F
        try {
            val mapZoomKey = floatPreferencesKey(MAP_ZOOM_KEY)
            return artSettingsDataSore.data.map { preferences ->
                preferences[mapZoomKey]
            }.first() ?: defaultZoom
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return defaultZoom
        }
    }

    suspend fun setUserName(username: String, userId: String) {
        try {
            userInfoDao.setUserName(username = username, id = userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            //Log, but don't do nothing
        }
    }

    suspend fun setUserEmail(email: String, userId: String) {
        try {
            userInfoDao.setUserEmail(email = email, id = userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            //Log, but don't do nothing
        }
    }

    suspend fun fetchCurrentUserInfo(): UserInfoEntity {
        try {
            var user = userInfoDao.getUser()
            return if(user == null) {
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return UserInfoEntity(
                id = UUID.randomUUID().toString(),
                username = null,
                email = null
            ) //todo: think how to handle this error
        }
    }

    fun observeUserMapArts(): Flow<List<MapArtEntity>> {
        return mapArtDao.getAll().catch { e ->
            throw LocalStorageErrorFailure(e.message)
        }
    }

    suspend fun fetchStaticMapImage(
        sketchArtType: SketchArtType,
        latitude: Double,
        longitude: Double,
        mapWidth: Int,
        mapHeight: Int,
        dir: File
    ): String {
        if(connectivityService.isInternetConnected()) {
            try {
                val mapZoom = getMapZoomPreference()
                //TODO: this is the implementation for the geoapifyApiService, it is ready to use when I create the art style of it
//                val response: ResponseBody = geoapifyApiService.getStaticMapImage(
//                    styleId = "toner",
//                    latitude = "lonlat:${longitude},${latitude}",
//                    mapWidth = mapWidth - MartpSketch.framePadding.toInt() - MartpSketch.frameThickness.toInt(),
//                    mapHeight = mapHeight - MartpSketch.framePadding.toInt() - MartpSketch.frameThickness.toInt(),
//                    mapZoom = mapZoom
//                )
                Log.i("MartpRepository", "mapZoom: " + mapZoom)
                val response: ResponseBody = mapboxApiService.getStaticMapImage(
                    styleId = sketchArtType.mapBoxMapStyle.id,
                    latitude = latitude,
                    longitude = longitude,
                    mapWidth = mapWidth,
                    mapHeight = mapHeight,
                    mapZoom = mapZoom
                )

                return withContext(ioDispatcher) {
                    val imageFile = File(dir,"image.png")

                    imageFile.createNewFile()

                    val fileOutputStream = FileOutputStream(imageFile)
                    fileOutputStream.write(response.bytes())
                    fileOutputStream.close()

                    imageFile.path
                }
            } catch (e: Exception) {
                throw e //TODO: this exception can't be thrown here
            }
        } else {
            throw NoInternetConnectionFailure(originalExceptionMessage = null)
        }
    }

    suspend fun saveMapArtEntity(mapArtEntity: MapArtEntity) {
        try {
            mapArtDao.insert(mapArtEntity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw LocalStorageErrorFailure(originalExceptionMessage = e.message)
        }
    }

}