package manoellribeiro.dev.martp.scenes.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import manoellribeiro.dev.martp.scenes.artStyleSettings.ArtStyleSettingsUiState
import manoellribeiro.dev.martp.scenes.gallery.GalleryUiState
import manoellribeiro.dev.martp.scenes.userInfo.UserInfoUiState
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MartpRepository
): ViewModel() {

    private var setUserNameJob: Job? = null
    private var setUserEmailJob: Job? = null
    private var setMapZoomJob: Job? = null
    private var setMapStyleJob: Job? = null

    private val _galleryState: MutableLiveData<GalleryUiState> = MutableLiveData<GalleryUiState>()
    val galleryState: LiveData<GalleryUiState> = _galleryState

    private fun emitNewGalleryState(newState: GalleryUiState) {
        _galleryState.value = newState
    }

    private val _userInfoState: MutableStateFlow<UserInfoUiState> = MutableStateFlow(UserInfoUiState.Loading)
    val userInfoState: StateFlow<UserInfoUiState> = _userInfoState.asStateFlow()

    private fun emitNewUserInfoState(newState: UserInfoUiState) {
        _userInfoState.value = newState
    }

    private val _artStyleSettingsState: MutableStateFlow<ArtStyleSettingsUiState> = MutableStateFlow(ArtStyleSettingsUiState.Loading)
    val artStyleSettingsState: StateFlow<ArtStyleSettingsUiState> = _artStyleSettingsState.asStateFlow()

    private fun emitNewArtStyleSettingsState(newState: ArtStyleSettingsUiState) {
        _artStyleSettingsState.value = newState
    }

    private val _isUserInfoBadgeVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isUserInfoBadgeVisible: StateFlow<Boolean> = _isUserInfoBadgeVisible.asStateFlow()

    fun getUserMapArts() {
        viewModelScope.launch {
            try {
                emitNewGalleryState(GalleryUiState.Loading)
                val mapArts = repository.fetchUserMapArts()
                if(mapArts.isNotEmpty()) {
                    emitNewGalleryState(GalleryUiState.NotEmptyList(mapArts))
                } else {
                    emitNewGalleryState(GalleryUiState.EmptyList)
                }
            } catch (failure: Failure) {
                emitNewGalleryState(GalleryUiState.Error(failure))
            }
        }
    }

    fun tryToGetUserInfo() {
        viewModelScope.launch {
            _isUserInfoBadgeVisible.value = false
            emitNewUserInfoState(UserInfoUiState.Loading)
            val userInfo = repository.fetchCurrentUserInfo()
            emitNewUserInfoState(UserInfoUiState.UserFound(userInfo))
        }
    }

    fun getArtStyleSettings() {
        viewModelScope.launch {
            emitNewArtStyleSettingsState(ArtStyleSettingsUiState.Loading)
            val mapZoom = repository.getMapZoomPreference()
            val artStyle = repository.getArtStylePreference()
            emitNewArtStyleSettingsState(
                ArtStyleSettingsUiState.SettingsLoaded(
                    mapZoom = mapZoom,
                    mapArtStyle = artStyle
                )
            )
        }
    }

    fun setUserName(username: String, userId: String) {
        setUserNameJob?.cancel()
        setUserNameJob = viewModelScope.launch {
            delay(2000.milliseconds)
            repository.setUserName(username, userId)
        }
    }

    fun setUserEmail(email: String, userId: String) {
        setUserEmailJob?.cancel()
        setUserEmailJob = viewModelScope.launch {
            delay(2000.milliseconds)
            repository.setUserEmail(email, userId)
        }
    }

    fun setMapZoom(mapZoom: Float) {
        setMapZoomJob?.cancel()
        setMapZoomJob = viewModelScope.launch {
            delay(1500.milliseconds)
            repository.setMapZoomPreference(mapZoom)
        }
    }

    fun setMapStyle(sketchArtType: SketchArtType) {
        setMapStyleJob?.cancel()
        setMapStyleJob = viewModelScope.launch {
            delay(1500.milliseconds)
            repository.setMapArtStylePreference(sketchArtType)
        }
    }

    fun handleBadgesVisibilities() {
        viewModelScope.launch {
            val user = repository.fetchCurrentUserInfo()
            _isUserInfoBadgeVisible.value = user.username.isNullOrEmpty() && user.email.isNullOrEmpty()
        }
    }
}