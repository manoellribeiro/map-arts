package manoellribeiro.dev.martp.scenes.createNewMapArt

sealed interface CreateNewMapArtEvent {
    data object ArtCreatedSuccessfully: CreateNewMapArtEvent
}