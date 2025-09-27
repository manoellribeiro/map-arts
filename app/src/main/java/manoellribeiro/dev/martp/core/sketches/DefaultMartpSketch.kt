package manoellribeiro.dev.martp.core.sketches

import android.R
import processing.core.PApplet
import processing.core.PImage

class DefaultMartpSketch(
    private val horizontalTilesCount: Int,
    private val verticalTilesCount: Int,
    private val padding: Int,
    private val canvasWidth: Float = 640.0F,
    private val canvasHeight: Float = 640.0F,
    private val imagePath: String,
    private val drawingFinishedCallback: () -> Unit
) : PApplet() {

    companion object {
        const val frameThickness = 30.0F
        const val framePadding = 30.0F
    }

    override fun settings() {
        size(canvasWidth.toInt(), canvasHeight.toInt())
    }

    override fun setup() {
        super.setup()
    }

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
        val colorsArray2 = arrayListOf(
            color(2, 244, 208),
            color(18, 13, 49),
            color(250, 121, 33),
        )

        //val streetPixels = arrayListOf();

        var nextXPixelAllowedToPutCircle = 0F
        var nextYPixelAllowedToPutCircle = 0F

        mapImage.loadPixels()

        for (y in 0..mapImage.height) {
            val xValuesForSameY = arrayListOf<Int>()
            for (x in 0..mapImage.width) {
                val currentPixelColor = mapImage.get(x, y)
                if(isStreetPixel(currentPixelColor) && y >= nextYPixelAllowedToPutCircle) {
                    val newPixelColor = colorsArray[(0 until colorsArray.size).random()]
                    //mapImage.set(x, y, color(255, 255, 255))
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
            //streetPixels.add()
        }

        mapImage.updatePixels()

    }

    private fun isStreetPixel(
        color: Int
    ): Boolean {
        val redValue = color shr 16 and 0xFF
        val greenValue = color shr 8 and 0xFF
        val blueValue = color and 0xFF
        val rangeOfStreetColorsForRGB = 56..64
        return redValue in rangeOfStreetColorsForRGB &&
            greenValue in rangeOfStreetColorsForRGB &&
            blueValue in rangeOfStreetColorsForRGB
    }

    private fun drawArtFrame() {
        noStroke()
        val frameGray = color(51, 51, 51)
        fill(frameGray)
        rect(0F, 0F, canvasWidth, canvasHeight)
        val white = color(255, 255, 255)
        fill(white)
        rect(
            frameThickness,
            frameThickness,
            canvasWidth - (2 * frameThickness),
            canvasHeight - (2 * frameThickness)
        )
    }

}
