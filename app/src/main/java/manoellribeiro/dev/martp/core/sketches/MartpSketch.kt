package manoellribeiro.dev.martp.core.sketches

import processing.core.PApplet

abstract class MartpSketch(
    private val horizontalTilesCount: Int,
    private val verticalTilesCount: Int,
    private val padding: Int,
    private val canvasWidth: Float = 640.0F,
    private val canvasHeight: Float = 640.0F,
    private val imagePath: String,
    private val drawingFinishedCallback: () -> Unit
): PApplet() {

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

    protected fun drawArtFrame() {
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