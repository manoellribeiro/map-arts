package manoellribeiro.dev.martp.core.extensions

import manoellribeiro.dev.martp.core.utils.ONE
import manoellribeiro.dev.martp.core.utils.ZERO

fun Int?.orZero() = this ?: ZERO
fun Int?.orOne() = this ?: ONE
fun Int?.isNegative() = this.orZero() < ZERO
fun Int?.isPositive() = this.orZero() > ZERO
fun Int?.isZero() = this.orZero() == ZERO
fun Int?.isZeroOrNegative() = this.orZero() <= ZERO
fun Int?.isZeroOrPositive() = this.orZero() >= ZERO