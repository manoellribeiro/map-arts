package manoellribeiro.dev.martp.core.services

import com.google.firebase.ai.GenerativeModel
import kotlinx.coroutines.CancellationException
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.models.failures.GeneratingAIContentFailure

class GenerateAIContentService(
    private val generativeModel: GenerativeModel,
    private val errorLogger: ErrorLogger
) {
    suspend fun generateTextContent(prompt: String): String? {
        try {
            val response = generativeModel.generateContent(prompt)
            return response.text
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLogger.logNonFatal(e, "GenerateAIContentService.generateTextContent")
            throw GeneratingAIContentFailure(originalExceptionMessage = e.message)
        }
    }
}