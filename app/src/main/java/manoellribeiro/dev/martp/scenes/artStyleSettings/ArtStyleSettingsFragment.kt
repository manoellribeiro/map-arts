package manoellribeiro.dev.martp.scenes.artStyleSettings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import manoellribeiro.dev.martp.core.components.compose.theme.TextGraySubTitle
import manoellribeiro.dev.martp.core.components.compose.theme.TextMiddleScreenInfoText
import manoellribeiro.dev.martp.core.components.compose.theme.TextScreenTitle
import manoellribeiro.dev.martp.core.components.compose.theme.TextTitle
import manoellribeiro.dev.martp.core.components.compose.theme.Yellow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import manoellribeiro.dev.martp.core.components.compose.theme.Blue
import manoellribeiro.dev.martp.core.components.compose.theme.LightL1
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
            val uiState by viewModel.artStyleSettingsState.observeAsState()
            Scaffold(
                containerColor = LightCream,
                contentWindowInsets = WindowInsets.safeDrawing,
            ) { contentPadding ->
                Column(modifier = Modifier.padding(contentPadding)) {
                    Header()
                    when(uiState) {
                        ArtStyleSettingsUiState.Loading -> LoadingScreen()
                        is ArtStyleSettingsUiState.SettingsLoaded -> SlideComponent(viewModel, (uiState as ArtStyleSettingsUiState.SettingsLoaded).mapZoom)
                        null -> TODO()
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
            ResetToDefaultButton()
        }
        Text(
            text = stringResource(R.string.customize_next_art),
            style = TextMiddleScreenInfoText,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp),
            color = DarkD4
        )
    }
}
@Composable
private fun ResetToDefaultButton() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(
                border = ButtonDefaults.outlinedButtonBorder(),
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(true) {
                Log.i("ChooseArtStyleFragment", "reset button")
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_reset_to_default),
            contentDescription = "Reset icon",
            modifier = Modifier.width(16.dp).height(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.reset),
            style = TextMiddleScreenInfoText,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlideComponent(
    viewModel: MainViewModel,
    initialSlideValue: Float
) {
    var sliderPosition by remember { mutableFloatStateOf(initialSlideValue) }
    Log.i("MainViewModel", "SlideComponent")
    Column(
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp
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
        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                viewModel.setMapZoom(sliderPosition)
            },
            colors = SliderDefaults.colors(
                thumbColor = Yellow,
                activeTrackColor = Yellow,
                inactiveTrackColor = LightL1
            ),
            
            valueRange = 10F..20F
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.close),
                style = TextMiddleScreenInfoText,
                color = DarkD4
            )
            Text(
                text = stringResource(R.string.far),
                style = TextMiddleScreenInfoText,
                color = DarkD4
            )
        }
    }
}

@Composable
fun ArtStyleCard(
    mapStyleModel: MapArtStyleModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            text = mapStyleModel.title,
            style = TextTitle
        )
        Spacer(modifier.height(8.dp))
        Image(
            painter = painterResource(mapStyleModel.drawableImage),
            contentDescription = ""
        )
        Spacer(modifier.height(8.dp))
        Text(
            mapStyleModel.description,
            style = TextGraySubTitle
        )
    }
}

@Composable
fun LoadingScreen() {
    CircularProgressIndicator(
        modifier = Modifier.width(64.dp),
        color = DarkD4,
        trackColor = Blue,
    )
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
            CircularProgressIndicator()
            //SlideComponent()
        }
    }
}