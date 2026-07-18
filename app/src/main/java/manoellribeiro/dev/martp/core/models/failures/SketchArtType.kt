package manoellribeiro.dev.martp.core.models.failures

import manoellribeiro.dev.martp.core.data.network.mapbox.models.MapboxMapStyle

enum class SketchArtType(val mapBoxMapStyle: MapboxMapStyle) {
    DEFAULT(MapboxMapStyle.DarkV11),
    POINTILLISM(MapboxMapStyle.DarkV11),
    //GEO_REALISTIC(MapboxMapStyle.SatelliteV9) //TODO: Mapbox does not support it yet and I did not find another service with something similar
}