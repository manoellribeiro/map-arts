package manoellribeiro.dev.martp.core.sketches

import manoellribeiro.dev.martp.core.extensions.blueValue
import manoellribeiro.dev.martp.core.extensions.greenValue
import manoellribeiro.dev.martp.core.extensions.isRgbColor
import manoellribeiro.dev.martp.core.extensions.orZero
import manoellribeiro.dev.martp.core.extensions.redValue
import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import manoellribeiro.dev.martp.core.services.sensors.MeasurableSensor
import manoellribeiro.dev.martp.core.sketches.particles.Particle
import processing.core.PImage
import processing.core.PVector

class ParticleSystemSketch(
    private val padding: Int,
    canvasWidth: Float = 640.0F,
    canvasHeight: Float = 640.0F,
    private val imagePath: String,
    private val gravitySensor: MeasurableSensor
) : MartpSketch(
    0,
    0,
    padding,
    canvasWidth,
    canvasHeight,
    imagePath,
) {

    val selectedColorSchema = intArrayOf(
        color(242, 126, 169), // #F27EA9
        color(54, 108, 217),  // #366CD9
        color(94, 173, 242),  // #5EADF2
        color(99, 110, 115),  // #636E73
        color(242, 230, 216), // #F2E6D8
        color(97, 142, 179), // #F2E6D8
    )

    private val particles = mutableListOf<Particle>()
    private var mapImagePixels = intArrayOf()

    private var mapImage: PImage? = null

    @Volatile var gravityX = 0F
    @Volatile var gravityY = 0F

    val tiltStrength = 0.4F // the constant that converts m/s² into pixels per frame²
    private val tiltForce = PVector(0F, 0F)

    val color1 = unhex("FFF27EA9")
    val color2 = unhex("FF366CD9")
    val color3 = unhex("FF5EADF2")

    override fun setup() {
        super.setup()

        mapImage = loadImage(imagePath)
        changePixelColors(mapImage!!)
        drawArtFrame()
        image(mapImage, frameThickness + framePadding, frameThickness + framePadding)
        filter(ERODE)
        gravitySensor.startListening()
        gravitySensor.setOnSensorValuesChangedListener { event ->
            gravityX = -event[0]
            gravityY = event[1]
        }
    }

    override fun draw() {
        image(mapImage, frameThickness + framePadding, frameThickness + framePadding)

        tiltForce.set(gravityX * tiltStrength, gravityY * tiltStrength)

        for (particle in particles) {
            particle.applyForce(tiltForce)
            particle.update(
                mapPixels = mapImagePixels,
                mapWidth = mapImage?.width.orZero(),
                mapHeight = mapImage?.height.orZero(),
                particles
            )
            particle.show(
                color1, color2, color3
            )
        }
    }

    val buildingOriginalColor = color(255, 36, 36)
    val buildingStrokeOriginalColor = color(69, 36, 255)
    val roadsOriginalColor = color(211, 255, 36)
    val roadsPathOriginalColor = color(93, 249, 192)
    val waterOriginalColor = color(234, 26, 234)

    // TODO: Getting the color of a single pixel with get(x, y) is easy, but not as fast as grabbing the data directly from pixels[]. The equivalent statement to get(x, y) using pixels[] is pixels[y*width+x]. See the reference for pixels[] for more information.
    private fun changePixelColors(mapImage: PImage) {
        mapImage.loadPixels()
        mapImagePixels = mapImage.pixels
        for (y in 0..mapImage.height) {
            val xValuesForSameY = arrayListOf<Int>()
            for (x in 0..mapImage.width) {
                val currentPixelColor = mapImage.get(x, y)

                when {
                    currentPixelColor.isRgbColor(
                        redValueToCompare = roadsOriginalColor.redValue(),
                        greenValueToCompare = roadsOriginalColor.greenValue(),
                        blueValueToCompare = roadsOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, selectedColorSchema[0])
                    }

                    currentPixelColor.isRgbColor(
                        redValueToCompare = roadsPathOriginalColor.redValue(),
                        greenValueToCompare = roadsPathOriginalColor.greenValue(),
                        blueValueToCompare = roadsPathOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, selectedColorSchema[1])
                    }

                    currentPixelColor.isRgbColor(
                        redValueToCompare = waterOriginalColor.redValue(),
                        greenValueToCompare = waterOriginalColor.greenValue(),
                        blueValueToCompare = waterOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, selectedColorSchema[2])
                    }

                    currentPixelColor.isRgbColor(
                        redValueToCompare = buildingStrokeOriginalColor.redValue(),
                        greenValueToCompare = buildingStrokeOriginalColor.greenValue(),
                        blueValueToCompare = buildingStrokeOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, selectedColorSchema[3])
                    }

                    currentPixelColor.isRgbColor(
                        redValueToCompare = buildingOriginalColor.redValue(),
                        greenValueToCompare = buildingOriginalColor.greenValue(),
                        blueValueToCompare = buildingOriginalColor.blueValue()
                    ) -> {
                        mapImage.set(x, y, selectedColorSchema[4])
                    }


//                    isBuildingPixel(currentPixelColor) -> {
//                        mapImage.set(x, y, color(255, 255, 255))
//                        if (random(1500F).toInt() == 1 && particles.size < 100) {
//                            val canvasX = x.toFloat() + frameThickness + framePadding
//                            val canvasY = y.toFloat() + frameThickness + framePadding
//
//
//                            val candidate = Particle(
//                                x = canvasX,
//                                y = canvasY,
//                                sketch = this,
//                                particleSize = arrayListOf(50F, 52F, 54F, 56F, 58F, 60F).random()
//                            )
//
//                            if(
//                                candidate.fits(
//                                    centerX = canvasX,
//                                    centerY = canvasY,
//                                    mapPixels = mapImagePixels,
//                                    mapWidth = mapImage.width,
//                                    mapHeight = mapImage.height
//                                ) &&
//                                !candidate.overlapsAnyParticle(canvasX, canvasY, particles)
//                            ) {
//                                particles.add(candidate)
//                            }
//
//                        }
//                    }
                    else -> {
                        mapImage.set(x, y, selectedColorSchema[5])
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
        get() = SketchArtType.PARTICLE_SYSTEM
}