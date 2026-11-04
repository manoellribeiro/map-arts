package manoellribeiro.dev.martp.core.extensions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StringExtensionsTest {

    // hexToColorInt

    @Test
    fun `if hex color has a hash prefix should return the opaque color on hexToColorInt`() {
        // Given
        val fakeHexColor = "#100C2D"

        // When
        val result = fakeHexColor.hexToColorInt()

        // Then
        assertEquals(0xFF100C2D.toInt(), result)
    }

    @Test
    fun `if hex color has no hash prefix should return the opaque color on hexToColorInt`() {
        // Given
        val fakeHexColor = "FFC971"

        // When
        val result = fakeHexColor.hexToColorInt()

        // Then
        assertEquals(0xFFFFC971.toInt(), result)
    }

    @Test
    fun `if hex color is lowercase should return the opaque color on hexToColorInt`() {
        // Given
        val fakeHexColor = "#f27ea9"

        // When
        val result = fakeHexColor.hexToColorInt()

        // Then
        assertEquals(0xFFF27EA9.toInt(), result)
    }

    @Test
    fun `if hex color is black should return opaque black on hexToColorInt`() {
        // Given
        val fakeHexColor = "#000000"

        // When
        val result = fakeHexColor.hexToColorInt()

        // Then
        assertEquals(0xFF000000.toInt(), result)
    }

    @Test
    fun `if hex color is white should return opaque white on hexToColorInt`() {
        // Given
        val fakeHexColor = "#FFFFFF"

        // When
        val result = fakeHexColor.hexToColorInt()

        // Then
        assertEquals(0xFFFFFFFF.toInt(), result)
    }

    @Test
    fun `if hex color is in the short format should throw IllegalArgumentException on hexToColorInt`() {
        // Given
        val fakeHexColor = "#FFF"

        // When / Then
        assertThrows<IllegalArgumentException> { fakeHexColor.hexToColorInt() }
    }

    @Test
    fun `if hex color already has an alpha channel should throw IllegalArgumentException on hexToColorInt`() {
        // Given
        val fakeHexColor = "#80100C2D"

        // When / Then
        assertThrows<IllegalArgumentException> { fakeHexColor.hexToColorInt() }
    }

    @Test
    fun `if hex color has non hex characters should throw NumberFormatException on hexToColorInt`() {
        // Given
        val fakeHexColor = "#GG0000"

        // When / Then
        assertThrows<NumberFormatException> { fakeHexColor.hexToColorInt() }
    }
}
