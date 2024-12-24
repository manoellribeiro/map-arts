package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.graphics.Bitmap
import android.health.connect.datatypes.HeightRecord
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import manoellribeiro.dev.martp.core.data.local.MartpDatabase
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.extensions.returnIfNull
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.services.LocationService
import manoellribeiro.dev.martp.core.sketches.DefaultMartpSketch
import manoellribeiro.dev.martp.scenes.gallery.GalleryUiState
import java.io.File
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateNewMapArtViewModel @Inject constructor(
    private val repository: MartpRepository,
    private val locationService: LocationService,
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
                mapWidth = canvasToDrawArtWidth - (2 * DefaultMartpSketch.frameThickness).toInt() - (2 * DefaultMartpSketch.framePadding).toInt(),
                mapHeight = canvasToDrawArtHeight - (2 * DefaultMartpSketch.frameThickness).toInt() - (2 * DefaultMartpSketch.framePadding).toInt(),
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
                latitude = currentArtLocation?.latitude?.toFloat().returnIfNull { 0F },
                longitude = currentArtLocation?.longitude?.toFloat().returnIfNull { 0F },
                dateInMillis = Calendar.getInstance().timeInMillis,
                imagePathLocation = directory.path + MartpDatabase.artsDirectoryName + artId + ".png"
            )
            repository.saveNewArtBitMap(
                newArtBitMap,
                newMartp.imagePathLocation,
                directory.path + MartpDatabase.artsDirectoryName
            ).await()

            repository.saveMapArtEntity(newMartp).await()

            emitNewState(CreateNewMapArtUiState.ArtCreatedSuccessfully)
        } catch (failure: Failure) {
            emitNewState(CreateNewMapArtUiState.Error(failure))
        } catch (e: Exception) {
            Log.i("CreateNewMapArtViewMode", e.message ?: "")
        }
    }

}