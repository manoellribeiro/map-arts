package manoellribeiro.dev.martp.core

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

val appCheckProviderFactory: AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
