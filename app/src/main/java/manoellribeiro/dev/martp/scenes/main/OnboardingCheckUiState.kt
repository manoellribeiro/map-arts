package manoellribeiro.dev.martp.scenes.main

/** Whether the app opens on the onboarding; the splash screen stays while it is Checking. */
sealed interface OnboardingCheckUiState {
    object Checking : OnboardingCheckUiState
    object ShowOnboarding : OnboardingCheckUiState
    object OnboardingAlreadySeen : OnboardingCheckUiState
}
