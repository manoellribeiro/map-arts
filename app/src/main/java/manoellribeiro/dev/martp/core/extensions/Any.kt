package manoellribeiro.dev.martp.core.extensions

fun<T> Any?.executeIfNotNull(executable: () -> T){
    if(this != null) executable()
}

fun<T> Any?.executeIfNull(executable: () -> T){
    if(this == null) executable()
}

fun<T> Any?.returnIfNull(executable: () -> T): T{
    return executable.invoke()
}