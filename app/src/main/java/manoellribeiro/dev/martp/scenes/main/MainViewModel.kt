package manoellribeiro.dev.martp.scenes.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.di.ApplicationScope
import manoellribeiro.dev.martp.core.services.MapArtPrinterService
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.models.SketchArtType
import manoellribeiro.dev.martp.scenes.artStyleSettings.ArtStyleSettingsUiState
import manoellribeiro.dev.martp.scenes.artStyleSettings.MapArtStyleModel
import manoellribeiro.dev.martp.scenes.gallery.GalleryUiState
import manoellribeiro.dev.martp.scenes.userInfo.UserInfoUiState
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MartpRepository,
    private val mapArtPrinterService: MapArtPrinterService,
    @ApplicationScope private val applicationScope: CoroutineScope
): ViewModel() {

    private var setUserNameJob: Job? = null
    private var setUserEmailJob: Job? = null
    private var setMapZoomJob: Job? = null
    private var setMapStyleJob: Job? = null
    private var setColorPaletteJob: Job? = null

    val galleryState: StateFlow<GalleryUiState> = repository.observeUserMapArts()
        .map { mapArts ->
            if(mapArts.isNotEmpty()) {
                GalleryUiState.NotEmptyList(mapArts)
            } else {
                GalleryUiState.EmptyList
            }
        }
        .catch { e ->
            if(e is Failure) {
                emit(GalleryUiState.Error(e))
            } else {
                throw e
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GalleryUiState.Loading
        )

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

    private val _onboardingCheckState: MutableStateFlow<OnboardingCheckUiState> = MutableStateFlow(OnboardingCheckUiState.Checking)
    val onboardingCheckState: StateFlow<OnboardingCheckUiState> = _onboardingCheckState.asStateFlow()

    fun checkOnboarding() {
        viewModelScope.launch {
            _onboardingCheckState.value = if (repository.hasSeenOnboarding()) {
                OnboardingCheckUiState.OnboardingAlreadySeen
            } else {
                OnboardingCheckUiState.ShowOnboarding
            }
        }
    }

    fun connectToPrinter() {
        applicationScope.launch {
            mapArtPrinterService.connect()
        }
    }

    private fun getAvailableArtStyles(): List<MapArtStyleModel> {
        return buildList {
            if (mapArtPrinterService.isAvailable) {
                add(
                    MapArtStyleModel(
                        titleId = R.string.printable_map,
                        descriptionId = R.string.printable_map_description,
                        drawableImageId = R.drawable.ic_printable_style,
                        sketchArtType = SketchArtType.PRINTABLE_MAP
                    )
                )
            }
            add(
                MapArtStyleModel(
                    titleId = R.string.colorful_map,
                    descriptionId = R.string.colorful_map_description,
                    drawableImageId = R.drawable.ic_colorful_map,
                    sketchArtType = SketchArtType.COLORFUL_MAP
                )
            )
            add(
                MapArtStyleModel(
                    titleId = R.string.classic,
                    descriptionId = R.string.classic_description,
                    drawableImageId = R.drawable.ic_martp_logo,
                    sketchArtType = SketchArtType.DEFAULT
                )
            )
            add(
                MapArtStyleModel(
                    titleId = R.string.pointillism,
                    descriptionId = R.string.pointillism_colorful_description,
                    drawableImageId = R.drawable.ic_pointllism,
                    sketchArtType = SketchArtType.POINTILLISM
                )
            )
            //Still work in progress
//    MapArtStyleModel(
//        titleId = R.string.water_flow,
//        descriptionId = R.string.water_flow_description,
//        drawableImageId = R.drawable.ic_water_flow,
//        sketchArtType = SketchArtType.WATER_FLOW
//    ),
            //Still work in progress
//    MapArtStyleModel(
//        titleId = R.string.walking_people,
//        descriptionId = R.string.walking_people_description,
//        drawableImageId = R.drawable.ic_walking_people,
//        sketchArtType = SketchArtType.PARTICLE_SYSTEM
//    ),
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
            val settingsLoadedState = coroutineScope {
                val colorPalettes = async { repository.getColorsPalettes() }
                val mapZoom = async { repository.getMapZoomPreference() }
                val artStyle = async { repository.getArtStylePreference() }
                val colorPaletteName = async { repository.getColorPalettePreference() }
                val loadedColorPalettes = colorPalettes.await()
                val savedColorPaletteName = colorPaletteName.await()
                ArtStyleSettingsUiState.SettingsLoaded(
                    mapZoom = mapZoom.await(),
                    mapArtStyle = artStyle.await(),
                    colorPaletteSelected = loadedColorPalettes
                        .firstOrNull { colorPalette -> colorPalette.name == savedColorPaletteName }
                        ?.name ?: loadedColorPalettes.first().name,
                    colorsPalettes = loadedColorPalettes,
                    artStylesAvailable = getAvailableArtStyles()
                )
            }
            emitNewArtStyleSettingsState(
                settingsLoadedState
            )
        }
    }

    fun setUserName(username: String, userId: String) {
        setUserNameJob?.cancel()
        setUserNameJob = viewModelScope.launch {
            delay(1000.milliseconds)
            repository.setUserName(username, userId)
        }
    }

    fun setUserEmail(email: String, userId: String) {
        setUserEmailJob?.cancel()
        setUserEmailJob = viewModelScope.launch {
            delay(1000.milliseconds)
            repository.setUserEmail(email, userId)
        }
    }

    fun setMapZoom(mapZoom: Float) {
        setMapZoomJob?.cancel()
        setMapZoomJob = viewModelScope.launch {
            delay(1000.milliseconds)
            repository.setMapZoomPreference(mapZoom)
        }
    }

    fun setMapStyle(sketchArtType: SketchArtType) {
        setMapStyleJob?.cancel()
        setMapStyleJob = viewModelScope.launch {
            delay(1000.milliseconds)
            repository.setMapArtStylePreference(sketchArtType)
        }
    }

    fun setColorPalette(name: String) {
        setColorPaletteJob?.cancel()
        setColorPaletteJob = viewModelScope.launch {
            delay(1000.milliseconds)
            repository.setColorPalettePreference(name)
        }
    }

    fun handleBadgesVisibilities() {
        viewModelScope.launch {
            val user = repository.fetchCurrentUserInfo()
            _isUserInfoBadgeVisible.value = user.username.isNullOrEmpty() && user.email.isNullOrEmpty()
        }
    }
}