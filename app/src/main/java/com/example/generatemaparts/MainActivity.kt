package com.example.generatemaparts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.generatemaparts.core.data.network.mapbox.MapboxApiService
import com.example.generatemaparts.core.data.repositories.MapboxRepository
import com.example.generatemaparts.core.extensions.executeIfNotNull
import kotlinx.coroutines.launch
import processing.android.CompatUtils
import processing.android.PFragment

class MainActivity : AppCompatActivity() {

    private var sketch: TilesMapSketch? = null
    private lateinit var requireLocationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frameLayout = FrameLayout(this)
        frameLayout.id = CompatUtils.getUniqueViewId()
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setContentView(frameLayout, layoutParams)

        val repository = MapboxRepository(
            MapboxApiService()
        )

        var imagePath = ""
        lifecycleScope.launch {
            imagePath = repository.fetchStaticMapImageUrlAsync(
                mapWidth = 600,
                mapHeight = 600,
                this@MainActivity.filesDir
            ).await()
            applicationContext.filesDir
            Log.i("IMAGEPATH", imagePath)

            sketch = TilesMapSketch(
                horizontalTilesCount = 5,
                verticalTilesCount = 10,
                padding = 20,
                canvasWidth = 600,
                canvasHeight = 1200,
                imagePath
            )

            val processingFragment = PFragment(sketch)
            processingFragment.setView(frameLayout, this@MainActivity)

        }
    }

    private fun setupRequireLocationPermissionLauncher() {
        requireLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { wasGranted ->
            if(wasGranted) {
                //do logic to require permission
            } else {
                // show user some thing about how we use their data
            }
        }
    }

    private fun requestForPermissionToAccessLocation() {
        val didUserAlreadyGivePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        when{
            didUserAlreadyGivePermission -> {
                //My user already gave the location permission
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {

            }
            else -> {
                requireLocationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }


        }
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