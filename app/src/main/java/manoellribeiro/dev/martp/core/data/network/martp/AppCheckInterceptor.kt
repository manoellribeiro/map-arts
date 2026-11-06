package manoellribeiro.dev.martp.core.data.network.martp

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.appCheck
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AppCheckInterceptor(
    private val errorLogger: ErrorLogger,
    private val appCheck: FirebaseAppCheck = Firebase.appCheck
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = try {
            Tasks.await(appCheck.getAppCheckToken(false)).token
        } catch (exception: Exception) {
            errorLogger.logNonFatal(exception, "AppCheckInterceptor.intercept")
            throw IOException("Could not get the App Check token", exception)
        }

        val request = chain.request()
            .newBuilder()
            .header(APP_CHECK_HEADER, token)
            .build()

        return chain.proceed(request)
    }

    companion object {
        const val APP_CHECK_HEADER = "X-Firebase-AppCheck"
    }
}
