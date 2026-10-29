package manoellribeiro.dev.martp.core.data.local.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import manoellribeiro.dev.martp.core.models.MapLayersColors
import manoellribeiro.dev.martp.core.models.SketchArtType

@Entity(tableName = "map_arts")
data class MapArtEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?, //description can be null and I can show the date instead of showing it on card
    val latitude: Float,
    val longitude: Float,
    val dateInMillis: Long,
    val imagePathLocation: String,
    val sketchArtType: SketchArtType? = null,
    val colorPalette: String? = null,
    @Embedded(prefix = "map_layer_")
    val colorsLayers: MapLayersColors? = null
)
