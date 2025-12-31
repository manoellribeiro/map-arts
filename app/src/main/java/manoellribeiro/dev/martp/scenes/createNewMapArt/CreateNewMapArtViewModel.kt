package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.graphics.Bitmap
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.data.local.MartpDatabase
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.services.ImageDescriptionService
import manoellribeiro.dev.martp.core.services.LocationService
import manoellribeiro.dev.martp.core.sketches.MartpSketch
import java.io.File
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class CreateNewMapArtViewModel @Inject constructor(
    private val repository: MartpRepository,
    private val locationService: LocationService,
    private val imageDescriptionService: ImageDescriptionService
): ViewModel() {

    private val _state: MutableLiveData<CreateNewMapArtUiState> = MutableLiveData()
    val state: LiveData<CreateNewMapArtUiState> = _state

    private fun emitNewState(newState: CreateNewMapArtUiState) {
        _state.value = newState
    }

    private var currentArtLocation: Location? = null

    suspend fun startToGenerateMapArt(
        directory: File,
        canvasToDrawArtWidth: Int,
        canvasToDrawArtHeight: Int
    ) {
        try {
            emitNewState(CreateNewMapArtUiState.Loading)
            val location = locationService.getCurrentLocation().await()
            currentArtLocation = location
            val staticImagePath = repository.fetchStaticMapImageAsync(
                longitude = location.longitude,
                latitude = location.latitude,
                mapWidth = canvasToDrawArtWidth - (2 * MartpSketch.frameThickness).toInt() - (2 * MartpSketch.framePadding).toInt(),
                mapHeight = canvasToDrawArtHeight - (2 * MartpSketch.frameThickness).toInt() - (2 * MartpSketch.framePadding).toInt(),
                dir = directory
            ).await()
            emitNewState(CreateNewMapArtUiState.ImageDownloaded(staticImagePath))
        } catch (failure: Failure) {
            emitNewState(CreateNewMapArtUiState.Error(failure))
        }
    }

    suspend fun saveArtToLocalDatabase(
        title: String,
        description: String?,
        newArtBitMap: Bitmap,
        directory: File
    ) {
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

            repository.saveMapArtEntity(newMartp).await()

            emitNewState(
                CreateNewMapArtUiState.ArtCreatedSuccessfully(
                    pathToStoreArtImage = newMartp.imagePathLocation
                )
            )
        } catch (failure: Failure) {
            emitNewState(CreateNewMapArtUiState.Error(failure))
        } catch (e: Exception) {
            Log.i("CreateNewMapArtViewMode", e.message ?: "")
        }
    }

    fun handleActionButtonState(
        titleCurrentText: String
    ) {
        if(titleCurrentText.length > 2) {
            emitNewState(CreateNewMapArtUiState.EnableActionButton)
        } else {
            emitNewState(CreateNewMapArtUiState.DisableActionButton)
        }
    }

    fun getRandomInputHintsIds(): CreateNewMapArtInputsHint {
        val titleHints = arrayListOf(
            R.string.art_title_hint_city_center, R.string.art_title_hint_grandma_house, R.string.art_title_hint_botanical_garden
        )
        val descriptionHints = arrayListOf(
            R.string.art_description_hint_beautiful_place, R.string.art_description_hint_special_place, R.string.art_description_hint_crazy_streets
        )

        return CreateNewMapArtInputsHint(
            titleInputId = titleHints.random(),
            descriptionInputId = descriptionHints.random()
        )
    }
}