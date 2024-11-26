package manoellribeiro.dev.martp.scenes.gallery

import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.models.failures.Failure

sealed interface GalleryUiState {
    object Loading: GalleryUiState
    data class Error(val failure: Failure): GalleryUiState
    object EmptyList: GalleryUiState
    data class NotEmptyList(val mapArts: List<MapArtEntity>): GalleryUiState
}