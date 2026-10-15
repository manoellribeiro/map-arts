package manoellribeiro.dev.martp.infra

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class MainDispatcherExtension : AfterEachCallback, BeforeEachCallback {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun beforeEach(context: ExtensionContext) {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun afterEach(context: ExtensionContext) {
        Dispatchers.resetMain()
    }
}
