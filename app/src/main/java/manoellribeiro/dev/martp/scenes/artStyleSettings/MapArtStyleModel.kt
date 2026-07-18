package manoellribeiro.dev.martp.scenes.artStyleSettings

import manoellribeiro.dev.martp.core.models.failures.SketchArtType

data class MapArtStyleModel(
    val titleId: Int,
    val descriptionId: Int,
    val drawableImageId: Int,
    val sketchArtType: SketchArtType
)
