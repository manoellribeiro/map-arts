package manoellribeiro.dev.martp.core.di

import android.content.Context
import android.location.Geocoder
import android.net.ConnectivityManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AndroidServicesModule {

    @Provides
    fun provideGeoCoder(
        @ApplicationContext context: Context
    ): Geocoder {
        return Geocoder(context)
    }

    @Provides
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    fun provideConnectivityManager(
        @ApplicationContext context: Context
    ): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Provides
    fun provideFirebaseGenerativeModel(
        @ApplicationContext context: Context
    ): GenerativeModel {
        return Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel("gemini-3-flash-preview")
    }

}