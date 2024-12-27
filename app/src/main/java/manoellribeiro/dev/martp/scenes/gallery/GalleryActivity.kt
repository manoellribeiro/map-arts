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
import androidx.recyclerview.widget.LinearLayoutManager
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.extensions.executeIfNotNull
import manoellribeiro.dev.martp.core.services.LocationService
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.extensions.gone
import manoellribeiro.dev.martp.core.extensions.visible
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.databinding.ActivityGalleryBinding
import manoellribeiro.dev.martp.databinding.MartpButtonEndIconBinding
import manoellribeiro.dev.martp.scenes.createNewMapArt.CreateNewMapArtActivity
import manoellribeiro.dev.martp.scenes.locationAcessDetails.LocationAccessDetailsActivity
import processing.android.CompatUtils

@AndroidEntryPoint
class GalleryActivity : AppCompatActivity() {

    private lateinit var requireLocationPermissionLauncher: ActivityResultLauncher<String>
    private val viewModel: GalleryViewModel by viewModels()
    private lateinit var binding: ActivityGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRequireLocationPermissionLauncher()
        setupObservables()
    }

    override fun onResume() {
        super.onResume()
        viewModel.getUserMapArts()
    }

    private fun setupObservables() {
        viewModel.state.observe(this) { state ->
            when(state) {
                GalleryUiState.EmptyList -> setupEmptyListState()
                is GalleryUiState.Error -> setupErrorState(state.failure)
                GalleryUiState.Loading -> setupLoadingState()
                is GalleryUiState.NotEmptyList -> setupNotEmptyListState(state.mapArts)
            }

        }
    }

    private fun setupNotEmptyListState(mapArts: List<MapArtEntity>) = with(binding) {
        emptyListTV.gone()
        errorTV.gone()
        emptyGalleryImageIV.gone()
        loadingIndicatorPB.gone()
        mapArtsRV.visible()
        mapArtsRV.layoutManager = LinearLayoutManager(this@GalleryActivity)
        mapArtsRV.adapter = MapArtsRecyclerViewAdapter(mapArts) {}
        newArtMB.visible()
        newArtMB.title = getString(R.string.create_new_art)
        newArtMB.setOnClickListener {
            verifyPermissionToAccessLocation()
        }
    }

    private fun setupLoadingState() = with(binding) {
        mapArtsRV.gone()
        emptyListTV.gone()
        errorTV.gone()
        emptyGalleryImageIV.gone()
        loadingIndicatorPB.visible()
        newArtMB.gone()
    }

    private fun setupErrorState(failure: Failure) = with(binding) {
        mapArtsRV.gone()
        emptyListTV.gone()
        errorTV.visible()
        emptyGalleryImageIV.gone()
        errorTV.text = getString(failure.messageToBeDisplayedToUserId)
        loadingIndicatorPB.gone()
        newArtMB.visible()
        newArtMB.title = getString(R.string.try_again)
        newArtMB.setOnClickListener {
            viewModel.getUserMapArts()
        }
    }

    private fun setupEmptyListState() = with(binding) {
        mapArtsRV.gone()
        emptyListTV.visible()
        emptyGalleryImageIV.visible()
        errorTV.gone()
        loadingIndicatorPB.gone()
        newArtMB.visible()
        newArtMB.title = getString(R.string.create_new_art)
        newArtMB.setOnClickListener {
            verifyPermissionToAccessLocation()
        }
    }

    private fun setupRequireLocationPermissionLauncher() {
        requireLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { wasGranted ->
            if(wasGranted) {
                openCreateNewMapArtScene()
            } else {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this@GalleryActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    openLocationAccessDetailScene()
                }
            }
        }
    }

    private fun openCreateNewMapArtScene() {
        val intent = Intent(this, CreateNewMapArtActivity::class.java)
        startActivity(intent)
    }

    private fun openLocationAccessDetailScene() {
        val intent = Intent(this, LocationAccessDetailsActivity::class.java)
        startActivity(intent)
    }

    private fun verifyPermissionToAccessLocation() {
        val didUserAlreadyGivePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        when{
            didUserAlreadyGivePermission -> {
                openCreateNewMapArtScene()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this@GalleryActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                openLocationAccessDetailScene()
            }
            else -> {
                requireLocationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }


        }
    }
}