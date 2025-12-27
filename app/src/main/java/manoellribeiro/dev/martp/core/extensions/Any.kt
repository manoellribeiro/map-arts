package manoellribeiro.dev.martp.core.extensions

fun<T> T?.executeIfNotNull(executable: () -> T){
    if(this != null) executable()
}

fun<T> T?.executeIfNull(executable: () -> T){
    if(this == null) executable()
}

inline fun <T> T?.orNull(result: () -> T): T = this ?: result.invoke()
