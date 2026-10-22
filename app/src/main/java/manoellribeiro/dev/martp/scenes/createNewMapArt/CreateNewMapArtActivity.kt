package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.os.Bundle
import android.os.Handler
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.extensions.dp
import manoellribeiro.dev.martp.core.extensions.executeIfNotNull
import manoellribeiro.dev.martp.core.extensions.gone
import manoellribeiro.dev.martp.core.extensions.toBitmap
import manoellribeiro.dev.martp.core.extensions.visible
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import manoellribeiro.dev.martp.core.services.sensors.GravitySensor
import manoellribeiro.dev.martp.core.sketches.ColorfulMapSketch
import manoellribeiro.dev.martp.core.sketches.DefaultMartpSketch
import manoellribeiro.dev.martp.core.sketches.MartpSketch
import manoellribeiro.dev.martp.core.sketches.PointillismMartpSketch
import manoellribeiro.dev.martp.core.sketches.ParticleSystemSketch
import manoellribeiro.dev.martp.core.sketches.WaterFlowMartpSketch
import manoellribeiro.dev.martp.core.utils.ColorPaletteProvider
import manoellribeiro.dev.martp.databinding.ActivityCreateNewMapArtBinding
import manoellribeiro.dev.martp.databinding.LayoutMapLayerLineBinding
import processing.android.PFragment


@AndroidEntryPoint
class CreateNewMapArtActivity: AppCompatActivity() {

    private lateinit var binding: ActivityCreateNewMapArtBinding
    private val viewModel: CreateNewMapArtViewModel by viewModels()
    private var sketch: MartpSketch? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateNewMapArtBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupObservables()
        setupViews()
        binding.mapArtsContainer.doOnLayout {
            viewModel.startToGenerateMapArt(
                directory = this@CreateNewMapArtActivity.filesDir,
                canvasToDrawArtWidth = binding.mapArtsContainer.width,
                canvasToDrawArtHeight = binding.mapArtsContainer.height
            )
        }
    }

    private fun setupViews() = with(binding) {
        ViewCompat.setOnApplyWindowInsetsListener(titleTV) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top + 16.dp(resources)
            }
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.setOnApplyWindowInsetsListener(actionMB) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupObservables() {
        lifecycleScope.launch {
            viewModel.state
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .distinctUntilChanged()
                .collect { state ->
                    when(state) {
                        is CreateNewMapArtUiState.Error -> handleStateError(state.failure)
                        is CreateNewMapArtUiState.ImageDownloaded -> handleImageDownloadedState(state)
                        CreateNewMapArtUiState.Idle -> Unit
                        is CreateNewMapArtUiState.Loading -> handleLoadingState(state.loadingMessageId)
                        CreateNewMapArtUiState.ActionButtonLoading -> handleActionButtonLoadingState()
                    }
                }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when(event) {
                        is CreateNewMapArtEvent.ArtCreatedSuccessfully -> handleArtCreatedSuccessfullyState(event.pathToStoreArtImage)
                    }
                }
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
            viewModel.startToGenerateMapArt(
                directory = this@CreateNewMapArtActivity.filesDir,
                canvasToDrawArtWidth = binding.mapArtsContainer.width,
                canvasToDrawArtHeight = binding.mapArtsContainer.height
            )
        }
        cityCountryTV.gone()
        latTV.gone()
        longTV.gone()
        mapColorsHoldersLL.gone()
        mapLayersTitleTV.gone()
        mapLayersSubTitleTV.gone()
        loadingMessageTV.gone()
    }

    private fun handleLoadingState(
        loadingMessageId: Int
    ) = with(binding) {
        titleTV.text = getString(R.string.we_are_generating_your_art)
        stateErrorS.visible()
        errorImageIV.gone()
        errorTextTV.gone()
        actionMB.gone()
        mapLayersSubTitleTV.gone()
        mapArtsContainer.gone()
        loadingIndicatorLAV.visible()
        loadingMessageTV.visible()
        loadingMessageTV.text = getString(loadingMessageId)
        titleTV.visible()
        cityCountryTV.gone()
        latTV.gone()
        longTV.gone()
        mapColorsHoldersLL.gone()
        mapLayersTitleTV.gone()
    }

    private fun addDefaultAndPointillismMapLayers() {
        LayoutMapLayerLineBinding.inflate(layoutInflater, binding.mapColorsHoldersLL, true).run {
            layerNameTV.text = getString(R.string.layer_roads_bridges_footpaths)
            colorHolderV.background.setTint(getColor(R.color.blue))
        }
        LayoutMapLayerLineBinding.inflate(layoutInflater, binding.mapColorsHoldersLL, true).run {
            layerNameTV.text = getString(R.string.layer_everything_else)
            colorHolderV.background.setTint(getColor(R.color.yellow))
        }
    }

    private fun addColorLayer(
        color: Int?,
        string: Int
    ) {
        color.executeIfNotNull {
            LayoutMapLayerLineBinding.inflate(layoutInflater, binding.mapColorsHoldersLL, true).run {
                layerNameTV.text = getString(string)
                colorHolderV.background.setTint(color!!)
            }
        }
    }

    private fun setColorsLayersForColorfulMap(colorLayerHolder: ColorfulMapSketch.ColorLayerHolder) {
        binding.mapColorsHoldersLL.removeAllViews()
        addColorLayer(colorLayerHolder.buildingColor, R.string.layer_buildings)
        addColorLayer(colorLayerHolder.buildingStrokeColor, R.string.layer_building_delimitations)
        addColorLayer(colorLayerHolder.waterColor, R.string.layer_water)
        addColorLayer(colorLayerHolder.roadsColor, R.string.layer_roads_tunnels_bridges)
        addColorLayer(colorLayerHolder.roadsPathColor, R.string.layer_road_paths)
        addColorLayer(colorLayerHolder.landOthersColor, R.string.layer_land_other)
    }

    private fun handleImageDownloadedState(
        state: CreateNewMapArtUiState.ImageDownloaded
    ) = with(binding) {
        mapArtsContainer.visible()
        loadingIndicatorLAV.gone()
        loadingMessageTV.gone()
        titleTV.visible()
        mapLayersTitleTV.visible()
        mapColorsHoldersLL.visible()
        sketch = when(state.sketchArtType) {
            SketchArtType.DEFAULT -> {
                addDefaultAndPointillismMapLayers()
                DefaultMartpSketch(
                    horizontalTilesCount = 4,
                    verticalTilesCount = 4,
                    padding = 20,
                    canvasWidth = mapArtsContainer.width.toFloat(),
                    canvasHeight = mapArtsContainer.height.toFloat(),
                    imagePath = state.staticMapImagePath,
                )
            }
            SketchArtType.POINTILLISM -> {
                addDefaultAndPointillismMapLayers()
                PointillismMartpSketch(
                    horizontalTilesCount = 0,
                    verticalTilesCount = 0,
                    padding = 0,
                    canvasWidth = mapArtsContainer.width.toFloat(),
                    canvasHeight = mapArtsContainer.height.toFloat(),
                    imagePath = state.staticMapImagePath,
                )
            }
            SketchArtType.WATER_FLOW -> WaterFlowMartpSketch(
                padding = 20,
                canvasWidth = mapArtsContainer.width.toFloat(),
                canvasHeight = mapArtsContainer.height.toFloat(),
                imagePath = state.staticMapImagePath,
            )
            SketchArtType.PARTICLE_SYSTEM -> ParticleSystemSketch(
                padding = 20,
                canvasWidth = mapArtsContainer.width.toFloat(),
                canvasHeight = mapArtsContainer.height.toFloat(),
                imagePath = state.staticMapImagePath,
                gravitySensor = GravitySensor(context = this@CreateNewMapArtActivity)
            )
            SketchArtType.COLORFUL_MAP -> {
                colorPaletteTV.visible()
                mapLayersSubTitleTV.visible()
                colorPaletteTV.text = getString(R.string.color_palette_value, state.colorPalette)
                ColorfulMapSketch(
                    padding = 20,
                    canvasWidth = mapArtsContainer.width.toFloat(),
                    canvasHeight = mapArtsContainer.height.toFloat(),
                    imagePath = state.staticMapImagePath,
                    colorPalette = ColorPaletteProvider.byNameOrDefault(state.colorPalette)
                ) {
                    runOnUiThread {
                        setColorsLayersForColorfulMap(it)
                    }
                }
            }
        }
        val processingFragment = PFragment(sketch)
        processingFragment.setView(mapArtsContainer, this@CreateNewMapArtActivity)
        latTV.visible()
        longTV.visible()
        latTV.text = getString(R.string.latitude_value, state.latitude.toString())
        longTV.text = getString(R.string.longitude_value, state.longitude.toString())
        if(state.title.isEmpty()) {
            cityCountryTV.gone()
        } else {
            cityCountryTV.visible()
            cityCountryTV.text = state.title
        }
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
        viewModel.saveArtToLocalDatabase(
            title = cityCountryTV.text.toString(),
            description = "", //todo: dropar essa coluna do banco
            directory = this@CreateNewMapArtActivity.filesDir,
            newArtBitMap = mapArtsContainer.toBitmap()
        )
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