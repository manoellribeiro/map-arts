package manoellribeiro.dev.martp.scenes.onboarding

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import manoellribeiro.dev.martp.infra.MainDispatcherExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class OnboardingViewModelTest {

    private lateinit var viewModel: OnboardingViewModel
    private lateinit var repository: MartpRepository

    @BeforeEach
    fun setup() {
        repository = mockk()
        viewModel = OnboardingViewModel(repository = repository)
    }

    private fun <T> TestScope.collectStates(stateFlow: StateFlow<T>): List<T> {
        val states = mutableListOf<T>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            stateFlow.toList(states)
        }
        return states
    }

    @Test
    fun `if completeOnboarding was not called should not mark the onboarding as completed`() = runTest {
        // Given
        // Nothing: the user is still on the onboarding

        // When
        val states = collectStates(viewModel.isOnboardingCompleted)
        advanceUntilIdle()

        // Then
        assertEquals(listOf(false), states)
        coVerify(exactly = 0) { repository.setOnboardingSeen() }
    }

    @Test
    fun `if the repository saves the onboarding as seen should mark the onboarding as completed`() = runTest {
        // Given
        coEvery { repository.setOnboardingSeen() } just runs

        // When
        val states = collectStates(viewModel.isOnboardingCompleted)
        viewModel.completeOnboarding()
        advanceUntilIdle()

        // Then
        assertEquals(listOf(false, true), states)
        coVerify(exactly = 1) { repository.setOnboardingSeen() }
    }

    @Test
    fun `if the repository fails to save the onboarding as seen should still mark the onboarding as completed`() = runTest {
        // Given
        coEvery { repository.setOnboardingSeen() } throws LocalStorageErrorFailure("fake onboarding save error")

        // When
        val states = collectStates(viewModel.isOnboardingCompleted)
        viewModel.completeOnboarding()
        advanceUntilIdle()

        // Then
        assertEquals(listOf(false, true), states)
        coVerify(exactly = 1) { repository.setOnboardingSeen() }
    }
}
