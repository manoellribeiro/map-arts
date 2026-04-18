package manoellribeiro.dev.martp.core.data.network.mapbox.models

sealed class MapboxMapStyle(val id: String) {
    object DarkV11: MapboxMapStyle("cmnxj3x4g000501sm6i905bl6")
    object SatelliteV9: MapboxMapStyle("satellite-v9")
}
