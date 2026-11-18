package manoellribeiro.dev.martp.core

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

val appCheckProviderFactory: AppCheckProviderFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
