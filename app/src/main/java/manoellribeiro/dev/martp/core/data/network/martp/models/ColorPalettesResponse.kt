package manoellribeiro.dev.martp.core.data.network.martp.models

import com.google.gson.annotations.SerializedName

data class ColorPalettesResponse(
    @SerializedName("palettes")
    val palettes: List<ColorPaletteResponse>
)

data class ColorPaletteResponse(
    @SerializedName("name")
    val name: String,
    @SerializedName("colors")
    val colors: List<String>
)
