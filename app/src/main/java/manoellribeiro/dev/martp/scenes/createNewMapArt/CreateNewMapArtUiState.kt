package manoellribeiro.dev.martp.scenes.createNewMapArt

import manoellribeiro.dev.martp.core.models.failures.Failure

sealed interface CreateNewMapArtUiState {

    object Loading: CreateNewMapArtUiState
    class Error(val failure: Failure): CreateNewMapArtUiState
    class ImageDownloaded(val imagePath: String): CreateNewMapArtUiState

}