package manoellribeiro.dev.martp.core.services

import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import manoellribeiro.dev.martp.core.di.IoDispatcher
import kotlin.coroutines.resume
import javax.inject.Inject


class GetAddressService @Inject constructor  (
    private val geocoder: Geocoder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun getAddress(
        latitude: Double,
        longitude: Double
    ): Address? {
        try {
            if(!Geocoder.isPresent()) {
                return null
            }
            return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
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
                }
            } else {
                withContext(ioDispatcher) {
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if(addresses.isNullOrEmpty()) {
                        null
                    } else {
                        addresses.first()
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
    }
}