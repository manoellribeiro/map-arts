package manoellribeiro.dev.martp.scenes.createNewMapArt

sealed interface CreateNewMapArtEvent {
    class ArtCreatedSuccessfully(val pathToStoreArtImage: String): CreateNewMapArtEvent
}