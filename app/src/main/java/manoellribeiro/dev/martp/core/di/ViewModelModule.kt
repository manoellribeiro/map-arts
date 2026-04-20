package manoellribeiro.dev.martp.core.di

import android.content.Context
import android.location.Geocoder
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.ai.GenerativeModel
import dagger.Binds
import dagger.Component.Factory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ActivityContext
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
import manoellribeiro.dev.martp.core.data.local.daos.UserInfoDao
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.services.ConnectivityService
import manoellribeiro.dev.martp.core.services.GenerateAIContentService
import manoellribeiro.dev.martp.core.services.GetAddressService
import manoellribeiro.dev.martp.core.services.LocationService
import manoellribeiro.dev.martp.core.utils.PromptGenerator

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {

    @Provides
    fun bindsMartpRepository(
        mapArtsDao: MapArtsDao,
        userInfoDao: UserInfoDao,
        mapboxApiService: MapboxApiService,
        connectivityService: ConnectivityService,
        artSettingsDataStore: DataStore<Preferences>
    ): MartpRepository {
        return MartpRepository(
            mapArtDao = mapArtsDao,
            connectivityService = connectivityService,
            mapboxApiService = mapboxApiService,
            userInfoDao = userInfoDao,
            artSettingsDataSore = artSettingsDataStore
        )
    }

    @Provides
    fun providesLocationService(fusedLocationProviderClient: FusedLocationProviderClient): LocationService {
        return LocationService(fusedLocationProviderClient)
    }

    @Provides
    fun providesGetAddressService(geocoder: Geocoder): GetAddressService {
        return GetAddressService(geocoder)
    }

    @Provides
    fun providesGenerateAIContentService(generativeModel: GenerativeModel): GenerateAIContentService {
        return GenerateAIContentService(generativeModel)
    }

    @Provides
    fun providesPromptGenerator(): PromptGenerator {
        return PromptGenerator()
    }

}