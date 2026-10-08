package manoellribeiro.dev.martp.core.sketches

import android.util.Log
import manoellribeiro.dev.martp.core.extensions.orZero
import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import manoellribeiro.dev.martp.core.sketches.MartpSketch.Companion.frameThickness
import processing.core.PApplet
import processing.core.PImage
import processing.core.PVector

class WalkingPeopleSketch(
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

    val particleSize = 10F

    private class Particle(
        val x: Float,
        val y: Float,
        val sketch: MartpSketch,
        val particleSize: Float
    ) {

        val position = PVector(x, y)

        val velocity = PVector.random2D()
        val acc = PVector(0F, 0F)

        fun update(
            mapPixels: IntArray, mapWidth: Int, mapHeight: Int
        ) {
            velocity.add(this.acc)
            acc.mult(0F)


            val nextX = position.x + velocity.x
            val nextY = position.y + velocity.y

            // moving only horizontally would hit a wall → reverse horizontal direction
            if (!fits(nextX, position.y, mapPixels, mapWidth, mapHeight)) velocity.x *= -1
            // moving only vertically would hit a wall → reverse vertical direction
            if (!fits(position.x, nextY, mapPixels, mapWidth, mapHeight)) velocity.y *= -1

            // move only if the final spot is safe
            if (fits(position.x + velocity.x, position.y + velocity.y, mapPixels, mapWidth, mapHeight)) {
                position.add(velocity)
            } else {
                velocity.mult(-1F)
            }
        }

        fun applyForce(force: PVector) {
            acc.add(force)
        }

        fun isWalkable(
            mapPixels: IntArray,
            canvasX: Float,
            canvasY: Float,
            mapWidth: Int,
            mapHeight: Int
        ): Boolean {
            val pixelY = (canvasY - (frameThickness + framePadding)).toInt()
            val pixelX = (canvasX - (frameThickness + framePadding)).toInt()

            val isInsideMap = pixelX in 0 until mapWidth && pixelY in 0 until mapHeight
            if (!isInsideMap) return false

            val pixelColor = mapPixels[pixelY * mapWidth + pixelX]
            return isWhiteColor(pixelColor) || isBuildingPixel(pixelColor)
        }

        fun fits(
            centerX: Float,
            centerY: Float,
            mapPixels: IntArray,
            mapWidth: Int,
            mapHeight: Int
        ): Boolean {

            val particleRadius = particleSize / 2F
            val outlinePointCount = 8

            if (!isWalkable(mapPixels, centerX, centerY, mapWidth, mapHeight)) return false

            for (outlinePointIndex in 0 until outlinePointCount) {
                val angle = outlinePointIndex * TWO_PI / outlinePointCount
                val outlineX = centerX + cos(angle) * particleRadius
                val outlineY = centerY + sin(angle) * particleRadius
                if (!isWalkable(mapPixels, outlineX, outlineY, mapWidth, mapHeight)) return false
            }
            return true
        }

        fun show() = with(sketch) {
            stroke(255F, 0F, 0F)
            strokeWeight(particleSize)
            point(position.x, position.y)
        }

        private fun isBuildingPixel(
            color: Int
        ): Boolean {
            val redValue = color shr 16 and 0xFF
            val greenValue = color shr 8 and 0xFF
            val blueValue = color and 0xFF
            return redValue > 254 &&
                    greenValue in 34..38 &&
                    blueValue in 34..38
        }

        private fun isWhiteColor(
            color: Int
        ): Boolean {
            val redValue = color shr 16 and 0xFF
            val greenValue = color shr 8 and 0xFF
            val blueValue = color and 0xFF
            return redValue == 255 &&
                    greenValue == 255 &&
                    blueValue == 255
        }
    }

    private val particles = mutableListOf<Particle>()
    private var mapImagePixels = intArrayOf()

    private var mapImage: PImage? = null

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
        for ((i, element) in particles.withIndex()) {
            element.update(
                mapPixels = mapImagePixels,
                mapWidth = mapImage?.width.orZero(),
                mapHeight = mapImage?.height.orZero(),
            )
            element.show()
            //particles[i].edges(imageColors = mapImagePixels, mapWidth = mapImage?.width.orZero())
        }
    }


    // TODO: Getting the color of a single pixel with get(x, y) is easy, but not as fast as grabbing the data directly from pixels[]. The equivalent statement to get(x, y) using pixels[] is pixels[y*width+x]. See the reference for pixels[] for more information.
    private fun changePixelColors(mapImage: PImage) {
        mapImage.loadPixels()
        mapImagePixels = mapImage.pixels
        for (y in 0..mapImage.height) {
            val xValuesForSameY = arrayListOf<Int>()
            for (x in 0..mapImage.width) {
                val currentPixelColor = mapImage.get(x, y)


                when {
                    isStreetPixel(currentPixelColor) -> {
                        mapImage.set(x, y, color(18, 13, 49))
                    }
                    isBuildingPixel(currentPixelColor) -> {
                        mapImage.set(x, y, color(255, 255, 255))
                        if (random(1000F).toInt() == 1 && particles.size < 150) {
                            val canvasX = x.toFloat() + frameThickness + framePadding
                            val canvasY = y.toFloat() + frameThickness + framePadding


                            val candidate = Particle(
                                x = canvasX,
                                y = canvasY,
                                sketch = this,
                                particleSize = particleSize
                            )
                            
                            if(
                                candidate.fits(
                                    centerX = canvasX,
                                    centerY = canvasY,
                                    mapPixels = mapImagePixels,
                                    mapWidth = mapImage.width,
                                    mapHeight = mapImage.height
                                )
                            ) {
                                particles.add(candidate)
                            }
                            
                        }
                    }
                    else -> {
                        mapImage.set(x, y, color(255, 201, 113))
                    }
                }
            }
        }
        mapImage.updatePixels()
    }


    private fun isBuildingPixel(
        color: Int
    ): Boolean {
        val redValue = color shr 16 and 0xFF
        val greenValue = color shr 8 and 0xFF
        val blueValue = color and 0xFF
        return redValue > 254 &&
                greenValue in 34..38 &&
                blueValue in 34..38
    }

    override val type: SketchArtType
        get() = SketchArtType.WALKING_PEOPLE
}