package manoellribeiro.dev.martp.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "color_palettes")
data class ColorPaletteEntity(
    @PrimaryKey
    val name: String,
    val colors: List<Int>,
    val position: Int
)
