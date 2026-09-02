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