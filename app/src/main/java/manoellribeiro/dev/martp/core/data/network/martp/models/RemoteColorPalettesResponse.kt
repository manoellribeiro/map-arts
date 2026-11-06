package manoellribeiro.dev.martp.core.data.network.martp.models

import com.google.gson.annotations.SerializedName

data class RemoteColorPalettesResponse(
    @SerializedName("palettes")
    val palettes: List<RemoteColorPalette>
)

data class RemoteColorPalette(
    @SerializedName("name")
    val name: String,
    @SerializedName("colors")
    val colors: List<String>,
)
