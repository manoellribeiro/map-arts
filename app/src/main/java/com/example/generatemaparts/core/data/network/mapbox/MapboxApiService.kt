package com.example.generatemaparts.core.data.network.mapbox

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// https://api.mapbox.com/styles/v1/{username}/{style_id}/static/{overlay}/{lon},{lat},{zoom},{bearing},{pitch}|{bbox}|{auto}/{width}x{height}{@2x}
// https://docs.mapbox.com/api/maps/static-images/
// https://square.github.io/retrofit/
// https://github.com/manoellribeiro/forecast-app/blob/master/app/src/main/java/com/example/forecastmvvm/data/network/WeatherApiService.kt

interface MapboxApiService {

    @GET("mapbox/{style_id}/static/[{lon},{lat}, 14]/{width}x{height}")
    fun getStaticMapImage(
        @Path("style_id") styleId: String,
        @Path("lat") latitude: Float,
        @Path("lon") longitude: Float,
        @Path("width") mapWidth: Int,
        @Path("height") height: Int,

    ) // TODO: Make the response object

    companion object {

        operator fun invoke(): MapboxApiService {

            // I need this to add the acess token to every call
            val requestInterceptor = Interceptor { chain ->

                val url = chain.request()
                    .url()
                    .newBuilder()
                    .addQueryParameter(
                        "access_token",
                        BuildConfig.MAPBOX_API_KEY
                    )
                    .build()

                val request = chain.request()
                    .newBuilder()
                    .url(url)
                    .build()

                return@Interceptor chain.proceed(request)
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(requestInterceptor)
                .build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MapboxApiService::class.java)
        }
    }
}
