package manoellribeiro.dev.martp.core.extensions

import android.content.res.Resources
import manoellribeiro.dev.martp.core.utils.ONE
import manoellribeiro.dev.martp.core.utils.ZERO

fun Int?.orZero() = this ?: ZERO
fun Int?.orOne() = this ?: ONE
fun Int?.isNegative() = this.orZero() < ZERO
fun Int?.isPositive() = this.orZero() > ZERO
fun Int?.isZero() = this.orZero() == ZERO
fun Int?.isZeroOrNegative() = this.orZero() <= ZERO
fun Int?.isZeroOrPositive() = this.orZero() >= ZERO
fun Int.dp(resources: Resources): Int {
    val scale = resources.displayMetrics.density
    return ((this * scale) + 0.5F).toInt()
}

fun Int.isRgbColor(
    redValueToCompare: Int,
    greenValueToCompare: Int,
    blueValueToCompare: Int
): Boolean {
    // values from the original color being checked
    val redValue = this.redValue()
    val greenValue = this.greenValue()
    val blueValue = this.blueValue()
    return redValue in (redValueToCompare - 2)..(redValueToCompare + 2) &&
            greenValue in (greenValueToCompare - 2)..(greenValueToCompare + 2) &&
            blueValue in (blueValueToCompare - 2)..(blueValueToCompare + 2)
}

fun Int.redValue(): Int {
    return this shr 16 and 0xFF
}

fun Int.greenValue(): Int {
    return this shr 8 and 0xFF
}

fun Int.blueValue(): Int {
    return this and 0xFF
}