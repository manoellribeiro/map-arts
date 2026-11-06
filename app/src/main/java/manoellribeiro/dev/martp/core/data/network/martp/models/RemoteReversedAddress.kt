package manoellribeiro.dev.martp.core.data.network.martp.models

import com.google.gson.annotations.SerializedName

data class RemoteReversedAddress(
    @SerializedName("street")
    val street: String?,
    @SerializedName("city")
    val city: String?,
    @SerializedName("country")
    val country: String?
)
