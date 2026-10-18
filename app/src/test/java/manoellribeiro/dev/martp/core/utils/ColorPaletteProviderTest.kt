package manoellribeiro.dev.martp.core.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ColorPaletteProviderTest {

    // region byNameOrDefault

    @Test
    fun `if name matches a color palette should return that color palette`() {
        // Given
        val fakeExistingName = "Hokusai Blue"

        // When
        val result = ColorPaletteProvider.byNameOrDefault(fakeExistingName)

        // Then
        assertEquals(fakeExistingName, result.name)
        assertSame(ColorPaletteProvider.all.first { it.name == fakeExistingName }, result)
    }

    @Test
    fun `if name matches the last color palette should return the last color palette`() {
        // Given
        val fakeLastName = ColorPaletteProvider.all.last().name

        // When
        val result = ColorPaletteProvider.byNameOrDefault(fakeLastName)

        // Then
        assertSame(ColorPaletteProvider.all.last(), result)
    }

    @Test
    fun `if name is null should return the first color palette`() {
        // Given
        val fakeName: String? = null

        // When
        val result = ColorPaletteProvider.byNameOrDefault(fakeName)

        // Then
        assertSame(ColorPaletteProvider.all.first(), result)
    }

    @Test
    fun `if name does not match any color palette should return the first color palette`() {
        // Given
        val fakeUnknownName = "Fake Unknown Palette"

        // When
        val result = ColorPaletteProvider.byNameOrDefault(fakeUnknownName)

        // Then
        assertSame(ColorPaletteProvider.all.first(), result)
    }

    @Test
    fun `if name is empty should return the first color palette`() {
        // Given
        val fakeEmptyName = ""

        // When
        val result = ColorPaletteProvider.byNameOrDefault(fakeEmptyName)

        // Then
        assertSame(ColorPaletteProvider.all.first(), result)
    }

    @Test
    fun `if name differs only in letter case should return the first color palette`() {
        // Given
        val fakeLowercaseName = "monet"

        // When
        val result = ColorPaletteProvider.byNameOrDefault(fakeLowercaseName)

        // Then
        assertSame(ColorPaletteProvider.all.first(), result)
    }

    @Test
    fun `if name is the default name used by the repository should return the Martp color palette`() {
        // Given
        // Same value as the default of MartpRepository.getColorPalettePreference
        val fakeRepositoryDefaultName = "Martp"

        // When
        val result = ColorPaletteProvider.byNameOrDefault(fakeRepositoryDefaultName)

        // Then
        assertEquals("Martp", result.name)
        assertSame(ColorPaletteProvider.all.first(), result)
    }

    // endregion

    // region all

    @Test
    fun `if color palettes are listed should have unique names`() {
        // Given
        val fakeNames = ColorPaletteProvider.all.map { it.name }

        // When
        val distinctNames = fakeNames.distinct()

        // Then
        // A duplicated name would make the second palette unreachable by byNameOrDefault
        assertEquals(fakeNames, distinctNames)
    }

    // endregion
}
