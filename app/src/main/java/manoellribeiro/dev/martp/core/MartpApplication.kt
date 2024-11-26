package manoellribeiro.dev.martp.core

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import manoellribeiro.dev.martp.core.data.local.MartpDatabase

@HiltAndroidApp
class MartpApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        MartpDatabase.initialize(applicationContext)
    }

}