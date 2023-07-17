package com.example.generatemaparts

import processing.core.PApplet

class Sketch: PApplet() {

    override fun settings() {
        size(600, 600)
    }

    override fun setup() {
        super.setup()
    }

    override fun draw() {
        if(mousePressed) {
            ellipse(mouseX.toFloat(), mouseY.toFloat(), 50F, 50F)
        }
    }

}