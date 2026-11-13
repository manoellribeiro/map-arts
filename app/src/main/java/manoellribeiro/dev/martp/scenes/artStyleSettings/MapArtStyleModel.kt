package manoellribeiro.dev.martp.scenes.artStyleSettings

import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.models.SketchArtType

data class MapArtStyleModel(
    val titleId: Int,
    val descriptionId: Int,
    val drawableImageId: Int,
    val sketchArtType: SketchArtType
)

val artStyleModels = listOf(
    MapArtStyleModel(
        titleId = R.string.colorful_map,
        descriptionId = R.string.colorful_map_description,
        drawableImageId = R.drawable.ic_colorful_map,
        sketchArtType = SketchArtType.COLORFUL_MAP
    ),
    MapArtStyleModel(
        titleId = R.string.classic,
        descriptionId = R.string.classic_description,
        drawableImageId = R.drawable.ic_martp_logo,
        sketchArtType = SketchArtType.DEFAULT
    ),
    MapArtStyleModel(
        titleId = R.string.pointillism,
        descriptionId = R.string.pointillism_colorful_description,
        drawableImageId = R.drawable.ic_pointllism,
        sketchArtType = SketchArtType.POINTILLISM
    ),
    //Still work in progress
//    MapArtStyleModel(
//        titleId = R.string.water_flow,
//        descriptionId = R.string.water_flow_description,
//        drawableImageId = R.drawable.ic_water_flow,
//        sketchArtType = SketchArtType.WATER_FLOW
//    ),
    //Still work in progress
//    MapArtStyleModel(
//        titleId = R.string.walking_people,
//        descriptionId = R.string.walking_people_description,
//        drawableImageId = R.drawable.ic_walking_people,
//        sketchArtType = SketchArtType.PARTICLE_SYSTEM
//    ),
)
