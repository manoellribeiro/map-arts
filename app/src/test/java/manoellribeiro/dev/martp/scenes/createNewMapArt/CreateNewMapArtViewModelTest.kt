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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.GeneratingAIContentFailure
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import manoellribeiro.dev.martp.core.models.failures.LocationPermissionNotGrantedFailure
import manoellribeiro.dev.martp.core.models.failures.NoInternetConnectionFailure
import manoellribeiro.dev.martp.core.models.failures.SketchArtType
import manoellribeiro.dev.martp.core.services.GenerateAIContentService
import manoellribeiro.dev.martp.core.services.GetAddressService
import manoellribeiro.dev.martp.core.services.LocationService
import manoellribeiro.dev.martp.core.utils.PromptGenerator
import manoellribeiro.dev.martp.infra.MainDispatcherExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class CreateNewMapArtViewModelTest {

    private lateinit var repository: MartpRepository
    private lateinit var locationService: LocationService
    private lateinit var getAddressService: GetAddressService
    private lateinit var generateAIContentService: GenerateAIContentService
    private lateinit var promptGenerator: PromptGenerator
    private lateinit var viewModel: CreateNewMapArtViewModel

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
        locality: String?,
        countryName: String?,
        thoroughfare: String?
    ): Address {
        val fakeAddress = mockk<Address>()
        every { fakeAddress.locality } returns locality
        every { fakeAddress.countryName } returns countryName
        every { fakeAddress.thoroughfare } returns thoroughfare
        return fakeAddress
    }

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
    fun `if all dependencies succeed should emit Loading then ImageDownloaded ui state`() = runTest {
        // Given
        val fakeDirectory = File("fake-directory")
        val fakeStaticImagePath = "fake-directory/image.png"
        val fakeLocation = createFakeLocation(latitude = -12.9714, longitude = -38.5014)
        val fakeAddress = createFakeAddress(
            locality = "Fake City",
            countryName = "Fake Country",
            thoroughfare = "Fake Street"
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
            directory = fakeDirectory,
            canvasToDrawArtWidth = 1000,
            canvasToDrawArtHeight = 2000
        )
        advanceUntilIdle()

        // Then
        assertEquals(3, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertSame(CreateNewMapArtUiState.Loading, states[1])
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            states[2]
        )
        assertEquals(fakeStaticImagePath, imageDownloadedState.staticMapImagePath)
        assertEquals("Fake City, Fake Country, Fake Street", imageDownloadedState.title)
        assertEquals(SketchArtType.POINTILLISM, imageDownloadedState.sketchArtType)
    }

    @Test
    fun `if all dependencies succeed should fetch the static map image with the canvas size minus the frame`() = runTest {
        // Given
        val fakeDirectory = File("fake-frame-directory")
        val fakeLatitude = 48.8584
        val fakeLongitude = 2.2945
        val fakeCanvasWidth = 1000
        val fakeCanvasHeight = 2000
        // 2 * MartpSketch.frameThickness (30) + 2 * MartpSketch.framePadding (30)
        val fakeFrameSize = 120
        val fakeLocation = createFakeLocation(latitude = fakeLatitude, longitude = fakeLongitude)
        val fakeAddress = createFakeAddress(
            locality = "Fake Frame City",
            countryName = "Fake Frame Country",
            thoroughfare = "Fake Frame Street"
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
    fun `if repository throws a Failure when getting the art style should emit Error ui state`() = runTest {
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
        assertSame(CreateNewMapArtUiState.Loading, states[1])
        val errorState = assertInstanceOf(CreateNewMapArtUiState.Error::class.java, states[2])
        assertSame(fakeFailure, errorState.failure)
        coVerify(exactly = 0) { getAddressService.getAddress(any(), any()) }
        coVerify(exactly = 0) {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `if location service throws a Failure should emit Error ui state`() = runTest {
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
        assertSame(CreateNewMapArtUiState.Loading, states[1])
        val errorState = assertInstanceOf(CreateNewMapArtUiState.Error::class.java, states[2])
        assertSame(fakeFailure, errorState.failure)
        coVerify(exactly = 0) { getAddressService.getAddress(any(), any()) }
        coVerify(exactly = 0) {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `if repository throws a Failure when fetching the static map image should emit Error ui state`() = runTest {
        // Given
        val fakeFailure = NoInternetConnectionFailure("fake no internet error")
        val fakeLocation = createFakeLocation(latitude = 3.5, longitude = 4.5)
        val fakeAddress = createFakeAddress(
            locality = "Fake Offline City",
            countryName = "Fake Offline Country",
            thoroughfare = "Fake Offline Street"
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
        assertEquals(3, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertSame(CreateNewMapArtUiState.Loading, states[1])
        val errorState = assertInstanceOf(CreateNewMapArtUiState.Error::class.java, states[2])
        assertSame(fakeFailure, errorState.failure)
    }

    @Test
    fun `if startToGenerateMapArt is called again while the first call is running should cancel the first call`() = runTest {
        // Given
        val fakeFirstDirectory = File("fake-first-directory")
        val fakeLastDirectory = File("fake-last-directory")
        val fakeLastStaticImagePath = "fake-last-directory/image.png"
        val fakeLocation = createFakeLocation(latitude = 5.5, longitude = 6.5)
        val fakeAddress = createFakeAddress(
            locality = "Fake Retry City",
            countryName = "Fake Retry Country",
            thoroughfare = "Fake Retry Street"
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
        coVerify(exactly = 0) {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), fakeFirstDirectory)
        }
        coVerify(exactly = 1) {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), fakeLastDirectory)
        }
        assertEquals(3, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertSame(CreateNewMapArtUiState.Loading, states[1])
        val imageDownloadedState = assertInstanceOf(
            CreateNewMapArtUiState.ImageDownloaded::class.java,
            states[2]
        )
        assertEquals(fakeLastStaticImagePath, imageDownloadedState.staticMapImagePath)
    }

    // endregion

    // region generateAiText

    @Test
    fun `if AI service returns a text should emit LoadingAIText then GeneratedAIText ui state`() = runTest {
        // Given
        val fakePrompt = "fake prompt"
        val fakeAiText = "Fake AI description of the place"
        every {
            promptGenerator.generateArtDescriptionPrompt(any(), any(), any(), any())
        } returns fakePrompt
        coEvery { generateAIContentService.generateTextContent(any()) } returns fakeAiText
        val states = collectStates()

        // When
        viewModel.generateAiText()
        advanceUntilIdle()

        // Then
        assertEquals(3, states.size)
        assertSame(CreateNewMapArtUiState.Idle, states[0])
        assertSame(CreateNewMapArtUiState.LoadingAIText, states[1])
        val generatedState = assertInstanceOf(
            CreateNewMapArtUiState.GeneratedAIText::class.java,
            states[2]
        )
        assertEquals(fakeAiText, generatedState.text)
        coVerify(exactly = 1) { generateAIContentService.generateTextContent(fakePrompt) }
    }

    @Test
    fun `if no map art was generated before should generate the prompt with empty address texts`() = runTest {
        // Given
        every {
            promptGenerator.generateArtDescriptionPrompt(any(), any(), any(), any())
        } returns "fake empty address prompt"
        coEvery { generateAIContentService.generateTextContent(any()) } returns "Fake AI text"

        // When
        viewModel.generateAiText()
        advanceUntilIdle()

        // Then
        verify(exactly = 1) {
            promptGenerator.generateArtDescriptionPrompt(
                countryName = "",
                cityName = "",
                streetName = "",
                languageToReturn = Locale.getDefault().displayName
            )
        }
    }

    @Test
    fun `if a map art was generated before should generate the prompt with the art address`() = runTest {
        // Given
        val fakeLocation = createFakeLocation(latitude = 7.5, longitude = 8.5)
        val fakeAddress = createFakeAddress(
            locality = "Fake Prompt City",
            countryName = "Fake Prompt Country",
            thoroughfare = "Fake Prompt Street"
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-prompt-directory/image.png"
        every {
            promptGenerator.generateArtDescriptionPrompt(any(), any(), any(), any())
        } returns "fake address prompt"
        coEvery { generateAIContentService.generateTextContent(any()) } returns "Fake AI text"
        viewModel.startToGenerateMapArt(
            directory = File("fake-prompt-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // When
        viewModel.generateAiText()
        advanceUntilIdle()

        // Then
        verify(exactly = 1) {
            promptGenerator.generateArtDescriptionPrompt(
                countryName = "Fake Prompt Country",
                cityName = "Fake Prompt City",
                streetName = "Fake Prompt Street",
                languageToReturn = Locale.getDefault().displayName
            )
        }
    }

    @Test
    fun `if the art address has null fields should generate the prompt with empty texts for them`() = runTest {
        // Given
        val fakeLocation = createFakeLocation(latitude = 9.5, longitude = 10.5)
        val fakeAddress = createFakeAddress(
            locality = null,
            countryName = "Fake Lonely Country",
            thoroughfare = null
        )
        coEvery { repository.getArtStylePreference() } returns SketchArtType.DEFAULT
        coEvery { locationService.getCurrentLocation() } returns fakeLocation
        coEvery { getAddressService.getAddress(any(), any()) } returns fakeAddress
        coEvery {
            repository.fetchStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns "fake-lonely-directory/image.png"
        every {
            promptGenerator.generateArtDescriptionPrompt(any(), any(), any(), any())
        } returns "fake lonely prompt"
        coEvery { generateAIContentService.generateTextContent(any()) } returns "Fake AI text"
        viewModel.startToGenerateMapArt(
            directory = File("fake-lonely-directory"),
            canvasToDrawArtWidth = 500,
            canvasToDrawArtHeight = 500
        )
        advanceUntilIdle()

        // When
        viewModel.generateAiText()
        advanceUntilIdle()

        // Then
        verify(exactly = 1) {
            promptGenerator.generateArtDescriptionPrompt(
                countryName = "Fake Lonely Country",
                cityName = "",
                streetName = "",
                languageToReturn = Locale.getDefault().displayName
            )
        }
    }

    @Test
    fun `if AI service returns null should emit ErrorGeneratingAIText ui state`() = runTest {
        // Given
        every {
            promptGenerator.generateArtDescriptionPrompt(any(), any(), any(), any())
        } returns "fake null text prompt"
        coEvery { generateAIContentService.generateTextContent(any()) } returns null
        val states = collectStates()

        // When
        viewModel.generateAiText()
        advanceUntilIdle()

        // Then
        assertEquals(
            listOf(
                CreateNewMapArtUiState.Idle,
                CreateNewMapArtUiState.LoadingAIText,
                CreateNewMapArtUiState.ErrorGeneratingAIText
            ),
            states
        )
    }

    @Test
    fun `if AI service returns a blank text should emit ErrorGeneratingAIText ui state`() = runTest {
        // Given
        every {
            promptGenerator.generateArtDescriptionPrompt(any(), any(), any(), any())
        } returns "fake blank text prompt"
        coEvery { generateAIContentService.generateTextContent(any()) } returns "   "
        val states = collectStates()

        // When
        viewModel.generateAiText()
        advanceUntilIdle()

        // Then
        assertEquals(
            listOf(
                CreateNewMapArtUiState.Idle,
                CreateNewMapArtUiState.LoadingAIText,
                CreateNewMapArtUiState.ErrorGeneratingAIText
            ),
            states
        )
    }

    @Test
    fun `if AI service throws a Failure should emit ErrorGeneratingAIText ui state`() = runTest {
        // Given
        every {
            promptGenerator.generateArtDescriptionPrompt(any(), any(), any(), any())
        } returns "fake failure prompt"
        coEvery {
            generateAIContentService.generateTextContent(any())
        } throws GeneratingAIContentFailure("fake AI error")
        val states = collectStates()

        // When
        viewModel.generateAiText()
        advanceUntilIdle()

        // Then
        assertEquals(
            listOf(
                CreateNewMapArtUiState.Idle,
                CreateNewMapArtUiState.LoadingAIText,
                CreateNewMapArtUiState.ErrorGeneratingAIText
            ),
            states
        )
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
            directory = fakeDirectory
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
            locality = "Fake Located City",
            countryName = "Fake Located Country",
            thoroughfare = "Fake Located Street"
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
            directory = File("fake-located-directory")
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
            directory = File("fake-unlocated-directory")
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
            directory = File("fake-no-description-directory")
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
            directory = fakeDirectory
        )
        viewModel.saveArtToLocalDatabase(
            title = "Fake Second Art Title",
            description = null,
            newArtBitMap = mockk<Bitmap>(),
            directory = fakeDirectory
        )
        advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { repository.saveMapArtEntity(any()) }
        assertEquals(2, fakeSavedMapArts.size)
        assertTrue(fakeSavedMapArts[0].id != fakeSavedMapArts[1].id)
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
            directory = File("fake-failing-directory")
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

    // endregion
}
