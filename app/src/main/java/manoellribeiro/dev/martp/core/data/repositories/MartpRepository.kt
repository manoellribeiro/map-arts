package manoellribeiro.dev.martp.core.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import manoellribeiro.dev.martp.core.data.local.daos.ColorPalettesDao
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
import manoellribeiro.dev.martp.core.data.local.daos.UserInfoDao
import manoellribeiro.dev.martp.core.data.local.entities.ColorPaletteEntity
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity
import manoellribeiro.dev.martp.core.data.network.geoapify.GeoapifyApiService
import manoellribeiro.dev.martp.core.data.network.martp.MartpApiService
import manoellribeiro.dev.martp.core.data.network.martp.models.RemoteMapLayersColors
import manoellribeiro.dev.martp.core.data.network.martp.models.RemoteReversedAddress
import manoellribeiro.dev.martp.core.data.network.martp.models.RemoteSaveMapArtRequest
import manoellribeiro.dev.martp.core.di.ApplicationScope
import manoellribeiro.dev.martp.core.di.IoDispatcher
import manoellribeiro.dev.martp.core.extensions.toHexColor
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import manoellribeiro.dev.martp.core.models.failures.MapboxApiErrorFailure
import manoellribeiro.dev.martp.core.models.failures.NoInternetConnectionFailure
import manoellribeiro.dev.martp.core.models.SketchArtType
import manoellribeiro.dev.martp.core.models.failures.MartpApiErrorFailure
import manoellribeiro.dev.martp.core.services.ConnectivityService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.http.Multipart
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class MartpRepository @Inject constructor(
    private val mapboxApiService: MapboxApiService,
    private val geoapifyApiService: GeoapifyApiService,
    private val martpApiService: MartpApiService,
    private val connectivityService: ConnectivityService,
    private val mapArtDao: MapArtsDao,
    private val userInfoDao: UserInfoDao,
    private val colorPalettesDao: ColorPalettesDao,
    private val artSettingsDataSore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val errorLogger: ErrorLogger
) {

    companion object {
        private const val MAP_ZOOM_KEY = "MAP_ZOOM_KEY"
        private const val MAP_ART_STYLE_KEY = "MAP_ART_STYLE_KEY"
        private const val SELECTED_COLOR_PALETTE = "SELECTED_COLOR_PALETTE"
        private val FALLBACK_COLOR_PALETTES = listOf(
            ColorPaletteEntity(
                name = "Martp",
                colors = listOf(
                    0xFF100C2D.toInt(),
                    0xFF120D31.toInt(),
                    0xFF4E3C41.toInt(),
                    0xFF896B51.toInt(),
                    0xFFFFC971.toInt(),
                    0xFFFFCE7E.toInt(),
                )
            )
        )
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
            errorLogger.logNonFatal(e, "MartpRepository.setColorPalettePreference")
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
            errorLogger.logNonFatal(e, "MartpRepository.getColorPalettePreference")
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
            errorLogger.logNonFatal(e, "MartpRepository.setMapArtStylePreference")
            throw LocalStorageErrorFailure(e.message)
        }
    }

    suspend fun getArtStylePreference(): SketchArtType {
        val defaultArtStyle = SketchArtType.COLORFUL_MAP
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
            errorLogger.logNonFatal(e, "MartpRepository.getArtStylePreference")
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
            errorLogger.logNonFatal(e, "MartpRepository.setMapZoomPreference")
            throw LocalStorageErrorFailure(e.message)
        }
    }

    suspend fun getMapZoomPreference(): Float {
        val defaultZoom = 16.5F
        try {
            val mapZoomKey = floatPreferencesKey(MAP_ZOOM_KEY)
            return artSettingsDataSore.data.map { preferences ->
                preferences[mapZoomKey]
            }.first() ?: defaultZoom
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "MartpRepository.getMapZoomPreference")
            return defaultZoom
        }
    }

    suspend fun setUserName(username: String, userId: String) {
        try {
            userInfoDao.setUserName(username = username, id = userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "MartpRepository.setUserName")
        }
    }

    suspend fun setUserEmail(email: String, userId: String) {
        try {
            userInfoDao.setUserEmail(email = email, id = userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "MartpRepository.setUserEmail")
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
            errorLogger.logNonFatal(e, "MartpRepository.fetchCurrentUserInfo")
            return UserInfoEntity(
                id = UUID.randomUUID().toString(),
                username = null,
                email = null
            )
        }
    }

    fun observeUserMapArts(): Flow<List<MapArtEntity>> {
        return mapArtDao.getAll().catch { e ->
            errorLogger.logNonFatal(e, "MartpRepository.observeUserMapArts")
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

                    FileOutputStream(imageFile).use { fileOutputStream ->
                        fileOutputStream.write(response.bytes())
                    }

                    imageFile.path
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorLogger.logNonFatal(e, "MartpRepository.fetchStaticMapImage")
                throw MapboxApiErrorFailure(originalExceptionMessage = e.message)
            }
        } else {
            throw NoInternetConnectionFailure(originalExceptionMessage = null)
        }
    }

    suspend fun saveMapArtEntity(
        mapArtEntity: MapArtEntity,
    ) {
        try {
            mapArtDao.insert(mapArtEntity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "MartpRepository.saveMapArtEntity")
            throw LocalStorageErrorFailure(originalExceptionMessage = e.message)
        }
    }

    fun uploadMapArt(
        mapArtEntity: MapArtEntity,
        userInfoEntity: UserInfoEntity
    ) {
        applicationScope.launch {
            try {
                martpApiService.saveMapArt(
                    art = createRemoteSaveMapArtRequestFromMapArtEntity(mapArtEntity, userInfoEntity),
                    image = createImageMultipartBody(mapArtEntity.imagePathLocation)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorLogger.logNonFatal(e, "MartpRepository.uploadMapArt")
            }
        }
    }

    private fun createImageMultipartBody(
        imagePathLocation: String
    ): MultipartBody.Part {
        val imageFile = File(imagePathLocation)
        return MultipartBody.Part.createFormData(
            name = "image",
            filename = imageFile.name,
            body = imageFile.asRequestBody("image/png".toMediaType())
        )
    }

    private fun createRemoteSaveMapArtRequestFromMapArtEntity(
        mapArtEntity: MapArtEntity,
        userInfoEntity: UserInfoEntity
    ): RemoteSaveMapArtRequest {
        return RemoteSaveMapArtRequest(
            id = mapArtEntity.id,
            title = mapArtEntity.title,
            latitude = mapArtEntity.latitude.toDouble(),
            longitude = mapArtEntity.longitude.toDouble(),
            dateInMillis = mapArtEntity.dateInMillis,
            sketchArtType = mapArtEntity.sketchArtType?.name,
            colorPalette = mapArtEntity.colorPalette,
            colorsLayers = RemoteMapLayersColors(
                roadsColor = mapArtEntity.colorsLayers?.roadsColor?.toHexColor(),
                buildingStrokeColor = mapArtEntity.colorsLayers?.buildingStrokeColor?.toHexColor(),
                buildingColor = mapArtEntity.colorsLayers?.buildingColor?.toHexColor(),
                roadsPathColor = mapArtEntity.colorsLayers?.roadsPathColor?.toHexColor(),
                waterColor = mapArtEntity.colorsLayers?.waterColor?.toHexColor(),
                landOthersColor = mapArtEntity.colorsLayers?.landOthersColor?.toHexColor(),
            ),
            email = userInfoEntity.email,
            username = userInfoEntity.username
        )
    }

    suspend fun getColorsPalettes(): List<ColorPaletteEntity> {
        val localColorPalettes = try {
            colorPalettesDao.getAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "MartpRepository.getColorsPalettes")
            emptyList()
        }
        if (localColorPalettes.isEmpty()) return fetchColorsPalettes()

        applicationScope.launch { fetchColorsPalettes() }
        return localColorPalettes
    }

    suspend fun fetchColorsPalettes(): List<ColorPaletteEntity> {
        val remoteColorPalettes = try {
            martpApiService.getColorPalettes().palettes.map { ColorPaletteEntity(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "MartpRepository.fetchColorsPalettes")
            return FALLBACK_COLOR_PALETTES
        }

        if (remoteColorPalettes.isEmpty()) return FALLBACK_COLOR_PALETTES

        try {
            colorPalettesDao.replaceAll(remoteColorPalettes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "MartpRepository.fetchColorsPalettes")
        }
        return remoteColorPalettes
    }

    suspend fun getSelectedColorPalette(): ColorPaletteEntity {
        val selectedColorPalette = try {
            colorPalettesDao.getByName(getColorPalettePreference())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "MartpRepository.getSelectedColorPalette")
            null
        }
        return selectedColorPalette ?: getColorsPalettes().first()
    }

    suspend fun getReversedAddressFromApi(
        latitude: Double,
        longitude: Double
    ): RemoteReversedAddress {
        try {
            return martpApiService.reverseGeocode(
                latitude = latitude,
                longitude = longitude,
                language = Locale.getDefault().language
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "MartpRepository.fetchColorsPalettes")
            throw MartpApiErrorFailure(originalExceptionMessage = e.message)
        }
    }
}