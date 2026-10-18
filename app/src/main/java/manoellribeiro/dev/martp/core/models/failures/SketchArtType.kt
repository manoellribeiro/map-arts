package manoellribeiro.dev.martp.core.models.failures

import manoellribeiro.dev.martp.core.data.network.mapbox.models.MapboxMapStyle

enum class SketchArtType(
    val mapBoxMapStyle: MapboxMapStyle,
    val minZoom: Float,
    val maxZoom: Float,
) {
    DEFAULT(MapboxMapStyle.DarkV11, minZoom = 14F, maxZoom = 15F),
    POINTILLISM(MapboxMapStyle.DarkV11, minZoom = 14F, maxZoom = 15F),
    COLORFUL_MAP(MapboxMapStyle.ColorfulMap, minZoom = 16F, maxZoom = 17F),
    WATER_FLOW(MapboxMapStyle.WaterFlow, minZoom = 14F, maxZoom = 15F), //Working in progress
    PARTICLE_SYSTEM(MapboxMapStyle.ColorfulMap, minZoom = 14F, maxZoom = 15F), // Working in progress
    //GEO_REALISTIC(MapboxMapStyle.SatelliteV9) //TODO: Mapbox does not support it yet and I did not find another service with something similar
    ;

    // Slider position goes from 0 (far, minZoom) to 1 (close, maxZoom)
    fun zoomToSliderPosition(zoom: Float): Float {
        val position = (zoom - minZoom) / (maxZoom - minZoom)
        return position.coerceIn(0F, 1F)
    }

    fun sliderPositionToZoom(sliderPosition: Float): Float {
        return minZoom + sliderPosition * (maxZoom - minZoom)
    }
}
