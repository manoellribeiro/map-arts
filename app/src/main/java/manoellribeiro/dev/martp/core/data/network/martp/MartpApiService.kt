package manoellribeiro.dev.martp.core.data.network.martp

import manoellribeiro.dev.martp.BuildConfig
import manoellribeiro.dev.martp.core.data.network.martp.models.RemoteColorPalettesResponse
import manoellribeiro.dev.martp.core.data.network.martp.models.RemoteReversedAddress
import manoellribeiro.dev.martp.core.data.network.martp.models.RemoteSaveMapArtRequest
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface MartpApiService {

    @GET("color-palettes")
    suspend fun getColorPalettes(): RemoteColorPalettesResponse

    @GET("geocoding/reverse")
    suspend fun reverseGeocode(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("language") language: String? = null,
    ): RemoteReversedAddress

    @Multipart
    @POST("map-arts")
    suspend fun saveMapArt(
        @Part("art") art: RemoteSaveMapArtRequest,
        @Part image: MultipartBody.Part,
    )

    companion object {

        operator fun invoke(errorLogger: ErrorLogger): MartpApiService {

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.BODY)
                redactHeader(AppCheckInterceptor.APP_CHECK_HEADER)
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(AppCheckInterceptor(errorLogger))
                .addInterceptor(loggingInterceptor)
                .build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("http://localhost:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MartpApiService::class.java)
        }
    }
}
