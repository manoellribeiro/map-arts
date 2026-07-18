package manoellribeiro.dev.martp.core.data.network.geoapify

import manoellribeiro.dev.martp.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GeoapifyApiService {

    //@GET("staticmap/{style_id}/static/{lon},{lat},{zoom},0,0/{width}x{height}")
    @GET("staticmap")
    suspend fun getStaticMapImageAsync(
        @Query("style") styleId: String,
        @Query("center") latitude: String,
        @Query("width") mapWidth: Int,
        @Query("height") mapHeight: Int,
        @Query("zoom") mapZoom: Float,
    ): ResponseBody

    companion object {

        operator fun invoke(): GeoapifyApiService {

            // I need this to add the acess token to every call
            val requestInterceptor = Interceptor { chain ->

                val url = chain.request()
                    .url
                    .newBuilder()
                    .addQueryParameter(
                        "styleCustomization",
                        "landcover_grass_fill:none|water_name_lakeline:none|water_name_way:none|water_name_sea:none|water_name_ocean:none|road_label_primary:none|road_label_secondary:none|place_label_park:none|place_label_village:none|place_label_city:none|place_label_town:none|place_state-label:none|place_label_country:none|place_label_continent:none"
                    )
                    .addQueryParameter(
                        "scaleFactor",
                        "2"
                    )
                    .addQueryParameter(
                        "apiKey",
                        BuildConfig.GEOAPIFY_API_KEY
                    )
                    .build()

                val request = chain.request()
                    .newBuilder()
                    .url(url)
                    .build()

                return@Interceptor chain.proceed(request)
            }

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.BODY)
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(requestInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("https://maps.geoapify.com/v1/")
                .build()
                .create(GeoapifyApiService::class.java)
        }
    }
}