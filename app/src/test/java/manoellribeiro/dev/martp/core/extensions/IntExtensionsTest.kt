package manoellribeiro.dev.martp.core.extensions

import android.content.res.Resources
import android.util.DisplayMetrics
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IntExtensionsTest {

    private lateinit var resources: Resources
    private lateinit var displayMetrics: DisplayMetrics

    @BeforeEach
    fun setup() {
        resources = mockk()
        displayMetrics = mockk()
    }

    // orZero

    @Test
    fun `if value is null should return zero on orZero`() {
        // Given
        val fakeValue: Int? = null

        // When
        val result = fakeValue.orZero()

        // Then
        assertEquals(0, result)
    }

    @Test
    fun `if value is not null should return the same value on orZero`() {
        // Given
        val fakeValue: Int? = 42

        // When
        val result = fakeValue.orZero()

        // Then
        assertEquals(42, result)
    }

    @Test
    fun `if value is negative should return the same value on orZero`() {
        // Given
        val fakeValue: Int? = -7

        // When
        val result = fakeValue.orZero()

        // Then
        assertEquals(-7, result)
    }

    // orOne

    @Test
    fun `if value is null should return one on orOne`() {
        // Given
        val fakeValue: Int? = null

        // When
        val result = fakeValue.orOne()

        // Then
        assertEquals(1, result)
    }

    @Test
    fun `if value is not null should return the same value on orOne`() {
        // Given
        val fakeValue: Int? = 13

        // When
        val result = fakeValue.orOne()

        // Then
        assertEquals(13, result)
    }

    @Test
    fun `if value is zero should return zero on orOne`() {
        // Given
        val fakeValue: Int? = 0

        // When
        val result = fakeValue.orOne()

        // Then
        assertEquals(0, result)
    }

    // isNegative

    @Test
    fun `if value is below zero should return true on isNegative`() {
        // Given
        val fakeValue: Int? = -1

        // When
        val result = fakeValue.isNegative()

        // Then
        assertTrue(result)
    }

    @Test
    fun `if value is zero should return false on isNegative`() {
        // Given
        val fakeValue: Int? = 0

        // When
        val result = fakeValue.isNegative()

        // Then
        assertFalse(result)
    }

    @Test
    fun `if value is above zero should return false on isNegative`() {
        // Given
        val fakeValue: Int? = 1

        // When
        val result = fakeValue.isNegative()

        // Then
        assertFalse(result)
    }

    @Test
    fun `if value is null should return false on isNegative`() {
        // Given
        val fakeValue: Int? = null

        // When
        val result = fakeValue.isNegative()

        // Then
        assertFalse(result)
    }

    // isPositive

    @Test
    fun `if value is above zero should return true on isPositive`() {
        // Given
        val fakeValue: Int? = 1

        // When
        val result = fakeValue.isPositive()

        // Then
        assertTrue(result)
    }

    @Test
    fun `if value is zero should return false on isPositive`() {
        // Given
        val fakeValue: Int? = 0

        // When
        val result = fakeValue.isPositive()

        // Then
        assertFalse(result)
    }

    @Test
    fun `if value is below zero should return false on isPositive`() {
        // Given
        val fakeValue: Int? = -1

        // When
        val result = fakeValue.isPositive()

        // Then
        assertFalse(result)
    }

    @Test
    fun `if value is null should return false on isPositive`() {
        // Given
        val fakeValue: Int? = null

        // When
        val result = fakeValue.isPositive()

        // Then
        assertFalse(result)
    }

    // isZero

    @Test
    fun `if value is zero should return true on isZero`() {
        // Given
        val fakeValue: Int? = 0

        // When
        val result = fakeValue.isZero()

        // Then
        assertTrue(result)
    }

    @Test
    fun `if value is null should return true on isZero`() {
        // Given
        val fakeValue: Int? = null

        // When
        val result = fakeValue.isZero()

        // Then
        assertTrue(result)
    }

    @Test
    fun `if value is above zero should return false on isZero`() {
        // Given
        val fakeValue: Int? = 1

        // When
        val result = fakeValue.isZero()

        // Then
        assertFalse(result)
    }

    @Test
    fun `if value is below zero should return false on isZero`() {
        // Given
        val fakeValue: Int? = -1

        // When
        val result = fakeValue.isZero()

        // Then
        assertFalse(result)
    }

    // isZeroOrNegative

    @Test
    fun `if value is below zero should return true on isZeroOrNegative`() {
        // Given
        val fakeValue: Int? = -1

        // When
        val result = fakeValue.isZeroOrNegative()

        // Then
        assertTrue(result)
    }

    @Test
    fun `if value is zero should return true on isZeroOrNegative`() {
        // Given
        val fakeValue: Int? = 0

        // When
        val result = fakeValue.isZeroOrNegative()

        // Then
        assertTrue(result)
    }

    @Test
    fun `if value is null should return true on isZeroOrNegative`() {
        // Given
        val fakeValue: Int? = null

        // When
        val result = fakeValue.isZeroOrNegative()

        // Then
        assertTrue(result)
    }

    @Test
    fun `if value is above zero should return false on isZeroOrNegative`() {
        // Given
        val fakeValue: Int? = 1

        // When
        val result = fakeValue.isZeroOrNegative()

        // Then
        assertFalse(result)
    }

    // isZeroOrPositive

    @Test
    fun `if value is above zero should return true on isZeroOrPositive`() {
        // Given
        val fakeValue: Int? = 1

        // When
        val result = fakeValue.isZeroOrPositive()

        // Then
        assertTrue(result)
    }

    @Test
    fun `if value is zero should return true on isZeroOrPositive`() {
        // Given
        val fakeValue: Int? = 0

        // When
        val result = fakeValue.isZeroOrPositive()

        // Then
        assertTrue(result)
    }

    @Test
    fun `if value is null should return true on isZeroOrPositive`() {
        // Given
        val fakeValue: Int? = null

        // When
        val result = fakeValue.isZeroOrPositive()

        // Then
        assertTrue(result)
    }

    @Test
    fun `if value is below zero should return false on isZeroOrPositive`() {
        // Given
        val fakeValue: Int? = -1

        // When
        val result = fakeValue.isZeroOrPositive()

        // Then
        assertFalse(result)
    }

    // dp

    @Test
    fun `if screen density is 1 should return the same value in pixels`() {
        // Given
        val fakeDp = 16
        displayMetrics.density = 1F
        every { resources.displayMetrics } returns displayMetrics

        // When
        val result = fakeDp.dp(resources)

        // Then
        assertEquals(16, result)
        verify(exactly = 1) { resources.displayMetrics }
    }

    @Test
    fun `if screen density is 2 should return the double of the value in pixels`() {
        // Given
        val fakeDp = 16
        displayMetrics.density = 2F
        every { resources.displayMetrics } returns displayMetrics

        // When
        val result = fakeDp.dp(resources)

        // Then
        assertEquals(32, result)
    }

    @Test
    fun `if scaled value ends in a half pixel should round it up`() {
        // Given
        val fakeDp = 3
        displayMetrics.density = 1.5F
        every { resources.displayMetrics } returns displayMetrics

        // When
        val result = fakeDp.dp(resources)

        // Then
        assertEquals(5, result)
    }

    @Test
    fun `if scaled value is below a half pixel should round it down`() {
        // Given
        val fakeDp = 10
        displayMetrics.density = 2.625F
        every { resources.displayMetrics } returns displayMetrics

        // When
        val result = fakeDp.dp(resources)

        // Then
        assertEquals(26, result)
    }

    @Test
    fun `if scaled value is above a half pixel should round it up`() {
        // Given
        val fakeDp = 9
        displayMetrics.density = 0.75F
        every { resources.displayMetrics } returns displayMetrics

        // When
        val result = fakeDp.dp(resources)

        // Then
        assertEquals(7, result)
    }

    @Test
    fun `if value is zero should return zero pixels in any screen density`() {
        // Given
        val fakeDp = 0
        displayMetrics.density = 3F
        every { resources.displayMetrics } returns displayMetrics

        // When
        val result = fakeDp.dp(resources)

        // Then
        assertEquals(0, result)
    }

    // redValue

    @Test
    fun `if color has a red channel should return it on redValue`() {
        // Given
        val fakeColor = 0xFF4E3C41.toInt()

        // When
        val result = fakeColor.redValue()

        // Then
        assertEquals(0x4E, result)
    }

    @Test
    fun `if color has no alpha channel should return the red channel on redValue`() {
        // Given
        val fakeColor = 0x00AB1234

        // When
        val result = fakeColor.redValue()

        // Then
        assertEquals(0xAB, result)
    }

    @Test
    fun `if color is black should return zero on redValue`() {
        // Given
        val fakeColor = 0xFF000000.toInt()

        // When
        val result = fakeColor.redValue()

        // Then
        assertEquals(0, result)
    }

    // greenValue

    @Test
    fun `if color has a green channel should return it on greenValue`() {
        // Given
        val fakeColor = 0xFF4E3C41.toInt()

        // When
        val result = fakeColor.greenValue()

        // Then
        assertEquals(0x3C, result)
    }

    @Test
    fun `if color is white should return 255 on greenValue`() {
        // Given
        val fakeColor = 0xFFFFFFFF.toInt()

        // When
        val result = fakeColor.greenValue()

        // Then
        assertEquals(255, result)
    }

    // blueValue

    @Test
    fun `if color has a blue channel should return it on blueValue`() {
        // Given
        val fakeColor = 0xFF4E3C41.toInt()

        // When
        val result = fakeColor.blueValue()

        // Then
        assertEquals(0x41, result)
    }

    @Test
    fun `if color is pure red should return zero on blueValue`() {
        // Given
        val fakeColor = 0xFFFF0000.toInt()

        // When
        val result = fakeColor.blueValue()

        // Then
        assertEquals(0, result)
    }

    // isRgbColor

    @Test
    fun `if color has exactly the compared rgb values should return true on isRgbColor`() {
        // Given
        val fakeColor = 0xFF64C832.toInt()

        // When
        val result = fakeColor.isRgbColor(
            redValueToCompare = 100,
            greenValueToCompare = 200,
            blueValueToCompare = 50
        )

        // Then
        assertTrue(result)
    }

    @Test
    fun `if every channel is 2 above the compared values should return true on isRgbColor`() {
        // Given
        val fakeColor = 0xFF66CA34.toInt()

        // When
        val result = fakeColor.isRgbColor(
            redValueToCompare = 100,
            greenValueToCompare = 200,
            blueValueToCompare = 50
        )

        // Then
        assertTrue(result)
    }

    @Test
    fun `if every channel is 2 below the compared values should return true on isRgbColor`() {
        // Given
        val fakeColor = 0xFF62C630.toInt()

        // When
        val result = fakeColor.isRgbColor(
            redValueToCompare = 100,
            greenValueToCompare = 200,
            blueValueToCompare = 50
        )

        // Then
        assertTrue(result)
    }

    @Test
    fun `if the red channel is 3 above the compared value should return false on isRgbColor`() {
        // Given
        val fakeColor = 0xFF67C832.toInt()

        // When
        val result = fakeColor.isRgbColor(
            redValueToCompare = 100,
            greenValueToCompare = 200,
            blueValueToCompare = 50
        )

        // Then
        assertFalse(result)
    }

    @Test
    fun `if the green channel is 3 below the compared value should return false on isRgbColor`() {
        // Given
        val fakeColor = 0xFF64C532.toInt()

        // When
        val result = fakeColor.isRgbColor(
            redValueToCompare = 100,
            greenValueToCompare = 200,
            blueValueToCompare = 50
        )

        // Then
        assertFalse(result)
    }

    @Test
    fun `if the blue channel is 3 above the compared value should return false on isRgbColor`() {
        // Given
        val fakeColor = 0xFF64C835.toInt()

        // When
        val result = fakeColor.isRgbColor(
            redValueToCompare = 100,
            greenValueToCompare = 200,
            blueValueToCompare = 50
        )

        // Then
        assertFalse(result)
    }

    @Test
    fun `if colors differ only in the alpha channel should return true on isRgbColor`() {
        // Given
        val fakeTransparentColor = 0x0064C832

        // When
        val result = fakeTransparentColor.isRgbColor(
            redValueToCompare = 100,
            greenValueToCompare = 200,
            blueValueToCompare = 50
        )

        // Then
        assertTrue(result)
    }

    @Test
    fun `if color is black and compared with zero values should return true on isRgbColor`() {
        // Given
        val fakeColor = 0xFF000000.toInt()

        // When
        val result = fakeColor.isRgbColor(
            redValueToCompare = 0,
            greenValueToCompare = 0,
            blueValueToCompare = 0
        )

        // Then
        assertTrue(result)
    }

    @Test
    fun `if color is white and compared with 255 values should return true on isRgbColor`() {
        // Given
        val fakeColor = 0xFFFFFFFF.toInt()

        // When
        val result = fakeColor.isRgbColor(
            redValueToCompare = 255,
            greenValueToCompare = 255,
            blueValueToCompare = 255
        )

        // Then
        assertTrue(result)
    }
}
