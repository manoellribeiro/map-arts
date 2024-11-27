package manoellribeiro.dev.martp.core.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Component.Factory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ActivityContext
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {

    @Provides
    fun bindsMartpRepository(
        mapArtsDao: MapArtsDao,
        mapboxApiService: MapboxApiService
    ): MartpRepository {
        return MartpRepository(
            mapArtDao = mapArtsDao,
            mapboxApiService = mapboxApiService
        )
    }

    @Provides
    fun provideFusedLocationProviderClient(
        @ActivityContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }


}