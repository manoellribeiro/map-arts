package manoellribeiro.dev.martp.scenes.chooseArtStyle

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.components.compose.theme.LightL5
import manoellribeiro.dev.martp.core.components.compose.theme.TextGraySubTitle
import manoellribeiro.dev.martp.core.components.compose.theme.TextMiddleScreenInfoText
import manoellribeiro.dev.martp.core.components.compose.theme.TextScreenTitle
import manoellribeiro.dev.martp.core.components.compose.theme.TextTitle
import manoellribeiro.dev.martp.core.extensions.putExtra
import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import manoellribeiro.dev.martp.scenes.createNewMapArt.CreateNewMapArtActivity

class ChooseArtStyleFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext())
        composeView.setContent {
            Scaffold(
                containerColor = LightL5,
                contentWindowInsets = WindowInsets.safeDrawing,
            ) { contentPadding ->
                Column(modifier = Modifier.padding(contentPadding)) {
                    Header()
                    StyleDescription()
                    ListOfStyles()
                }
            }
        }
        return composeView
    }
}

private fun navigateToCreateNewArtScreen(context: Context, type: SketchArtType) {
    val intent = Intent(context, CreateNewMapArtActivity::class.java)
    intent.putExtra(type)
    context.startActivity(intent)
    (context as ComponentActivity).finish()
}

@Composable
private fun ListOfStyles() {
    val context = LocalContext.current
    LazyColumn {
        item {
            ArtStyleCard(
                modifier = Modifier.clickable {
                    navigateToCreateNewArtScreen(context, SketchArtType.DEFAULT)
                },
                mapStyleModel = MapArtStyleModel(
                    title = stringResource(R.string.martp_default),
                    description = stringResource(R.string.martp_default_description),
                    drawableImage = R.drawable.ic_default_martp_style
                )
            )
        }
        item {
            ArtStyleCard(
                modifier = Modifier.clickable {
                    navigateToCreateNewArtScreen(context, SketchArtType.POINTILLISM)
                },
                mapStyleModel = MapArtStyleModel(
                    title = stringResource(R.string.pointillism),
                    description = stringResource(R.string.pointillism_description),
                    drawableImage = R.drawable.ic_pointllism_martp_style
                )
            )
        }
    }
}

@Composable
private fun StyleDescription() {
    Text(
        text = stringResource(R.string.choose_art_style_description),
        style = TextMiddleScreenInfoText,
        modifier = Modifier.padding(12.dp)
    )
}

@Composable
private fun Header() {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        IconButton(
            onClick = {
                onBackPressedDispatcher?.onBackPressed()
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back Screen Button"
            )
        }
        Text(
            modifier = Modifier.align(alignment = Alignment.Center),
            text = stringResource(R.string.choose_art_style_title),
            style = TextScreenTitle
        )
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            Header()
            StyleDescription()
            ListOfStyles()
        }
    }
}