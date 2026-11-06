package manoellribeiro.dev.martp.core.data.local.converters

import androidx.room.TypeConverter

class ColorsConverter {

    @TypeConverter
    fun fromColors(colors: List<Int>): String = colors.joinToString(separator = SEPARATOR)

    @TypeConverter
    fun toColors(value: String): List<Int> =
        if (value.isBlank()) emptyList() else value.split(SEPARATOR).map(String::toInt)

    private companion object {
        const val SEPARATOR = ","
    }
}
