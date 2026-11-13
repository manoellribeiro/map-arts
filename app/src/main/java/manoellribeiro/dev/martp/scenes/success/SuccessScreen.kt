package manoellribeiro.dev.martp.scenes.success

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.components.compose.ArtSettingRow
import manoellribeiro.dev.martp.core.components.compose.ArtStyleIndicator
import manoellribeiro.dev.martp.core.components.compose.ColorPaletteIndicator
import manoellribeiro.dev.martp.core.components.compose.MapZoomIndicator
import manoellribeiro.dev.martp.core.components.compose.MartpPrimaryButton
import manoellribeiro.dev.martp.core.components.compose.theme.Blue
import manoellribeiro.dev.martp.core.components.compose.theme.BlueAlpha80
import manoellribeiro.dev.martp.core.components.compose.theme.TextGraySubTitle
import manoellribeiro.dev.martp.core.components.compose.theme.TextScreenTitle
import manoellribeiro.dev.martp.core.components.compose.theme.TextTitle
import manoellribeiro.dev.martp.core.components.compose.theme.WhiteAlpha80
import manoellribeiro.dev.martp.core.components.compose.theme.LightCream
import manoellribeiro.dev.martp.core.components.compose.theme.White
import manoellribeiro.dev.martp.core.components.compose.theme.Yellow
import manoellribeiro.dev.martp.core.sketches.MartpSketch
import kotlin.math.max
import kotlin.math.roundToInt

// The share button keeps the same proportion to the art on every screen size
private const val ShareButtonArtWidthFraction = 0.116f
private const val ShareButtonGapArtWidthFraction = 0.012f
private val ShareButtonMinSize = 24.dp

@Composable
fun SuccessScreen(
    imagePathLocation: String,
    onShareClick: () -> Unit,
    onCreateAnotherArtClick: () -> Unit
) {
    val artImage by produceState<ImageBitmap?>(initialValue = null, imagePathLocation) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(imagePathLocation)?.asImageBitmap()
        }
    }
    SuccessContent(
        artImage = artImage,
        onShareClick = onShareClick,
        onCreateAnotherArtClick = onCreateAnotherArtClick
    )
}

@Composable
private fun SuccessContent(
    artImage: ImageBitmap?,
    onShareClick: () -> Unit,
    onCreateAnotherArtClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightCream)
    ) {
        // The yellow band goes behind the status bar and ends right below the art status
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Yellow)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 20.dp)
        ) {
            SuccessHeader()
            SavedArtWithStatus(
                artImage = artImage,
                onShareClick = onShareClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp)
            )
        }
        Column(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
            )
        ) {
            RemixSection(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp))
            MartpPrimaryButton(
                text = stringResource(R.string.success_create_another_art),
                onClick = onCreateAnotherArtClick,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun SuccessHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                tint = Blue,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.success_saved_to_gallery).uppercase(),
                style = TextTitle,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                color = Blue
            )
        }
        Text(
            text = stringResource(R.string.success_title),
            style = TextScreenTitle,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center,
            color = Blue,
            modifier = Modifier
                .padding(top = 8.dp)
                .semantics { heading() }
        )
    }
}

// The art and its status are centered together, so extra height goes around them instead of between them
@Composable
private fun SavedArtWithStatus(
    artImage: ImageBitmap?,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spotlightColor = WhiteAlpha80
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .drawWithCache {
                val spotlight = Brush.radialGradient(
                    colors = listOf(spotlightColor, Color.Transparent),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension / 2f
                )
                onDrawBehind { drawRect(spotlight) }
            }
    ) {
        if (artImage != null) {
            SavedArt(
                artImage = artImage,
                onShareClick = onShareClick,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        ApprovalStatusCaption(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

@Composable
private fun SavedArt(
    artImage: ImageBitmap,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.aspectRatio(artImage.width.toFloat() / artImage.height)) {
        Image(
            bitmap = artImage,
            contentDescription = stringResource(R.string.success_saved_art_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        ShareIconButton(
            onClick = onShareClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .shareButtonPlacement(artImageWidth = artImage.width)
        )
    }
}

// Sizes the button from the art width and pushes it past the sketch frame drawn on the saved image, onto the map
private fun Modifier.shareButtonPlacement(artImageWidth: Int) = layout { measurable, constraints ->
    val artWidth = constraints.maxWidth
    val buttonSize = max((artWidth * ShareButtonArtWidthFraction).roundToInt(), ShareButtonMinSize.roundToPx())
    val frameFraction = (MartpSketch.frameThickness + MartpSketch.framePadding) / artImageWidth
    val inset = (artWidth * (frameFraction + ShareButtonGapArtWidthFraction)).roundToInt()
    val placeable = measurable.measure(Constraints.fixed(buttonSize, buttonSize))
    layout(buttonSize + inset, buttonSize + inset) {
        placeable.place(0, inset)
    }
}

@Composable
private fun ShareIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(BlueAlpha80)
            .border(1.5.dp, White, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_share),
            contentDescription = stringResource(R.string.success_share),
            tint = White,
            modifier = Modifier.fillMaxSize(0.45f)
        )
    }
}

@Composable
private fun ApprovalStatusCaption(modifier: Modifier = Modifier) {
    val dotColor = Blue
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                modifier = Modifier
                    .size(8.dp)
                    .drawBehind {
                        drawCircle(color = dotColor.copy(alpha = 0.35f), radius = size.minDimension)
                        drawCircle(color = dotColor)
                    }
            )
            Text(
                text = stringResource(R.string.success_awaiting_approval),
                style = TextTitle,
                color = Blue,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            text = stringResource(R.string.success_awaiting_approval_description),
            style = TextGraySubTitle,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
        )
    }
}

@Composable
private fun RemixSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.success_remix_title),
            style = TextTitle,
            fontSize = 16.sp,
            color = Blue,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.success_remix_body, stringResource(R.string.menu_edit)),
            style = TextGraySubTitle,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
        )
        ArtSettingRow(
            title = stringResource(R.string.art_style),
            description = stringResource(
                R.string.success_remix_art_style_description,
                stringResource(R.string.colorful_map),
                stringResource(R.string.classic),
                stringResource(R.string.pointillism)
            ),
            showTopDivider = false
        ) {
            ArtStyleIndicator()
        }
        ArtSettingRow(
            title = stringResource(R.string.color_palette),
            description = stringResource(R.string.success_remix_color_palette_description),
            showTopDivider = true
        ) {
            ColorPaletteIndicator()
        }
        ArtSettingRow(
            title = stringResource(R.string.map_zoom),
            description = stringResource(R.string.success_remix_map_zoom_description),
            showTopDivider = true
        ) {
            MapZoomIndicator()
        }
    }
}

@Preview(device = "spec:width=393dp,height=852dp")
@Composable
private fun SuccessContentPreview() {
    SuccessContent(
        artImage = null,
        onShareClick = {},
        onCreateAnotherArtClick = {}
    )
}

@Preview(device = "spec:width=360dp,height=640dp")
@Composable
private fun SuccessContentSmallPhonePreview() {
    SuccessContent(
        artImage = null,
        onShareClick = {},
        onCreateAnotherArtClick = {}
    )
}
