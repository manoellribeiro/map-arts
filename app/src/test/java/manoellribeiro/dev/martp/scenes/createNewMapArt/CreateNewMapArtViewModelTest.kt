package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.location.Address
import android.location.Location
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.data.local.entities.ColorPaletteEntity
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity
import manoellribeiro.dev.martp.core.data.network.martp.models.RemoteReversedAddress
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import manoellribeiro.dev.martp.core.models.failures.LocationPermissionNotGrantedFailure
import manoellribeiro.dev.martp.core.models.failures.MapboxApiErrorFailure
import manoellribeiro.dev.martp.core.models.failures.MartpApiErrorFailure
import manoellribeiro.dev.martp.core.models.failures.NoInternetConnectionFailure
import manoellribeiro.dev.martp.core.models.failures.UnknownErrorFailure
import manoellribeiro.dev.martp.core.models.MapLayersColors
import manoellribeiro.dev.martp.core.models.SketchArtType
import manoellribeiro.dev.martp.core.services.GenerateAIContentService
import manoellribeiro.dev.martp.core.services.GetAddressService
import manoellribeiro.dev.martp.core.services.LocationService
import manoellribeiro.dev.martp.core.utils.PromptGenerator
import manoellribeiro.dev.martp.infra.MainDispatcherExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class CreateNewMapArtViewModelTest {

    private lateinit var repository: MartpRepository
    private lateinit var locationService: LocationService
    private lateinit var getAddressService: GetAddressService
    private lateinit var generateAIContentService: GenerateAIContentService
    private lateinit var promptGenerator: PromptGenerator
    private lateinit var viewModel: CreateNewMapArtViewModel

    // 2 * MartpSketch.frameThickness (30) + 2 * MartpSketch.framePadding (30)
    private val fakeFrameSize = 120

    @BeforeEach
    fun setup() {
        // Initialize mocks
        repository = mockk()
        locationService = mockk()
        getAddressService = mockk()
        generateAIContentService = mockk()
        promptGenerator = mockk()

        // Create the class under test
        viewModel = CreateNewMapArtViewModel(
            repository = repository,
            locationService = locationService,
            getAddressService = getAddressService,
            generateAIContentService = generateAIContentService,
            promptGenerator = promptGenerator
        )
    }

    private fun TestScope.collectStates(): List<CreateNewMapArtUiState> {
        val states = mutableListOf<CreateNewMapArtUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.toList(states)
        }
        return states
    }

    private fun TestScope.collectEvents(): List<CreateNewMapArtEvent> {
        val events = mutableListOf<CreateNewMapArtEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    private fun createFakeLocation(
        latitude: Double,
        longitude: Double
    ): Location {
        val fakeLocation = mockk<Location>()
        every { fakeLocation.latitude } returns latitude
        every { fakeLocation.longitude } returns longitude
        return fakeLocation
    }

    private fun createFakeUserInfo(
        id: String = "fake-user-id",
        username: String? = "Fake User",
        email: String? = "fake.user@example.com"
    ): UserInfoEntity {
        return UserInfoEntity(id = id, username = username, email = email)
    }

    private fun createFakeAddress(
        thoroughfare: String?,
        locality: String?,
        countryName: String?
    ): Address {
        val fakeAddress = mockk<Address>()
        every { fakeAddress.thoroughfare } returns thoroughfare
        every { fakeAddress.locality } returns locality
        every { fakeAddress.countryName } returns countryName
        return fakeAddress
    }

    private val fakeAccessingLocationLoading = CreateNewMapArtUiState.Loading(R.string.accessing_your_location)
    private val fakeDownloadingImageLoading = CreateNewMapArtUiState.Loading(R.string.downloading_map_image)

    // region initial state

    @Test
    fun `if no method is called should keep the Idle ui state and send no events`() = runTest {
        // Given
        val states = collectStates()
        val events = collectEvents()

        // When
        advanceUntilIdle()

        // Then
        assertEquals(listOf(CreateNewMapArtUiState.Idle), states)
        assertSame(CreateNewMapArtUiState.Idle, viewModel.state.value)
        assertTrue(events.isEmpty())
    }

    // endregion

    // region startToGenerateMapArt

    @Test
    fun `if all dependencies succeed should emit the two Loading messages then ImageDownloaded ui state`() = runTest {
        // Given
        val fakeLatitude = -12.9714
        val fakeLongitude = -38.5014
        val fakeStaticImagePath = "fake-directory/image.png"
        val fakeUserInfo = createFakeUserInfo(id = "fake-happy-path-user-id")
        val fakeLocation = createFakeLocation(latitude = fakeLatitude, longitude = fakeLongitude)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Street",
            locality = "Fake City",
            countryName = "Fake Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.POINTILLISM
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns fakeUserInfo
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns fakeStaticImagePath
        val states = collectStates()

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-directory"),
            canvasToDrawArtWidth = 1000,
            canvasToDrawArtHeight = 1000
        )
        advanceUntilIdle()

        // Then
        assertEquals(4, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertEquals(fakeAccessingLocationLoading, states[1])
        assertEquals(fakeDownloadingImageLoading, states[2])
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            states[3]
        )
        assertEquals(fakeStaticImagePath, imageDownloadedState.staticMapImagePath)
        assertEquals("Fake Street, Fake City, Fake Country", imageDownloadedState.title)
        assertEquals(SketchArtType.POINTILLISM, imageDownloadedState.sketchArtType)
        assertEquals(fakeLatitude, imageDownloadedState.latitude)
        assertEquals(fakeLongitude, imageDownloadedState.longitude)
        assertEquals(fakeUserInfo, imageDownloadedState.user)
        coVerify(exactly = 1) { repository.fetchCurrentUserInfo() }
    }

    @Test
    fun `if the art style is not COLORFUL_MAP should emit ImageDownloaded without color palette and not read it`() = runTest {
        // Given
        val fakeLocation = createFakeLocation(latitude = 40.4168, longitude = -3.7038)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Default Street",
            locality = "Fake Default City",
            countryName = "Fake Default Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-default-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-default-directory"),
            canvasToDrawArtWidth = 600,
            canvasToDrawArtHeight = 800
        )
        advanceUntilIdle()

        // Then
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            viewModel.state.value
        )
        assertNull(imageDownloadedState.colorPalette)
        coVerify(exactly = 0) { repository.getSelectedColorPalette() }
    }

    @Test
    fun `if the art style is COLORFUL_MAP should emit ImageDownloaded with the selected color palette`() = runTest {
        // Given
        val fakeColorPalette = ColorPaletteEntity(
            name = "Fake Colorful Palette",
            colors = listOf(0xFF102030.toInt(), 0xFF405060.toInt())
        )
        val fakeLocation = createFakeLocation(latitude = 35.6586, longitude = 139.7454)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Colorful Street",
            locality = "Fake Colorful City",
            countryName = "Fake Colorful Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.COLORFUL_MAP
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery { repository.getSelectedColorPalette() } returns fakeColorPalette
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-colorful-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-colorful-directory"),
            canvasToDrawArtWidth = 600,
            canvasToDrawArtHeight = 800
        )
        advanceUntilIdle()

        // Then
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            viewModel.state.value
        )
        assertEquals(fakeColorPalette, imageDownloadedState.colorPalette)
        assertEquals(SketchArtType.COLORFUL_MAP, imageDownloadedState.sketchArtType)
        coVerify(exactly = 1) { repository.getSelectedColorPalette() }
    }

    @Test
    fun `if all dependencies succeed should fetch the address and the static map image with the location and the canvas size minus the frame`() = runTest {
        // Given
        val fakeDirectory = File("fake-frame-directory")
        val fakeLatitude = 48.8584
        val fakeLongitude = 2.2945
        val fakeCanvasWidth = 1000
        val fakeCanvasHeight = 900
        val fakeLocation = createFakeLocation(latitude = fakeLatitude, longitude = fakeLongitude)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Frame Street",
            locality = "Fake Frame City",
            countryName = "Fake Frame Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-frame-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = fakeDirectory,
            canvasToDrawArtWidth = fakeCanvasWidth,
            canvasToDrawArtHeight = fakeCanvasHeight
        )
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.getArtStylePreference() }
        coVerify(exactly = 1) { locationService.getCurrentLocation() }
        coVerify(exactly = 1) { getAddressService.getAddress(fakeLatitude, fakeLongitude) }
        coVerify(exactly = 1) {
            repository.fetchStaticMapImage(
                sketchArtType = SketchArtType.DEFAULT,
                latitude = fakeLatitude,
                longitude = fakeLongitude,
                mapWidth = fakeCanvasWidth - fakeFrameSize,
                mapHeight = fakeCanvasHeight - fakeFrameSize,
                dir = fakeDirectory
            )
        }
    }

    @Test
    fun `if the canvas height minus the frame is above 1280 should fetch the static map image with a height of 1280`() = runTest {
        // Given
        val fakeCanvasWidth = 1080
        val fakeCanvasHeight = 2400
        val fakeLocation = createFakeLocation(latitude = 51.5007, longitude = -0.1246)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Tall Street",
            locality = "Fake Tall City",
            countryName = "Fake Tall Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-tall-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-tall-directory"),
            canvasToDrawArtWidth = fakeCanvasWidth,
            canvasToDrawArtHeight = fakeCanvasHeight
        )
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) {
            repository.fetchStaticMapImage(
                sketchArtType = any(),
                latitude = any(),
                longitude = any(),
                mapWidth = fakeCanvasWidth - fakeFrameSize,
                mapHeight = 1280,
                dir = any()
            )
        }
    }

    @Test
    fun `if the canvas height minus the frame is exactly 1280 should fetch the static map image with a height of 1280`() = runTest {
        // Given
        val fakeCanvasHeight = 1280 + fakeFrameSize
        val fakeLocation = createFakeLocation(latitude = 52.5163, longitude = 13.3777)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Boundary Street",
            locality = "Fake Boundary City",
            countryName = "Fake Boundary Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-boundary-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-boundary-directory"),
            canvasToDrawArtWidth = 720,
            canvasToDrawArtHeight = fakeCanvasHeight
        )
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) {
            repository.fetchStaticMapImage(any(), any(), any(), any(), mapHeight = 1280, dir = any())
        }
    }

    @Test
    fun `if get address service returns null should emit ImageDownloaded with the martp api address as title`() = runTest {
        // Given
        val fakeLatitude = -33.8568
        val fakeLongitude = 151.2153
        val fakeLocation = createFakeLocation(latitude = fakeLatitude, longitude = fakeLongitude)
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns null
        coEvery { repository.getReversedAddressFromApi(any(), any()) } returns RemoteReversedAddress(
            street = "Fake Api Street",
            city = "Fake Api City",
            country = "Fake Api Country"
        )
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-api-address-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-api-address-directory"),
            canvasToDrawArtWidth = 600,
            canvasToDrawArtHeight = 800
        )
        advanceUntilIdle()

        // Then
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            viewModel.state.value
        )
        assertEquals("Fake Api Street, Fake Api City, Fake Api Country", imageDownloadedState.title)
        coVerify(exactly = 1) { repository.getReversedAddressFromApi(fakeLatitude, fakeLongitude) }
    }

    @Test
    fun `if get address service returns an address should not call the martp api reverse geocoding`() = runTest {
        // Given
        val fakeLocation = createFakeLocation(latitude = 41.9028, longitude = 12.4964)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Android Street",
            locality = "Fake Android City",
            countryName = "Fake Android Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-android-address-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-android-address-directory"),
            canvasToDrawArtWidth = 600,
            canvasToDrawArtHeight = 800
        )
        advanceUntilIdle()

        // Then
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            viewModel.state.value
        )
        assertEquals("Fake Android Street, Fake Android City, Fake Android Country", imageDownloadedState.title)
        coVerify(exactly = 0) { repository.getReversedAddressFromApi(any(), any()) }
    }

    @Test
    fun `if the martp api address has no street and a blank city should emit ImageDownloaded with only the country as title`() = runTest {
        // Given
        val fakeLocation = createFakeLocation(latitude = 27.9881, longitude = 86.9250)
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns null
        coEvery { repository.getReversedAddressFromApi(any(), any()) } returns RemoteReversedAddress(
            street = null,
            city = " ",
            country = "Fake Mountain Country"
        )
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-mountain-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-mountain-directory"),
            canvasToDrawArtWidth = 600,
            canvasToDrawArtHeight = 800
        )
        advanceUntilIdle()

        // Then
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            viewModel.state.value
        )
        assertEquals("Fake Mountain Country", imageDownloadedState.title)
    }

    @Test
    fun `if both get address service and the martp api have no address should emit ImageDownloaded with an empty title`() = runTest {
        // Given
        val fakeLocation = createFakeLocation(latitude = -48.8767, longitude = -123.3933)
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns null
        coEvery { repository.getReversedAddressFromApi(any(), any()) } returns RemoteReversedAddress(
            street = null,
            city = null,
            country = null
        )
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-nowhere-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-nowhere-directory"),
            canvasToDrawArtWidth = 600,
            canvasToDrawArtHeight = 800
        )
        advanceUntilIdle()

        // Then
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            viewModel.state.value
        )
        assertEquals("", imageDownloadedState.title)
    }

    @Test
    fun `if the martp api throws a Failure when reverse geocoding should emit Error ui state after the downloading Loading message`() = runTest {
        // Given
        val fakeFailure = MartpApiErrorFailure("fake reverse geocode error")
        val fakeLocation = createFakeLocation(latitude = 11.5, longitude = 12.5)
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns null
        coEvery { repository.getReversedAddressFromApi(any(), any()) } throws fakeFailure
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-reverse-error-directory/image.png"
        val states = collectStates()

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-reverse-error-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // Then
        assertEquals(4, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertEquals(fakeAccessingLocationLoading, states[1])
        assertEquals(fakeDownloadingImageLoading, states[2])
        val errorState = assertInstanceOf(CreateNewMapArtUiState.Error::class.java, states[3])
        assertSame(fakeFailure, errorState.failure)
    }

    @Test
    fun `if repository throws a Failure when getting the selected color palette should emit Error ui state`() = runTest {
        // Given
        val fakeFailure = LocalStorageErrorFailure("fake selected palette error")
        val fakeLocation = createFakeLocation(latitude = 13.5, longitude = 14.5)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Paletteless Street",
            locality = "Fake Paletteless City",
            countryName = "Fake Paletteless Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.COLORFUL_MAP
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery { repository.getSelectedColorPalette() } throws fakeFailure
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-paletteless-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-paletteless-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // Then
        val errorState = assertInstanceOf(CreateNewMapArtUiState.Error::class.java, viewModel.state.value)
        assertSame(fakeFailure, errorState.failure)
    }

    @Test
    fun `if the address has no thoroughfare, locality or country name should emit ImageDownloaded with an empty title`() = runTest {
        // Given
        val fakeLocation = createFakeLocation(latitude = 0.5, longitude = -160.5)
        val fakeAddress = createFakeAddress(
            thoroughfare = null,
            locality = null,
            countryName = null
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-ocean-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-ocean-directory"),
            canvasToDrawArtWidth = 600,
            canvasToDrawArtHeight = 800
        )
        advanceUntilIdle()

        // Then
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            viewModel.state.value
        )
        assertEquals("", imageDownloadedState.title)
    }

    @Test
    fun `if the address has no thoroughfare should emit ImageDownloaded with the locality and the country name as title`() = runTest {
        // Given
        val fakeLocation = createFakeLocation(latitude = 64.1466, longitude = -21.9426)
        val fakeAddress = createFakeAddress(
            thoroughfare = null,
            locality = "Fake Streetless City",
            countryName = "Fake Streetless Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-streetless-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-streetless-directory"),
            canvasToDrawArtWidth = 600,
            canvasToDrawArtHeight = 800
        )
        advanceUntilIdle()

        // Then
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            viewModel.state.value
        )
        assertEquals("Fake Streetless City, Fake Streetless Country", imageDownloadedState.title)
    }

    @Test
    fun `if the address has no country name should emit ImageDownloaded with the thoroughfare and the locality as title without a trailing separator`() = runTest {
        // Given
        val fakeLocation = createFakeLocation(latitude = 59.3293, longitude = 18.0686)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Countryless Street",
            locality = "Fake Countryless City",
            countryName = null
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-countryless-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-countryless-directory"),
            canvasToDrawArtWidth = 600,
            canvasToDrawArtHeight = 800
        )
        advanceUntilIdle()

        // Then
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            viewModel.state.value
        )
        assertEquals("Fake Countryless Street, Fake Countryless City", imageDownloadedState.title)
    }

    @Test
    fun `if the address has only a thoroughfare should emit ImageDownloaded with only the thoroughfare as title`() = runTest {
        // Given
        val fakeLocation = createFakeLocation(latitude = -33.8688, longitude = 151.2093)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Lonely Street",
            locality = null,
            countryName = null
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-lonely-street-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-lonely-street-directory"),
            canvasToDrawArtWidth = 600,
            canvasToDrawArtHeight = 800
        )
        advanceUntilIdle()

        // Then
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            viewModel.state.value
        )
        assertEquals("Fake Lonely Street", imageDownloadedState.title)
    }

    @Test
    fun `if the address has a blank locality should emit ImageDownloaded without an empty part in the title`() = runTest {
        // Given
        val fakeLocation = createFakeLocation(latitude = 40.4168, longitude = -3.7038)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Blank City Street",
            locality = " ",
            countryName = "Fake Blank City Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-blank-city-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-blank-city-directory"),
            canvasToDrawArtWidth = 600,
            canvasToDrawArtHeight = 800
        )
        advanceUntilIdle()

        // Then
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            viewModel.state.value
        )
        assertEquals("Fake Blank City Street, Fake Blank City Country", imageDownloadedState.title)
    }

    @Test
    fun `if repository throws a Failure when getting the art style should emit Error ui state without downloading the image`() = runTest {
        // Given
        val fakeFailure = LocalStorageErrorFailure("fake art style error")
        val fakeLocation = createFakeLocation(latitude = 1.5, longitude = 2.5)
        coEvery { repository.getArtStylePreference() } throws fakeFailure
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        val states = collectStates()

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-art-style-error-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // Then
        assertEquals(3, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertEquals(fakeAccessingLocationLoading, states[1])
        val errorState = assertInstanceOf(CreateNewMapArtUiState.Error::class.java, states[2])
        assertSame(fakeFailure, errorState.failure)
        coVerify(exactly = 0) { getAddressService.getAddress(any(), any()) }
        coVerify(exactly = 0) {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `if location service throws a Failure should emit Error ui state without downloading the image`() = runTest {
        // Given
        val fakeFailure = LocationPermissionNotGrantedFailure("fake location error")
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } throws fakeFailure
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        val states = collectStates()

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-location-error-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // Then
        assertEquals(3, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertEquals(fakeAccessingLocationLoading, states[1])
        val errorState = assertInstanceOf(CreateNewMapArtUiState.Error::class.java, states[2])
        assertSame(fakeFailure, errorState.failure)
        coVerify(exactly = 0) { getAddressService.getAddress(any(), any()) }
        coVerify(exactly = 0) {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `if get address service throws a Failure should emit Error ui state after the downloading Loading message`() = runTest {
        // Given
        val fakeFailure = NoInternetConnectionFailure("fake address no internet error")
        val fakeLocation = createFakeLocation(latitude = 3.5, longitude = 4.5)
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } throws fakeFailure
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-address-error-directory/image.png"
        val states = collectStates()

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-address-error-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // Then
        assertEquals(4, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertEquals(fakeAccessingLocationLoading, states[1])
        assertEquals(fakeDownloadingImageLoading, states[2])
        val errorState = assertInstanceOf(CreateNewMapArtUiState.Error::class.java, states[3])
        assertSame(fakeFailure, errorState.failure)
    }

    @Test
    fun `if repository throws a Failure when fetching the static map image should emit Error ui state after the downloading Loading message`() = runTest {
        // Given
        val fakeFailure = MapboxApiErrorFailure("fake mapbox error")
        val fakeLocation = createFakeLocation(latitude = 5.5, longitude = 6.5)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Offline Street",
            locality = "Fake Offline City",
            countryName = "Fake Offline Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } throws fakeFailure
        val states = collectStates()

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-offline-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // Then
        assertEquals(4, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertEquals(fakeAccessingLocationLoading, states[1])
        assertEquals(fakeDownloadingImageLoading, states[2])
        val errorState = assertInstanceOf(CreateNewMapArtUiState.Error::class.java, states[3])
        assertSame(fakeFailure, errorState.failure)
    }

    @Test
    fun `if startToGenerateMapArt is called again after an Error should emit ImageDownloaded ui state`() = runTest {
        // Given
        val fakeFailure = LocationPermissionNotGrantedFailure("fake first location error")
        val fakeStaticImagePath = "fake-retry-after-error-directory/image.png"
        val fakeLocation = createFakeLocation(latitude = 7.5, longitude = 8.5)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Retry Street",
            locality = "Fake Retry City",
            countryName = "Fake Retry Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } throws fakeFailure andThen fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns fakeStaticImagePath
        val states = collectStates()
        viewModel.startToGenerateMapArt(
            directory = File("fake-retry-after-error-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-retry-after-error-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // Then
        assertEquals(6, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertEquals(fakeAccessingLocationLoading, states[1])
        assertInstanceOf(CreateNewMapArtUiState.Error::class.java, states[2])
        assertEquals(fakeAccessingLocationLoading, states[3])
        assertEquals(fakeDownloadingImageLoading, states[4])
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            states[5]
        )
        assertEquals(fakeStaticImagePath, imageDownloadedState.staticMapImagePath)
        coVerify(exactly = 2) { locationService.getCurrentLocation() }
    }

    @Test
    fun `if startToGenerateMapArt is called again while the first call is running should cancel the first call`() = runTest {
        // Given
        val fakeFirstDirectory = File("fake-first-directory")
        val fakeLastDirectory = File("fake-last-directory")
        val fakeLastStaticImagePath = "fake-last-directory/image.png"
        val fakeLocation = createFakeLocation(latitude = 9.5, longitude = 10.5)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Cancel Street",
            locality = "Fake Cancel City",
            countryName = "Fake Cancel Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } coAnswers {
            delay(1_000)
            fakeLocation
        }
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns fakeLastStaticImagePath
        val states = collectStates()

        // When
        viewModel.startToGenerateMapArt(
            directory = fakeFirstDirectory,
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceTimeBy(500)
        viewModel.startToGenerateMapArt(
            directory = fakeLastDirectory,
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { locationService.getCurrentLocation() }
        coVerify(exactly = 1) { getAddressService.getAddress(any(), any()) }
        coVerify(exactly = 0) {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), fakeFirstDirectory)
        }
        coVerify(exactly = 1) {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), fakeLastDirectory)
        }
        // The second Loading(accessing_your_location) is equal to the first one, so the StateFlow does not emit it again
        assertEquals(4, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertEquals(fakeAccessingLocationLoading, states[1])
        assertEquals(fakeDownloadingImageLoading, states[2])
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            states[3]
        )
        assertEquals(fakeLastStaticImagePath, imageDownloadedState.staticMapImagePath)
    }

    // endregion

    // region saveArt

    @Test
    fun `if repository saves the art should send ArtCreatedSuccessfully event and keep the ActionButtonLoading ui state`() = runTest {
        // Given
        val fakeUserInfo = createFakeUserInfo(id = "fake-success-user-id")
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        coEvery { repository.saveMapArtEntity(any()) } just runs
        every { repository.uploadMapArt(any(), any()) } just runs
        val states = collectStates()
        val events = collectEvents()

        // When
        viewModel.saveArt(
            title = "Fake Art Title",
            description = "Fake art description",
            directory = File("fake-save-directory"),
            mapLayersColors = null,
            updateUserInfo = fakeUserInfo,
            saveImage = {}
        )
        advanceUntilIdle()

        // Then
        assertEquals(listOf(CreateNewMapArtEvent.ArtCreatedSuccessfully), events)
        assertEquals(
            listOf(CreateNewMapArtUiState.Idle, CreateNewMapArtUiState.ActionButtonLoading),
            states
        )
        coVerify(exactly = 1) { repository.saveMapArtEntity(any()) }
        verify(exactly = 1) { repository.uploadMapArt(any(), fakeUserInfo) }
    }

    @Test
    fun `if repository saves the art should save the image, the entity and upload the same entity at the art image path`() = runTest {
        // Given
        val fakeDirectory = File("fake-image-path-directory")
        val fakeUserInfo = createFakeUserInfo(id = "fake-image-path-user-id")
        val fakeSavedImagePaths = mutableListOf<String>()
        val savedMapArtSlot = slot<MapArtEntity>()
        val uploadedMapArtSlot = slot<MapArtEntity>()
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        coEvery { repository.saveMapArtEntity(capture(savedMapArtSlot)) } just runs
        every { repository.uploadMapArt(capture(uploadedMapArtSlot), any()) } just runs

        // When
        viewModel.saveArt(
            title = "Fake Image Path Art Title",
            description = null,
            directory = fakeDirectory,
            mapLayersColors = null,
            updateUserInfo = fakeUserInfo,
            saveImage = { path -> fakeSavedImagePaths.add(path) }
        )
        advanceUntilIdle()

        // Then
        val expectedImagePath = fakeDirectory.path + "/map-arts/" + savedMapArtSlot.captured.id + ".png"
        assertEquals(listOf(expectedImagePath), fakeSavedImagePaths)
        assertEquals(expectedImagePath, savedMapArtSlot.captured.imagePathLocation)
        assertEquals(savedMapArtSlot.captured, uploadedMapArtSlot.captured)
    }

    @Test
    fun `if repository saves the art should save the user info before saving and uploading the art`() = runTest {
        // Given
        val fakeUserId = "fake-ordered-user-id"
        val fakeUserInfo = createFakeUserInfo(
            id = fakeUserId,
            username = "Fake Ordered User",
            email = "fake.ordered.user@example.com"
        )
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        coEvery { repository.saveMapArtEntity(any()) } just runs
        every { repository.uploadMapArt(any(), any()) } just runs

        // When
        viewModel.saveArt(
            title = "Fake Ordered Art Title",
            description = null,
            directory = File("fake-ordered-directory"),
            mapLayersColors = null,
            updateUserInfo = fakeUserInfo,
            saveImage = {}
        )
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.setUserName("Fake Ordered User", fakeUserId) }
        coVerify(exactly = 1) { repository.setUserEmail("fake.ordered.user@example.com", fakeUserId) }
        coVerifyOrder {
            repository.setUserName(any(), any())
            repository.setUserEmail(any(), any())
            repository.saveMapArtEntity(any())
            repository.uploadMapArt(any(), any())
        }
    }

    @Test
    fun `if the user info has no username and no email should save them as empty texts`() = runTest {
        // Given
        val fakeUserId = "fake-anonymous-user-id"
        val fakeUserInfo = createFakeUserInfo(id = fakeUserId, username = null, email = null)
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        coEvery { repository.saveMapArtEntity(any()) } just runs
        every { repository.uploadMapArt(any(), any()) } just runs

        // When
        viewModel.saveArt(
            title = "Fake Anonymous Art Title",
            description = null,
            directory = File("fake-anonymous-directory"),
            mapLayersColors = null,
            updateUserInfo = fakeUserInfo,
            saveImage = {}
        )
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.setUserName("", fakeUserId) }
        coVerify(exactly = 1) { repository.setUserEmail("", fakeUserId) }
        verify(exactly = 1) { repository.uploadMapArt(any(), fakeUserInfo) }
    }

    @Test
    fun `if a map art was generated before should save the entity with the art location`() = runTest {
        // Given
        val fakeTitle = "Fake Located Art Title"
        val fakeDescription = "Fake located art description"
        val fakeLatitude = -22.9519
        val fakeLongitude = -43.2105
        val fakeLocation = createFakeLocation(latitude = fakeLatitude, longitude = fakeLongitude)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Located Street",
            locality = "Fake Located City",
            countryName = "Fake Located Country"
        )
        val mapArtSlot = slot<MapArtEntity>()
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-located-directory/image.png"
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        coEvery { repository.saveMapArtEntity(capture(mapArtSlot)) } just runs
        every { repository.uploadMapArt(any(), any()) } just runs
        viewModel.startToGenerateMapArt(
            directory = File("fake-located-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()
        val fakeTimeBeforeSaving = Calendar.getInstance().timeInMillis

        // When
        viewModel.saveArt(
            title = fakeTitle,
            description = fakeDescription,
            directory = File("fake-located-directory"),
            mapLayersColors = null,
            updateUserInfo = createFakeUserInfo(),
            saveImage = {}
        )
        advanceUntilIdle()

        // Then
        val fakeTimeAfterSaving = Calendar.getInstance().timeInMillis
        assertEquals(fakeTitle, mapArtSlot.captured.title)
        assertEquals(fakeDescription, mapArtSlot.captured.description)
        assertEquals(fakeLatitude.toFloat(), mapArtSlot.captured.latitude)
        assertEquals(fakeLongitude.toFloat(), mapArtSlot.captured.longitude)
        assertTrue(mapArtSlot.captured.id.isNotBlank())
        assertTrue(mapArtSlot.captured.dateInMillis in fakeTimeBeforeSaving..fakeTimeAfterSaving)
    }

    @Test
    fun `if no map art was generated before should save the entity with zero latitude and longitude`() = runTest {
        // Given
        val mapArtSlot = slot<MapArtEntity>()
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        coEvery { repository.saveMapArtEntity(capture(mapArtSlot)) } just runs
        every { repository.uploadMapArt(any(), any()) } just runs

        // When
        viewModel.saveArt(
            title = "Fake Unlocated Art Title",
            description = "Fake unlocated art description",
            directory = File("fake-unlocated-directory"),
            mapLayersColors = null,
            updateUserInfo = createFakeUserInfo(),
            saveImage = {}
        )
        advanceUntilIdle()

        // Then
        assertEquals(0F, mapArtSlot.captured.latitude)
        assertEquals(0F, mapArtSlot.captured.longitude)
    }

    @Test
    fun `if description is null should save the entity with a null description`() = runTest {
        // Given
        val fakeTitle = "Fake No Description Art Title"
        val mapArtSlot = slot<MapArtEntity>()
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        coEvery { repository.saveMapArtEntity(capture(mapArtSlot)) } just runs
        every { repository.uploadMapArt(any(), any()) } just runs

        // When
        viewModel.saveArt(
            title = fakeTitle,
            description = null,
            directory = File("fake-no-description-directory"),
            mapLayersColors = null,
            updateUserInfo = createFakeUserInfo(),
            saveImage = {}
        )
        advanceUntilIdle()

        // Then
        assertEquals(fakeTitle, mapArtSlot.captured.title)
        assertNull(mapArtSlot.captured.description)
    }

    @Test
    fun `if saveArt is called twice should save two entities with different ids`() = runTest {
        // Given
        val fakeDirectory = File("fake-twice-directory")
        val fakeSavedMapArts = mutableListOf<MapArtEntity>()
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        coEvery { repository.saveMapArtEntity(capture(fakeSavedMapArts)) } just runs
        every { repository.uploadMapArt(any(), any()) } just runs
        val events = collectEvents()

        // When
        viewModel.saveArt(
            title = "Fake First Art Title",
            description = null,
            directory = fakeDirectory,
            mapLayersColors = null,
            updateUserInfo = createFakeUserInfo(),
            saveImage = {}
        )
        viewModel.saveArt(
            title = "Fake Second Art Title",
            description = null,
            directory = fakeDirectory,
            mapLayersColors = null,
            updateUserInfo = createFakeUserInfo(),
            saveImage = {}
        )
        advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { repository.saveMapArtEntity(any()) }
        verify(exactly = 2) { repository.uploadMapArt(any(), any()) }
        assertEquals(2, fakeSavedMapArts.size)
        assertNotEquals(fakeSavedMapArts[0].id, fakeSavedMapArts[1].id)
        assertEquals(
            listOf(CreateNewMapArtEvent.ArtCreatedSuccessfully, CreateNewMapArtEvent.ArtCreatedSuccessfully),
            events
        )
    }

    @Test
    fun `if repository throws a Failure when saving the art should emit Error ui state without uploading it or sending an event`() = runTest {
        // Given
        val fakeFailure = LocalStorageErrorFailure("fake save error")
        val fakeSavedImagePaths = mutableListOf<String>()
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        coEvery { repository.saveMapArtEntity(any()) } throws fakeFailure
        val states = collectStates()
        val events = collectEvents()

        // When
        viewModel.saveArt(
            title = "Fake Failing Art Title",
            description = "Fake failing art description",
            directory = File("fake-failing-directory"),
            mapLayersColors = null,
            updateUserInfo = createFakeUserInfo(),
            saveImage = { path -> fakeSavedImagePaths.add(path) }
        )
        advanceUntilIdle()

        // Then
        assertEquals(3, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertSame(CreateNewMapArtUiState.ActionButtonLoading, states[1])
        val errorState = assertInstanceOf(CreateNewMapArtUiState.Error::class.java, states[2])
        assertSame(fakeFailure, errorState.failure)
        assertTrue(events.isEmpty())
        assertEquals(1, fakeSavedImagePaths.size)
        verify(exactly = 0) { repository.uploadMapArt(any(), any()) }
    }

    @Test
    fun `if saving the image throws an exception that is not a Failure should emit Error ui state with UnknownErrorFailure`() = runTest {
        // Given
        val fakeErrorMessage = "fake save image error"
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        val states = collectStates()
        val events = collectEvents()

        // When
        viewModel.saveArt(
            title = "Fake Unsaved Image Art Title",
            description = null,
            directory = File("fake-unsaved-image-directory"),
            mapLayersColors = null,
            updateUserInfo = createFakeUserInfo(),
            saveImage = { throw IllegalStateException(fakeErrorMessage) }
        )
        advanceUntilIdle()

        // Then
        assertEquals(3, states.size)
        assertSame(CreateNewMapArtUiState.ActionButtonLoading, states[1])
        val errorState = assertInstanceOf(CreateNewMapArtUiState.Error::class.java, states[2])
        val unknownErrorFailure = assertInstanceOf(UnknownErrorFailure::class.java, errorState.failure)
        assertEquals(fakeErrorMessage, unknownErrorFailure.message)
        assertTrue(events.isEmpty())
        coVerify(exactly = 0) { repository.saveMapArtEntity(any()) }
        verify(exactly = 0) { repository.uploadMapArt(any(), any()) }
    }

    @Test
    fun `if saving the art is cancelled should not emit Error ui state`() = runTest {
        // Given
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        val states = collectStates()
        val events = collectEvents()

        // When
        viewModel.saveArt(
            title = "Fake Cancelled Art Title",
            description = null,
            directory = File("fake-cancelled-directory"),
            mapLayersColors = null,
            updateUserInfo = createFakeUserInfo(),
            saveImage = { throw CancellationException("fake save cancellation") }
        )
        advanceUntilIdle()

        // Then
        assertEquals(2, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertSame(CreateNewMapArtUiState.ActionButtonLoading, states[1])
        assertTrue(events.isEmpty())
        coVerify(exactly = 0) { repository.saveMapArtEntity(any()) }
        verify(exactly = 0) { repository.uploadMapArt(any(), any()) }
    }

    @Test
    fun `if a colorful map art was generated before should save the entity with its art style, color palette name and layer colors`() = runTest {
        // Given
        val fakeColorPalette = ColorPaletteEntity(
            name = "Fake Saved Palette",
            colors = listOf(0xFF111111.toInt(), 0xFF222222.toInt())
        )
        val fakeMapLayersColors = MapLayersColors(
            roadsColor = 0x111111,
            buildingStrokeColor = 0x222222,
            buildingColor = 0x333333,
            roadsPathColor = 0x444444,
            waterColor = 0x555555,
            landOthersColor = 0x666666
        )
        val mapArtSlot = slot<MapArtEntity>()
        coEvery { repository.getArtStylePreference() } returns SketchArtType.COLORFUL_MAP
        coEvery { repository.getSelectedColorPalette() } returns fakeColorPalette
        coEvery { locationService.getCurrentLocation() } returns createFakeLocation(latitude = 48.8584, longitude = 2.2945)
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns createFakeAddress(
            thoroughfare = "Fake Colorful Save Street",
            locality = null,
            countryName = null
        )
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-colorful-directory/image.png"
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        coEvery { repository.saveMapArtEntity(capture(mapArtSlot)) } just runs
        every { repository.uploadMapArt(any(), any()) } just runs
        viewModel.startToGenerateMapArt(
            directory = File("fake-colorful-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // When
        viewModel.saveArt(
            title = "Fake Colorful Art Title",
            description = null,
            directory = File("fake-colorful-directory"),
            mapLayersColors = fakeMapLayersColors,
            updateUserInfo = createFakeUserInfo(),
            saveImage = {}
        )
        advanceUntilIdle()

        // Then
        assertEquals(SketchArtType.COLORFUL_MAP, mapArtSlot.captured.sketchArtType)
        assertEquals("Fake Saved Palette", mapArtSlot.captured.colorPalette)
        assertEquals(fakeMapLayersColors, mapArtSlot.captured.colorsLayers)
    }

    @Test
    fun `if a default map art was generated before should save the entity with its art style and no color palette or layer colors`() = runTest {
        // Given
        val mapArtSlot = slot<MapArtEntity>()
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns createFakeLocation(latitude = 35.6586, longitude = 139.7454)
        coEvery { repository.fetchCurrentUserInfo() } returns createFakeUserInfo()
        coEvery { getAddressService.getAddress(any(), any()) } returns createFakeAddress(
            thoroughfare = "Fake Default Save Street",
            locality = null,
            countryName = null
        )
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-default-directory/image.png"
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        coEvery { repository.saveMapArtEntity(capture(mapArtSlot)) } just runs
        every { repository.uploadMapArt(any(), any()) } just runs
        viewModel.startToGenerateMapArt(
            directory = File("fake-default-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // When
        viewModel.saveArt(
            title = "Fake Default Art Title",
            description = null,
            directory = File("fake-default-directory"),
            mapLayersColors = null,
            updateUserInfo = createFakeUserInfo(),
            saveImage = {}
        )
        advanceUntilIdle()

        // Then
        assertEquals(SketchArtType.DEFAULT, mapArtSlot.captured.sketchArtType)
        assertNull(mapArtSlot.captured.colorPalette)
        assertNull(mapArtSlot.captured.colorsLayers)
    }

    @Test
    fun `if no map art was generated before should save the entity with no art style, color palette or layer colors`() = runTest {
        // Given
        val mapArtSlot = slot<MapArtEntity>()
        coEvery { repository.setUserName(any(), any()) } just runs
        coEvery { repository.setUserEmail(any(), any()) } just runs
        coEvery { repository.saveMapArtEntity(capture(mapArtSlot)) } just runs
        every { repository.uploadMapArt(any(), any()) } just runs

        // When
        viewModel.saveArt(
            title = "Fake Ungenerated Art Title",
            description = null,
            directory = File("fake-ungenerated-directory"),
            mapLayersColors = null,
            updateUserInfo = createFakeUserInfo(),
            saveImage = {}
        )
        advanceUntilIdle()

        // Then
        assertNull(mapArtSlot.captured.sketchArtType)
        assertNull(mapArtSlot.captured.colorPalette)
        assertNull(mapArtSlot.captured.colorsLayers)
    }

    // endregion
}
