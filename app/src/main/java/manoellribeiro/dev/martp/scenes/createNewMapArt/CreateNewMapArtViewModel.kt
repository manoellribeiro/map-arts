package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.health.connect.datatypes.HeightRecord
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.services.LocationService
import manoellribeiro.dev.martp.core.sketches.DefaultMartpSketch
import manoellribeiro.dev.martp.scenes.gallery.GalleryUiState
import java.io.File
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

    suspend fun startToGenerateMapArt(
        directory: File,
        canvasToDrawArtWidth: Int,
        canvasToDrawArtHeight: Int
    ) {
        try {
            emitNewState(CreateNewMapArtUiState.Loading)
            val location = locationService.getCurrentLocation().await()
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
}