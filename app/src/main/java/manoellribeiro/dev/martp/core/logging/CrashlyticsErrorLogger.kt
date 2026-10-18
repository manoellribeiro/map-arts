package manoellribeiro.dev.martp.core.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import manoellribeiro.dev.martp.BuildConfig

class CrashlyticsErrorLogger(
    private val crashlytics: FirebaseCrashlytics
) : ErrorLogger {

    override fun logNonFatal(throwable: Throwable, operation: String) {
        crashlytics.log("Non-fatal in $operation")
        crashlytics.recordException(throwable)
        if (BuildConfig.DEBUG) {
            Log.e("ErrorLogger", operation, throwable)
        }
    }
}
