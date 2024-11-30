package manoellribeiro.dev.martp.scenes.gallery

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.services.LocationService
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: MartpRepository,
    private val locationService: LocationService
): ViewModel() {

    private val _state: MutableLiveData<GalleryUiState> = MutableLiveData<GalleryUiState>()
    val state: LiveData<GalleryUiState> = _state

    private fun emitNewState(newState: GalleryUiState) {
        _state.value = newState
    }

    suspend fun printLocationData() {
        val location = locationService.getCurrentLocation().await()
        Log.i("GalleryViewModel", "latitude: ${location.latitude} \n longitude: ${location.longitude}")
    }

    fun getUserMapArts() = viewModelScope.launch {
        try {
            emitNewState(GalleryUiState.Loading)
            val mapArts = repository.fetchUserMapArts().await()
            if(mapArts.isNotEmpty()) {
                emitNewState(GalleryUiState.NotEmptyList(mapArts))
            } else {
                emitNewState(GalleryUiState.EmptyList)
            }
        } catch (failure: Failure) {
            emitNewState(GalleryUiState.Error(failure))
        }
    }
}