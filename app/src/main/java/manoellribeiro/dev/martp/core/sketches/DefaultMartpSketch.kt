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
    private val drawingFinishedCallback: () -> Unit
) : MartpSketch(
    horizontalTilesCount,
    verticalTilesCount,
    padding,
    canvasWidth,
    canvasHeight,
    imagePath,
    drawingFinishedCallback
) {


    override fun draw() {
        val mapImage = loadImage(imagePath)
        changePixelColors(mapImage)
        drawArtFrame()
        image(mapImage, frameThickness + framePadding, frameThickness + framePadding)
        filter(ERODE)
        noLoop()
        drawingFinishedCallback.invoke()
    }

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

        mapImage.loadPixels()

        for (y in 0..mapImage.height) {
            val xValuesForSameY = arrayListOf<Int>()
            for (x in 0..mapImage.width) {
                val currentPixelColor = mapImage.get(x, y)
                if(isStreetPixel(currentPixelColor)) {
                    val newPixelColor = colorsArray[(0 until colorsArray.size).random()]
                    mapImage.set(x, y, color(18, 13, 49))
                } else {
                    mapImage.set(x, y, color(255, 201, 113))
                }
            }
            //streetPixels.add()
        }
        mapImage.updatePixels()
    }

    override val type: SketchArtType
        get() = SketchArtType.DEFAULT
}