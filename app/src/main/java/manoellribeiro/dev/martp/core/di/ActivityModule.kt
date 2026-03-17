package manoellribeiro.dev.martp.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.services.GenerateAIContentService
import manoellribeiro.dev.martp.core.services.GetAddressService
import manoellribeiro.dev.martp.core.services.LocationService
import manoellribeiro.dev.martp.core.utils.PromptGenerator
import manoellribeiro.dev.martp.scenes.createNewMapArt.CreateNewMapArtViewModel
import manoellribeiro.dev.martp.scenes.gallery.GalleryViewModel

@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

    @Provides
    fun providesGalleryViewModel(
        repository: MartpRepository,
    ): GalleryViewModel {
        return GalleryViewModel(
            repository = repository,
        )
    }

    @Provides
    fun providesCreateNewMapArtViewModel(
        repository: MartpRepository,
        locationService: LocationService,
        getAddressService: GetAddressService,
        generateAIContentService: GenerateAIContentService,
        promptGenerator: PromptGenerator
    ): CreateNewMapArtViewModel {
        return CreateNewMapArtViewModel(
            repository, locationService, getAddressService, generateAIContentService, promptGenerator
        )
    }


}