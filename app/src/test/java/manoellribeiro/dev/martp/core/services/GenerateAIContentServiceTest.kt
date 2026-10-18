package manoellribeiro.dev.martp.core.services

import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.models.failures.GeneratingAIContentFailure
import manoellribeiro.dev.martp.infra.MainDispatcherExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherExtension::class)
class GenerateAIContentServiceTest {

    private lateinit var generateAIContentService: GenerateAIContentService
    private lateinit var generativeModel: GenerativeModel
    private lateinit var fakeResponse: GenerateContentResponse
    private lateinit var errorLogger: ErrorLogger

    @BeforeEach
    fun setup() {
        generativeModel = mockk()
        fakeResponse = mockk()
        errorLogger = mockk()

        generateAIContentService = GenerateAIContentService(
            generativeModel = generativeModel,
            errorLogger = errorLogger
        )
    }

    @Test
    fun `if generative model returns a response with text should return the text`() = runTest {
        // Given
        val fakePrompt = "fake prompt asking for a title"
        val fakeGeneratedText = "fake generated title"
        every { fakeResponse.text } returns fakeGeneratedText
        coEvery { generativeModel.generateContent(fakePrompt) } returns fakeResponse

        // When
        val generatedText = generateAIContentService.generateTextContent(fakePrompt)

        // Then
        assertEquals(fakeGeneratedText, generatedText)
        coVerify(exactly = 1) { generativeModel.generateContent(fakePrompt) }
    }

    @Test
    fun `if generative model returns a response without text should return null`() = runTest {
        // Given
        val fakePrompt = "fake prompt asking for a description"
        every { fakeResponse.text } returns null
        coEvery { generativeModel.generateContent(fakePrompt) } returns fakeResponse

        // When
        val generatedText = generateAIContentService.generateTextContent(fakePrompt)

        // Then
        assertNull(generatedText)
        coVerify(exactly = 1) { generativeModel.generateContent(fakePrompt) }
    }

    @Test
    fun `if generative model throws an exception should throw a GeneratingAIContentFailure with its message`() = runTest {
        // Given
        val fakePrompt = "fake prompt that fails"
        val fakeErrorMessage = "fake generative model error"
        coEvery { generativeModel.generateContent(fakePrompt) } throws IllegalStateException(fakeErrorMessage)
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<GeneratingAIContentFailure> {
            generateAIContentService.generateTextContent(fakePrompt)
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        assertEquals(R.string.generate_ai_content_failure_message, failure.messageToBeDisplayedToUserId)
        coVerify(exactly = 1) { generativeModel.generateContent(fakePrompt) }
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalStateException>(), "GenerateAIContentService.generateTextContent") }
    }

    @Test
    fun `if generative model throws an exception without message should throw a GeneratingAIContentFailure with null message`() = runTest {
        // Given
        val fakePrompt = "fake prompt that fails without message"
        coEvery { generativeModel.generateContent(fakePrompt) } throws IllegalStateException()
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<GeneratingAIContentFailure> {
            generateAIContentService.generateTextContent(fakePrompt)
        }

        // Then
        assertNull(failure.message)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalStateException>(), "GenerateAIContentService.generateTextContent") }
    }

    @Test
    fun `if reading the response text throws an exception should throw a GeneratingAIContentFailure`() = runTest {
        // Given
        val fakePrompt = "fake prompt with a broken response"
        val fakeErrorMessage = "fake response text error"
        every { fakeResponse.text } throws IllegalStateException(fakeErrorMessage)
        coEvery { generativeModel.generateContent(fakePrompt) } returns fakeResponse
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<GeneratingAIContentFailure> {
            generateAIContentService.generateTextContent(fakePrompt)
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalStateException>(), "GenerateAIContentService.generateTextContent") }
    }

    @Test
    fun `if generative model throws a CancellationException should rethrow the same exception`() = runTest {
        // Given
        val fakePrompt = "fake prompt that gets cancelled"
        val fakeCancellationException = CancellationException("fake cancellation")
        coEvery { generativeModel.generateContent(fakePrompt) } throws fakeCancellationException

        // When
        val thrownException = assertThrows<CancellationException> {
            generateAIContentService.generateTextContent(fakePrompt)
        }

        // Then
        assertSame(fakeCancellationException, thrownException)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }
}
