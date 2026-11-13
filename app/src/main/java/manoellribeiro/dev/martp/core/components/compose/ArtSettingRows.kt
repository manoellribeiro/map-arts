package manoellribeiro.dev.martp.core.components.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.components.compose.theme.Blue
import manoellribeiro.dev.martp.core.components.compose.theme.BlueAlpha12
import manoellribeiro.dev.martp.core.components.compose.theme.BlueAlpha8
import manoellribeiro.dev.martp.core.components.compose.theme.ChipShape
import manoellribeiro.dev.martp.core.components.compose.theme.DarkD4
import manoellribeiro.dev.martp.core.components.compose.theme.DarkD5
import manoellribeiro.dev.martp.core.components.compose.theme.ErrorMedium
import manoellribeiro.dev.martp.core.components.compose.theme.Green
import manoellribeiro.dev.martp.core.components.compose.theme.GreenDarker
import manoellribeiro.dev.martp.core.components.compose.theme.LightL2
import manoellribeiro.dev.martp.core.components.compose.theme.LightL3
import manoellribeiro.dev.martp.core.components.compose.theme.LightYellow
import manoellribeiro.dev.martp.core.components.compose.theme.TextGraySubTitle
import manoellribeiro.dev.martp.core.components.compose.theme.TextTitle
import manoellribeiro.dev.martp.core.components.compose.theme.Yellow
import manoellribeiro.dev.martp.scenes.artStyleSettings.artStyleModels

// The art settings a user can change in the Edit tab, each with a small picture of it. Shared by
// the success screen and the onboarding.

/** One art setting (art style, color palette, map zoom) with a small picture of it on the left. */
@Composable
fun ArtSettingRow(
    title: String,
    description: String?,
    showTopDivider: Boolean,
    indicator: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showTopDivider) Modifier.topDivider(BlueAlpha8) else Modifier)
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Box(modifier = Modifier.width(80.dp)) {
            indicator()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(text = title, style = TextTitle, color = Blue)
            if (description != null) {
                Text(text = description, style = TextGraySubTitle)
            }
        }
    }
}

private fun Modifier.topDivider(color: Color) = drawBehind {
    drawLine(
        color = color,
        start = Offset.Zero,
        end = Offset(size.width, 0f),
        strokeWidth = 1.dp.toPx()
    )
}

@Composable
fun ArtStyleIndicator() {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        artStyleModels.forEachIndexed { index, artStyleModel ->
            val isHighlighted = index == 0
            Icon(
                painter = painterResource(artStyleModel.drawableImageId),
                contentDescription = null,
                tint = if (isHighlighted) Blue else DarkD4,
                modifier = Modifier
                    .size(24.dp)
                    .background(if (isHighlighted) Yellow else LightL3, ChipShape)
                    .padding(5.dp)
            )
        }
    }
}

@Composable
fun ColorPaletteIndicator() {
    val swatchBorderColor = BlueAlpha12
    val colorsRows = listOf(
        listOf(Blue, Yellow, Green),
        listOf(ErrorMedium, GreenDarker, LightYellow)
    )
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (colorsRow in colorsRows) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (color in colorsRow) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, CircleShape)
                            .border(1.dp, swatchBorderColor, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun MapZoomIndicator() {
    val activeColor = Yellow
    val inactiveColor = LightL2
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_zoom_out),
            contentDescription = null,
            tint = DarkD5,
            modifier = Modifier.size(12.dp)
        )
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .drawBehind {
                    val centerY = size.height / 2f
                    val thumbX = size.width / 2f
                    val trackHeight = 3.dp.toPx()
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, centerY),
                        end = Offset(thumbX, centerY),
                        strokeWidth = trackHeight,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = inactiveColor,
                        start = Offset(thumbX, centerY),
                        end = Offset(size.width, centerY),
                        strokeWidth = trackHeight,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = activeColor,
                        radius = size.height / 2f,
                        center = Offset(thumbX, centerY)
                    )
                }
        )
        Icon(
            painter = painterResource(R.drawable.ic_zoom_in),
            contentDescription = null,
            tint = DarkD5,
            modifier = Modifier.size(12.dp)
        )
    }
}
