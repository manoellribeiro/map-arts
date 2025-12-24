package manoellribeiro.dev.martp.scenes.chooseArtStyle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.components.compose.theme.TextGraySubTitle
import manoellribeiro.dev.martp.core.components.compose.theme.TextMiddleScreenInfoText
import manoellribeiro.dev.martp.core.components.compose.theme.TextScreenTitle
import manoellribeiro.dev.martp.core.components.compose.theme.TextTitle

class ChooseArtStyleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { contentPadding ->
                Column(modifier = Modifier.padding(contentPadding)) {
                    Header()
                    StyleDescription()
                    ListOfStyles()
                }
            }
        }
    }
}

@Composable
private fun ListOfStyles() {
    LazyColumn {
        item {
            ArtStyleCard(
                mapStyleModel = MapArtStyleModel(
                    title = stringResource(R.string.martp_default),
                    description = stringResource(R.string.martp_default_description),
                    drawableImage = R.drawable.ic_default_martp_style
                )
            )
        }
        item {
            ArtStyleCard(
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        IconButton(
            onClick = {}
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
            .clickable {  }
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