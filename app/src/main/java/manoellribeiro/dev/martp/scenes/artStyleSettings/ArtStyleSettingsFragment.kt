package manoellribeiro.dev.martp.scenes.artStyleSettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.components.compose.theme.DarkD4
import manoellribeiro.dev.martp.core.components.compose.theme.LightCream
import manoellribeiro.dev.martp.core.components.compose.theme.TextMiddleScreenInfoText
import manoellribeiro.dev.martp.core.components.compose.theme.TextScreenTitle
import manoellribeiro.dev.martp.core.components.compose.theme.Yellow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import manoellribeiro.dev.martp.core.components.compose.theme.Blue
import manoellribeiro.dev.martp.core.components.compose.theme.DarkD5
import manoellribeiro.dev.martp.core.components.compose.theme.LightL1
import manoellribeiro.dev.martp.core.components.compose.theme.LightL5
import manoellribeiro.dev.martp.core.components.compose.theme.LightYellow
import manoellribeiro.dev.martp.core.components.compose.theme.White
import manoellribeiro.dev.martp.core.data.network.mapbox.models.MapboxMapStyle
import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import manoellribeiro.dev.martp.core.utils.ColorPaletteProvider
import manoellribeiro.dev.martp.scenes.main.MainViewModel
import kotlin.getValue

class ArtStyleSettingsFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext())
        composeView.setContent {
            val uiState by viewModel.artStyleSettingsState.collectAsStateWithLifecycle()
            Scaffold(
                containerColor = LightCream,
                // The bottom navigation bar already makes room for the system navigation bar
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ) { contentPadding ->
                Column(
                    modifier = Modifier.padding(contentPadding).fillMaxSize()
                ) {
                    when(val state = uiState) {
                        ArtStyleSettingsUiState.Loading -> {
                            Header()
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(64.dp),
                                    color = DarkD4,
                                    trackColor = Blue,
                                )
                            }
                        }
                        is ArtStyleSettingsUiState.SettingsLoaded -> {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                Header()

                                var selectedArtStyle by remember { mutableStateOf(state.mapArtStyle) }
                                // The slider holds its position (0..1) instead of the zoom itself, so switching to an
                                // art style with another zoom range keeps the thumb where it was
                                var zoomSliderPosition by remember {
                                    mutableFloatStateOf(state.mapArtStyle.zoomToSliderPosition(state.mapZoom))
                                }

                                SlideComponent(
                                    sliderPosition = { zoomSliderPosition },
                                    onSliderPositionChange = { zoomSliderPosition = it },
                                    onSliderPositionChangeFinished = {
                                        viewModel.setMapZoom(selectedArtStyle.sliderPositionToZoom(zoomSliderPosition))
                                    }
                                )
                                ArtStyleSelectors(
                                    selectedArtStyle = selectedArtStyle,
                                    onArtStyleSelected = { artStyle ->
                                        selectedArtStyle = artStyle
                                        viewModel.setMapStyle(artStyle)
                                        viewModel.setMapZoom(artStyle.sliderPositionToZoom(zoomSliderPosition))
                                    }
                                )

                                if(selectedArtStyle == SketchArtType.COLORFUL_MAP) {
                                    ColorPaletteSelectors(viewModel, state.colorPaletteSelected)
                                }
                            }
                        }
                    }
                }
            }
        }
        viewModel.getArtStyleSettings()
        return composeView
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Image(
            modifier = Modifier
                .padding(start = 11.dp)
                .width(100.dp)
                .height(50.dp),
            painter = painterResource(R.drawable.ic_martp_logo),
            contentDescription = ""
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.art_settings),
                style = TextScreenTitle,
                fontSize = 20.sp
            )
        }
        Text(
            text = stringResource(R.string.customize_next_art),
            style = TextMiddleScreenInfoText,
            modifier = Modifier.padding(start = 16.dp),
            color = DarkD4
        )
    }
}

@Composable
private fun SlideComponent(
    sliderPosition: () -> Float,
    onSliderPositionChange: (Float) -> Unit,
    onSliderPositionChangeFinished: () -> Unit
) {
    Column(
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp
        )
    ) {
        Text(
            text = stringResource(R.string.map_zoom),
            style = TextScreenTitle,
            fontSize = 18.sp
        )
        Text(
            text = stringResource(R.string.control_map_zoom),
            style = TextMiddleScreenInfoText,
            color = DarkD4
        )
        ZoomSlider(
            sliderPosition = sliderPosition,
            onSliderPositionChange = onSliderPositionChange,
            onSliderPositionChangeFinished = onSliderPositionChangeFinished
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.far),
                style = TextMiddleScreenInfoText,
                color = DarkD4
            )
            Text(
                text = stringResource(R.string.close),
                style = TextMiddleScreenInfoText,
                color = DarkD4
            )
        }
    }
}

@Composable
private fun ZoomSlider(
    sliderPosition: () -> Float,
    onSliderPositionChange: (Float) -> Unit,
    onSliderPositionChangeFinished: () -> Unit
) {
    Slider(
        value = sliderPosition(),
        onValueChange = onSliderPositionChange,
        onValueChangeFinished = onSliderPositionChangeFinished,
        colors = SliderDefaults.colors(
            thumbColor = Yellow,
            activeTrackColor = Yellow,
            inactiveTrackColor = LightL1
        ),
    )
}

@Composable
fun ArtStyleSelectors(
    selectedArtStyle: SketchArtType,
    onArtStyleSelected: (SketchArtType) -> Unit
) {
    Column(
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
        )
    ) {
        Text(
            text = stringResource(R.string.art_style),
            style = TextScreenTitle,
            fontSize = 18.sp
        )
        Text(
            modifier = Modifier.padding(vertical = 4.dp),
            text = stringResource(R.string.choose_visual_style),
            style = TextMiddleScreenInfoText,
            color = DarkD4
        )
        Column {
            ArtStyleCardSelector(
                isSelected = selectedArtStyle == SketchArtType.DEFAULT,
                onSelect = onArtStyleSelected,
                mapArtStyleModel = MapArtStyleModel(
                    titleId = R.string.classic,
                    descriptionId = R.string.classic_description,
                    drawableImageId = R.drawable.ic_martp_logo,
                    sketchArtType = SketchArtType.DEFAULT
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            ArtStyleCardSelector(
                isSelected = selectedArtStyle == SketchArtType.COLORFUL_MAP,
                onSelect = onArtStyleSelected,
                mapArtStyleModel = MapArtStyleModel(
                    titleId = R.string.colorful_map,
                    descriptionId = R.string.colorful_map_description,
                    drawableImageId = R.drawable.ic_colorful_map,
                    sketchArtType = SketchArtType.COLORFUL_MAP
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            ArtStyleCardSelector(
                isSelected = selectedArtStyle == SketchArtType.POINTILLISM,
                onSelect = onArtStyleSelected,
                mapArtStyleModel = MapArtStyleModel(
                    titleId = R.string.pointillism,
                    descriptionId = R.string.pointillism_colorful_description,
                    drawableImageId = R.drawable.ic_pointllism,
                    sketchArtType = SketchArtType.POINTILLISM
                )
            )
            //Still work in progress
//            Spacer(modifier = Modifier.height(8.dp))
//            ArtStyleCardSelector(
//                viewModel = viewModel,
//                currentArtStyleSelected = selectedArtStyle,
//                mapArtStyleModel = MapArtStyleModel(
//                    titleId = R.string.water_flow,
//                    descriptionId = R.string.water_flow_description,
//                    drawableImageId = R.drawable.ic_water_flow,
//                    sketchArtType = SketchArtType.WATER_FLOW
//                )
//            )
            //Stil work in progress
//            Spacer(modifier = Modifier.height(8.dp))
//            ArtStyleCardSelector(
//                viewModel = viewModel,
//                currentArtStyleSelected = selectedArtStyle,
//                mapArtStyleModel = MapArtStyleModel(
//                    titleId = R.string.walking_people,
//                    descriptionId = R.string.walking_people_description,
//                    drawableImageId = R.drawable.ic_walking_people,
//                    sketchArtType = SketchArtType.PARTICLE_SYSTEM
//                )
//            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ColorPaletteSelectors(
    viewModel: MainViewModel,
    loadedColorPalette: String
) {
    var selectedColorPaletteName by remember {
        mutableStateOf(ColorPaletteProvider.byNameOrDefault(loadedColorPalette).name)
    }
    val onColorPaletteSelected: (String) -> Unit = { colorPaletteName ->
        selectedColorPaletteName = colorPaletteName
        viewModel.setColorPalette(colorPaletteName)
    }
    Column(
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
        )
    ) {

        Text(
            text = stringResource(R.string.color_palette),
            style = TextScreenTitle,
            fontSize = 18.sp
        )

        Text(
            modifier = Modifier.padding(vertical = 4.dp),
            text = stringResource(R.string.choose_complete_color_set),
            style = TextMiddleScreenInfoText,
            color = DarkD4
        )

        for(colorPalette in ColorPaletteProvider.all) {
            ColorPaletteCardSelector(
                colorPalette = colorPalette,
                isSelected = colorPalette.name == selectedColorPaletteName,
                onSelect = onColorPaletteSelected
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ArtStyleCardSelector(
    mapArtStyleModel: MapArtStyleModel,
    isSelected: Boolean,
    onSelect: (SketchArtType) -> Unit,
) {
    Card(
        onClick = { onSelect(mapArtStyleModel.sketchArtType) },
        colors = CardDefaults.cardColors(
            containerColor = White,
        ),
        border = if(isSelected) BorderStroke(
            2.dp,
            Yellow
        ) else BorderStroke(
            0.5.dp, LightL5
        ),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Box(
                modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if(isSelected) LightYellow else LightL1)
                            .height(44.dp)
                            .width(44.dp)
            ) {
                Icon(
                    painter = painterResource(mapArtStyleModel.drawableImageId),
                    contentDescription = "Card Icon",
                    tint = if(isSelected) Yellow else  DarkD4,
                    modifier = Modifier
                        .width(28.dp)
                        .height(28.dp)
                        .align(Alignment.Center)
                )
            }
            Column(
                modifier = Modifier.padding(start = 8.dp, end = 4.dp).weight(1f)
            ) {
                Text(
                    text = stringResource(mapArtStyleModel.titleId),
                    style = TextScreenTitle,
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(mapArtStyleModel.descriptionId),
                    style = TextMiddleScreenInfoText,
                    fontSize = 12.sp,
                    color = DarkD4,
                )
            }
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                RadioButton(
                    selected = isSelected,
                    colors = RadioButtonColors(
                        selectedColor = Yellow,
                        unselectedColor = DarkD5,
                        disabledSelectedColor = White,
                        disabledUnselectedColor = White
                    ),
                    onClick = { onSelect(mapArtStyleModel.sketchArtType) }
                )
            }
        }
    }
}

@Composable
fun ColorPaletteCardSelector(
    colorPalette: ColorPalette,
    isSelected: Boolean,
    onSelect: (String) -> Unit,
) {
    Card(
        onClick = { onSelect(colorPalette.name) },
        colors = CardDefaults.cardColors(
            containerColor = White,
        ),
        border = if(isSelected) BorderStroke(
            2.dp,
            Yellow
        ) else BorderStroke(
            0.5.dp, LightL5
        ),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 12.dp)
        ) {
            Column(
            ) {
                Text(
                    text = colorPalette.name,
                    style = TextScreenTitle,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for(color in colorPalette.colors) {
                        Box(
                            modifier =
                                Modifier
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .height(24.dp)
                                    .width(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1F))
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                RadioButton(
                    selected = isSelected,
                    colors = RadioButtonColors(
                        selectedColor = Yellow,
                        unselectedColor = DarkD5,
                        disabledSelectedColor = White,
                        disabledUnselectedColor = White
                    ),
                    onClick = { onSelect(colorPalette.name) }
                )
            }
        }

    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Scaffold(
        containerColor = LightCream,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            Header()
            //CircularProgressIndicator()
            //ArtStyleSelectors()
        }
    }
}