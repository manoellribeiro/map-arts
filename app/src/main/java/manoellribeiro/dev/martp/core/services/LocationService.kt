package manoellribeiro.dev.martp.core.services

import android.location.Location

interface LocationService {
    suspend fun getCurrentLocation(): Location
}
