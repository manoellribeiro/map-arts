package manoellribeiro.dev.martp.core.data.network.martp

import manoellribeiro.dev.martp.BuildConfig
import manoellribeiro.dev.martp.core.data.network.martp.models.ColorPalettesResponse
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface MartpApiService {

    @GET("color-palettes")
    suspend fun getColorPalettes(): ColorPalettesResponse

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
                .baseUrl(BuildConfig.MARTP_API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MartpApiService::class.java)
        }
    }
}
