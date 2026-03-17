package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.location.Address
import android.os.Bundle
import android.text.InputType
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.extensions.getEnumExtra
import manoellribeiro.dev.martp.core.extensions.gone
import manoellribeiro.dev.martp.core.extensions.orNull
import manoellribeiro.dev.martp.core.extensions.toBitmap
import manoellribeiro.dev.martp.core.extensions.visible
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import manoellribeiro.dev.martp.core.sketches.DefaultMartpSketch
import manoellribeiro.dev.martp.core.sketches.MartpSketch
import manoellribeiro.dev.martp.core.sketches.PointillismMartpSketch
import manoellribeiro.dev.martp.databinding.ActivityCreateNewMapArtBinding
import processing.android.PFragment
import kotlin.properties.Delegates

@AndroidEntryPoint
class CreateNewMapArtActivity: AppCompatActivity() {

    private lateinit var binding: ActivityCreateNewMapArtBinding
    private val viewModel: CreateNewMapArtViewModel by viewModels()
    private var sketch: MartpSketch? = null
    private lateinit var type: SketchArtType


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateNewMapArtBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getExtras(savedInstanceState)
        setupObservables()
    }

    private fun getExtras(savedInstanceState: Bundle?) {
        type = intent.getEnumExtra<SketchArtType>().orNull { SketchArtType.DEFAULT }
    }

    override fun onResume() {
        super.onResume()
        binding.mapArtsContainer.doOnLayout {
            lifecycleScope.launch {
                viewModel.startToGenerateMapArt(
                    directory = this@CreateNewMapArtActivity.filesDir,
                    canvasToDrawArtWidth = binding.mapArtsContainer.width,
                    canvasToDrawArtHeight = binding.mapArtsContainer.height
                )
            }
        }
    }

    private fun setupObservables() {
        viewModel.state.observe(this@CreateNewMapArtActivity) { state ->
            when(state) {
                is CreateNewMapArtUiState.Error -> handleStateError(state.failure)
                is CreateNewMapArtUiState.ImageDownloaded -> handleImageDownloadedState(state.staticMapImagePath, state.address)
                CreateNewMapArtUiState.Loading -> handleLoadingState()
                CreateNewMapArtUiState.ActionButtonLoading -> handleActionButtonLoadingState()
                is CreateNewMapArtUiState.ArtCreatedSuccessfully -> handleArtCreatedSuccessfullyState(state.pathToStoreArtImage)
                CreateNewMapArtUiState.DisableActionButton -> disableActionButton()
                CreateNewMapArtUiState.EnableActionButton -> enableActionButton()
            }
        }
    }

    private fun disableActionButton() = with(binding) {
        actionMB.isEnabled = false
        actionMB.isClickable = false
    }

    private fun enableActionButton()  = with(binding) {
        actionMB.isEnabled = true
        actionMB.isClickable = true
        actionMB.setOnClickListener {
            saveArtToLocalDatabase()
        }
    }

    private fun handleActionButtonLoadingState() = with(binding) {
        actionMB.enableLoadingState(
            ContextCompat.getColor(
                this@CreateNewMapArtActivity,
                R.color.white
            )
        )
    }

    private fun handleArtCreatedSuccessfullyState(pathToStoreArtImage: String) {
        sketch?.save(pathToStoreArtImage)
        finish()
    }

    private fun handleStateError(failure: Failure) = with(binding) {
        backIB.visible()
        backIB.setOnClickListener {
            finish()
        }
        stateErrorS.visible()
        errorImageIV.visible()
        errorTextTV.visible()
        errorTextTV.text = getString(failure.messageToBeDisplayedToUserId)
        mapArtsContainer.gone()
        titleTV.gone()
        actionMB.visible()
        actionMB.isEnabled = true
        actionMB.isClickable = true
        actionMB.title = getString(R.string.try_again)
        actionMB.setOnClickListener {
            lifecycleScope.launch {
                viewModel.startToGenerateMapArt(
                    directory = this@CreateNewMapArtActivity.filesDir,
                    canvasToDrawArtWidth = binding.mapArtsContainer.width,
                    canvasToDrawArtHeight = binding.mapArtsContainer.height
                )
            }
        }
        cityCountryTV.gone()
        descriptionTV.gone()
    }

    private fun handleLoadingState() = with(binding) {
        backIB.visible()
        backIB.setOnClickListener {
            finish()
        }
        titleTV.text = getString(R.string.we_are_generating_your_art)
        stateErrorS.gone()
        errorImageIV.gone()
        errorTextTV.gone()
        actionMB.gone()
        mapArtsContainer.visible()
        titleTV.visible()
        cityCountryTV.gone()
        descriptionTV.gone()
    }

    private fun handleImageDownloadedState(
        imagePath: String,
        address: Address?
    ) = with(binding) {
        backIB.visible()
        backIB.setOnClickListener {
            finish()
        }
        mapArtsContainer.visible()
        titleTV.visible()
        sketch = when(type) {
            SketchArtType.DEFAULT -> DefaultMartpSketch(
                horizontalTilesCount = 0,
                verticalTilesCount = 0,
                padding = 0,
                canvasWidth = mapArtsContainer.width.toFloat(),
                canvasHeight = mapArtsContainer.height.toFloat(),
                imagePath = imagePath,
            )
            SketchArtType.POINTILLISM -> PointillismMartpSketch(
                horizontalTilesCount = 0,
                verticalTilesCount = 0,
                padding = 0,
                canvasWidth = mapArtsContainer.width.toFloat(),
                canvasHeight = mapArtsContainer.height.toFloat(),
                imagePath = imagePath,
            )
        }
        val processingFragment = PFragment(sketch)
        processingFragment.setView(mapArtsContainer, this@CreateNewMapArtActivity)
        if(address != null) {
            cityCountryTV.visible()
            descriptionTV.visible()
            cityCountryTV.text = address.countryName + ", " + address.locality
            descriptionTV.text = address.thoroughfare
        } else {
            cityCountryTV.gone()
            descriptionTV.gone()
        }
        stateErrorS.gone()
        errorImageIV.gone()
        errorTextTV.gone()
        titleTV.text = getString(R.string.this_is_your_new_art)
        actionMB.visible()
        actionMB.isClickable = false
        actionMB.title = getString(R.string.save_art)
    }

    private fun saveArtToLocalDatabase() = with(binding) {
        lifecycleScope.launch {
            viewModel.saveArtToLocalDatabase(
                title = "asdasda", //handle this
                description = "asdsada", // todo: handle this
                directory = this@CreateNewMapArtActivity.filesDir,
                newArtBitMap = mapArtsContainer.toBitmap()
            )
        }
    }
}