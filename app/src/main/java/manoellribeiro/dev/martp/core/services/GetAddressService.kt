package manoellribeiro.dev.martp.core.services

import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import manoellribeiro.dev.martp.core.models.failures.NoInternetConnectionFailure
import kotlin.coroutines.resume
import javax.inject.Inject
import kotlin.coroutines.resumeWithException


class GetAddressService @Inject constructor  (
    private val geocoder: Geocoder,
    private val connectivityService: ConnectivityService
) {
    fun getAddress(
        latitude: Double,
        longitude: Double
    ): Deferred<Address?> {
        try {
            return CoroutineScope(Dispatchers.IO).async {
                val result = suspendCancellableCoroutine { continuation ->
                    if(connectivityService.isInternetConnected()) {
                        if(Geocoder.isPresent()) {
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                geocoder.getFromLocation(latitude, longitude, 1, object :
                                    Geocoder.GeocodeListener {
                                    override fun onError(errorMessage: String?) {
                                        continuation.resume(null)
                                    }
                                    override fun onGeocode(addresses: List<Address?>) {
                                        if(addresses.isEmpty()) {
                                            continuation.resume(null)
                                        } else {
                                            val address = addresses.first()
                                            continuation.resume(address)
                                        }
                                    }
                                })
                            } else {
                                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                                if(addresses.isNullOrEmpty()) {
                                    continuation.resume(null)
                                } else {
                                    continuation.resume(addresses.first())
                                }
                            }
                        } else {
                            continuation.resume(null)
                        }
                    } else {
                        continuation.resumeWithException(NoInternetConnectionFailure(originalExceptionMessage = null))
                    }
                }
                return@async result
            }
        } catch (e: Exception) {
            return CompletableDeferred(null)
        }
    }
}