package manoellribeiro.dev.martp.core.sketches

import android.util.Log
import manoellribeiro.dev.martp.core.data.local.entities.ColorPaletteEntity
import manoellribeiro.dev.martp.core.extensions.blueValue
import manoellribeiro.dev.martp.core.extensions.greenValue
import manoellribeiro.dev.martp.core.extensions.hexToColorInt
import manoellribeiro.dev.martp.core.extensions.isRgbColor
import manoellribeiro.dev.martp.core.extensions.redValue
import manoellribeiro.dev.martp.core.models.MapLayersColors
import manoellribeiro.dev.martp.core.models.SketchArtType
import processing.core.PImage

class PrintableMapSketch(
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

    private var mapImage: PImage? = null
    private val whiteColor = "#FFFFFF".hexToColorInt()
    private val blackColor = "#000000".hexToColorInt()
    override fun setup() {
        super.setup()

        mapImage = loadImage(imagePath)
        changePixelColors(mapImage!!)
        drawArtFrame()
        image(mapImage, frameThickness + framePadding, frameThickness + framePadding)
        filter(ERODE)
    }

    override fun draw() {
        image(mapImage, frameThickness + framePadding, frameThickness + framePadding)
    }

    val buildingOriginalColor = color(255, 36, 36)
    val buildingStrokeOriginalColor = color(69, 36, 255)
    val roadsOriginalColor = color(211, 255, 36)
    val roadsPathOriginalColor = color(93, 249, 192)
    val waterOriginalColor = color(234, 26, 234)

    private fun changePixelColors(mapImage: PImage) {
        mapImage.loadPixels()
        val mapImagePixels = mapImage.pixels
        for (y in 0..<mapImage.height) {
            for (x in 0..<mapImage.width) {
                val currentPixelColor = mapImagePixels[y*mapImage.width+x]
                when {
                    currentPixelColor.isRgbColor(
                        redValueToCompare = roadsOriginalColor.redValue(),
                        greenValueToCompare = roadsOriginalColor.greenValue(),
                        blueValueToCompare = roadsOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, whiteColor)
                    }

                    currentPixelColor.isRgbColor(
                        redValueToCompare = roadsPathOriginalColor.redValue(),
                        greenValueToCompare = roadsPathOriginalColor.greenValue(),
                        blueValueToCompare = roadsPathOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, whiteColor)
                    }

                    currentPixelColor.isRgbColor(
                        redValueToCompare = waterOriginalColor.redValue(),
                        greenValueToCompare = waterOriginalColor.greenValue(),
                        blueValueToCompare = waterOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, whiteColor)
                    }

                    currentPixelColor.isRgbColor(
                        redValueToCompare = buildingStrokeOriginalColor.redValue(),
                        greenValueToCompare = buildingStrokeOriginalColor.greenValue(),
                        blueValueToCompare = buildingStrokeOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, whiteColor)
                    }

                    currentPixelColor.isRgbColor(
                        redValueToCompare = buildingOriginalColor.redValue(),
                        greenValueToCompare = buildingOriginalColor.greenValue(),
                        blueValueToCompare = buildingOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, whiteColor)
                    }
                    else -> {
                        mapImage.set(x, y, blackColor)
                    }
                }
            }
        }
        mapImage.updatePixels()
    }

    override val type: SketchArtType
        get() = SketchArtType.PRINTABLE_MAP
}