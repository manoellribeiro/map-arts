package manoellribeiro.dev.martp.core.data.network.mapbox

import manoellribeiro.dev.martp.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path

// https://api.mapbox.com/styles/v1/{username}/{style_id}/static/{overlay}/{lon},{lat},{zoom},{bearing},{pitch}|{bbox}|{auto}/{width}x{height}{@2x}
// https://docs.mapbox.com/api/maps/static-images/
// https://square.github.io/retrofit/
// https://github.com/manoellribeiro/forecast-app/blob/master/app/src/main/java/com/example/forecastmvvm/data/network/WeatherApiService.kt
// https://docs.mapbox.com/api/maps/static-images/#marker -> to add optional pin to image

interface MapboxApiService {

    @GET("mapbox/{style_id}/static/{lon},{lat},15,0,0/{width}x{height}")
    suspend fun getStaticMapImageAsync(
        @Path("style_id") styleId: String,
        @Path("lat") latitude: Double,
        @Path("lon") longitude: Double,
        @Path("width") mapWidth: Int,
        @Path("height") mapHeight: Int,
    ): ResponseBody

    companion object {

        operator fun invoke(): MapboxApiService {

            // I need this to add the acess token to every call
            val requestInterceptor = Interceptor { chain ->

                val url = chain.request()
                    .url
                    .newBuilder()
                    .addQueryParameter(
                        "access_token",
                        BuildConfig.MAPBOX_API_KEY
                    )
                    .addQueryParameter(
                        "logo",
                        "false"
                    ).
                    addQueryParameter(
                        "attribution",
                        "false"
                    )
                    .addQueryParameter(
                        "before_layer",
                        "false"
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
                .baseUrl("https://api.mapbox.com/styles/v1/")
                .build()
                .create(MapboxApiService::class.java)
        }
    }
}
