package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.graphics.Bitmap
import android.location.Address
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.data.local.MartpDatabase
import manoellribeiro.dev.martp.core.data.local.entities.ColorPaletteEntity
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.MapLayersColors
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import manoellribeiro.dev.martp.core.models.SketchArtType
import manoellribeiro.dev.martp.core.models.failures.UnknownErrorFailure
import manoellribeiro.dev.martp.core.services.GenerateAIContentService
import manoellribeiro.dev.martp.core.services.GetAddressService
import manoellribeiro.dev.martp.core.services.LocationService
import manoellribeiro.dev.martp.core.sketches.MartpSketch
import manoellribeiro.dev.martp.core.utils.PromptGenerator
import java.io.File
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class CreateNewMapArtViewModel @Inject constructor(
    private val repository: MartpRepository,
    private val locationService: LocationService,
    private val getAddressService: GetAddressService,
    private val generateAIContentService: GenerateAIContentService,
    private val promptGenerator: PromptGenerator
): ViewModel() {

    private val _state: MutableStateFlow<CreateNewMapArtUiState> = MutableStateFlow(CreateNewMapArtUiState.Idle)
    val state: StateFlow<CreateNewMapArtUiState> = _state.asStateFlow()

    private val _events: Channel<CreateNewMapArtEvent> = Channel(Channel.BUFFERED)
    val events: Flow<CreateNewMapArtEvent> = _events.receiveAsFlow()

    private fun emitNewState(newState: CreateNewMapArtUiState) {
        _state.value = newState
    }

    private var currentArtLocation: Location? = null
    private var currentSketchArtType: SketchArtType? = null
    private var currentColorPalette: ColorPaletteEntity? = null
    private var generateMapArtJob: Job? = null

    fun startToGenerateMapArt(
        directory: File,
        canvasToDrawArtWidth: Int,
        canvasToDrawArtHeight: Int,
    ) {
        generateMapArtJob?.cancel()
        generateMapArtJob = viewModelScope.launch {
            try {
                emitNewState(CreateNewMapArtUiState.Loading(R.string.accessing_your_location))
                val (sketchArtType, location, user) = coroutineScope {
                    val sketchArtType = async { repository.getArtStylePreference() }
                    val location = async { locationService.getCurrentLocation() }
                    val user = async { repository.fetchCurrentUserInfo() }
                    Triple(sketchArtType.await(), location.await(), user.await())
                }
                currentArtLocation = location
                val horizontalTilesCount = 4
                val verticalTilesCount = 4
                val horizontalPaddingsNumber = horizontalTilesCount + 1
                val verticalPaddingsNumber = verticalTilesCount + 1
                val padding = 20
                emitNewState(CreateNewMapArtUiState.Loading(R.string.downloading_map_image))
                val (title, staticImagePath, colorPalette) = coroutineScope {
                    val title = async {
                        getAddressTitle(location.latitude, location.longitude)
                    }
                    val colorPalette = async {
                        if(sketchArtType == SketchArtType.COLORFUL_MAP) {
                            repository.getSelectedColorPalette()
                        } else null
                    }
                    val staticImagePath = async {
                        repository.fetchStaticMapImage(
                            sketchArtType = sketchArtType,
                            longitude = location.longitude,
                            latitude = location.latitude,
                            mapWidth = canvasToDrawArtWidth - (2 * MartpSketch.frameThickness).toInt() - (2 * MartpSketch.framePadding).toInt(),
                            mapHeight = min(canvasToDrawArtHeight - (2 * MartpSketch.frameThickness).toInt() - (2 * MartpSketch.framePadding).toInt(), 1280),
                            //mapWidth = canvasToDrawArtWidth - (horizontalPaddingsNumber * padding) - (2 * MartpSketch.frameThickness).toInt() - (2 * MartpSketch.framePadding).toInt(), this is the right one to use when creating arts with tiles
                            //mapHeight = canvasToDrawArtHeight - (verticalPaddingsNumber * padding) - (2 * MartpSketch.frameThickness).toInt() - (2 * MartpSketch.framePadding).toInt(), this is the right one to use when creating arts with tiles
                            dir = directory
                        )
                    }
                    Triple(title.await(), staticImagePath.await(), colorPalette.await())
                }
                currentSketchArtType = sketchArtType
                currentColorPalette = colorPalette
                emitNewState(
                    CreateNewMapArtUiState.ImageDownloaded(
                        staticMapImagePath = staticImagePath,
                        title = title,
                        sketchArtType = sketchArtType,
                        colorPalette = colorPalette,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        user = user
                    )
                )
            } catch (failure: Failure) {
                emitNewState(CreateNewMapArtUiState.Error(failure))
            }
        }
    }

    private suspend fun getAddressTitle(
        latitude: Double,
        longitude: Double
    ): String {
        val addressFromAndroidGeocode = getAddressService.getAddress(latitude, longitude)
        if(addressFromAndroidGeocode == null) {
            val addressFromTheApi = repository.getReversedAddressFromApi(latitude, longitude)
            return buildTitleWithAddress(
                street = addressFromTheApi.street,
                city = addressFromTheApi.city,
                country = addressFromTheApi.country
            )
        }
        return buildTitleWithAddress(
            street = addressFromAndroidGeocode.thoroughfare,
            city = addressFromAndroidGeocode.locality,
            country = addressFromAndroidGeocode.countryName
        )
    }

    private fun buildTitleWithAddress(
        street: String?,
        city: String?,
        country: String?
    ): String {
        return listOfNotNull(street, city, country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
    }

    fun saveArt(
        title: String,
        description: String?,
        directory: File,
        mapLayersColors: MapLayersColors?,
        updateUserInfo: UserInfoEntity,
        saveImage: (path: String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                emitNewState(CreateNewMapArtUiState.ActionButtonLoading)
                repository.setUserName(updateUserInfo.username.orEmpty(), updateUserInfo.id)
                repository.setUserEmail(updateUserInfo.email.orEmpty(), updateUserInfo.id)
                val artId = UUID.randomUUID().toString()
                val newMartp = MapArtEntity(
                    id = artId,
                    title = title,
                    description = description,
                    latitude = currentArtLocation?.latitude?.toFloat() ?: 0F ,
                    longitude = currentArtLocation?.longitude?.toFloat() ?:0F ,
                    dateInMillis = Calendar.getInstance().timeInMillis,
                    imagePathLocation = directory.path + MartpDatabase.artsDirectoryName + artId + ".png",
                    sketchArtType = currentSketchArtType,
                    colorPalette = currentColorPalette?.name,
                    colorsLayers = mapLayersColors
                )
                saveImage(newMartp.imagePathLocation)
                repository.saveMapArtEntity(newMartp)
                repository.uploadMapArt(newMartp, updateUserInfo)

                _events.send(CreateNewMapArtEvent.ArtCreatedSuccessfully)
            } catch (failure: Failure) {
                emitNewState(CreateNewMapArtUiState.Error(failure))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emitNewState(CreateNewMapArtUiState.Error(UnknownErrorFailure(originalExceptionMessage = e.message)))
            }
        }
    }
}