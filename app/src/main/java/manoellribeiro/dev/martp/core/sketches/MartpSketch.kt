package manoellribeiro.dev.martp.core.sketches

import manoellribeiro.dev.martp.core.models.SketchArtType
import processing.core.PApplet
import processing.core.PGraphics

abstract class MartpSketch(
    private val horizontalTilesCount: Int,
    private val verticalTilesCount: Int,
    private val padding: Int,
    val canvasWidth: Float = 640.0F,
    val canvasHeight: Float = 640.0F,
    private val imagePath: String,
): PApplet() {

    companion object {
        const val frameThickness = 30.0F
        const val framePadding = 30.0F
    }

    abstract val type: SketchArtType

    override fun settings() {
        size(canvasWidth.toInt(), canvasHeight.toInt())
    }

    override fun setup() {
        super.setup()
    }

    protected fun isStreetPixel(
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

    //Tone is one of the geoapify pre defined style
    protected fun isStreetPixelForTone(
        color: Int
    ): Boolean {
        val redValue = color shr 16 and 0xFF
        val greenValue = color shr 8 and 0xFF
        val blueValue = color and 0xFF
        val rangeOfStreetColorsForRGB = 0..30
        return redValue in rangeOfStreetColorsForRGB &&
                greenValue in rangeOfStreetColorsForRGB &&
                blueValue in rangeOfStreetColorsForRGB
    }

    /**
     * Draws the frame on the screen by default, or on [target] when the art is being composed
     * in an offscreen buffer.
     */
    protected fun drawArtFrame(target: PGraphics = g) {
        target.noStroke()
        val frameGray = color(51, 51, 51)
        target.fill(frameGray)
        target.rect(0F, 0F, canvasWidth, canvasHeight)
        val white = color(255, 255, 255)
        target.fill(white)
        target.rect(
            frameThickness,
            frameThickness,
            canvasWidth - (2 * frameThickness),
            canvasHeight - (2 * frameThickness)
        )
    }

}