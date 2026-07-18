package manoellribeiro.dev.martp.scenes.main

import android.os.Handler
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import manoellribeiro.dev.martp.scenes.artStyleSettings.ArtStyleSettingsUiState
import manoellribeiro.dev.martp.scenes.gallery.GalleryUiState
import manoellribeiro.dev.martp.scenes.userInfo.UserInfoUiState
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MartpRepository,
    private val setMapZoomHandler: Handler,
    private val setMapStyleHandler: Handler
): ViewModel() {

    private val _galleryState: MutableLiveData<GalleryUiState> = MutableLiveData<GalleryUiState>()
    val galleryState: LiveData<GalleryUiState> = _galleryState

    private fun emitNewGalleryState(newState: GalleryUiState) {
        _galleryState.value = newState
    }

    private val _userInfoState: MutableLiveData<UserInfoUiState> = MutableLiveData<UserInfoUiState>()
    val userInfoState: LiveData<UserInfoUiState> = _userInfoState

    private fun emitNewUserInfoState(newState: UserInfoUiState) {
        _userInfoState.value = newState
    }

    private val _artStyleSettingsState: MutableLiveData<ArtStyleSettingsUiState> = MutableLiveData<ArtStyleSettingsUiState>()
    val artStyleSettingsState: LiveData<ArtStyleSettingsUiState> = _artStyleSettingsState

    private fun emitNewArtStyleSettingsState(newState: ArtStyleSettingsUiState) {
        _artStyleSettingsState.value = newState
    }

    private val _mainState: MutableLiveData<MainUiState> = MutableLiveData<MainUiState>()
    val mainState: LiveData<MainUiState> = _mainState

    private fun emitNewMainState(newState: MainUiState) {
        _mainState.value = newState
    }

    fun getUserMapArts() = viewModelScope.launch {
        try {
            emitNewGalleryState(GalleryUiState.Loading)
            val mapArts = repository.fetchUserMapArts().await()
            if(mapArts.isNotEmpty()) {
                emitNewGalleryState(GalleryUiState.NotEmptyList(mapArts))
            } else {
                emitNewGalleryState(GalleryUiState.EmptyList)
            }
        } catch (failure: Failure) {
            emitNewGalleryState(GalleryUiState.Error(failure))
        }
    }

    fun tryToGetUserInfo() = viewModelScope.launch {
        emitNewMainState(
            MainUiState.SetUserInfoBadgeVisibility(
                visible = false
            )
        )
        emitNewUserInfoState(UserInfoUiState.Loading)
        val userInfo = repository.fetchCurrentUserInfo().await()
        emitNewUserInfoState(UserInfoUiState.UserFound(userInfo))
    }

    fun getArtStyleSettings() = viewModelScope.launch {
        emitNewArtStyleSettingsState(ArtStyleSettingsUiState.Loading)
        val mapZoom = repository.getMapZoomPreference().await()
        val artStyle = repository.getArtStylePreference().await()
        emitNewArtStyleSettingsState(
            ArtStyleSettingsUiState.SettingsLoaded(
                mapZoom = mapZoom,
                mapArtStyle = artStyle
            )
        )
    }

    fun setUserName(username: String, userId: String) = viewModelScope.launch {
        repository.setUserName(username, userId)
    }

    fun setUserEmail(email: String, userId: String) = viewModelScope.launch {
        repository.setUserEmail(email, userId)
    }

    private fun createSetMapZoomRunnable(mapZoom: Float): Runnable {
        return Runnable {
            repository.setMapZoomPreference(mapZoom)
        }
    }

    fun setMapZoom(mapZoom: Float) {
        Log.i("MartpRepository", "mapZoom main viewModel: " + mapZoom.toString())
        setMapZoomHandler.removeCallbacksAndMessages(null)
        setMapZoomHandler.postDelayed(createSetMapZoomRunnable(mapZoom), 1500)
    }

    private fun createSetMapStyleRunnable(sketchArtType: SketchArtType): Runnable {
        return Runnable {
            repository.setMapArtStylePreference(sketchArtType)
        }
    }

    fun setMapStyle(sketchArtType: SketchArtType) {
        setMapStyleHandler.removeCallbacksAndMessages(null)
        setMapStyleHandler.postDelayed(createSetMapStyleRunnable(sketchArtType), 1500)
    }

    fun handleBadgesVisibilities() = viewModelScope.launch {
        val user = repository.fetchCurrentUserInfo().await()
        emitNewMainState(
            MainUiState.SetUserInfoBadgeVisibility(
                visible = user.username.isNullOrEmpty() && user.email.isNullOrEmpty()
            )
        )
    }
}