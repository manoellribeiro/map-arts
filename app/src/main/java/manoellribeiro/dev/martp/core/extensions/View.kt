package manoellribeiro.dev.martp.core.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

fun View.gone() {
    if (this.visibility != View.GONE)
        this.visibility = View.GONE
}

fun View.visible() {
    if (this.visibility != View.VISIBLE)
        this.visibility = View.VISIBLE
}

fun View.invisible() {
    if (this.visibility != View.INVISIBLE)
        this.visibility = View.INVISIBLE
}

fun View.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    this.draw(canvas)
    return bitmap
}