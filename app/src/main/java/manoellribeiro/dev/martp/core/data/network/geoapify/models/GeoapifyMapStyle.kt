package manoellribeiro.dev.martp.core.data.network.geoapify.models

sealed class GeoapifyMapStyle(val id: String) {
    object DarkV11: GeoapifyMapStyle("cmnxj3x4g000501sm6i905bl6")
    object SatelliteV9: GeoapifyMapStyle("cmo8uy9zp007401s80fro24ex")
}
