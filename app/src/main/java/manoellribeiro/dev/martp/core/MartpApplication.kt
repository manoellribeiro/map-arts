package manoellribeiro.dev.martp.core

import android.app.Application
import android.os.Build
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import manoellribeiro.dev.martp.core.data.local.MartpDatabase

@HiltAndroidApp
class MartpApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance().setCustomKey("build_fingerprint", Build.FINGERPRINT)
    }

}