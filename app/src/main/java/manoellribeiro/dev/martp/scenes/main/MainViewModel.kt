package manoellribeiro.dev.martp.scenes.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.scenes.gallery.GalleryUiState
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MartpRepository,
): ViewModel() {

    private val _galleryState: MutableLiveData<GalleryUiState> = MutableLiveData<GalleryUiState>()
    val galleryState: LiveData<GalleryUiState> = _galleryState

    private fun emitNewGalleryState(newState: GalleryUiState) {
        _galleryState.value = newState
    }

    fun getUserMapArts() = viewModelScope.launch {
        try {
            emitNewGalleryState(GalleryUiState.Loading)
            val mapArts = repository.fetchUserMapArts().await()
            if(mapArts.isNotEmpty()) {
                emitNewGalleryState(GalleryUiState.NotEmptyList(mapArts))
            } else {
                emitNewGalleryState(GalleryUiState.EmptyList)
            }
        } catch (failure: Failure) {
            emitNewGalleryState(GalleryUiState.Error(failure))
        }
    }
}