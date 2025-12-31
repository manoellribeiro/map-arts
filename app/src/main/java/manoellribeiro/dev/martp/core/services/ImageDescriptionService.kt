package manoellribeiro.dev.martp.core.services

import android.graphics.Bitmap
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.imagedescription.ImageDescriber
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.suspendCancellableCoroutine
import manoellribeiro.dev.martp.core.models.failures.UnavailableImageDescriptorFailure
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ImageDescriptionService @Inject constructor(
    private val imageDescriber: ImageDescriber
) {

    fun prepareImageDescription(): Deferred<Unit> = CoroutineScope(Dispatchers.IO).async { //TODO: search the right way to start this coroutine
        try {
            val featureStatus = imageDescriber.checkFeatureStatus().await()
            if(featureStatus == FeatureStatus.DOWNLOADABLE) {
                downloadFeature().await()
            } else if(featureStatus == FeatureStatus.UNAVAILABLE) {
                throw UnavailableImageDescriptorFailure(null)
            }
        } catch (e: Exception) {
            throw UnavailableImageDescriptorFailure(e.message)
        }
    }

    fun getImageDescription(
        imageBitmap: Bitmap
    ): Deferred<String> = CoroutineScope(Dispatchers.IO).async {

        val imageDescriptionRequest = ImageDescriptionRequest
            .builder(imageBitmap)
            .build()
        val imageDescription = imageDescriber.runInference(imageDescriptionRequest).await().description
        return@async imageDescription
    }

    private fun downloadFeature(): Deferred<Unit> = CoroutineScope(Dispatchers.IO).async {
        suspendCancellableCoroutine { continuation ->
            imageDescriber.downloadFeature(object: DownloadCallback {
                override fun onDownloadCompleted() {
                    continuation.resume(Unit)
                }

                override fun onDownloadFailed(e: GenAiException) {
                    continuation.resumeWithException(UnavailableImageDescriptorFailure(e.message))
                }

                override fun onDownloadProgress(p0: Long) {}

                override fun onDownloadStarted(p0: Long) {}

            })
        }
    }


    fun closeImageDescriber() {
        imageDescriber.close()
    }

}