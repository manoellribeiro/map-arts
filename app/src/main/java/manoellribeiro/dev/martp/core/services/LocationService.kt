package manoellribeiro.dev.martp.core.services

import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.models.failures.LocationDisabledFailure
import manoellribeiro.dev.martp.core.models.failures.LocationPermissionNotGrantedFailure
import manoellribeiro.dev.martp.core.models.failures.UnknownErrorFailure
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationService @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) {

    fun getCurrentLocation(): Deferred<Location> {
        try {
            return CoroutineScope(Dispatchers.IO).async {
                val result = suspendCancellableCoroutine<Location> { continuation ->
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        CancellationTokenSource().token
                    ).addOnCompleteListener {
                        if(it.isSuccessful) {
                            if(it.result !== null) {
                                continuation.resume(it.result)
                            } else {
                                continuation.resumeWithException(
                                    LocationDisabledFailure(
                                        originalExceptionMessage = it.exception?.message
                                    )
                                )
                            }
                        } else {
                            continuation.resumeWithException(
                                LocationPermissionNotGrantedFailure(
                                    originalExceptionMessage = it.exception?.message
                                )
                            )
                        }
                    }
                }
                return@async result
            }
        } catch (e: Exception) {
            throw UnknownErrorFailure(
                originalExceptionMessage = e.message
            )
        } catch (e: SecurityException) {
            throw LocationPermissionNotGrantedFailure(
                originalExceptionMessage = e.message
            )
        } catch (failure: Failure) {
            throw failure
        }
    }
}