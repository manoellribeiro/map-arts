package manoellribeiro.dev.martp.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import manoellribeiro.dev.martp.core.data.network.martp.models.RemoteColorPalette
import manoellribeiro.dev.martp.core.extensions.hexToColorInt

@Entity(tableName = "color_palettes")
data class ColorPaletteEntity(
    @PrimaryKey
    val name: String,
    val colors: List<Int>,
) {
    constructor(remoteColorPalette: RemoteColorPalette) : this(
        name = remoteColorPalette.name,
        colors = remoteColorPalette.colors.map { it.hexToColorInt() }
    )
}
