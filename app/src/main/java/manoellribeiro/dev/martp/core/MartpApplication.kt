package manoellribeiro.dev.martp.core

import android.app.Application
import android.os.Build
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp
import manoellribeiro.dev.martp.core.data.local.MartpDatabase

@HiltAndroidApp
class MartpApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)

        Firebase.appCheck.installAppCheckProviderFactory(appCheckProviderFactory)

        FirebaseCrashlytics.getInstance().setCustomKey("build_fingerprint", Build.FINGERPRINT)
    }

}