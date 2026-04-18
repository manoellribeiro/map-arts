package manoellribeiro.dev.martp.core.sketches

import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import processing.core.PApplet
import processing.core.PImage

class DefaultMartpSketch(
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
) {


    override fun draw() {
        val mapImage = loadImage(imagePath)
        changePixelColors(mapImage)
        drawArtFrame()
        image(mapImage, frameThickness + framePadding, frameThickness + framePadding)
        filter(ERODE)
        noLoop()
    }

    private fun changePixelColors(mapImage: PImage) {
        mapImage.loadPixels()

        for (y in 0..mapImage.height) {
            val xValuesForSameY = arrayListOf<Int>()
            for (x in 0..mapImage.width) {
                val currentPixelColor = mapImage.get(x, y)
                if(isStreetPixel(currentPixelColor)) {
                    mapImage.set(x, y, color(18, 13, 49))
                } else {
                    mapImage.set(x, y, color(255, 201, 113))
                }
            }
        }
        mapImage.updatePixels()
    }

    override val type: SketchArtType
        get() = SketchArtType.DEFAULT
}