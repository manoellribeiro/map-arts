package manoellribeiro.dev.martp.scenes.userInfo

import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.scenes.gallery.GalleryUiState

sealed interface UserInfoUiState {
    object Loading: UserInfoUiState
    data class UserFound(val user: UserInfoEntity): UserInfoUiState
}