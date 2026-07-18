package manoellribeiro.dev.martp.core.data.network.geoapify.models

import com.google.gson.annotations.SerializedName

data class GeoapifyStaticMapImageResponse(
    @SerializedName("imgUrl")
    val imageUrl: String
)