package manoellribeiro.dev.martp.core.extensions

import android.content.Intent

inline fun <reified T : Enum<T>> Intent.putExtra(value: T): Intent =
    putExtra(T::class.java.name, value.ordinal)

inline fun <reified T: Enum<T>> Intent.getEnumExtra(): T? =
    getIntExtra(T::class.java.name, -1)
        .takeUnless { it == -1 }
        ?.let { T::class.java.enumConstants[it] }