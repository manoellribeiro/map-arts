package manoellribeiro.dev.martp.scenes.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.Failure
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: MartpRepository
) : ViewModel() {

    private val _isOnboardingCompleted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    /** Skip and Start creating both end the onboarding for good. */
    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                repository.setOnboardingSeen()
            } catch (e: Failure) {
                // Already logged by the repository; the user still goes on to the app
            }
            _isOnboardingCompleted.value = true
        }
    }
}
