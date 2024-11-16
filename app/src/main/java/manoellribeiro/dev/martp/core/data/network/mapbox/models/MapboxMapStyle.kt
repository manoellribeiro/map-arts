package manoellribeiro.dev.martp.core.data.network.mapbox.models

sealed class MapboxMapStyle(val id: String) {
    object DarkV11: MapboxMapStyle("dark-v11")
}
