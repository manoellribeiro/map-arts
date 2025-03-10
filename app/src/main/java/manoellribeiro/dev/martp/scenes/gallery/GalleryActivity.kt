package manoellribeiro.dev.martp.scenes.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.extensions.gone
import manoellribeiro.dev.martp.core.extensions.visible
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.databinding.ActivityGalleryBinding
import manoellribeiro.dev.martp.scenes.createNewMapArt.CreateNewMapArtActivity
import manoellribeiro.dev.martp.scenes.locationAcessDetails.LocationAccessDetailsActivity
import manoellribeiro.dev.martp.scenes.martpProjectInfo.MartpProjectInfoActivity
import java.io.File

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

    private fun shareArtImage(imagePathLocation: String) {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        sharingIntent.type = "image/*"
        val uri = FileProvider.getUriForFile(
            this,
            applicationContext.packageName + ".provider",
            File(imagePathLocation)
        )
        sharingIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_art_message))
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivities(arrayOf(Intent.createChooser(sharingIntent, "Share with")))
    }

    private fun setupNotEmptyListState(mapArts: List<MapArtEntity>) = with(binding) {
        infoIB.visible()
        infoIB.setOnClickListener { openMartpProjectInfoActivity() }
        emptyListTV.gone()
        errorTV.gone()
        emptyGalleryImageIV.gone()
        loadingIndicatorPB.gone()
        mapArtsRV.visible()
        mapArtsRV.layoutManager = LinearLayoutManager(this@GalleryActivity)
        mapArtsRV.adapter = MapArtsRecyclerViewAdapter(
            mapArts = mapArts,
            onClickListener = { mapArt ->  },
            onShareButtonClickListener = { imagePath -> shareArtImage(imagePath)}
        )
        newArtMB.visible()
        newArtMB.title = getString(R.string.create_new_art)
        newArtMB.setOnClickListener {
            verifyPermissionToAccessLocation()
        }
    }

    private fun setupLoadingState() = with(binding) {
        infoIB.gone()
        mapArtsRV.gone()
        emptyListTV.gone()
        errorTV.gone()
        emptyGalleryImageIV.gone()
        loadingIndicatorPB.visible()
        newArtMB.gone()
    }

    private fun setupErrorState(failure: Failure) = with(binding) {
        infoIB.visible()
        infoIB.setOnClickListener { openMartpProjectInfoActivity() }
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
        infoIB.visible()
        infoIB.setOnClickListener { openMartpProjectInfoActivity() }
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

    private fun openMartpProjectInfoActivity() {
        val intent = Intent(this, MartpProjectInfoActivity::class.java)
        startActivity(intent)
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