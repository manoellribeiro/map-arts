package manoellribeiro.dev.martp.core.sketches

import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import processing.core.PImage

//TODO: this is a unfinished sketch
class WaterFlowMartpSketch(
    private val horizontalTilesCount: Int,
    private val verticalTilesCount: Int,
    private val padding: Int,
    private val canvasWidth: Float = 640.0F,
    private val canvasHeight: Float = 640.0F,
    private val imagePath: String,
) : MartpSketch(
    horizontalTilesCount,
    verticalTilesCount,
    padding,
    canvasWidth,
    canvasHeight,
    imagePath,
)  {

    override fun setup() {
        super.setup()
        val mapImage = loadImage(imagePath)
        changePixelColors(mapImage)
        drawArtFrame()
        image(mapImage, frameThickness + framePadding, frameThickness + framePadding)
        filter(ERODE)
        val brazilianColors = arrayListOf(
            color(254, 251, 0),
            color(255, 255, 255),
            color(1, 33, 105)
        )
        val prideColors = arrayListOf(
            color(228, 3, 3),
            color(255, 140, 0),
            color(255, 237, 0),
            color(0, 128, 38),
            color(0, 76, 255),
            color(115, 41, 130),
        )
        pixelsToAddCircles.forEach {
            fill(brazilianColors.random())
            ellipseMode(RADIUS)
            ellipse(it.first.toFloat() + frameThickness + framePadding, it.second.toFloat() + frameThickness + framePadding, 4F, 4F)
        }
    }

    override fun draw() {
        val brazilianColors = arrayListOf(
            color(254, 251, 0),
            color(255, 255, 255),
            color(1, 33, 105)
        )
        pixelsToAddCircles.forEach {
            fill(brazilianColors.random())
            ellipseMode(RADIUS)
            ellipse(it.first.toFloat() + frameThickness + framePadding, it.second.toFloat() + frameThickness + framePadding, 4F, 4F)
        }
    }

    private val pixelsToAddCircles = arrayListOf<Pair<Int, Int>>()

    private fun changePixelColors(mapImage: PImage) {
        var nextXPixelAllowedToPutCircle = 0F
        var nextYPixelAllowedToPutCircle = 0F

        mapImage.loadPixels()

        for (y in 0..mapImage.height) {
            for (x in 0..mapImage.width) {
                val currentPixelColor = mapImage.get(x, y)
                if(isStreetPixel(currentPixelColor) && y >= nextYPixelAllowedToPutCircle) {
                    if(x >= nextXPixelAllowedToPutCircle) {
                        pixelsToAddCircles.add(Pair(x, y))
                        nextXPixelAllowedToPutCircle = nextXPixelAllowedToPutCircle + 8
                    }
                } else {
                    mapImage.set(x, y, color(0, 151, 57))
                }
            }
            nextXPixelAllowedToPutCircle = 0F

            if(y >= nextYPixelAllowedToPutCircle) {
                nextYPixelAllowedToPutCircle = nextYPixelAllowedToPutCircle + 8
            }
        }
        mapImage.updatePixels()
    }

    override val type: SketchArtType
        get() = SketchArtType.POINTILLISM
}
