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
}
