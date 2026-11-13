package manoellribeiro.dev.martp.scenes.onboarding

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.components.compose.ArtSettingRow
import manoellribeiro.dev.martp.core.components.compose.ArtStyleIndicator
import manoellribeiro.dev.martp.core.components.compose.ColorPaletteIndicator
import manoellribeiro.dev.martp.core.components.compose.MapZoomIndicator
import manoellribeiro.dev.martp.core.components.compose.MartpPrimaryButton
import manoellribeiro.dev.martp.core.components.compose.theme.Blue
import manoellribeiro.dev.martp.core.components.compose.theme.BlueAlpha12
import manoellribeiro.dev.martp.core.components.compose.theme.ChipShape
import manoellribeiro.dev.martp.core.components.compose.theme.DarkD4
import manoellribeiro.dev.martp.core.components.compose.theme.DarkD5
import manoellribeiro.dev.martp.core.components.compose.theme.GreenDarker
import manoellribeiro.dev.martp.core.components.compose.theme.LightCream
import manoellribeiro.dev.martp.core.components.compose.theme.LightL3
import manoellribeiro.dev.martp.core.components.compose.theme.LightYellow
import manoellribeiro.dev.martp.core.components.compose.theme.TextGraySubTitle
import manoellribeiro.dev.martp.core.components.compose.theme.TextScreenTitle
import manoellribeiro.dev.martp.core.components.compose.theme.TextTitle
import manoellribeiro.dev.martp.core.components.compose.theme.Yellow

private enum class OnboardingStep { LOCATION_ART, SIGNATURE, EXHIBITION }

private val onboardingSteps = OnboardingStep.entries

// The example art card shows a real place and a fixed date, like a card of the gallery
private const val ExampleArtTitle = "Salvador, Brazil"
private const val ExampleArtDate = "05/11/2026 13:10"

// Before the example art is decoded its frame is landscape, like the arts made on a phone
private const val DefaultArtAspectRatio = 7f / 4f

private val PageHorizontalPadding = 24.dp
private val SelectedDotWidth = 22.dp
private val DotSize = 6.dp

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val resources = LocalContext.current.resources
    // Decoded once, off the main thread, and shared by the pages the pager creates and disposes
    val exampleArt by produceState<ImageBitmap?>(initialValue = null, resources) {
        value = decodeImage(resources, R.drawable.onboarding_example_art)
    }
    val globe by produceState<ImageBitmap?>(initialValue = null, resources) {
        value = decodeImage(resources, R.drawable.onboarding_globe)
    }
    OnboardingContent(
        exampleArt = exampleArt,
        globe = globe,
        onFinish = onFinish
    )
}

private suspend fun decodeImage(resources: Resources, @DrawableRes imageId: Int): ImageBitmap? =
    withContext(Dispatchers.IO) {
        BitmapFactory.decodeResource(resources, imageId)?.asImageBitmap()
    }

@Composable
private fun OnboardingContent(
    exampleArt: ImageBitmap?,
    globe: ImageBitmap?,
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingSteps.size })
    val coroutineScope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val isLastPage = currentPage == onboardingSteps.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightCream)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        OnboardingTopBar(
            currentPage = currentPage,
            pageCount = onboardingSteps.size,
            showSkip = !isLastPage,
            onSkipClick = onFinish
        )
        HorizontalPager(
            state = pagerState,
            key = { page -> onboardingSteps[page].name },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (onboardingSteps[page]) {
                OnboardingStep.LOCATION_ART -> LocationArtStep(exampleArt = exampleArt)
                OnboardingStep.SIGNATURE -> SignatureStep(exampleArt = exampleArt)
                OnboardingStep.EXHIBITION -> ExhibitionStep(globe = globe)
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
        ) {
            PagerDots(pageCount = onboardingSteps.size, currentPage = currentPage)
            MartpPrimaryButton(
                text = stringResource(if (isLastPage) R.string.onboarding_start_creating else R.string.onboarding_next),
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        coroutineScope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                    }
                }
            )
        }
    }
}

@Composable
private fun OnboardingTopBar(
    currentPage: Int,
    pageCount: Int,
    showSkip: Boolean,
    onSkipClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 24.dp, end = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_step_count, currentPage + 1, pageCount).uppercase(),
            style = TextTitle,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp,
            color = DarkD5
        )
        if (showSkip) {
            TextButton(onClick = onSkipClick) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    style = TextTitle,
                    fontSize = 15.sp,
                    color = GreenDarker
                )
            }
        }
    }
}

@Composable
private fun LocationArtStep(exampleArt: ImageBitmap?) {
    OnboardingStepPage(
        title = stringResource(R.string.onboarding_art_title),
        description = stringResource(R.string.onboarding_art_description),
        visual = { ExampleArtCard(art = exampleArt, signature = null) },
        footer = { ArtChoices(modifier = Modifier.padding(top = 16.dp)) }
    )
}

@Composable
private fun SignatureStep(exampleArt: ImageBitmap?) {
    OnboardingStepPage(
        title = stringResource(R.string.onboarding_signature_title),
        description = stringResource(
            R.string.onboarding_signature_description,
            stringResource(R.string.menu_user)
        ),
        visual = {
            ExampleArtCard(
                art = exampleArt,
                signature = stringResource(R.string.onboarding_example_username)
            )
        }
    )
}

@Composable
private fun ExhibitionStep(globe: ImageBitmap?) {
    OnboardingStepPage(
        title = stringResource(R.string.onboarding_exhibition_title),
        description = stringResource(R.string.onboarding_exhibition_description),
        visual = {
            GlobeImage(
                globe = globe,
                contentDescription = stringResource(R.string.onboarding_globe_description)
            )
        }
    )
}

// The exported globe brings the art card sticking out on its right, so the image is wider than the
// globe. The globe (as tall as the image) is centered and the card may reach into the page padding.
@Composable
private fun GlobeImage(
    globe: ImageBitmap?,
    contentDescription: String
) {
    if (globe == null) {
        Box(
            modifier = Modifier
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .background(LightL3)
        )
        return
    }
    val imageToGlobeRatio = globe.width.toFloat() / globe.height
    Image(
        bitmap = globe,
        contentDescription = contentDescription,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.layout { measurable, constraints ->
            val maxWidth = constraints.maxWidth
            // With the globe centered, the image ends half a globe plus the card after the middle
            val diameterFittingTheCard =
                (maxWidth / 2f + PageHorizontalPadding.toPx()) / (imageToGlobeRatio - 0.5f)
            val diameter = minOf(diameterFittingTheCard, maxWidth.toFloat(), constraints.maxHeight.toFloat())
                .roundToInt()
            val placeable = measurable.measure(
                Constraints.fixed((diameter * imageToGlobeRatio).roundToInt(), diameter)
            )
            layout(maxWidth, diameter) {
                placeable.place((maxWidth - diameter) / 2, 0)
            }
        }
    )
}

// The visual takes the height left by the texts, so it shrinks on small phones instead of pushing them away
@Composable
private fun OnboardingStepPage(
    title: String,
    description: String,
    visual: @Composable () -> Unit,
    footer: (@Composable () -> Unit)? = null
) {
    val spotlightColor = Yellow.copy(alpha = 0.25f)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PageHorizontalPadding)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .drawWithCache {
                    val spotlight = Brush.radialGradient(
                        colors = listOf(spotlightColor, Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension / 2f
                    )
                    onDrawBehind { drawRect(spotlight) }
                }
                .padding(vertical = 8.dp)
        ) {
            visual()
        }
        Text(
            text = title,
            style = TextScreenTitle,
            fontSize = 24.sp,
            lineHeight = 29.sp,
            color = Blue,
            modifier = Modifier
                .padding(top = 16.dp)
                .semantics { heading() }
        )
        Text(
            text = description,
            style = TextGraySubTitle,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        footer?.invoke()
    }
}

// Built like a card of the gallery: the art (which brings its own frame), its place and its date
@Composable
private fun ExampleArtCard(
    art: ImageBitmap?,
    signature: String?,
    modifier: Modifier = Modifier
) {
    val aspectRatio = if (art != null) art.width.toFloat() / art.height else DefaultArtAspectRatio
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
    ) {
        OnboardingImage(
            image = art,
            contentDescription = stringResource(R.string.onboarding_example_art_description),
            modifier = Modifier
                .weight(1f, fill = false)
                .aspectRatio(aspectRatio)
        )
        Text(
            text = ExampleArtTitle,
            style = TextTitle,
            fontSize = 15.sp,
            color = Blue,
            modifier = Modifier.padding(top = 10.dp)
        )
        if (signature != null) {
            Signature(
                username = signature,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = null,
                tint = DarkD4,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = ExampleArtDate,
                style = TextGraySubTitle,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun Signature(
    username: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .background(LightYellow, ChipShape)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_person_edit),
            contentDescription = null,
            tint = Blue,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = username,
            style = TextTitle,
            fontSize = 13.sp,
            color = Blue
        )
    }
}

// Until the image is decoded its place is kept with a plain surface, so the layout doesn't jump
@Composable
private fun OnboardingImage(
    image: ImageBitmap?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(LightL3))
    }
}

@Composable
private fun ArtChoices(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.onboarding_choices_title),
            style = TextTitle,
            fontSize = 16.sp,
            color = Blue,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.onboarding_choices_description, stringResource(R.string.menu_edit)),
            style = TextGraySubTitle,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
        )
        ArtSettingRow(
            title = stringResource(R.string.art_style),
            description = null,
            showTopDivider = false
        ) {
            ArtStyleIndicator()
        }
        ArtSettingRow(
            title = stringResource(R.string.color_palette),
            description = null,
            showTopDivider = true
        ) {
            ColorPaletteIndicator()
        }
        ArtSettingRow(
            title = stringResource(R.string.map_zoom),
            description = null,
            showTopDivider = true
        ) {
            MapZoomIndicator()
        }
    }
}

// Decorative: the step count in the top bar already tells where the user is
@Composable
private fun PagerDots(pageCount: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clearAndSetSemantics {}
    ) {
        repeat(pageCount) { page ->
            PagerDot(isSelected = page == currentPage)
        }
    }
}

@Composable
private fun PagerDot(isSelected: Boolean) {
    val width by animateDpAsState(
        targetValue = if (isSelected) SelectedDotWidth else DotSize,
        label = "PagerDotWidth"
    )
    Box(
        modifier = Modifier
            // The animated width is read while measuring, so the animation doesn't recompose the dot
            .layout { measurable, _ ->
                val widthPx = width.roundToPx()
                val heightPx = DotSize.roundToPx()
                val placeable = measurable.measure(Constraints.fixed(widthPx, heightPx))
                layout(widthPx, heightPx) { placeable.place(0, 0) }
            }
            .background(if (isSelected) Blue else BlueAlpha12, CircleShape)
    )
}

@Preview(device = "spec:width=393dp,height=852dp")
@Composable
private fun OnboardingContentPreview() {
    OnboardingContent(
        exampleArt = null,
        globe = null,
        onFinish = {}
    )
}

@Preview(device = "spec:width=360dp,height=640dp")
@Composable
private fun OnboardingContentSmallPhonePreview() {
    OnboardingContent(
        exampleArt = null,
        globe = null,
        onFinish = {}
    )
}
