package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.graphics.Bitmap
import android.location.Address
import android.location.Location
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
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
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import manoellribeiro.dev.martp.core.models.failures.LocationPermissionNotGrantedFailure
import manoellribeiro.dev.martp.core.models.failures.MapboxApiErrorFailure
import manoellribeiro.dev.martp.core.models.failures.NoInternetConnectionFailure
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
        val fakeLocation = createFakeLocation(latitude = fakeLatitude, longitude = fakeLongitude)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Street",
            locality = "Fake City",
            countryName = "Fake Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.POINTILLISM
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
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
        coVerify(exactly = 0) { repository.getColorPalettePreference() }
    }

    @Test
    fun `if the art style is COLORFUL_MAP should emit ImageDownloaded with the saved color palette`() = runTest {
        // Given
        val fakeColorPaletteName = "Fake Colorful Palette"
        val fakeLocation = createFakeLocation(latitude = 35.6586, longitude = 139.7454)
        val fakeAddress = createFakeAddress(
            thoroughfare = "Fake Colorful Street",
            locality = "Fake Colorful City",
            countryName = "Fake Colorful Country"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.COLORFUL_MAP
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery { repository.getColorPalettePreference() } returns fakeColorPaletteName
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
        assertEquals(fakeColorPaletteName, imageDownloadedState.colorPalette)
        assertEquals(SketchArtType.COLORFUL_MAP, imageDownloadedState.sketchArtType)
        coVerify(exactly = 1) { repository.getColorPalettePreference() }
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
    fun `if get address service returns null should emit ImageDownloaded with an empty title`() = runTest {
        // Given
        val fakeLocation = createFakeLocation(latitude = -33.8568, longitude = 151.2153)
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { getAddressService.getAddress(any(), any()) } returns null
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-no-address-directory/image.png"

        // When
        viewModel.startToGenerateMapArt(
            directory = File("fake-no-address-directory"),
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

    // region saveArtToLocalDatabase

    @Test
    fun `if repository saves the art should send ArtCreatedSuccessfully event with the art image path`() = runTest {
        // Given
        val fakeDirectory = File("fake-save-directory")
        val fakeBitmap = mockk<Bitmap>()
        val mapArtSlot = slot<MapArtEntity>()
        coEvery { repository.saveMapArtEntity(capture(mapArtSlot)) } just runs
        val states = collectStates()
        val events = collectEvents()

        // When
        viewModel.saveArtToLocalDatabase(
            title = "Fake Art Title",
            description = "Fake art description",
            newArtBitMap = fakeBitmap,
            directory = fakeDirectory,
            mapLayersColors = null
        )
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.saveMapArtEntity(any()) }
        val expectedImagePath = fakeDirectory.path + "/map-arts/" + mapArtSlot.captured.id + ".png"
        assertEquals(expectedImagePath, mapArtSlot.captured.imagePathLocation)
        assertEquals(1, events.size)
        val artCreatedEvent = assertInstanceOf(
            CreateNewMapArtEvent.ArtCreatedSuccessfully::class.java,
            events[0]
        )
        assertEquals(expectedImagePath, artCreatedEvent.pathToStoreArtImage)
        assertEquals(
            listOf(CreateNewMapArtUiState.Idle, CreateNewMapArtUiState.ActionButtonLoading),
            states
        )
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
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-located-directory/image.png"
        coEvery { repository.saveMapArtEntity(capture(mapArtSlot)) } just runs
        viewModel.startToGenerateMapArt(
            directory = File("fake-located-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()
        val fakeTimeBeforeSaving = Calendar.getInstance().timeInMillis

        // When
        viewModel.saveArtToLocalDatabase(
            title = fakeTitle,
            description = fakeDescription,
            newArtBitMap = mockk<Bitmap>(),
            directory = File("fake-located-directory"),
            mapLayersColors = null
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
        coEvery { repository.saveMapArtEntity(capture(mapArtSlot)) } just runs

        // When
        viewModel.saveArtToLocalDatabase(
            title = "Fake Unlocated Art Title",
            description = "Fake unlocated art description",
            newArtBitMap = mockk<Bitmap>(),
            directory = File("fake-unlocated-directory"),
            mapLayersColors = null
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
        coEvery { repository.saveMapArtEntity(capture(mapArtSlot)) } just runs

        // When
        viewModel.saveArtToLocalDatabase(
            title = fakeTitle,
            description = null,
            newArtBitMap = mockk<Bitmap>(),
            directory = File("fake-no-description-directory"),
            mapLayersColors = null
        )
        advanceUntilIdle()

        // Then
        assertEquals(fakeTitle, mapArtSlot.captured.title)
        assertNull(mapArtSlot.captured.description)
    }

    @Test
    fun `if saveArtToLocalDatabase is called twice should save two entities with different ids`() = runTest {
        // Given
        val fakeDirectory = File("fake-twice-directory")
        val fakeSavedMapArts = mutableListOf<MapArtEntity>()
        coEvery { repository.saveMapArtEntity(capture(fakeSavedMapArts)) } just runs
        val events = collectEvents()

        // When
        viewModel.saveArtToLocalDatabase(
            title = "Fake First Art Title",
            description = null,
            newArtBitMap = mockk<Bitmap>(),
            directory = fakeDirectory,
            mapLayersColors = null
        )
        viewModel.saveArtToLocalDatabase(
            title = "Fake Second Art Title",
            description = null,
            newArtBitMap = mockk<Bitmap>(),
            directory = fakeDirectory,
            mapLayersColors = null
        )
        advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { repository.saveMapArtEntity(any()) }
        assertEquals(2, fakeSavedMapArts.size)
        assertNotEquals(fakeSavedMapArts[0].id, fakeSavedMapArts[1].id)
        assertEquals(2, events.size)
    }

    @Test
    fun `if repository throws a Failure when saving the art should emit Error ui state and send no event`() = runTest {
        // Given
        val fakeFailure = LocalStorageErrorFailure("fake save error")
        coEvery { repository.saveMapArtEntity(any()) } throws fakeFailure
        val states = collectStates()
        val events = collectEvents()

        // When
        viewModel.saveArtToLocalDatabase(
            title = "Fake Failing Art Title",
            description = "Fake failing art description",
            newArtBitMap = mockk<Bitmap>(),
            directory = File("fake-failing-directory"),
            mapLayersColors = null
        )
        advanceUntilIdle()

        // Then
        assertEquals(3, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertSame(CreateNewMapArtUiState.ActionButtonLoading, states[1])
        val errorState = assertInstanceOf(CreateNewMapArtUiState.Error::class.java, states[2])
        assertSame(fakeFailure, errorState.failure)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `if a colorful map art was generated before should save the entity with its art style, color palette and layer colors`() = runTest {
        // Given
        val fakeColorPaletteName = "Fake Saved Palette"
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
        coEvery { repository.getColorPalettePreference() } returns fakeColorPaletteName
        coEvery { locationService.getCurrentLocation() } returns createFakeLocation(latitude = 48.8584, longitude = 2.2945)
        coEvery { getAddressService.getAddress(any(), any()) } returns null
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-colorful-directory/image.png"
        coEvery { repository.saveMapArtEntity(capture(mapArtSlot)) } just runs
        viewModel.startToGenerateMapArt(
            directory = File("fake-colorful-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // When
        viewModel.saveArtToLocalDatabase(
            title = "Fake Colorful Art Title",
            description = null,
            newArtBitMap = mockk<Bitmap>(),
            directory = File("fake-colorful-directory"),
            mapLayersColors = fakeMapLayersColors
        )
        advanceUntilIdle()

        // Then
        assertEquals(SketchArtType.COLORFUL_MAP, mapArtSlot.captured.sketchArtType)
        assertEquals(fakeColorPaletteName, mapArtSlot.captured.colorPalette)
        assertEquals(fakeMapLayersColors, mapArtSlot.captured.colorsLayers)
    }

    @Test
    fun `if a default map art was generated before should save the entity with its art style and no color palette or layer colors`() = runTest {
        // Given
        val mapArtSlot = slot<MapArtEntity>()
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns createFakeLocation(latitude = 35.6586, longitude = 139.7454)
        coEvery { getAddressService.getAddress(any(), any()) } returns null
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-default-directory/image.png"
        coEvery { repository.saveMapArtEntity(capture(mapArtSlot)) } just runs
        viewModel.startToGenerateMapArt(
            directory = File("fake-default-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // When
        viewModel.saveArtToLocalDatabase(
            title = "Fake Default Art Title",
            description = null,
            newArtBitMap = mockk<Bitmap>(),
            directory = File("fake-default-directory"),
            mapLayersColors = null
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
        coEvery { repository.saveMapArtEntity(capture(mapArtSlot)) } just runs

        // When
        viewModel.saveArtToLocalDatabase(
            title = "Fake Ungenerated Art Title",
            description = null,
            newArtBitMap = mockk<Bitmap>(),
            directory = File("fake-ungenerated-directory"),
            mapLayersColors = null
        )
        advanceUntilIdle()

        // Then
        assertNull(mapArtSlot.captured.sketchArtType)
        assertNull(mapArtSlot.captured.colorPalette)
        assertNull(mapArtSlot.captured.colorsLayers)
    }

    // endregion
}
