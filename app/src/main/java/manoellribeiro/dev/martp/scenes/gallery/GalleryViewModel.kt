package manoellribeiro.dev.martp.scenes.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.Failure

class GalleryViewModel(
    private val repository: MartpRepository
): ViewModel() {

    private val _state: MutableLiveData<GalleryUiState> = MutableLiveData<GalleryUiState>()
    val state: LiveData<GalleryUiState> = _state

    private fun emitNewState(newState: GalleryUiState) {
        _state.value = newState
    }

    fun getUserMapArtsLiveData() = viewModelScope.launch {
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