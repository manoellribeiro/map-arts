package manoellribeiro.dev.martp.core.services

import android.location.Location
import android.location.LocationManager
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.models.failures.LocationDisabledFailure
import manoellribeiro.dev.martp.core.models.failures.LocationPermissionNotGrantedFailure
import manoellribeiro.dev.martp.core.models.failures.UnknownErrorFailure
import java.util.concurrent.Executor

class PlatformLocationService(
    private val locationManager: LocationManager,
    private val errorLogger: ErrorLogger,
    private val callbackExecutor: Executor = Executor { it.run() }
) : LocationService {

    override suspend fun getCurrentLocation(): Location {
        val firstLocation = CompletableDeferred<Location>()
        val listener = LocationListenerCompat { location -> firstLocation.complete(location) }
        try {
            if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
                throw LocationDisabledFailure(originalExceptionMessage = "Location is disabled on the device")
            }
            val providers = usableEnabledProviders()
            if (providers.isEmpty()) {
                throw LocationDisabledFailure(
                    originalExceptionMessage = "No usable location provider is enabled"
                ).also { errorLogger.logNonFatal(it, "PlatformLocationService.getCurrentLocation") }
            }

            providers.forEach { provider ->
                LocationManagerCompat.requestLocationUpdates(
                    locationManager,
                    provider,
                    locationRequest,
                    callbackExecutor,
                    listener
                )
            }
            return withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) { firstLocation.await() }
                ?: throw LocationDisabledFailure(originalExceptionMessage = null).also {
                    errorLogger.logNonFatal(
                        IllegalStateException("No location from $providers in $LOCATION_TIMEOUT_MILLIS ms"),
                        "PlatformLocationService.getCurrentLocation"
                    )
                }
        } catch (e: CancellationException) {
            throw e
        } catch (failure: Failure) {
            throw failure
        } catch (e: SecurityException) {
            errorLogger.logNonFatal(e, "PlatformLocationService.getCurrentLocation")
            throw LocationPermissionNotGrantedFailure(originalExceptionMessage = e.message)
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "PlatformLocationService.getCurrentLocation")
            throw UnknownErrorFailure(originalExceptionMessage = e.message)
        } finally {
            LocationManagerCompat.removeUpdates(locationManager, listener)
        }
    }

    private fun usableEnabledProviders(): List<String> {
        val enabledProviders = locationManager.getProviders(true)
        return USABLE_PROVIDERS.filter { it in enabledProviders }
    }

    companion object {
        // GPS without Google's assisted data can take minutes on its first fix
        private const val LOCATION_TIMEOUT_MILLIS = 120_000L
        // LocationManager.FUSED_PROVIDER, only listed on API 31+
        private const val PLATFORM_FUSED_PROVIDER = "fused"
        private val USABLE_PROVIDERS = listOf(
            PLATFORM_FUSED_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER
        )
        private val locationRequest = LocationRequestCompat.Builder(0L)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
            .build()
    }
}
