package manoellribeiro.dev.martp.scenes.artStyleSettings

import manoellribeiro.dev.martp.core.data.local.entities.ColorPaletteEntity
import manoellribeiro.dev.martp.core.models.SketchArtType

sealed interface ArtStyleSettingsUiState {

    object Loading: ArtStyleSettingsUiState
    data class Error(val messageId: Int): ArtStyleSettingsUiState
    data class SettingsLoaded(
        val mapZoom: Float,
        val mapArtStyle: SketchArtType,
        val colorsPalettes: List<ColorPaletteEntity>,
        val colorPaletteSelected: String
    ): ArtStyleSettingsUiState

}