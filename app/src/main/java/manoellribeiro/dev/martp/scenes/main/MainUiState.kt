package manoellribeiro.dev.martp.scenes.main

sealed interface MainUiState {
    class SetUserInfoBadgeVisibility(val visible: Boolean): MainUiState
}