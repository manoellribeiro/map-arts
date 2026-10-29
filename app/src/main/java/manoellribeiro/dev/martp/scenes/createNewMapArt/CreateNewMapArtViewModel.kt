package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.graphics.Bitmap
import android.location.Address
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.MapLayersColors
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.models.SketchArtType
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
    private var currentArtAddress: Address? = null
    private var currentSketchArtType: SketchArtType? = null
    private var currentColorPalette: String? = null
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
                val (sketchArtType, location) = coroutineScope {
                    val sketchArtType = async { repository.getArtStylePreference() }
                    val location = async { locationService.getCurrentLocation() }
                    sketchArtType.await() to location.await()
                }
                currentArtLocation = location
                val horizontalTilesCount = 4
                val verticalTilesCount = 4
                val horizontalPaddingsNumber = horizontalTilesCount + 1
                val verticalPaddingsNumber = verticalTilesCount + 1
                val padding = 20
                emitNewState(CreateNewMapArtUiState.Loading(R.string.downloading_map_image))
                val (address, staticImagePath, colorPalette) = coroutineScope {
                    val address = async {
                        getAddressService.getAddress(
                            location.latitude,
                            location.longitude
                        )
                    }
                    val colorPalette = async {
                        if(sketchArtType == SketchArtType.COLORFUL_MAP) {
                            repository.getColorPalettePreference()
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
                    Triple(address.await(), staticImagePath.await(), colorPalette.await())
                }
                currentArtAddress = address
                currentSketchArtType = sketchArtType
                currentColorPalette = colorPalette
                emitNewState(
                    CreateNewMapArtUiState.ImageDownloaded(
                        staticMapImagePath = staticImagePath,
                        title = buildTitleWithAddress(address),
                        sketchArtType = sketchArtType,
                        colorPalette = colorPalette,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                )
            } catch (failure: Failure) {
                emitNewState(CreateNewMapArtUiState.Error(failure))
            }
        }
    }

    private fun buildTitleWithAddress(address: Address?): String {
        return listOfNotNull(address?.thoroughfare, address?.locality, address?.countryName)
            .filter { it.isNotBlank() }
            .joinToString(", ")
    }

    fun saveArtToLocalDatabase(
        title: String,
        description: String?,
        newArtBitMap: Bitmap,
        directory: File,
        mapLayersColors: MapLayersColors?
    ) {
        viewModelScope.launch {
            try {
                emitNewState(CreateNewMapArtUiState.ActionButtonLoading)
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
                    colorPalette = currentColorPalette,
                    colorsLayers = mapLayersColors
                )

                repository.saveMapArtEntity(newMartp)

                _events.send(
                    CreateNewMapArtEvent.ArtCreatedSuccessfully(
                        pathToStoreArtImage = newMartp.imagePathLocation
                    )
                )
            } catch (failure: Failure) {
                emitNewState(CreateNewMapArtUiState.Error(failure))
            }
        }
    }
}