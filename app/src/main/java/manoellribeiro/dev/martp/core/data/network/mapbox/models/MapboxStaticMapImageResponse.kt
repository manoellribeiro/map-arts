package manoellribeiro.dev.martp.core.data.network.mapbox.models

import com.google.gson.annotations.SerializedName

data class MapboxStaticMapImageResponse(
    @SerializedName("imgUrl")
    val imageUrl: String
)