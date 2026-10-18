package manoellribeiro.dev.martp.core.services

import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import manoellribeiro.dev.martp.core.di.IoDispatcher
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.models.failures.NoInternetConnectionFailure
import kotlin.coroutines.resume
import javax.inject.Inject
import kotlin.coroutines.resumeWithException


class GetAddressService @Inject constructor  (
    private val geocoder: Geocoder,
    private val connectivityService: ConnectivityService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val sdkVersionService: SdkVersionService,
    private val errorLogger: ErrorLogger
) {
    suspend fun getAddress(
        latitude: Double,
        longitude: Double
    ): Address? {
        try {
            if(!connectivityService.isInternetConnected()) {
                throw NoInternetConnectionFailure(originalExceptionMessage = null)
            }
            if(!Geocoder.isPresent()) {
                return null
            }
            return if(sdkVersionService.isAtLeastTiramisu()) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1, object :
                        Geocoder.GeocodeListener {
                        override fun onError(errorMessage: String?) {
                            errorLogger.logNonFatal(
                                IllegalStateException("Geocoder error: $errorMessage"),
                                "GetAddressService.getAddress"
                            )
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
        } catch (failure: Failure) {
            throw failure
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "GetAddressService.getAddress")
            return null
        }
    }
}