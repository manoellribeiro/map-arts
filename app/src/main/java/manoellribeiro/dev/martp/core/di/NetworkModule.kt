package manoellribeiro.dev.martp.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    fun providesMapboxApiService(): MapboxApiService {
        return MapboxApiService()
    }

}