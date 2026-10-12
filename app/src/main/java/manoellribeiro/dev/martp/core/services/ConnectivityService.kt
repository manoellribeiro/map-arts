package manoellribeiro.dev.martp.core.services

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import javax.inject.Inject

data class ConnectivityService @Inject constructor(
    private val connectivityManager: ConnectivityManager
) {

    fun isInternetConnected(): Boolean {
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(
            networkCapabilities
        ) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}