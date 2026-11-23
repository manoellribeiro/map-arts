package manoellribeiro.dev.martp.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.services.MapArtPrinterService
import manoellribeiro.dev.martp.core.services.SunmiMapArtPrinterService
import manoellribeiro.dev.martp.printerhardware.services.SmartPosPrinterService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PrinterModule {

    @Singleton
    @Provides
    fun providesMapArtPrinterService(
        @ApplicationContext context: Context,
        errorLogger: ErrorLogger
    ): MapArtPrinterService {
        return SunmiMapArtPrinterService(SmartPosPrinterService(context, errorLogger))
    }

}
