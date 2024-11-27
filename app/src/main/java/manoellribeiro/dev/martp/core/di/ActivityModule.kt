package manoellribeiro.dev.martp.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.services.LocationService
import manoellribeiro.dev.martp.scenes.gallery.GalleryViewModel

@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

    @Provides
    fun providesGalleryViewModel(
        repository: MartpRepository,
        locationService: LocationService
    ): GalleryViewModel {
        return GalleryViewModel(
            repository = repository,
            locationService = locationService
        )
    }


}