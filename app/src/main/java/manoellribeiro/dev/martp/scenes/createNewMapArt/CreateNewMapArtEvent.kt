package manoellribeiro.dev.martp.scenes.createNewMapArt

sealed interface CreateNewMapArtEvent {
    data class ArtCreatedSuccessfully(val imagePathLocation: String): CreateNewMapArtEvent
}
