package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.data.local.entities.ColorPaletteEntity
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.extensions.dp
import manoellribeiro.dev.martp.core.extensions.executeIfNotNull
import manoellribeiro.dev.martp.core.extensions.formatDateHour
import manoellribeiro.dev.martp.core.extensions.gone
import manoellribeiro.dev.martp.core.extensions.hexToColorInt
import manoellribeiro.dev.martp.core.extensions.toBitmap
import manoellribeiro.dev.martp.core.extensions.visible
import manoellribeiro.dev.martp.core.models.MapLayersColors
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.models.SketchArtType
import manoellribeiro.dev.martp.core.services.sensors.GravitySensor
import manoellribeiro.dev.martp.core.sketches.ColorfulMapSketch
import manoellribeiro.dev.martp.core.sketches.DefaultMartpSketch
import manoellribeiro.dev.martp.core.sketches.MartpSketch
import manoellribeiro.dev.martp.core.sketches.PointillismMartpSketch
import manoellribeiro.dev.martp.core.sketches.ParticleSystemSketch
import manoellribeiro.dev.martp.core.sketches.PrintableMapSketch
import manoellribeiro.dev.martp.core.sketches.WaterFlowMartpSketch
import manoellribeiro.dev.martp.databinding.ActivityCreateNewMapArtBinding
import manoellribeiro.dev.martp.databinding.LayoutMapLayerLineBinding
import manoellribeiro.dev.martp.databinding.LayoutPrintableMapArtBinding
import manoellribeiro.dev.martp.scenes.success.SuccessActivity
import manoellribeiro.dev.martp.scenes.userInfoBottomSheet.SetUserInfoBottomSheetFragment
import processing.android.PFragment
import java.io.File
import java.util.Calendar


@AndroidEntryPoint
class CreateNewMapArtActivity: AppCompatActivity() {

    private lateinit var binding: ActivityCreateNewMapArtBinding
    private val viewModel: CreateNewMapArtViewModel by viewModels()
    private var sketch: MartpSketch? = null
    private var mapLayersColors: MapLayersColors? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateNewMapArtBinding.inflate(layoutInflater)
        enableEdgeToEdge()
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
                topMargin = insets.top + 8.dp(resources)
            }
            windowInsets
        }
        ViewCompat.setOnApplyWindowInsetsListener(mapArtsContainer) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top + 8.dp(resources)
            }
            windowInsets
        }
        ViewCompat.setOnApplyWindowInsetsListener(actionMB) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + 8.dp(resources)
            }
            windowInsets
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
                        is CreateNewMapArtEvent.ArtCreatedSuccessfully -> {
                            startActivity(SuccessActivity.newIntent(this@CreateNewMapArtActivity, event.imagePathLocation))
                            finish()
                        }
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

    private fun handleStateError(failure: Failure) = with(binding) {
        stateErrorS.visible()
        errorImageIV.visible()
        errorTextTV.visible()
        errorTextTV.text = getString(failure.messageToBeDisplayedToUserId)
        mapArtsContainer.gone()
        titleTV.gone()
        loadingIndicatorLAV.gone()
        actionMB.visible()
        actionMB.disableLoadingState()
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
        usernameInfoContainerLL.gone()
        colorPaletteTV.gone()
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
        usernameInfoContainerLL.gone()
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

    private fun setColorsLayersForColorfulMap(colorLayerHolder: MapLayersColors) {
        mapLayersColors = colorLayerHolder
        binding.mapColorsHoldersLL.removeAllViews()
        addColorLayer(colorLayerHolder.buildingColor, R.string.layer_buildings)
        addColorLayer(colorLayerHolder.buildingStrokeColor, R.string.layer_building_delimitations)
        addColorLayer(colorLayerHolder.waterColor, R.string.layer_water)
        addColorLayer(colorLayerHolder.roadsColor, R.string.layer_roads_tunnels_bridges)
        addColorLayer(colorLayerHolder.roadsPathColor, R.string.layer_road_paths)
        addColorLayer(colorLayerHolder.landOthersColor, R.string.layer_land_other)
    }

    private fun setColorsLayersForPrintableMap(colorLayerHolder: MapLayersColors) {
        mapLayersColors = colorLayerHolder
        binding.mapColorsHoldersLL.removeAllViews()
        addColorLayer(colorLayerHolder.buildingColor, R.string.layer_buildings_water_roads_footpaths)
        addColorLayer(colorLayerHolder.landOthersColor, R.string.layer_land_other)
    }

    private fun handleImageDownloadedState(
        state: CreateNewMapArtUiState.ImageDownloaded
    ) = with(binding) {
        mapLayersColors = null
        mapArtsContainer.visible()
        loadingIndicatorLAV.gone()
        loadingMessageTV.gone()
        titleTV.gone()
        mapLayersTitleTV.visible()
        mapColorsHoldersLL.visible()
        if(state.sketchArtType == SketchArtType.PRINTABLE_MAP) usernameInfoContainerLL.gone() else usernameInfoContainerLL.visible()
        if(!state.user.username.isNullOrEmpty()) {
            usernameTV.text = state.user.username
            addUsernameMB.setStrokeColor(getColor(R.color.light_l3))
            addUsernameMB.setButtonBackGroundColor(getColor(R.color.white))
        } else {
            usernameTV.text = getString(R.string.anonymous)
        }
        addUsernameMB.setOnClickListener {
           SetUserInfoBottomSheetFragment(
                userInfo = state.user,
                onSaveAnonymously = {
                    saveArt(state.user, state.title)
                },
                onSaveWithMyName = { updatedUser ->
                    saveArt(updatedUser, state.title)
                }
            ).apply {
                show(supportFragmentManager, "SetUserInfoBottomSheetFragment")
            }
        }

        sketch = when(state.sketchArtType) {
            SketchArtType.DEFAULT -> {
                addDefaultAndPointillismMapLayers()
                DefaultMartpSketch(
                    horizontalTilesCount = 4,
                    verticalTilesCount = 4,
                    padding = 20,
                    canvasWidth = state.canvasWidth.toFloat(),
                    canvasHeight = state.canvasHeight.toFloat(),
                    imagePath = state.staticMapImagePath,
                )
            }
            SketchArtType.POINTILLISM -> {
                addDefaultAndPointillismMapLayers()
                PointillismMartpSketch(
                    horizontalTilesCount = 0,
                    verticalTilesCount = 0,
                    padding = 0,
                    canvasWidth = state.canvasWidth.toFloat(),
                    canvasHeight = state.canvasHeight.toFloat(),
                    imagePath = state.staticMapImagePath,
                )
            }
            SketchArtType.WATER_FLOW -> WaterFlowMartpSketch(
                padding = 20,
                canvasWidth = state.canvasWidth.toFloat(),
                canvasHeight = state.canvasHeight.toFloat(),
                imagePath = state.staticMapImagePath,
            )
            SketchArtType.PARTICLE_SYSTEM -> ParticleSystemSketch(
                padding = 20,
                canvasWidth = state.canvasWidth.toFloat(),
                canvasHeight = state.canvasHeight.toFloat(),
                imagePath = state.staticMapImagePath,
                gravitySensor = GravitySensor(context = this@CreateNewMapArtActivity)
            )
            SketchArtType.COLORFUL_MAP -> {
                colorPaletteTV.visible()
                mapLayersSubTitleTV.visible()
                colorPaletteTV.text = getString(R.string.color_palette_value, state.colorPalette?.name)
                ColorfulMapSketch(
                    padding = 20,
                    canvasWidth = state.canvasWidth.toFloat(),
                    canvasHeight = state.canvasHeight.toFloat(),
                    imagePath = state.staticMapImagePath,
                    colorPalette = state.colorPalette!!
                ) {
                    runOnUiThread {
                        setColorsLayersForColorfulMap(it)
                    }
                }
            }
            SketchArtType.PRINTABLE_MAP -> {
                mapArtsContainer.updateLayoutParams<LinearLayout.LayoutParams> {
                    width = state.canvasWidth
                    height = state.canvasHeight
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                colorPaletteTV.gone()
                mapLayersSubTitleTV.visible()
                mapLayersSubTitleTV.text = getString(R.string.tap_map_to_invert_colors)
                PrintableMapSketch(
                    padding = 20,
                    canvasWidth = state.canvasWidth.toFloat(),
                    canvasHeight = state.canvasHeight.toFloat(),
                    imagePath = state.staticMapImagePath
                ) {
                    runOnUiThread {
                        setColorsLayersForPrintableMap(it)
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
        cityCountryTV.visible()
        cityCountryTV.text = state.title.ifEmpty { getString(R.string.unknown_location) }
        stateErrorS.gone()
        errorImageIV.gone()
        errorTextTV.gone()
        titleTV.text = getString(R.string.this_is_your_new_art)
        actionMB.visible()
        actionMB.isClickable = true
        actionMB.title = getString(if(state.sketchArtType == SketchArtType.PRINTABLE_MAP) R.string.print else R.string.save_art)
        actionMB.setOnClickListener {
            if(state.sketchArtType == SketchArtType.PRINTABLE_MAP) {
                printArt(state)
            } else {
                saveArt(state.user, state.title)
            }
        }
    }

    private fun printArt(state: CreateNewMapArtUiState.ImageDownloaded) {
        val mapBitmap = sketch?.toPrintableBitmap() ?: return
        viewModel.printArt(createPrintableArtBitmap(mapBitmap, state))
    }

    private fun createPrintableArtBitmap(
        mapBitmap: Bitmap,
        state: CreateNewMapArtUiState.ImageDownloaded
    ): Bitmap {
        val printableArt = LayoutPrintableMapArtBinding.inflate(layoutInflater).apply {
            mapIV.setImageBitmap(mapBitmap)
            titleTV.text = state.title.ifEmpty { getString(R.string.unknown_location) }
            latitudeTV.text = getString(R.string.latitude_value, state.latitude.toString())
            longitudeTV.text = getString(R.string.longitude_value, state.longitude.toString())
            dateTV.text = Calendar.getInstance().formatDateHour()
        }.root
        printableArt.measure(
            View.MeasureSpec.makeMeasureSpec(state.canvasWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        printableArt.layout(0, 0, printableArt.measuredWidth, printableArt.measuredHeight)
        return createBitmap(printableArt.measuredWidth, printableArt.measuredHeight).applyCanvas {
            drawColor(Color.WHITE)
            printableArt.draw(this)
        }
    }

    private fun MartpSketch.toPrintableBitmap(): Bitmap {
        val currentFrame = get()
        currentFrame.loadPixels()
        return Bitmap.createBitmap(
            currentFrame.pixels, currentFrame.width, currentFrame.height, Bitmap.Config.ARGB_8888
        )
    }

    private fun saveArt(user: UserInfoEntity, title: String) {
        viewModel.saveArt(
            title = title,
            description = "", //todo: dropar essa coluna do banco
            directory = this@CreateNewMapArtActivity.filesDir,
            mapLayersColors = mapLayersColors,
            updateUserInfo = user,
            saveImage = { path ->
                File(path).parentFile?.mkdirs()
                sketch?.get()?.save(path) == true
            }
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
