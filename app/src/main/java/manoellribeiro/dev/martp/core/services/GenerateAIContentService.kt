package manoellribeiro.dev.martp.core.services

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import manoellribeiro.dev.martp.core.models.failures.GeneratingAIContentFailure

class GenerateAIContentService(
    private val generativeModel: GenerativeModel
) {
    fun generateTextContent(prompt: String): Deferred<String?> {
        return CoroutineScope(Dispatchers.IO).async {
            try {
                val response = generativeModel.generateContent(prompt)
                return@async response.text
            } catch (e: Exception) {
                throw GeneratingAIContentFailure(originalExceptionMessage = e.message)
            }
        }
    }
}