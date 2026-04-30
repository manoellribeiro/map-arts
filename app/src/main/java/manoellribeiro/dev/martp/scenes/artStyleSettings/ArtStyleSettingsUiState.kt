package manoellribeiro.dev.martp.scenes.artStyleSettings

import manoellribeiro.dev.martp.core.models.failures.SketchArtType

sealed interface ArtStyleSettingsUiState {

    object Loading: ArtStyleSettingsUiState
    data class SettingsLoaded(
        val mapZoom: Float,
        val mapArtStyle: SketchArtType
    ): ArtStyleSettingsUiState

}