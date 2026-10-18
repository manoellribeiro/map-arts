package manoellribeiro.dev.martp.core.services

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.models.failures.LocationDisabledFailure
import manoellribeiro.dev.martp.core.models.failures.LocationPermissionNotGrantedFailure
import manoellribeiro.dev.martp.core.models.failures.UnknownErrorFailure
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationService @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val errorLogger: ErrorLogger
) {

    suspend fun getCurrentLocation(): Location {
        try {
            return suspendCancellableCoroutine { continuation ->
                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).addOnCompleteListener {
                    if(it.isSuccessful) {
                        if(it.result !== null) {
                            continuation.resume(it.result)
                        } else {
                            errorLogger.logNonFatal(
                                IllegalStateException("Location task succeeded without a location"),
                                "LocationService.getCurrentLocation"
                            )
                            continuation.resumeWithException(
                                LocationDisabledFailure(
                                    originalExceptionMessage = it.exception?.message
                                )
                            )
                        }
                    } else {
                        errorLogger.logNonFatal(
                            it.exception ?: IllegalStateException("Location task failed without an exception"),
                            "LocationService.getCurrentLocation"
                        )
                        continuation.resumeWithException(
                            LocationPermissionNotGrantedFailure(
                                originalExceptionMessage = it.exception?.message
                            )
                        )
                    }
                }
                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (failure: Failure) {
            throw failure
        } catch (e: SecurityException) {
            errorLogger.logNonFatal(e, "LocationService.getCurrentLocation")
            throw LocationPermissionNotGrantedFailure(
                originalExceptionMessage = e.message
            )
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "LocationService.getCurrentLocation")
            throw UnknownErrorFailure(
                originalExceptionMessage = e.message
            )
        }
    }
}