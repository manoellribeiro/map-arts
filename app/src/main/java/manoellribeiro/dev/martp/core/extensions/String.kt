package manoellribeiro.dev.martp.core.extensions

private const val HEX_COLOR_LENGTH = 6
private const val OPAQUE_ALPHA = 0xFF000000

fun String.hexToColorInt(): Int {
    val hex = removePrefix("#")
    require(hex.length == HEX_COLOR_LENGTH) { "Expected a color in the #RRGGBB format but was $this" }
    return (OPAQUE_ALPHA or hex.toLong(radix = 16)).toInt()
}
