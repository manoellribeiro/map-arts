package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.extensions.gone
import manoellribeiro.dev.martp.core.extensions.toBitmap
import manoellribeiro.dev.martp.core.extensions.visible
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.sketches.DefaultMartpSketch
import manoellribeiro.dev.martp.databinding.ActivityCreateNewMapArtBinding
import processing.android.PFragment
import java.util.Timer
import kotlin.concurrent.schedule

@AndroidEntryPoint
class CreateNewMapArtActivity: AppCompatActivity() {

    private lateinit var binding: ActivityCreateNewMapArtBinding
    private val viewModel: CreateNewMapArtViewModel by viewModels()
    private var sketch: DefaultMartpSketch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateNewMapArtBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupObservables()
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
                is CreateNewMapArtUiState.ImageDownloaded -> handleImageDownloadedState(state.staticMapImagePath)
                CreateNewMapArtUiState.Loading -> handleLoadingState()
                CreateNewMapArtUiState.ActionButtonLoading -> handleActionButtonLoadingState()
                is CreateNewMapArtUiState.ArtCreatedSuccessfully -> handleArtCreatedSuccessfullyState(state.pathToStoreArtImage)
            }
        }
    }

    private fun handleActionButtonLoadingState() = with(binding) {
        actionMB.enableLoadingState(
            ContextCompat.getColor(
                this@CreateNewMapArtActivity,
                R.color.white
            )
        )
        titleMTI.inputType = InputType.TYPE_NULL
        descriptionMTI.inputType = InputType.TYPE_NULL
    }

    private fun handleArtCreatedSuccessfullyState(pathToStoreArtImage: String) {
        sketch?.save(pathToStoreArtImage)
        finish()
    }

    private fun handleStateError(failure: Failure) = with(binding) {
        stateErrorS.visible()
        errorImageIV.visible()
        errorTextTV.visible()
        errorTextTV.text = getString(failure.messageToBeDisplayedToUserId)
        mapArtsContainer.gone()
        titleTV.gone()
        actionMB.visible()
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
    }

    private fun handleLoadingState() = with(binding) {
        titleTV.text = getString(R.string.we_are_generating_your_art)
        titleMTI.gone()
        descriptionMTI.gone()
        stateErrorS.gone()
        errorImageIV.gone()
        errorTextTV.gone()
        actionMB.gone()
        mapArtsContainer.visible()
        titleTV.visible()
    }

    private fun handleImageDownloadedState(imagePath: String) = with(binding) {
        mapArtsContainer.visible()
        titleTV.visible()
        sketch = DefaultMartpSketch(
            horizontalTilesCount = 0,
            verticalTilesCount = 0,
            padding = 0,
            canvasWidth = mapArtsContainer.width.toFloat(),
            canvasHeight = mapArtsContainer.height.toFloat(),
            imagePath = imagePath,
            drawingFinishedCallback = {
                runOnUiThread {
                    titleMTI.showKeyboard()
                }
            }
        )
        val processingFragment = PFragment(sketch)
        processingFragment.setView(mapArtsContainer, this@CreateNewMapArtActivity)
        titleMTI.visible()
        descriptionMTI.visible()
        stateErrorS.gone()
        errorImageIV.gone()
        errorTextTV.gone()
        titleTV.text = getString(R.string.this_is_your_new_art)
        actionMB.visible()
        actionMB.title = getString(R.string.save_art)
        actionMB.setOnClickListener {
            lifecycleScope.launch {
                viewModel.saveArtToLocalDatabase(
                    title = titleMTI.currentText,
                    description = descriptionMTI.currentText,
                    directory = this@CreateNewMapArtActivity.filesDir,
                    newArtBitMap = mapArtsContainer.toBitmap()
                )
            }
        }
    }
}