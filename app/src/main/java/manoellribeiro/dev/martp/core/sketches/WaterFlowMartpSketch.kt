package manoellribeiro.dev.martp.core.sketches

import android.util.Log
import androidx.compose.runtime.currentRecomposeScope
import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import processing.core.PImage

class WaterFlowMartpSketch(
    private val padding: Int,
    canvasWidth: Float = 640.0F,
    canvasHeight: Float = 640.0F,
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

    private val waterPixelsToAnimate = arrayListOf<WaterPixel>()
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
        waterPixelsToAnimate.forEach { waterPixel ->
            mapImage.set(
                waterPixel.coordinates.first,
                waterPixel.coordinates.second,
                color(waterPixel.redValue, waterPixel.greenValue, waterPixel.blueValue)
            )
        }
        mapImage.updatePixels()
    }

    var xoff = 0F

    override fun draw() {
        waterPixelsToAnimate.forEach { waterPixel ->
            val x = waterPixel.coordinates.first
            val y = waterPixel.coordinates.second
//            mapImage.set(
//                waterPixel.coordinates.first,
//                waterPixel.coordinates.second,
//                color(
//                    (waterPixel.redValue * noise(x + xoff)).toInt(),
//                    (waterPixel.greenValue * noise(y + xoff)).toInt(),
//                    waterPixel.blueValue
//                )
//            )
//            xoff += 0.01F
//            Log.i("frameCount", frameCount.toString())
            mapImage.set(
                waterPixel.coordinates.first,
                waterPixel.coordinates.second,
                color(
                    (waterPixel.redValue * noise((0.01 * x + frameCount / 30).toFloat(), (0.03*y).toFloat(), (frameCount / 20).toFloat())).toInt(),
                    (waterPixel.greenValue * noise(((y + x)*0.04).toFloat(), (0.01*x - frameCount / 30).toFloat(), (frameCount / 50).toFloat())).toInt(),
                    waterPixel.blueValue
                )
            )
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
                        val color = oceanBlueColors.random()
                        waterPixelsToAnimate.add(
                            WaterPixel(
                                coordinates = Pair(x, y),
                                redValue = color shr 16 and 0xFF,
                                greenValue = color shr 8 and 0xFF,
                                blueValue = color and 0xFF
                            )
                        )
                    }
                }
            }
        }
    }

    private data class WaterPixel(
        val coordinates: Pair<Int, Int>,
        val redValue: Int,
        val greenValue: Int,
        val blueValue: Int
    )

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
