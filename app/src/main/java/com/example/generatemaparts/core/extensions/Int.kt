package com.example.generatemaparts.core.extensions

import com.example.generatemaparts.core.utils.ONE
import com.example.generatemaparts.core.utils.ZERO

fun Int?.orZero() = this ?: ZERO
fun Int?.orOne() = this ?: ONE
fun Int?.isNegative() = this.orZero() < ZERO
fun Int?.isPositive() = this.orZero() > ZERO
fun Int?.isZero() = this.orZero() == ZERO
fun Int?.isZeroOrNegative() = this.orZero() <= ZERO
fun Int?.isZeroOrPositive() = this.orZero() >= ZERO