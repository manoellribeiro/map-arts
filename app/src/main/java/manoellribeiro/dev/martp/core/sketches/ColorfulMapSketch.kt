package manoellribeiro.dev.martp.core.sketches

import manoellribeiro.dev.martp.core.extensions.blueValue
import manoellribeiro.dev.martp.core.extensions.greenValue
import manoellribeiro.dev.martp.core.extensions.isRgbColor
import manoellribeiro.dev.martp.core.extensions.redValue
import manoellribeiro.dev.martp.core.models.MapLayersColors
import manoellribeiro.dev.martp.core.models.SketchArtType
import manoellribeiro.dev.martp.scenes.artStyleSettings.ColorPalette
import processing.core.PGraphics
import processing.core.PImage

class ColorfulMapSketch(
    private val padding: Int,
    canvasWidth: Float = 640.0F,
    canvasHeight: Float = 640.0F,
    private val imagePath: String,
    private val colorPalette: ColorPalette,
    val onColorsShuffled: (MapLayersColors) -> Unit
) : MartpSketch(
    0,
    0,
    padding,
    canvasWidth,
    canvasHeight,
    imagePath,
) {

    private var tempMapImage: PImage? = null

    /**
     * The finished art (frame + colored map + erode filter). The screen surface is recreated
     * whenever the view is detached (e.g. a gallery item scrolled away), so draw() must be able
     * to repaint everything, and copying this buffer is much cheaper than filtering every frame.
     */
    private var artBuffer: PGraphics? = null

    override fun setup() {
        super.setup()

        val mapImage = loadImage(imagePath)
        changePixelColors(mapImage)
        artBuffer = composeArt(mapImage)
    }

    override fun draw() {
        artBuffer?.let { image(it, 0F, 0F) }
    }

    override fun mousePressed() {
        if(tempMapImage == null) {
            tempMapImage = loadImage(imagePath)
            changePixelColors(tempMapImage!!)
            artBuffer = composeArt(tempMapImage!!)
            tempMapImage = null
        }
    }

    private fun composeArt(mapImage: PImage): PGraphics {
        val buffer = createGraphics(width, height)
        buffer.beginDraw()
        drawArtFrame(buffer)
        buffer.image(mapImage, frameThickness + framePadding, frameThickness + framePadding)
        buffer.filter(ERODE)
        buffer.endDraw()
        return buffer
    }

    val buildingOriginalColor = color(255, 36, 36)
    val buildingStrokeOriginalColor = color(69, 36, 255)
    val roadsOriginalColor = color(211, 255, 36)
    val roadsPathOriginalColor = color(93, 249, 192)
    val waterOriginalColor = color(234, 26, 234)

    private fun changePixelColors(mapImage: PImage) {
        mapImage.loadPixels()
        val mapImagePixels = mapImage.pixels
        val randomColors = colorPalette.colors.shuffled()
        val colorHolder = MapLayersColors()
        for (y in 0..<mapImage.height) {
            for (x in 0..<mapImage.width) {
                val currentPixelColor = mapImagePixels[y*mapImage.width+x]
                when {
                    currentPixelColor.isRgbColor(
                        redValueToCompare = roadsOriginalColor.redValue(),
                        greenValueToCompare = roadsOriginalColor.greenValue(),
                        blueValueToCompare = roadsOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, randomColors[0])
                        colorHolder.roadsColor = randomColors[0]
                    }

                    currentPixelColor.isRgbColor(
                        redValueToCompare = roadsPathOriginalColor.redValue(),
                        greenValueToCompare = roadsPathOriginalColor.greenValue(),
                        blueValueToCompare = roadsPathOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, randomColors[1])
                        colorHolder.roadsPathColor = randomColors[1]
                    }

                    currentPixelColor.isRgbColor(
                        redValueToCompare = waterOriginalColor.redValue(),
                        greenValueToCompare = waterOriginalColor.greenValue(),
                        blueValueToCompare = waterOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, randomColors[2])
                        colorHolder.waterColor = randomColors[2]
                    }

                    currentPixelColor.isRgbColor(
                        redValueToCompare = buildingStrokeOriginalColor.redValue(),
                        greenValueToCompare = buildingStrokeOriginalColor.greenValue(),
                        blueValueToCompare = buildingStrokeOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, randomColors[3])
                        colorHolder.buildingStrokeColor = randomColors[3]
                    }

                    currentPixelColor.isRgbColor(
                        redValueToCompare = buildingOriginalColor.redValue(),
                        greenValueToCompare = buildingOriginalColor.greenValue(),
                        blueValueToCompare = buildingOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, randomColors[4])
                        colorHolder.buildingColor = randomColors[4]
                    }
                    else -> {
                        mapImage.set(x, y, randomColors[5])
                        colorHolder.landOthersColor = randomColors[5]
                    }
                }
            }
        }
        mapImage.updatePixels()
        onColorsShuffled(colorHolder)
    }

    override val type: SketchArtType
        get() = SketchArtType.COLORFUL_MAP
}