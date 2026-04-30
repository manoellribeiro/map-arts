package manoellribeiro.dev.martp.core.sketches

import android.util.Log
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
) : MartpSketch(
    horizontalTilesCount,
    verticalTilesCount,
    padding,
    canvasWidth,
    canvasHeight,
    imagePath,
) {


    override fun draw() {
        val mapImage = loadImage(imagePath)
        changePixelColors(mapImage)
        drawArtFrame()
        if(horizontalTilesCount > 0) {
            createTiles(mapImage)
        } else {
            image(mapImage, frameThickness + framePadding, frameThickness + framePadding)
        }
        filter(ERODE)
        noLoop()
    }

    private fun changePixelColors(mapImage: PImage) {
        Log.i("DefaultMartpSketch", "changePixelColors begin")
        mapImage.loadPixels()

        for (y in 0..mapImage.height) {
            val xValuesForSameY = arrayListOf<Int>()
            for (x in 0..mapImage.width) {
                val currentPixelColor = mapImage.get(x, y)
                if(isStreetPixel(currentPixelColor)) {
                    mapImage.set(x, y, color(18, 13, 49))
                } else {
                    mapImage.set(x, y, color(255, 201, 113))
                }
            }
        }
        mapImage.updatePixels()
        Log.i("DefaultMartpSketch", "changePixelColors final")
    }

    private fun createTiles(mapImage: PImage) {
        Log.i("DefaultMartpSketch", "createTiles Begin")
        var mapTiles = arrayListOf<PImage>()
        val tileWidth = mapImage.width / horizontalTilesCount
        val tileHeight = mapImage.height / verticalTilesCount

        for(y in 0 ..<mapImage.height step tileHeight) {
            for(x in 0..<mapImage.width step tileWidth) {
                mapTiles.add(mapImage.get(x, y, tileWidth, tileHeight))
            }
        }

        //mapTiles.shuffle()

        var tileX = padding + frameThickness + framePadding
        var tileY = padding + frameThickness + framePadding
        for(tile in mapTiles) {
            image(tile, tileX, tileY)
            tileX += tileWidth + padding
            if(tileX >= width - (frameThickness + framePadding)) {
                tileX = padding + frameThickness + framePadding
                tileY += tileHeight + padding
            }
        }
        Log.i("DefaultMartpSketch", "createTiles final")
    }

    override val type: SketchArtType
        get() = SketchArtType.DEFAULT
}