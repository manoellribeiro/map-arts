package manoellribeiro.dev.martp.core.sketches

import android.util.Log
import androidx.compose.runtime.currentRecomposeScope
import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import processing.core.PImage

class WaterFlowMartpSketch(
    private val padding: Int,
    private val canvasWidth: Float = 640.0F,
    private val canvasHeight: Float = 640.0F,
    private val imagePath: String,
) : MartpSketch(
    0,
    0,
    padding,
    canvasWidth,
    canvasHeight,
    imagePath,
) {

    val current = arrayListOf<Int>()
    val previous = arrayListOf<Int>()

    private val waterPixelsToAnimate = arrayListOf<Pair<Int, Int>>()
    private val waterTextPixelsToPaint = arrayListOf<Pair<Int, Int>>()
    private lateinit var mapImage: PImage
    private val oceanBlueColors = arrayListOf(
        color(3, 4, 94),
        color(2, 62, 138),
        color(0, 119, 182),
        color(0, 150, 199),
        color(0, 180, 216),
        color(72, 202, 228),
        color(144, 224, 239),
        color(173, 232, 244),
        color(202, 240, 248),
    )

    override fun setup() {
        mapImage = loadImage(imagePath)
        changePixelColors(mapImage)
        drawArtFrame()
        image(mapImage, frameThickness + framePadding, frameThickness + framePadding)
        filter(ERODE)
        smooth()
        waterPixelsToAnimate.forEach {
            mapImage.set(it.first, it.second, oceanBlueColors.random())
        }
        mapImage.updatePixels()
    }


    override fun draw() {
        waterPixelsToAnimate.forEach {
            mapImage.set(it.first, it.second, oceanBlueColors.random())
        }
        mapImage.updatePixels()
        image(mapImage, frameThickness + framePadding, frameThickness + framePadding)
    }

    // TODO: Getting the color of a single pixel with get(x, y) is easy, but not as fast as grabbing the data directly from pixels[]. The equivalent statement to get(x, y) using pixels[] is pixels[y*width+x]. See the reference for pixels[] for more information.
    private fun changePixelColors(mapImage: PImage) {
        mapImage.loadPixels()

        for (y in 0..mapImage.height) {
            for (x in 0..mapImage.width) {
                val currentPixelColor = mapImage.get(x, y)
                when {
                    isWaterPixel(currentPixelColor) -> {
                        waterPixelsToAnimate.add(Pair(x, y))
                    }
                }
            }
        }
    }

    private fun isWaterPixel(
        color: Int
    ): Boolean {
        val redValue = color shr 16 and 0xFF
        val greenValue = color shr 8 and 0xFF
        val blueValue = color and 0xFF
        return redValue in 28..30 &&
                greenValue in 20..22 &&
                blueValue in 243..245
    }

    override val type: SketchArtType
        get() = SketchArtType.WATER_FLOW
}
