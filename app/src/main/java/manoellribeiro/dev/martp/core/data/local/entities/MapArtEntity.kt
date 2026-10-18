package manoellribeiro.dev.martp.core.data.local.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import manoellribeiro.dev.martp.core.models.MapLayersColors
import manoellribeiro.dev.martp.core.models.SketchArtType

@Entity(tableName = "map_arts")
data class MapArtEntity(
    //old attributes that keeps
    @PrimaryKey
    val id: String,
    val title: String,
    val latitude: Float,
    val longitude: Float,
    val dateInMillis: Long,
    val imagePathLocation: String = "",

    //I don't use it anymore, but room only support drop columns with API 34+
    val description: String? = null,

    //new attributes
    val originalMapImageLocation: String?,
    val sketchArtType: SketchArtType?,
    val colorPalette: String?,
    @Embedded(prefix = "map_layer_")
    val colorsLayers: MapLayersColors?


)