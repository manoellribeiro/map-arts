package manoellribeiro.dev.martp.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.CoroutineScope
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.services.GenerateAIContentService
import manoellribeiro.dev.martp.core.services.GetAddressService
import manoellribeiro.dev.martp.core.services.LocationService
import manoellribeiro.dev.martp.core.services.MapArtPrinterService
import manoellribeiro.dev.martp.core.utils.PromptGenerator
import manoellribeiro.dev.martp.scenes.createNewMapArt.CreateNewMapArtViewModel
import manoellribeiro.dev.martp.scenes.main.MainViewModel

@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

    @Provides
    fun providesMainViewModel(
        repository: MartpRepository,
        mapArtPrinterService: MapArtPrinterService,
        @ApplicationScope applicationScope: CoroutineScope
    ): MainViewModel {
        return MainViewModel(
            repository = repository,
            mapArtPrinterService = mapArtPrinterService,
            applicationScope = applicationScope
        )
    }

    @Provides
    fun providesCreateNewMapArtViewModel(
        repository: MartpRepository,
        locationService: LocationService,
        getAddressService: GetAddressService,
        generateAIContentService: GenerateAIContentService,
        promptGenerator: PromptGenerator,
        errorLogger: ErrorLogger,
        mapArtPrinterService: MapArtPrinterService
    ): CreateNewMapArtViewModel {
        return CreateNewMapArtViewModel(
            repository,
            locationService,
            getAddressService,
            generateAIContentService,
            promptGenerator,
            errorLogger,
            mapArtPrinterService
        )
    }


}