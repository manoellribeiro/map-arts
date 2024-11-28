package manoellribeiro.dev.martp.scenes.gallery

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
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.extensions.executeIfNotNull
import manoellribeiro.dev.martp.core.services.LocationService
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.TilesMapSketch
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.databinding.ActivityGalleryBinding
import manoellribeiro.dev.martp.databinding.MartpButtonEndIconBinding
import processing.android.CompatUtils
import processing.android.PFragment

@AndroidEntryPoint
class GalleryActivity : AppCompatActivity() {

    private var sketch: TilesMapSketch? = null
    private lateinit var requireLocationPermissionLauncher: ActivityResultLauncher<String>
    private val viewModel: GalleryViewModel by viewModels()
    private lateinit var binding: ActivityGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRequireLocationPermissionLauncher()
    }

    override fun onResume() {
        super.onResume()
        requestForPermissionToAccessLocation()
    }

    private fun setupRequireLocationPermissionLauncher() {
        requireLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { wasGranted ->
            if(wasGranted) {
                generateMapArt()
                lifecycleScope.launch {
                    viewModel.printLocationData()
                }
            } else {
                // show user some thing about how we use their data
            }
        }
    }

    private fun generateMapArt() {
        // try {
        //     lifecycleScope.launch {
        //         val location = locationService.getCurrentLocation().await()
        //         val imagePath = repository.fetchStaticMapImageUrlAsync(
        //             longitude = location.longitude,
        //             latitude = location.latitude,
        //             mapWidth = 500,
        //             mapHeight = 500,
        //             dir = this@GalleryActivity.filesDir
        //         ).await()
        //         applicationContext.filesDir
        //         Log.i("IMAGEPATH", imagePath)
        //
        //         sketch = TilesMapSketch(
        //             horizontalTilesCount = 5,
        //             verticalTilesCount = 10,
        //             padding = 20,
        //             canvasWidth = 620F,
        //             canvasHeight = 620F,
        //             imagePath
        //         )
        //
        //         val frameLayout = FrameLayout(this@GalleryActivity)
        //         frameLayout.id = CompatUtils.getUniqueViewId()
        //         val layoutParams = ViewGroup.LayoutParams(
        //             ViewGroup.LayoutParams.MATCH_PARENT,
        //             ViewGroup.LayoutParams.MATCH_PARENT
        //         )
        //         setContentView(frameLayout, layoutParams)
        //
        //         val processingFragment = PFragment(sketch)
        //         processingFragment.setView(frameLayout, this@GalleryActivity)
        //     }
        //
        // } catch (e: SecurityException) {
        //     //throw some error about not having the location access
        // }
    }

    private fun requestForPermissionToAccessLocation() {
        val didUserAlreadyGivePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        when{
            didUserAlreadyGivePermission -> {
                generateMapArt()
                lifecycleScope.launch {
                    viewModel.printLocationData()
                }
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this@GalleryActivity,
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

    // override fun onNewIntent(intent: Intent?) {
    //     super.onNewIntent(intent)
    //     sketch.executeIfNotNull {
    //         sketch?.onNewIntent(intent)
    //     }
    // }

}