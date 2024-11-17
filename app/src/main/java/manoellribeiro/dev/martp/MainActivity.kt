package manoellribeiro.dev.martp

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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.data.repositories.MapboxRepository
import manoellribeiro.dev.martp.core.extensions.executeIfNotNull
import manoellribeiro.dev.martp.core.services.LocationService
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import processing.android.CompatUtils
import processing.android.PFragment

class MainActivity : AppCompatActivity() {

    private var sketch: TilesMapSketch? = null
    private lateinit var requireLocationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var repository: MapboxRepository
    private lateinit var locationService: LocationService

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setupRequireLocationPermissionLauncher()
        locationService = LocationService(LocationServices.getFusedLocationProviderClient(this))
        repository = MapboxRepository(
            MapboxApiService()
        )
    }

    override fun onResume() {
        super.onResume()
        requestForPermissionToAccessLocation()
    }

    private fun setupRequireLocationPermissionLauncher() {
        requireLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { wasGranted ->
            if(wasGranted) {
                generateMapArt()
            } else {
                // show user some thing about how we use their data
            }
        }
    }

    private fun generateMapArt() {
        try {
            lifecycleScope.launch {
                val location = locationService.getCurrentLocation().await()
                val imagePath = repository.fetchStaticMapImageUrlAsync(
                    longitude = location.longitude,
                    latitude = location.latitude,
                    mapWidth = 500,
                    mapHeight = 500,
                    dir = this@MainActivity.filesDir
                ).await()
                applicationContext.filesDir
                Log.i("IMAGEPATH", imagePath)

                sketch = TilesMapSketch(
                    horizontalTilesCount = 5,
                    verticalTilesCount = 10,
                    padding = 20,
                    canvasWidth = 620F,
                    canvasHeight = 620F,
                    imagePath
                )

                val frameLayout = FrameLayout(this@MainActivity)
                frameLayout.id = CompatUtils.getUniqueViewId()
                val layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setContentView(frameLayout, layoutParams)

                val processingFragment = PFragment(sketch)
                processingFragment.setView(frameLayout, this@MainActivity)
            }

        } catch (e: SecurityException) {
            //throw some error about not having the location access
        }
    }

    private fun requestForPermissionToAccessLocation() {
        val didUserAlreadyGivePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        when{
            didUserAlreadyGivePermission -> {
                generateMapArt()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // I need to show ui to my user explaining how I'm using their location and why they should give it to me
            }
            else -> {
                requireLocationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }


        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        sketch.executeIfNotNull {
            sketch?.onNewIntent(intent)
        }
    }

}