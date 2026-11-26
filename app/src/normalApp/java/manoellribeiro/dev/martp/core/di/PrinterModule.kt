package manoellribeiro.dev.martp.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import manoellribeiro.dev.martp.core.services.MapArtPrinterService
import manoellribeiro.dev.martp.core.services.UnavailableMapArtPrinterService

@Module
@InstallIn(SingletonComponent::class)
object PrinterModule {

    @Provides
    fun providesMapArtPrinterService(): MapArtPrinterService {
        return UnavailableMapArtPrinterService()
    }

}
