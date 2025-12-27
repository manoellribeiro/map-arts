package manoellribeiro.dev.martp.core.sketches

import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import processing.core.PImage

class PointillismMartpSketch(
    private val horizontalTilesCount: Int,
    private val verticalTilesCount: Int,
    private val padding: Int,
    private val canvasWidth: Float = 640.0F,
    private val canvasHeight: Float = 640.0F,
    private val imagePath: String,
    private val drawingFinishedCallback: () -> Unit
) : MartpSketch(
    horizontalTilesCount,
    verticalTilesCount,
    padding,
    canvasWidth,
    canvasHeight,
    imagePath,
    drawingFinishedCallback
)  {
    override fun draw() {
        val mapImage = loadImage(imagePath)
        changePixelColors(mapImage)
        drawArtFrame()
        image(mapImage, frameThickness + framePadding, frameThickness + framePadding)
        filter(ERODE)
        fill(color(18, 13, 49))
        pixelsToAddCircles.forEach {
            ellipseMode(RADIUS)
            ellipse(it.first.toFloat() + frameThickness + framePadding, it.second.toFloat() + frameThickness + framePadding, 5F, 5F)
        }

        noLoop()
        drawingFinishedCallback.invoke()
    }

    private val pixelsToAddCircles = arrayListOf<Pair<Int, Int>>()

    private fun changePixelColors(mapImage: PImage) {
        val colorsArray = arrayListOf(
            color(204, 88, 3),
            color(226, 113, 29),
            color(255, 149, 5),
        )

        var nextXPixelAllowedToPutCircle = 0F
        var nextYPixelAllowedToPutCircle = 0F

        mapImage.loadPixels()

        for (y in 0..mapImage.height) {
            for (x in 0..mapImage.width) {
                val currentPixelColor = mapImage.get(x, y)
                if(isStreetPixel(currentPixelColor) && y >= nextYPixelAllowedToPutCircle) {
                    if(x >= nextXPixelAllowedToPutCircle) {
                        pixelsToAddCircles.add(Pair(x, y))
                        nextXPixelAllowedToPutCircle = nextXPixelAllowedToPutCircle + 7
                    }
                } else {
                    mapImage.set(x, y, color(255, 201, 113))
                }
            }
            nextXPixelAllowedToPutCircle = 0F

            if(y >= nextYPixelAllowedToPutCircle) {
                nextYPixelAllowedToPutCircle = nextYPixelAllowedToPutCircle + 7
            }
        }
        mapImage.updatePixels()
    }

    override val type: SketchArtType
        get() = SketchArtType.POINTILLISM
}
