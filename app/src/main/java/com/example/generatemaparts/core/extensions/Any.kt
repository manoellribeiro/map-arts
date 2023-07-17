package com.example.generatemaparts.core.extensions

fun<T> Any?.executeIfNotNull(executable: () -> T){
    if(this != null) executable()
}

fun<T> Any?.executeIfNull(executable: () -> T){
    if(this == null) executable()
}