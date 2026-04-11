package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.os.Bundle
import android.os.Handler
import android.widget.TextView
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

    private fun getExtras(savedInstanceState: Bundle?) {
        type = intent.getEnumExtra<SketchArtType>().orNull { SketchArtType.DEFAULT }
    }

    private fun setupObservables() {
        viewModel.state.observe(this@CreateNewMapArtActivity) { state ->
            when(state) {
                is CreateNewMapArtUiState.Error -> handleStateError(state.failure)
                is CreateNewMapArtUiState.ImageDownloaded -> handleImageDownloadedState(state.staticMapImagePath, state.title, aiDescription = state.aiDescription)
                CreateNewMapArtUiState.Loading -> handleLoadingState()
                CreateNewMapArtUiState.ActionButtonLoading -> handleActionButtonLoadingState()
                is CreateNewMapArtUiState.ArtCreatedSuccessfully -> handleArtCreatedSuccessfullyState(state.pathToStoreArtImage)
                CreateNewMapArtUiState.ErrorGeneratingAIText -> handleErrorGeneratingAIText()
                is CreateNewMapArtUiState.GeneratedAIText -> handleGeneratedAIText(state.text)
                CreateNewMapArtUiState.LoadingAIText -> handleLoadingAIText()
            }
        }
    }

    private fun handleErrorGeneratingAIText() = with(binding) {
        aiTextTV.text = getString(R.string.create_description_with_ai)
        aiPoweradeIV.visible()
        loadingAIIndicatorPB.gone()
        aiPoweredLL.isClickable = true
        aiPoweredLL.setOnClickListener {
            lifecycleScope.launch {
                viewModel.generateAiText()
            }
        }
    }

    private fun handleLoadingAIText() = with(binding) {
        aiTextTV.text = getString(R.string.thinking)
        aiPoweradeIV.gone()
        loadingAIIndicatorPB.visible()
        aiPoweredLL.setOnClickListener(null)
        aiPoweredLL.isClickable = false
    }

    private fun handleGeneratedAIText(text: String) = with(binding) {
        descriptionTV.visible()
        typingAnimation(descriptionTV, text, 1) {
            aiTextTV.text = getString(R.string.ai_powered)
            aiPoweradeIV.visible()
            loadingAIIndicatorPB.gone()
            aiPoweredLL.setOnClickListener(null)
            aiPoweredLL.isClickable = false
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
        stateErrorS.visible()
        errorImageIV.visible()
        errorTextTV.visible()
        errorTextTV.text = getString(failure.messageToBeDisplayedToUserId)
        mapArtsContainer.gone()
        titleTV.gone()
        loadingIndicatorLAV.gone()
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
        aiPoweredLL.gone()
    }

    private fun handleLoadingState() = with(binding) {
        titleTV.text = getString(R.string.we_are_generating_your_art)
        stateErrorS.gone()
        errorImageIV.gone()
        errorTextTV.gone()
        actionMB.gone()
        mapArtsContainer.gone()
        loadingIndicatorLAV.visible()
        titleTV.visible()
        cityCountryTV.gone()
        descriptionTV.gone()
        aiPoweredLL.gone()
    }

    private fun handleImageDownloadedState(
        imagePath: String,
        title: String,
        aiDescription: String
    ) = with(binding) {
        mapArtsContainer.visible()
        loadingIndicatorLAV.gone()
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
        cityCountryTV.visible()
        descriptionTV.gone()
        aiPoweredLL.visible()
        aiPoweredLL.isClickable = true
        aiPoweredLL.setOnClickListener {
            lifecycleScope.launch {
                viewModel.generateAiText()
            }
        }
        cityCountryTV.text = title
        stateErrorS.gone()
        errorImageIV.gone()
        errorTextTV.gone()
        titleTV.text = getString(R.string.this_is_your_new_art)
        actionMB.visible()
        actionMB.isClickable = true
        actionMB.title = getString(R.string.save_art)
        actionMB.setOnClickListener {
            saveArtToLocalDatabase()
        }
    }

    private fun saveArtToLocalDatabase() = with(binding) {
        lifecycleScope.launch {
            viewModel.saveArtToLocalDatabase(
                title = cityCountryTV.text.toString(),
                description = descriptionTV.text.toString(), //todo: entender o que acontece se esse não estiver preenchido
                directory = this@CreateNewMapArtActivity.filesDir,
                newArtBitMap = mapArtsContainer.toBitmap()
            )
        }
    }

    private fun typingAnimation(
        view: TextView,
        text: String,
        length: Int,
        onComplete: () -> Unit
    ) {
        //todo: add a check to see if the text is empty to prevent crashes
        var delay = 25L
        if(Character.isWhitespace(text.elementAt(length-1))){
            delay = 50L
        }
        view.text = text.substring(0,length)
        when (length) {
            text.length -> {
                onComplete.invoke()
                return
            }
            else -> Handler().postDelayed({
                typingAnimation(view, text, length + 1, onComplete)
            }, delay)
        }
    }
}