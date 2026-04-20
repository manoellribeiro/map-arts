package manoellribeiro.dev.martp.scenes.artStyleSettings

sealed interface ArtStyleSettingsUiState {

    object Loading: ArtStyleSettingsUiState
    data class SettingsLoaded(val mapZoom: Float): ArtStyleSettingsUiState

}