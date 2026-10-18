package manoellribeiro.dev.martp.scenes.createNewMapArt

import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.models.SketchArtType

sealed interface CreateNewMapArtUiState {

    object Idle: CreateNewMapArtUiState
    data class Loading(val loadingMessageId: Int): CreateNewMapArtUiState
    object ActionButtonLoading: CreateNewMapArtUiState
    class Error(val failure: Failure): CreateNewMapArtUiState
    class ImageDownloaded(
        val staticMapImagePath: String,
        val title: String,
        val sketchArtType: SketchArtType,
        val latitude: Double,
        val longitude: Double,
        val colorPalette: String?
    ): CreateNewMapArtUiState
}