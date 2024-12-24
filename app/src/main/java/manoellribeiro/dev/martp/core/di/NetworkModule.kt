package manoellribeiro.dev.martp.core.di

import android.net.ConnectivityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.services.ConnectivityService

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    fun providesMapboxApiService(): MapboxApiService {
        return MapboxApiService()
    }

    @Provides
    fun providesConnectivityService(
        connectivityManager: ConnectivityManager
    ): ConnectivityService {
        return ConnectivityService(connectivityManager)
    }

}