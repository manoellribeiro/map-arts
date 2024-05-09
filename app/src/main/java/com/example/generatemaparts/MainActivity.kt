package com.example.generatemaparts

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import com.example.generatemaparts.core.data.network.mapbox.MapboxApiService
import com.example.generatemaparts.core.data.repositories.MapboxRepository
import com.example.generatemaparts.core.extensions.executeIfNotNull
import kotlinx.coroutines.launch
import processing.android.CompatUtils
import processing.android.PFragment

class MainActivity : AppCompatActivity() {

    private var sketch: TilesMapSketch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frameLayout = FrameLayout(this)
        frameLayout.id = CompatUtils.getUniqueViewId()
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setContentView(frameLayout, layoutParams)

        sketch = TilesMapSketch(
            horizontalTilesCount = 5,
            verticalTilesCount = 10,
            padding = 20,
            canvasWidth = 600,
            canvasHeight = 1200
        )

        val repository = MapboxRepository(
            MapboxApiService()
        )

        lifecycleScope.launch {
            repository.fetchStaticMapImageUrlAsync(
                mapWidth = 480,
                mapHeight = 980
            ).await()
        }


        val processingFragment = PFragment(sketch)
        processingFragment.setView(frameLayout, this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        sketch.executeIfNotNull {
            sketch?.onRequestPermissionsResult(
                requestCode, permissions, grantResults
            )
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        sketch.executeIfNotNull {
            sketch?.onNewIntent(intent)
        }
    }

}