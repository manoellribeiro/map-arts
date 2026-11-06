package manoellribeiro.dev.martp.core.data.local.converters

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ColorsConverterTest {

    private lateinit var converter: ColorsConverter

    @BeforeEach
    fun setup() {
        converter = ColorsConverter()
    }

    // region fromColors

    @Test
    fun `if colors has many values should join them with commas on fromColors`() {
        // Given
        val fakeColors = listOf(1, 22, 333)

        // When
        val result = converter.fromColors(fakeColors)

        // Then
        assertEquals("1,22,333", result)
    }

    @Test
    fun `if colors are opaque should keep their negative int values on fromColors`() {
        // Given
        val fakeColors = listOf(0xFF100C2D.toInt(), 0xFFFFFFFF.toInt())

        // When
        val result = converter.fromColors(fakeColors)

        // Then
        assertEquals("${0xFF100C2D.toInt()},-1", result)
    }

    @Test
    fun `if colors has a single value should return it without commas on fromColors`() {
        // Given
        val fakeColors = listOf(0xFF000000.toInt())

        // When
        val result = converter.fromColors(fakeColors)

        // Then
        assertEquals("-16777216", result)
    }

    @Test
    fun `if colors is empty should return an empty text on fromColors`() {
        // Given
        val fakeColors = emptyList<Int>()

        // When
        val result = converter.fromColors(fakeColors)

        // Then
        assertEquals("", result)
    }

    // endregion

    // region toColors

    @Test
    fun `if value has many comma separated numbers should return them as colors on toColors`() {
        // Given
        val fakeValue = "1,22,333"

        // When
        val result = converter.toColors(fakeValue)

        // Then
        assertEquals(listOf(1, 22, 333), result)
    }

    @Test
    fun `if value has negative numbers should return the opaque colors on toColors`() {
        // Given
        val fakeValue = "-16777216,-1"

        // When
        val result = converter.toColors(fakeValue)

        // Then
        assertEquals(listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), result)
    }

    @Test
    fun `if value has a single number should return a list with one color on toColors`() {
        // Given
        val fakeValue = "42"

        // When
        val result = converter.toColors(fakeValue)

        // Then
        assertEquals(listOf(42), result)
    }

    @Test
    fun `if value is empty should return an empty list on toColors`() {
        // Given
        val fakeValue = ""

        // When
        val result = converter.toColors(fakeValue)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `if value is blank should return an empty list on toColors`() {
        // Given
        val fakeValue = "   "

        // When
        val result = converter.toColors(fakeValue)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `if colors are converted to text and back should return the same colors on toColors`() {
        // Given
        val fakeColors = listOf(
            0xFF100C2D.toInt(),
            0xFF120D31.toInt(),
            0xFF4E3C41.toInt(),
            0xFF896B51.toInt(),
            0xFFFFC971.toInt(),
            0xFFFFCE7E.toInt()
        )

        // When
        val result = converter.toColors(converter.fromColors(fakeColors))

        // Then
        assertEquals(fakeColors, result)
    }

    @Test
    fun `if an empty list is converted to text and back should return an empty list on toColors`() {
        // Given
        val fakeColors = emptyList<Int>()

        // When
        val result = converter.toColors(converter.fromColors(fakeColors))

        // Then
        assertEquals(fakeColors, result)
    }

    // endregion
}
