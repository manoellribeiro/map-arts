package com.example.generatemaparts.core.data.repositories

import com.example.generatemaparts.core.data.network.mapbox.MapboxApiService
import com.example.generatemaparts.core.data.network.mapbox.models.MapboxMapStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class MapboxRepository(
    private val mapboxApiService: MapboxApiService
) {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun fetchStaticMapImageUrlAsync(
        mapWidth: Int,
        mapHeight: Int
    ): Deferred<String> = ioScope.async {
        try {
            //TODO: Verify for internet connection
            val response = mapboxApiService.getStaticMapImageAsync(
                styleId = MapboxMapStyle.DarkV11.id,
                latitude = -12.999917850449139, //TODO: get from location services
                longitude = -38.50773055149209, //TODO: get from location services
                mapWidth = mapWidth,
                mapHeight = mapHeight
            )
            return@async response.imageUrl
        } catch (e: Exception) {
            throw e
        }
    }

}