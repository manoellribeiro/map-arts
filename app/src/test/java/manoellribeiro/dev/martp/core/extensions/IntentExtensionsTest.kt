package manoellribeiro.dev.martp.core.extensions

import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private enum class FakeArtStyle { FAKE_FIRST_STYLE, FAKE_SECOND_STYLE, FAKE_THIRD_STYLE }

private enum class FakeMapTheme { FAKE_LIGHT_THEME, FAKE_DARK_THEME }

class IntentExtensionsTest {

    private lateinit var intent: Intent

    @BeforeEach
    fun setup() {
        intent = mockk()
    }

    // putExtra

    @Test
    fun `if an enum value is put should save its ordinal under the enum class name`() {
        // Given
        val fakeArtStyle = FakeArtStyle.FAKE_SECOND_STYLE
        val fakeExtraKey = FakeArtStyle::class.java.name
        every { intent.putExtra(fakeExtraKey, 1) } returns intent

        // When
        intent.putExtra(fakeArtStyle)

        // Then
        verify(exactly = 1) { intent.putExtra(fakeExtraKey, 1) }
    }

    @Test
    fun `if the first enum value is put should save the ordinal zero`() {
        // Given
        val fakeArtStyle = FakeArtStyle.FAKE_FIRST_STYLE
        val fakeExtraKey = FakeArtStyle::class.java.name
        every { intent.putExtra(fakeExtraKey, 0) } returns intent

        // When
        intent.putExtra(fakeArtStyle)

        // Then
        verify(exactly = 1) { intent.putExtra(fakeExtraKey, 0) }
    }

    @Test
    fun `if an enum value is put should return the intent returned by the android putExtra`() {
        // Given
        val fakeArtStyle = FakeArtStyle.FAKE_THIRD_STYLE
        val fakeReturnedIntent = mockk<Intent>()
        every { intent.putExtra(FakeArtStyle::class.java.name, 2) } returns fakeReturnedIntent

        // When
        val result = intent.putExtra(fakeArtStyle)

        // Then
        assertSame(fakeReturnedIntent, result)
    }

    @Test
    fun `if values of two enum types are put should save each one under its own class name`() {
        // Given
        val fakeArtStyle = FakeArtStyle.FAKE_THIRD_STYLE
        val fakeMapTheme = FakeMapTheme.FAKE_DARK_THEME
        every { intent.putExtra(FakeArtStyle::class.java.name, 2) } returns intent
        every { intent.putExtra(FakeMapTheme::class.java.name, 1) } returns intent

        // When
        intent.putExtra(fakeArtStyle)
        intent.putExtra(fakeMapTheme)

        // Then
        verify(exactly = 1) { intent.putExtra(FakeArtStyle::class.java.name, 2) }
        verify(exactly = 1) { intent.putExtra(FakeMapTheme::class.java.name, 1) }
    }

    // getEnumExtra

    @Test
    fun `if intent has the ordinal of an enum value should return that enum value`() {
        // Given
        val fakeExtraKey = FakeArtStyle::class.java.name
        every { intent.getIntExtra(fakeExtraKey, -1) } returns 2

        // When
        val result = intent.getEnumExtra<FakeArtStyle>()

        // Then
        assertEquals(FakeArtStyle.FAKE_THIRD_STYLE, result)
        verify(exactly = 1) { intent.getIntExtra(fakeExtraKey, -1) }
    }

    @Test
    fun `if intent has the ordinal zero should return the first enum value`() {
        // Given
        every { intent.getIntExtra(FakeArtStyle::class.java.name, -1) } returns 0

        // When
        val result = intent.getEnumExtra<FakeArtStyle>()

        // Then
        assertEquals(FakeArtStyle.FAKE_FIRST_STYLE, result)
    }

    @Test
    fun `if intent has no extra for the enum should return null`() {
        // Given
        every { intent.getIntExtra(FakeArtStyle::class.java.name, -1) } returns -1

        // When
        val result = intent.getEnumExtra<FakeArtStyle>()

        // Then
        assertNull(result)
    }

    @Test
    fun `if intent has only the extra of another enum type should return null`() {
        // Given
        every { intent.getIntExtra(FakeMapTheme::class.java.name, -1) } returns 1
        every { intent.getIntExtra(FakeArtStyle::class.java.name, -1) } returns -1

        // When
        val result = intent.getEnumExtra<FakeArtStyle>()

        // Then
        assertNull(result)
        verify(exactly = 0) { intent.getIntExtra(FakeMapTheme::class.java.name, any()) }
    }
}
