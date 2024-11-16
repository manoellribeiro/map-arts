package manoellribeiro.dev.martp

import processing.core.PApplet

class TilesMapSketch(
    private val horizontalTilesCount: Int,
    private val verticalTilesCount: Int,
    private val padding: Int,
    private val canvasWidth: Int = 640,
    private val canvasHeight: Int = 640,
    private val imagePath: String
) : PApplet() {

    override fun settings() {
        size(600, 600)
    }

    override fun setup() {
        super.setup()
    }

    override fun draw() {
        if (mousePressed) {
            ellipse(mouseX.toFloat(), mouseY.toFloat(), 50F, 50F)
        }
        val img = loadImage(imagePath)
        image(img, 0F, 0F)
    }
}
