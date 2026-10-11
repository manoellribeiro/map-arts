package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.content.Context
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.data.local.MartpDatabase
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.services.GenerateAIContentService
import manoellribeiro.dev.martp.core.services.GetAddressService
import manoellribeiro.dev.martp.core.services.LocationService
import manoellribeiro.dev.martp.core.sketches.MartpSketch
import manoellribeiro.dev.martp.core.utils.PromptGenerator
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateNewMapArtViewModel @Inject constructor(
    private val repository: MartpRepository,
    private val locationService: LocationService,
    private val getAddressService: GetAddressService,
    private val generateAIContentService: GenerateAIContentService,
    private val promptGenerator: PromptGenerator
): ViewModel() {

    private val _state: MutableLiveData<CreateNewMapArtUiState> = MutableLiveData()
    val state: LiveData<CreateNewMapArtUiState> = _state

    private fun emitNewState(newState: CreateNewMapArtUiState) {
        _state.value = newState
    }

    private var currentArtLocation: Location? = null
    private var currentArtAddress: Address? = null
    private var generateMapArtJob: Job? = null

    fun startToGenerateMapArt(
        directory: File,
        canvasToDrawArtWidth: Int,
        canvasToDrawArtHeight: Int,
    ) {
        generateMapArtJob?.cancel()
        generateMapArtJob = viewModelScope.launch {
            try {
                emitNewState(CreateNewMapArtUiState.Loading)
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
                val (address, staticImagePath) = coroutineScope {
                    val address = async {
                        getAddressService.getAddress(
                            location.latitude,
                            location.longitude
                        )
                    }
                    val staticImagePath = async {
                        repository.fetchStaticMapImage(
                            sketchArtType = sketchArtType,
                            longitude = location.longitude,
                            latitude = location.latitude,
                            mapWidth = canvasToDrawArtWidth - (2 * MartpSketch.frameThickness).toInt() - (2 * MartpSketch.framePadding).toInt(),
                            mapHeight = canvasToDrawArtHeight - (2 * MartpSketch.frameThickness).toInt() - (2 * MartpSketch.framePadding).toInt(),
                            //mapWidth = canvasToDrawArtWidth - (horizontalPaddingsNumber * padding) - (2 * MartpSketch.frameThickness).toInt() - (2 * MartpSketch.framePadding).toInt(), this is the right one to use when creating arts with tiles
                            //mapHeight = canvasToDrawArtHeight - (verticalPaddingsNumber * padding) - (2 * MartpSketch.frameThickness).toInt() - (2 * MartpSketch.framePadding).toInt(), this is the right one to use when creating arts with tiles
                            dir = directory
                        )
                    }
                    address.await() to staticImagePath.await()
                }
                currentArtAddress = address
                emitNewState(
            CreateNewMapArtUiState.ImageDownloaded(
                            staticMapImagePath = staticImagePath,
                            title = address?.locality + ", " + address?.countryName + ", " + address?.thoroughfare,
                            sketchArtType = sketchArtType
                        )
                )
            } catch (failure: Failure) {
                emitNewState(CreateNewMapArtUiState.Error(failure))
            }
        }
    }

    fun generateAiText() {
        viewModelScope.launch {
            try {
                emitNewState(CreateNewMapArtUiState.LoadingAIText)
                val text = generateAIContentService.generateTextContent(promptGenerator.generateArtDescriptionPrompt(
                    countryName = currentArtAddress?.countryName.orEmpty(),
                    cityName = currentArtAddress?.locality.orEmpty(),
                    streetName = currentArtAddress?.thoroughfare.orEmpty(),
                    languageToReturn = Locale.getDefault().displayName
                ))
                if(text.isNullOrBlank()) {
                    emitNewState(CreateNewMapArtUiState.ErrorGeneratingAIText)
                } else {
                    emitNewState(CreateNewMapArtUiState.GeneratedAIText(text))
                }
            } catch (failure: Failure) {
                emitNewState(CreateNewMapArtUiState.ErrorGeneratingAIText)
            }
        }
    }

    fun saveArtToLocalDatabase(
        title: String,
        description: String?,
        newArtBitMap: Bitmap,
        directory: File
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
                    imagePathLocation = directory.path + MartpDatabase.artsDirectoryName + artId + ".png"
                )

                repository.saveMapArtEntity(newMartp)

                emitNewState(
                    CreateNewMapArtUiState.ArtCreatedSuccessfully(
                        pathToStoreArtImage = newMartp.imagePathLocation
                    )
                )
            } catch (failure: Failure) {
                emitNewState(CreateNewMapArtUiState.Error(failure))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.i("CreateNewMapArtViewMode", e.message ?: "")
            }
        }
    }
}