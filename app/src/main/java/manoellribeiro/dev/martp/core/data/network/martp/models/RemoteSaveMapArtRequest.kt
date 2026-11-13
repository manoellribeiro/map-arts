package manoellribeiro.dev.martp.core.data.network.martp.models

import com.google.gson.annotations.SerializedName

data class RemoteSaveMapArtRequest(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("dateInMillis")
    val dateInMillis: Long,
    @SerializedName("sketchArtType")
    val sketchArtType: String? = null,
    @SerializedName("colorPalette")
    val colorPalette: String? = null,
    @SerializedName("colorsLayers")
    val colorsLayers: RemoteMapLayersColors? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("username")
    val username: String? = null,
)
data class RemoteMapLayersColors(
    @SerializedName("roadsColor")
    val roadsColor: String? = null,
    @SerializedName("buildingStrokeColor")
    val buildingStrokeColor: String? = null,
    @SerializedName("buildingColor")
    val buildingColor: String? = null,
    @SerializedName("roadsPathColor")
    val roadsPathColor: String? = null,
    @SerializedName("waterColor")
    val waterColor: String? = null,
    @SerializedName("landOthersColor")
    val landOthersColor: String? = null,
)
