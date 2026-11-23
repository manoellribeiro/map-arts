package manoellribeiro.dev.martp.core.di

import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.ai.GenerativeModel
import dagger.Binds
import dagger.Component.Factory
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import manoellribeiro.dev.martp.core.data.local.daos.ColorPalettesDao
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
import manoellribeiro.dev.martp.core.data.local.daos.UserInfoDao
import manoellribeiro.dev.martp.core.data.network.geoapify.GeoapifyApiService
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.data.network.martp.MartpApiService
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.services.ConnectivityService
import manoellribeiro.dev.martp.core.services.FusedLocationService
import manoellribeiro.dev.martp.core.services.GenerateAIContentService
import manoellribeiro.dev.martp.core.services.GetAddressService
import manoellribeiro.dev.martp.core.services.LocationService
import manoellribeiro.dev.martp.core.services.PlatformLocationService
import manoellribeiro.dev.martp.core.services.SdkVersionService
import manoellribeiro.dev.martp.core.utils.PromptGenerator

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {

    @Provides
    fun bindsMartpRepository(
        mapArtsDao: MapArtsDao,
        userInfoDao: UserInfoDao,
        colorPalettesDao: ColorPalettesDao,
        mapboxApiService: MapboxApiService,
        geoapifyApiService: GeoapifyApiService,
        martpApiService: MartpApiService,
        connectivityService: ConnectivityService,
        artSettingsDataStore: DataStore<Preferences>,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @ApplicationScope applicationScope: CoroutineScope,
        errorLogger: ErrorLogger
    ): MartpRepository {
        return MartpRepository(
            mapArtDao = mapArtsDao,
            connectivityService = connectivityService,
            mapboxApiService = mapboxApiService,
            userInfoDao = userInfoDao,
            artSettingsDataSore = artSettingsDataStore,
            geoapifyApiService = geoapifyApiService,
            ioDispatcher = ioDispatcher,
            applicationScope = applicationScope,
            errorLogger = errorLogger,
            martpApiService = martpApiService,
            colorPalettesDao = colorPalettesDao
        )
    }

    @Provides
    fun providesLocationService(
        @ApplicationContext context: Context,
        fusedLocationProviderClient: Lazy<FusedLocationProviderClient>,
        locationManager: LocationManager,
        errorLogger: ErrorLogger
    ): LocationService {
        val isGooglePlayServicesAvailable = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        return if (isGooglePlayServicesAvailable) {
            FusedLocationService(fusedLocationProviderClient.get(), errorLogger)
        } else {
            PlatformLocationService(locationManager, errorLogger)
        }
    }

    @Provides
    fun providesGetAddressService(
        geocoder: Geocoder,
        connectivityService: ConnectivityService,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        sdkVersionService: SdkVersionService,
        errorLogger: ErrorLogger
    ): GetAddressService {
        return GetAddressService(geocoder, connectivityService, ioDispatcher, sdkVersionService, errorLogger)
    }

    @Provides
    fun providesSdkVersionService(): SdkVersionService {
        return SdkVersionService()
    }

    @Provides
    fun providesGenerateAIContentService(
        generativeModel: GenerativeModel,
        errorLogger: ErrorLogger
    ): GenerateAIContentService {
        return GenerateAIContentService(generativeModel, errorLogger)
    }

    @Provides
    fun providesPromptGenerator(): PromptGenerator {
        return PromptGenerator()
    }

}