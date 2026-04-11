package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.location.Address
import manoellribeiro.dev.martp.core.models.failures.Failure

sealed interface CreateNewMapArtUiState {

    object LoadingAIText: CreateNewMapArtUiState
    class GeneratedAIText(val text: String): CreateNewMapArtUiState
    object ErrorGeneratingAIText: CreateNewMapArtUiState

    object Loading: CreateNewMapArtUiState
    object ActionButtonLoading: CreateNewMapArtUiState
    class Error(val failure: Failure): CreateNewMapArtUiState
    class ImageDownloaded(
        val staticMapImagePath: String,
        val title: String,
        val aiDescription: String
    ): CreateNewMapArtUiState
    class ArtCreatedSuccessfully(val pathToStoreArtImage: String): CreateNewMapArtUiState
}