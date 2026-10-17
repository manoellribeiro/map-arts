package manoellribeiro.dev.martp.core.sketches.particles

import android.graphics.Color
import manoellribeiro.dev.martp.core.sketches.MartpSketch
import manoellribeiro.dev.martp.core.sketches.MartpSketch.Companion.framePadding
import manoellribeiro.dev.martp.core.sketches.MartpSketch.Companion.frameThickness
import processing.core.PApplet
import processing.core.PApplet.cos
import processing.core.PApplet.sin
import processing.core.PConstants.TWO_PI
import processing.core.PVector

class Particle(
    val x: Float,
    val y: Float,
    val sketch: MartpSketch,
    val particleSize: Float // the particle size should be proportional of the zoomMap
) {

    private var particleRadius = particleSize / 2
    val maxSpeed = 2F
    val bounceFactor = 0.7F
    val position = PVector(x, y)

    val velocity = PVector(0F, 0F)
    val acc = PVector(0F, 0F)

    private val outlineOffsetsX = FloatArray(8) { outlinePointIndex ->
        cos(outlinePointIndex * TWO_PI / 8) * particleRadius
    }

    private val outlineOffsetsY = FloatArray(8) { outlinePointIndex ->
        sin(outlinePointIndex * TWO_PI / 8) * particleRadius
    }

    fun update(
        mapPixels: IntArray, mapWidth: Int, mapHeight: Int, allParticles: List<Particle>
    ) {
        velocity.add(this.acc)
        acc.mult(0F)
        velocity.limit(maxSpeed)

        // 1. horizontal: move if it fits, otherwise stop horizontally
        val nextX = position.x + velocity.x
        if (
            fits(nextX, position.y, mapPixels, mapWidth, mapHeight) &&
            !overlapsAnyParticle(nextX, position.y, allParticles)
        ) {
            position.x = nextX
        } else {
            velocity.x *= -bounceFactor
        }

        // 2. vertical: move if it fits, otherwise stop vertically
        val nextY = position.y + velocity.y
        if (
            fits(position.x, nextY, mapPixels, mapWidth, mapHeight) &&
            !overlapsAnyParticle(position.x, nextY, allParticles)
        ) {
            position.y = nextY
        } else {
            velocity.y *= -bounceFactor
        }
    }

    fun applyForce(force: PVector) {
        acc.add(force)
    }

    fun overlapsAnyParticle(
        centerX: Float,
        centerY: Float,
        allParticles: List<Particle>
    ): Boolean {

        for (otherParticle in allParticles) {
            if(otherParticle === this) continue
            val distanceBetweenCenters = PApplet.dist(
                centerX, centerY,
                otherParticle.position.x,
                otherParticle.position.y
            )
            val minimumDistance = particleSize / 2 + otherParticle.particleSize / 2
            if(distanceBetweenCenters < minimumDistance) return true
        }

        return false

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
            val outlineX = centerX + outlineOffsetsX[outlinePointIndex]
            val outlineY = centerY + outlineOffsetsY[outlinePointIndex]
            if (!isWalkable(mapPixels, outlineX, outlineY, mapWidth, mapHeight)) return false
        }
        return true
    }

    fun show(
        color1: Int,
        color2: Int,
        color3: Int,
    ) = with(sketch) {
        fill(
            color1
        )
        ellipseMode(PApplet.RADIUS)
        circle(position.x, position.y, particleRadius)
        fill(
            color2
        )
        circle(position.x, position.y, particleRadius / 2)
        fill(
            color3
        )
        circle(position.x, position.y, particleRadius / 4)
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