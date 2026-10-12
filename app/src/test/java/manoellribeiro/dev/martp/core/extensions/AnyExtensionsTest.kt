package manoellribeiro.dev.martp.core.extensions

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnyExtensionsTest {

    private lateinit var executable: () -> String

    @BeforeEach
    fun setup() {
        executable = mockk()
    }

    @Test
    fun `if receiver is not null should run the executable of executeIfNotNull`() {
        // Given
        val fakeUsername: String? = "fake username"
        every { executable() } returns "fake executable result"

        // When
        fakeUsername.executeIfNotNull(executable)

        // Then
        verify(exactly = 1) { executable() }
    }

    @Test
    fun `if receiver is null should not run the executable of executeIfNotNull`() {
        // Given
        val fakeUsername: String? = null

        // When
        fakeUsername.executeIfNotNull(executable)

        // Then
        verify(exactly = 0) { executable() }
    }

    @Test
    fun `if receiver is null should run the executable of executeIfNull`() {
        // Given
        val fakeUsername: String? = null
        every { executable() } returns "fake executable result"

        // When
        fakeUsername.executeIfNull(executable)

        // Then
        verify(exactly = 1) { executable() }
    }

    @Test
    fun `if receiver is not null should not run the executable of executeIfNull`() {
        // Given
        val fakeUsername: String? = "fake username"

        // When
        fakeUsername.executeIfNull(executable)

        // Then
        verify(exactly = 0) { executable() }
    }

    @Test
    fun `if receiver is not null should return the receiver and not call the orNull fallback`() {
        // Given
        val fakeUsername: String? = "fake username"

        // When
        val result = fakeUsername.orNull(executable)

        // Then
        assertEquals("fake username", result)
        verify(exactly = 0) { executable() }
    }

    @Test
    fun `if receiver is null should return the value of the orNull fallback`() {
        // Given
        val fakeUsername: String? = null
        val fakeFallbackUsername = "fake fallback username"
        every { executable() } returns fakeFallbackUsername

        // When
        val result = fakeUsername.orNull(executable)

        // Then
        assertEquals(fakeFallbackUsername, result)
        verify(exactly = 1) { executable() }
    }

    @Test
    fun `if receiver is an empty string should return the receiver and not call the orNull fallback`() {
        // Given
        val fakeUsername: String? = ""

        // When
        val result = fakeUsername.orNull(executable)

        // Then
        assertEquals("", result)
        verify(exactly = 0) { executable() }
    }
}
