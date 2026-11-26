package manoellribeiro.dev.martp.core.sketches

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
    val onColorsInverted: (MapLayersColors) -> Unit
) : MartpSketch(
    0,
    0,
    padding,
    canvasWidth,
    canvasHeight,
    imagePath,
) {

    private var mapImage: PImage? = null
    private var tempMapImage: PImage? = null
    private var isInverted = false
    private val whiteColor = "#FFFFFF".hexToColorInt()
    private val blackColor = "#000000".hexToColorInt()
    override fun setup() {
        super.setup()

        mapImage = loadImage(imagePath)
        changePixelColors(mapImage!!)
        image(mapImage, 0F, 0F)
        filter(ERODE)
    }

    override fun draw() {
        image(mapImage, 0F, 0F)
    }

    override fun mousePressed() {
        if (tempMapImage == null) {
            isInverted = !isInverted
            tempMapImage = loadImage(imagePath)
            changePixelColors(tempMapImage!!)
            mapImage = tempMapImage
            tempMapImage = null
        }
    }

    val buildingOriginalColor = color(255, 36, 36)
    val buildingStrokeOriginalColor = color(69, 36, 255)
    val roadsOriginalColor = color(211, 255, 36)
    val roadsPathOriginalColor = color(93, 249, 192)
    val waterOriginalColor = color(234, 26, 234)

    private val layersOriginalColors = listOf(
        buildingOriginalColor,
        buildingStrokeOriginalColor,
        roadsOriginalColor,
        roadsPathOriginalColor,
        waterOriginalColor
    )

    private fun changePixelColors(mapImage: PImage) {
        val layersColor = if (isInverted) blackColor else whiteColor
        val landColor = if (isInverted) whiteColor else blackColor
        mapImage.loadPixels()
        val mapImagePixels = mapImage.pixels
        for (y in 0..<mapImage.height) {
            for (x in 0..<mapImage.width) {
                val currentPixelColor = mapImagePixels[y * mapImage.width + x]
                val isLayerPixel = layersOriginalColors.any { layerOriginalColor ->
                    currentPixelColor.isRgbColor(
                        redValueToCompare = layerOriginalColor.redValue(),
                        greenValueToCompare = layerOriginalColor.greenValue(),
                        blueValueToCompare = layerOriginalColor.blueValue()
                    )
                }
                mapImage.set(x, y, if (isLayerPixel) layersColor else landColor)
            }
        }
        mapImage.updatePixels()
        onColorsInverted(
            MapLayersColors(
                roadsColor = layersColor,
                buildingStrokeColor = layersColor,
                buildingColor = layersColor,
                roadsPathColor = layersColor,
                waterColor = layersColor,
                landOthersColor = landColor
            )
        )
    }

    override val type: SketchArtType
        get() = SketchArtType.PRINTABLE_MAP
}
