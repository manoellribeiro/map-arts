package manoellribeiro.dev.martp.core.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.core.data.local.daos.ColorPalettesDao
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
import manoellribeiro.dev.martp.core.data.local.daos.UserInfoDao
import manoellribeiro.dev.martp.core.data.local.entities.ColorPaletteEntity
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity
import manoellribeiro.dev.martp.core.data.network.geoapify.GeoapifyApiService
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.data.network.mapbox.models.MapboxMapStyle
import manoellribeiro.dev.martp.core.data.network.martp.MartpApiService
import manoellribeiro.dev.martp.core.data.network.martp.models.RemoteColorPalette
import manoellribeiro.dev.martp.core.data.network.martp.models.RemoteColorPalettesResponse
import manoellribeiro.dev.martp.core.data.network.martp.models.RemoteMapLayersColors
import manoellribeiro.dev.martp.core.data.network.martp.models.RemoteReversedAddress
import manoellribeiro.dev.martp.core.data.network.martp.models.RemoteSaveMapArtRequest
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.models.MapLayersColors
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import manoellribeiro.dev.martp.core.models.failures.MapboxApiErrorFailure
import manoellribeiro.dev.martp.core.models.failures.MartpApiErrorFailure
import manoellribeiro.dev.martp.core.models.failures.NoInternetConnectionFailure
import manoellribeiro.dev.martp.core.models.SketchArtType
import manoellribeiro.dev.martp.core.services.ConnectivityService
import manoellribeiro.dev.martp.infra.MainDispatcherExtension
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class MartpRepositoryTest {

    private lateinit var repository: MartpRepository
    private lateinit var mapboxApiService: MapboxApiService
    private lateinit var geoapifyApiService: GeoapifyApiService
    private lateinit var martpApiService: MartpApiService
    private lateinit var colorPalettesDao: ColorPalettesDao
    private lateinit var applicationScope: CoroutineScope
    private lateinit var mapArtsDao: MapArtsDao
    private lateinit var connectivityService: ConnectivityService
    private lateinit var userInfoDao: UserInfoDao
    private lateinit var artSettingsDataSore: DataStore<Preferences>
    private lateinit var errorLogger: ErrorLogger

    // Same names as the private constants of MartpRepository
    private val fakeMapArtStyleKey = stringPreferencesKey("MAP_ART_STYLE_KEY")
    private val fakeMapZoomKey = floatPreferencesKey("MAP_ZOOM_KEY")
    private val fakeColorPaletteKey = stringPreferencesKey("SELECTED_COLOR_PALETTE")

    // Same values as the FALLBACK_COLOR_PALETTES of MartpRepository
    private val fakeFallbackColorPalette = ColorPaletteEntity(
        name = "Martp",
        colors = listOf(
            0xFF100C2D.toInt(),
            0xFF120D31.toInt(),
            0xFF4E3C41.toInt(),
            0xFF896B51.toInt(),
            0xFFFFC971.toInt(),
            0xFFFFCE7E.toInt()
        )
    )

    @BeforeEach
    fun setup() {
        mapboxApiService = mockk()
        geoapifyApiService = mockk()
        martpApiService = mockk()
        colorPalettesDao = mockk()
        mapArtsDao = mockk()
        connectivityService = mockk()
        userInfoDao = mockk()
        artSettingsDataSore = mockk()
        errorLogger = mockk()
        // Shares the scheduler of runTest (see MainDispatcherExtension), so the work launched in the
        // background only runs when the test advances the scheduler
        applicationScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher())

        repository = MartpRepository(
            mapboxApiService = mapboxApiService,
            geoapifyApiService = geoapifyApiService,
            martpApiService = martpApiService,
            connectivityService = connectivityService,
            mapArtDao = mapArtsDao,
            userInfoDao = userInfoDao,
            colorPalettesDao = colorPalettesDao,
            artSettingsDataSore = artSettingsDataSore,
            ioDispatcher = UnconfinedTestDispatcher(),
            applicationScope = applicationScope,
            errorLogger = errorLogger
        )
    }

    private fun createFakeMapArt(
        id: String = "fake-map-art-id",
        description: String? = "fake map art description"
    ) = MapArtEntity(
        id = id,
        title = "fake map art title",
        description = description,
        latitude = -12.97f,
        longitude = -38.5f,
        dateInMillis = 1_700_000_000_000L,
        imagePathLocation = "/fake/path/image.png"
    )

    // region setColorPalettePreference

    @Test
    fun `if data store is empty should save the color palette name in the color palette key`() = runTest {
        // Given
        val fakeColorPaletteName = "Fake Hokusai Palette"
        val fakeCurrentPreferences = emptyPreferences()
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { artSettingsDataSore.updateData(capture(transformSlot)) } coAnswers {
            transformSlot.captured(fakeCurrentPreferences)
        }

        // When
        repository.setColorPalettePreference(fakeColorPaletteName)

        // Then
        val updatedPreferences = transformSlot.captured(fakeCurrentPreferences)
        assertEquals(fakeColorPaletteName, updatedPreferences[fakeColorPaletteKey])
        coVerify(exactly = 1) { artSettingsDataSore.updateData(any()) }
    }

    @Test
    fun `if data store already has preferences should replace the color palette and keep the art style and the map zoom`() = runTest {
        // Given
        val fakeNewColorPaletteName = "Fake New Palette"
        val fakeMapZoom = 16.25f
        val fakeCurrentPreferences = preferencesOf(
            fakeColorPaletteKey to "Fake Old Palette",
            fakeMapArtStyleKey to "COLORFUL_MAP",
            fakeMapZoomKey to fakeMapZoom
        )
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { artSettingsDataSore.updateData(capture(transformSlot)) } coAnswers {
            transformSlot.captured(fakeCurrentPreferences)
        }

        // When
        repository.setColorPalettePreference(fakeNewColorPaletteName)

        // Then
        val updatedPreferences = transformSlot.captured(fakeCurrentPreferences)
        assertEquals(fakeNewColorPaletteName, updatedPreferences[fakeColorPaletteKey])
        assertEquals("COLORFUL_MAP", updatedPreferences[fakeMapArtStyleKey])
        assertEquals(fakeMapZoom, updatedPreferences[fakeMapZoomKey])
    }

    @Test
    fun `if data store throws an exception when saving the color palette should throw LocalStorageErrorFailure`() = runTest {
        // Given
        val fakeErrorMessage = "fake color palette write error"
        coEvery { artSettingsDataSore.updateData(any()) } throws IOException(fakeErrorMessage)
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<LocalStorageErrorFailure> {
            repository.setColorPalettePreference("Fake Failing Palette")
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IOException>(), "MartpRepository.setColorPalettePreference") }
    }

    @Test
    fun `if saving the color palette is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake color palette cancellation")
        coEvery { artSettingsDataSore.updateData(any()) } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.setColorPalettePreference("Fake Cancelled Palette")
        }

        // Then
        assertSame(fakeCancellation, thrown)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region getColorPalettePreference

    @Test
    fun `if data store has a saved color palette should return its name`() = runTest {
        // Given
        val fakeColorPaletteName = "Fake Saved Palette"
        every { artSettingsDataSore.data } returns flowOf(preferencesOf(fakeColorPaletteKey to fakeColorPaletteName))

        // When
        val result = repository.getColorPalettePreference()

        // Then
        assertEquals(fakeColorPaletteName, result)
        verify(exactly = 1) { artSettingsDataSore.data }
    }

    @Test
    fun `if data store has no saved color palette should return the default Martp color palette`() = runTest {
        // Given
        every { artSettingsDataSore.data } returns flowOf(emptyPreferences())

        // When
        val result = repository.getColorPalettePreference()

        // Then
        assertEquals("Martp", result)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    @Test
    fun `if data store throws an exception when reading the color palette should return the default Martp color palette`() = runTest {
        // Given
        every { artSettingsDataSore.data } returns flow { throw IOException("fake color palette read error") }
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.getColorPalettePreference()

        // Then
        assertEquals("Martp", result)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IOException>(), "MartpRepository.getColorPalettePreference") }
    }

    @Test
    fun `if reading the color palette is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake color palette read cancellation")
        every { artSettingsDataSore.data } returns flow { throw fakeCancellation }

        // When
        val thrown = assertThrows<CancellationException> {
            repository.getColorPalettePreference()
        }

        // Then
        assertSame(fakeCancellation, thrown)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region setMapArtStylePreference

    @Test
    fun `if data store is empty should save the sketch art type name in the art style key`() = runTest {
        // Given
        val fakeCurrentPreferences = emptyPreferences()
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { artSettingsDataSore.updateData(capture(transformSlot)) } coAnswers {
            transformSlot.captured(fakeCurrentPreferences)
        }

        // When
        repository.setMapArtStylePreference(SketchArtType.POINTILLISM)

        // Then
        val updatedPreferences = transformSlot.captured(fakeCurrentPreferences)
        assertEquals("POINTILLISM", updatedPreferences[fakeMapArtStyleKey])
        coVerify(exactly = 1) { artSettingsDataSore.updateData(any()) }
    }

    @Test
    fun `if data store already has preferences should replace the art style and keep the map zoom`() = runTest {
        // Given
        val fakeMapZoom = 11.5f
        val fakeCurrentPreferences = preferencesOf(
            fakeMapArtStyleKey to "POINTILLISM",
            fakeMapZoomKey to fakeMapZoom
        )
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { artSettingsDataSore.updateData(capture(transformSlot)) } coAnswers {
            transformSlot.captured(fakeCurrentPreferences)
        }

        // When
        repository.setMapArtStylePreference(SketchArtType.DEFAULT)

        // Then
        val updatedPreferences = transformSlot.captured(fakeCurrentPreferences)
        assertEquals("DEFAULT", updatedPreferences[fakeMapArtStyleKey])
        assertEquals(fakeMapZoom, updatedPreferences[fakeMapZoomKey])
    }

    @Test
    fun `if data store throws an exception when saving the art style should throw LocalStorageErrorFailure`() = runTest {
        // Given
        val fakeErrorMessage = "fake art style write error"
        coEvery { artSettingsDataSore.updateData(any()) } throws IOException(fakeErrorMessage)
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<LocalStorageErrorFailure> {
            repository.setMapArtStylePreference(SketchArtType.POINTILLISM)
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IOException>(), "MartpRepository.setMapArtStylePreference") }
    }

    @Test
    fun `if saving the art style is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake art style cancellation")
        coEvery { artSettingsDataSore.updateData(any()) } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.setMapArtStylePreference(SketchArtType.POINTILLISM)
        }

        // Then
        assertSame(fakeCancellation, thrown)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region getArtStylePreference

    @Test
    fun `if data store has a saved art style should return the matching SketchArtType`() = runTest {
        // Given
        every { artSettingsDataSore.data } returns flowOf(preferencesOf(fakeMapArtStyleKey to "POINTILLISM"))

        // When
        val result = repository.getArtStylePreference()

        // Then
        assertEquals(SketchArtType.POINTILLISM, result)
        verify(exactly = 1) { artSettingsDataSore.data }
    }

    @Test
    fun `if data store has no saved art style should return the COLORFUL_MAP SketchArtType`() = runTest {
        // Given
        every { artSettingsDataSore.data } returns flowOf(emptyPreferences())

        // When
        val result = repository.getArtStylePreference()

        // Then
        assertEquals(SketchArtType.COLORFUL_MAP, result)
    }

    @Test
    fun `if data store has an unknown art style name should return the COLORFUL_MAP SketchArtType`() = runTest {
        // Given
        val fakeUnknownArtStyleName = "FAKE_UNKNOWN_ART_STYLE"
        every { artSettingsDataSore.data } returns flowOf(preferencesOf(fakeMapArtStyleKey to fakeUnknownArtStyleName))
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.getArtStylePreference()

        // Then
        assertEquals(SketchArtType.COLORFUL_MAP, result)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalArgumentException>(), "MartpRepository.getArtStylePreference") }
    }

    @Test
    fun `if data store throws an exception when reading the art style should return the COLORFUL_MAP SketchArtType`() = runTest {
        // Given
        every { artSettingsDataSore.data } returns flow { throw IOException("fake art style read error") }
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.getArtStylePreference()

        // Then
        assertEquals(SketchArtType.COLORFUL_MAP, result)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IOException>(), "MartpRepository.getArtStylePreference") }
    }

    @Test
    fun `if reading the art style is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake art style read cancellation")
        every { artSettingsDataSore.data } returns flow { throw fakeCancellation }

        // When
        val thrown = assertThrows<CancellationException> {
            repository.getArtStylePreference()
        }

        // Then
        assertSame(fakeCancellation, thrown)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region setMapZoomPreference

    @Test
    fun `if data store is empty should save the map zoom in the map zoom key`() = runTest {
        // Given
        val fakeMapZoom = 13.25f
        val fakeCurrentPreferences = emptyPreferences()
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { artSettingsDataSore.updateData(capture(transformSlot)) } coAnswers {
            transformSlot.captured(fakeCurrentPreferences)
        }

        // When
        repository.setMapZoomPreference(fakeMapZoom)

        // Then
        val updatedPreferences = transformSlot.captured(fakeCurrentPreferences)
        assertEquals(fakeMapZoom, updatedPreferences[fakeMapZoomKey])
        coVerify(exactly = 1) { artSettingsDataSore.updateData(any()) }
    }

    @Test
    fun `if data store already has preferences should replace the map zoom and keep the art style`() = runTest {
        // Given
        val fakeNewMapZoom = 17f
        val fakeCurrentPreferences = preferencesOf(
            fakeMapArtStyleKey to "POINTILLISM",
            fakeMapZoomKey to 9.5f
        )
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { artSettingsDataSore.updateData(capture(transformSlot)) } coAnswers {
            transformSlot.captured(fakeCurrentPreferences)
        }

        // When
        repository.setMapZoomPreference(fakeNewMapZoom)

        // Then
        val updatedPreferences = transformSlot.captured(fakeCurrentPreferences)
        assertEquals(fakeNewMapZoom, updatedPreferences[fakeMapZoomKey])
        assertEquals("POINTILLISM", updatedPreferences[fakeMapArtStyleKey])
    }

    @Test
    fun `if data store throws an exception when saving the map zoom should throw LocalStorageErrorFailure`() = runTest {
        // Given
        val fakeErrorMessage = "fake map zoom write error"
        coEvery { artSettingsDataSore.updateData(any()) } throws IOException(fakeErrorMessage)
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<LocalStorageErrorFailure> {
            repository.setMapZoomPreference(12f)
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IOException>(), "MartpRepository.setMapZoomPreference") }
    }

    @Test
    fun `if saving the map zoom is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake map zoom cancellation")
        coEvery { artSettingsDataSore.updateData(any()) } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.setMapZoomPreference(12f)
        }

        // Then
        assertSame(fakeCancellation, thrown)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region getMapZoomPreference

    @Test
    fun `if data store has a saved map zoom should return it`() = runTest {
        // Given
        val fakeMapZoom = 18.75f
        every { artSettingsDataSore.data } returns flowOf(preferencesOf(fakeMapZoomKey to fakeMapZoom))

        // When
        val result = repository.getMapZoomPreference()

        // Then
        assertEquals(fakeMapZoom, result)
        verify(exactly = 1) { artSettingsDataSore.data }
    }

    @Test
    fun `if data store has no saved map zoom should return the default zoom of 16 point 5`() = runTest {
        // Given
        every { artSettingsDataSore.data } returns flowOf(emptyPreferences())

        // When
        val result = repository.getMapZoomPreference()

        // Then
        assertEquals(16.5f, result)
    }

    @Test
    fun `if data store throws an exception when reading the map zoom should return the default zoom of 16 point 5`() = runTest {
        // Given
        every { artSettingsDataSore.data } returns flow { throw IOException("fake map zoom read error") }
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.getMapZoomPreference()

        // Then
        assertEquals(16.5f, result)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IOException>(), "MartpRepository.getMapZoomPreference") }
    }

    @Test
    fun `if reading the map zoom is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake map zoom read cancellation")
        every { artSettingsDataSore.data } returns flow { throw fakeCancellation }

        // When
        val thrown = assertThrows<CancellationException> {
            repository.getMapZoomPreference()
        }

        // Then
        assertSame(fakeCancellation, thrown)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region setUserName

    @Test
    fun `if userInfoDao saves the username should forward the username and the user id`() = runTest {
        // Given
        val fakeUsername = "Fake Username"
        val fakeUserId = "fake-user-id-for-username"
        coEvery { userInfoDao.setUserName(any(), any()) } just runs

        // When
        repository.setUserName(username = fakeUsername, userId = fakeUserId)

        // Then
        coVerify(exactly = 1) { userInfoDao.setUserName(id = fakeUserId, username = fakeUsername) }
    }

    @Test
    fun `if userInfoDao throws an exception when saving the username should not throw`() = runTest {
        // Given
        val fakeUsername = "Fake Failing Username"
        val fakeUserId = "fake-user-id-for-failing-username"
        coEvery { userInfoDao.setUserName(any(), any()) } throws IllegalStateException("fake username error")
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        assertDoesNotThrow {
            repository.setUserName(username = fakeUsername, userId = fakeUserId)
        }

        // Then
        coVerify(exactly = 1) { userInfoDao.setUserName(id = fakeUserId, username = fakeUsername) }
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalStateException>(), "MartpRepository.setUserName") }
    }

    @Test
    fun `if saving the username is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake username cancellation")
        coEvery { userInfoDao.setUserName(any(), any()) } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.setUserName(username = "Fake Cancelled Username", userId = "fake-cancelled-user-id")
        }

        // Then
        assertSame(fakeCancellation, thrown)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region setUserEmail

    @Test
    fun `if userInfoDao saves the email should forward the email and the user id`() = runTest {
        // Given
        val fakeEmail = "fake.user@example.com"
        val fakeUserId = "fake-user-id-for-email"
        coEvery { userInfoDao.setUserEmail(any(), any()) } just runs

        // When
        repository.setUserEmail(email = fakeEmail, userId = fakeUserId)

        // Then
        coVerify(exactly = 1) { userInfoDao.setUserEmail(id = fakeUserId, email = fakeEmail) }
    }

    @Test
    fun `if userInfoDao throws an exception when saving the email should not throw`() = runTest {
        // Given
        val fakeEmail = "fake.failing.user@example.com"
        val fakeUserId = "fake-user-id-for-failing-email"
        coEvery { userInfoDao.setUserEmail(any(), any()) } throws IllegalStateException("fake email error")
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        assertDoesNotThrow {
            repository.setUserEmail(email = fakeEmail, userId = fakeUserId)
        }

        // Then
        coVerify(exactly = 1) { userInfoDao.setUserEmail(id = fakeUserId, email = fakeEmail) }
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalStateException>(), "MartpRepository.setUserEmail") }
    }

    @Test
    fun `if saving the email is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake email cancellation")
        coEvery { userInfoDao.setUserEmail(any(), any()) } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.setUserEmail(email = "fake.cancelled.user@example.com", userId = "fake-cancelled-user-id")
        }

        // Then
        assertSame(fakeCancellation, thrown)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region fetchCurrentUserInfo

    @Test
    fun `if userInfoDao has a user should return it without inserting a new one`() = runTest {
        // Given
        val fakeUserInfo = UserInfoEntity(
            id = "fake-existing-user-id",
            username = "Fake Existing User",
            email = "fake.existing.user@example.com"
        )
        coEvery { userInfoDao.getUser() } returns fakeUserInfo

        // When
        val result = repository.fetchCurrentUserInfo()

        // Then
        assertEquals(fakeUserInfo, result)
        coVerify(exactly = 1) { userInfoDao.getUser() }
        coVerify(exactly = 0) { userInfoDao.insert(any()) }
    }

    @Test
    fun `if userInfoDao has no user should insert and return a new user without username and email`() = runTest {
        // Given
        val userInfoSlot = slot<UserInfoEntity>()
        coEvery { userInfoDao.getUser() } returns null
        coEvery { userInfoDao.insert(capture(userInfoSlot)) } just runs

        // When
        val result = repository.fetchCurrentUserInfo()

        // Then
        assertEquals(userInfoSlot.captured, result)
        assertNull(result.username)
        assertNull(result.email)
        assertEquals(result.id, UUID.fromString(result.id).toString())
        coVerify(exactly = 1) { userInfoDao.insert(any()) }
    }

    @Test
    fun `if userInfoDao throws an exception when getting the user should return a new user without username and email`() = runTest {
        // Given
        coEvery { userInfoDao.getUser() } throws IllegalStateException("fake get user error")
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.fetchCurrentUserInfo()

        // Then
        assertNull(result.username)
        assertNull(result.email)
        assertEquals(result.id, UUID.fromString(result.id).toString())
        coVerify(exactly = 0) { userInfoDao.insert(any()) }
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalStateException>(), "MartpRepository.fetchCurrentUserInfo") }
    }

    @Test
    fun `if getting the user is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake get user cancellation")
        coEvery { userInfoDao.getUser() } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.fetchCurrentUserInfo()
        }

        // Then
        assertSame(fakeCancellation, thrown)
        coVerify(exactly = 0) { userInfoDao.insert(any()) }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region observeUserMapArts

    @Test
    fun `if mapArtDao emits map arts should emit the same list`() = runTest {
        // Given
        val fakeMapArts = listOf(
            createFakeMapArt(id = "fake-map-art-id-1"),
            createFakeMapArt(id = "fake-map-art-id-2", description = null)
        )
        every { mapArtsDao.getAll() } returns flowOf(fakeMapArts)

        // When
        val result = repository.observeUserMapArts().first()

        // Then
        assertEquals(fakeMapArts, result)
        verify(exactly = 1) { mapArtsDao.getAll() }
    }

    @Test
    fun `if mapArtDao emits an empty list should emit an empty list`() = runTest {
        // Given
        every { mapArtsDao.getAll() } returns flowOf(emptyList())

        // When
        val result = repository.observeUserMapArts().first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `if mapArtDao flow throws an exception should throw LocalStorageErrorFailure`() = runTest {
        // Given
        val fakeErrorMessage = "fake get all map arts error"
        every { mapArtsDao.getAll() } returns flow { throw IllegalStateException(fakeErrorMessage) }
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<LocalStorageErrorFailure> {
            repository.observeUserMapArts().first()
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalStateException>(), "MartpRepository.observeUserMapArts") }
    }

    // endregion

    // region fetchStaticMapImage

    @Test
    fun `if there is no internet connection should throw NoInternetConnectionFailure without calling mapbox`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        every { connectivityService.isInternetConnected() } returns false

        // When
        val failure = assertThrows<NoInternetConnectionFailure> {
            repository.fetchStaticMapImage(
                sketchArtType = SketchArtType.DEFAULT,
                latitude = -12.9714,
                longitude = -38.5014,
                mapWidth = 400,
                mapHeight = 600,
                dir = fakeDir
            )
        }

        // Then
        assertNull(failure.message)
        assertFalse(File(fakeDir, "image.png").exists())
        coVerify(exactly = 0) { mapboxApiService.getStaticMapImage(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    @Test
    fun `if mapbox returns the image should write it to image png inside dir and return its path`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        val fakeLatitude = -12.9714
        val fakeLongitude = -38.5014
        val fakeMapWidth = 400
        val fakeMapHeight = 600
        val fakeMapZoom = 12.5f
        val fakeImageBytes = byteArrayOf(1, 2, 3, 4, 5)
        val fakeResponseBody = mockk<ResponseBody>()
        every { fakeResponseBody.bytes() } returns fakeImageBytes
        every { connectivityService.isInternetConnected() } returns true
        every { artSettingsDataSore.data } returns flowOf(preferencesOf(fakeMapZoomKey to fakeMapZoom))
        coEvery {
            mapboxApiService.getStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns fakeResponseBody

        // When
        val result = repository.fetchStaticMapImage(
            sketchArtType = SketchArtType.POINTILLISM,
            latitude = fakeLatitude,
            longitude = fakeLongitude,
            mapWidth = fakeMapWidth,
            mapHeight = fakeMapHeight,
            dir = fakeDir
        )

        // Then
        val expectedImageFile = File(fakeDir, "image.png")
        assertEquals(expectedImageFile.path, result)
        assertArrayEquals(fakeImageBytes, expectedImageFile.readBytes())
        coVerify(exactly = 1) {
            mapboxApiService.getStaticMapImage(
                styleId = MapboxMapStyle.DarkV11.id,
                latitude = fakeLatitude,
                longitude = fakeLongitude,
                mapWidth = fakeMapWidth,
                mapHeight = fakeMapHeight,
                mapZoom = fakeMapZoom
            )
        }
    }

    @Test
    fun `if there is no saved map zoom should request the mapbox image with the default zoom of 16 point 5`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        val fakeResponseBody = mockk<ResponseBody>()
        every { fakeResponseBody.bytes() } returns byteArrayOf(9, 8, 7)
        every { connectivityService.isInternetConnected() } returns true
        every { artSettingsDataSore.data } returns flowOf(emptyPreferences())
        coEvery {
            mapboxApiService.getStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns fakeResponseBody

        // When
        repository.fetchStaticMapImage(
            sketchArtType = SketchArtType.DEFAULT,
            latitude = 48.8584,
            longitude = 2.2945,
            mapWidth = 320,
            mapHeight = 480,
            dir = fakeDir
        )

        // Then
        coVerify(exactly = 1) {
            mapboxApiService.getStaticMapImage(any(), any(), any(), any(), any(), mapZoom = 16.5f)
        }
    }

    @Test
    fun `if image png already exists inside dir should overwrite it with the new image`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        val fakeOldImageBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70)
        val fakeNewImageBytes = byteArrayOf(11, 22)
        File(fakeDir, "image.png").writeBytes(fakeOldImageBytes)
        val fakeResponseBody = mockk<ResponseBody>()
        every { fakeResponseBody.bytes() } returns fakeNewImageBytes
        every { connectivityService.isInternetConnected() } returns true
        every { artSettingsDataSore.data } returns flowOf(emptyPreferences())
        coEvery {
            mapboxApiService.getStaticMapImage(any(), any(), any(), any(), any(), any())
        } returns fakeResponseBody

        // When
        val result = repository.fetchStaticMapImage(
            sketchArtType = SketchArtType.DEFAULT,
            latitude = 35.6586,
            longitude = 139.7454,
            mapWidth = 360,
            mapHeight = 640,
            dir = fakeDir
        )

        // Then
        assertArrayEquals(fakeNewImageBytes, File(result).readBytes())
    }

    @Test
    fun `if mapbox throws an exception should throw MapboxApiErrorFailure with the original message`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        val fakeErrorMessage = "fake mapbox network error"
        every { connectivityService.isInternetConnected() } returns true
        every { artSettingsDataSore.data } returns flowOf(emptyPreferences())
        coEvery {
            mapboxApiService.getStaticMapImage(any(), any(), any(), any(), any(), any())
        } throws IOException(fakeErrorMessage)
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<MapboxApiErrorFailure> {
            repository.fetchStaticMapImage(
                sketchArtType = SketchArtType.DEFAULT,
                latitude = 51.5007,
                longitude = -0.1246,
                mapWidth = 400,
                mapHeight = 600,
                dir = fakeDir
            )
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        assertFalse(File(fakeDir, "image.png").exists())
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IOException>(), "MartpRepository.fetchStaticMapImage") }
    }

    @Test
    fun `if fetching the static map image is cancelled should rethrow the CancellationException`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        val fakeCancellation = CancellationException("fake mapbox cancellation")
        every { connectivityService.isInternetConnected() } returns true
        every { artSettingsDataSore.data } returns flowOf(emptyPreferences())
        coEvery {
            mapboxApiService.getStaticMapImage(any(), any(), any(), any(), any(), any())
        } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.fetchStaticMapImage(
                sketchArtType = SketchArtType.DEFAULT,
                latitude = 41.8902,
                longitude = 12.4922,
                mapWidth = 400,
                mapHeight = 600,
                dir = fakeDir
            )
        }

        // Then
        assertSame(fakeCancellation, thrown)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region saveMapArtEntity

    @Test
    fun `if mapArtDao inserts the map art should forward the same entity`() = runTest {
        // Given
        val fakeMapArt = createFakeMapArt(id = "fake-map-art-id-to-save")
        val mapArtSlot = slot<MapArtEntity>()
        coEvery { mapArtsDao.insert(capture(mapArtSlot)) } just runs

        // When
        repository.saveMapArtEntity(fakeMapArt)

        // Then
        assertEquals(fakeMapArt, mapArtSlot.captured)
        coVerify(exactly = 1) { mapArtsDao.insert(any()) }
    }

    @Test
    fun `if mapArtDao throws an exception when inserting should throw LocalStorageErrorFailure with the original message`() = runTest {
        // Given
        val fakeErrorMessage = "fake insert map art error"
        coEvery { mapArtsDao.insert(any()) } throws IllegalStateException(fakeErrorMessage)
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<LocalStorageErrorFailure> {
            repository.saveMapArtEntity(createFakeMapArt())
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalStateException>(), "MartpRepository.saveMapArtEntity") }
    }

    @Test
    fun `if mapArtDao throws an exception without message when inserting should throw LocalStorageErrorFailure with null message`() = runTest {
        // Given
        coEvery { mapArtsDao.insert(any()) } throws IllegalStateException()
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<LocalStorageErrorFailure> {
            repository.saveMapArtEntity(createFakeMapArt())
        }

        // Then
        assertNull(failure.message)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalStateException>(), "MartpRepository.saveMapArtEntity") }
    }

    @Test
    fun `if inserting the map art is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake insert map art cancellation")
        coEvery { mapArtsDao.insert(any()) } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.saveMapArtEntity(createFakeMapArt())
        }

        // Then
        assertSame(fakeCancellation, thrown)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    @Test
    fun `if mapArtDao inserts the map art should not upload it to the martp api`() = runTest {
        // Given
        coEvery { mapArtsDao.insert(any()) } just runs

        // When
        repository.saveMapArtEntity(createFakeMapArt(id = "fake-map-art-id-not-uploaded"))
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { mapArtsDao.insert(any()) }
        coVerify(exactly = 0) { martpApiService.saveMapArt(any(), any()) }
    }

    // endregion

    // region uploadMapArt

    @Test
    fun `if uploadMapArt is called should not upload the map art before the application scope runs`() = runTest {
        // Given
        val fakeUserInfo = UserInfoEntity(id = "fake-pending-user-id", username = null, email = null)
        coEvery { martpApiService.saveMapArt(any(), any()) } just runs

        // When
        repository.uploadMapArt(createFakeMapArt(id = "fake-pending-upload-id"), fakeUserInfo)

        // Then
        coVerify(exactly = 0) { martpApiService.saveMapArt(any(), any()) }
    }

    @Test
    fun `if the map art has art style, color palette and layer colors should upload them with the colors in hex`() = runTest {
        // Given
        val fakeMapArt = createFakeMapArt(id = "fake-colorful-upload-id").copy(
            sketchArtType = SketchArtType.COLORFUL_MAP,
            colorPalette = "Fake Upload Palette",
            colorsLayers = MapLayersColors(
                roadsColor = 0xFF100C2D.toInt(),
                buildingStrokeColor = 0xFF120D31.toInt(),
                buildingColor = 0x804E3C41.toInt(),
                roadsPathColor = 0x00896B51,
                waterColor = 0xFFffc971.toInt(),
                landOthersColor = 0xFF00000A.toInt()
            )
        )
        val fakeUserInfo = UserInfoEntity(
            id = "fake-uploader-id",
            username = "Fake Uploader",
            email = "fake.uploader@example.com"
        )
        val requestSlot = slot<RemoteSaveMapArtRequest>()
        coEvery { martpApiService.saveMapArt(capture(requestSlot), any()) } just runs

        // When
        repository.uploadMapArt(fakeMapArt, fakeUserInfo)
        advanceUntilIdle()

        // Then
        assertEquals(
            RemoteSaveMapArtRequest(
                id = "fake-colorful-upload-id",
                title = fakeMapArt.title,
                latitude = fakeMapArt.latitude.toDouble(),
                longitude = fakeMapArt.longitude.toDouble(),
                dateInMillis = fakeMapArt.dateInMillis,
                sketchArtType = "COLORFUL_MAP",
                colorPalette = "Fake Upload Palette",
                colorsLayers = RemoteMapLayersColors(
                    roadsColor = "#100C2D",
                    buildingStrokeColor = "#120D31",
                    buildingColor = "#4E3C41",
                    roadsPathColor = "#896B51",
                    waterColor = "#FFC971",
                    landOthersColor = "#00000A"
                ),
                email = "fake.uploader@example.com",
                username = "Fake Uploader"
            ),
            requestSlot.captured
        )
        coVerify(exactly = 1) { martpApiService.saveMapArt(any(), any()) }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    @Test
    fun `if the map art has no art style, color palette or layer colors should upload them as null`() = runTest {
        // Given
        val fakeMapArt = createFakeMapArt(id = "fake-plain-upload-id")
        val fakeUserInfo = UserInfoEntity(id = "fake-anonymous-user-id", username = null, email = null)
        val requestSlot = slot<RemoteSaveMapArtRequest>()
        coEvery { martpApiService.saveMapArt(capture(requestSlot), any()) } just runs

        // When
        repository.uploadMapArt(fakeMapArt, fakeUserInfo)
        advanceUntilIdle()

        // Then
        assertNull(requestSlot.captured.sketchArtType)
        assertNull(requestSlot.captured.colorPalette)
        assertEquals(RemoteMapLayersColors(), requestSlot.captured.colorsLayers)
        assertNull(requestSlot.captured.email)
        assertNull(requestSlot.captured.username)
    }

    @Test
    fun `if the map art has only some layer colors should upload the missing ones as null`() = runTest {
        // Given
        val fakeMapArt = createFakeMapArt(id = "fake-partial-colors-upload-id").copy(
            colorsLayers = MapLayersColors(waterColor = 0xFF1A2B3C.toInt())
        )
        val fakeUserInfo = UserInfoEntity(id = "fake-partial-user-id", username = "Fake Partial", email = null)
        val requestSlot = slot<RemoteSaveMapArtRequest>()
        coEvery { martpApiService.saveMapArt(capture(requestSlot), any()) } just runs

        // When
        repository.uploadMapArt(fakeMapArt, fakeUserInfo)
        advanceUntilIdle()

        // Then
        assertEquals(RemoteMapLayersColors(waterColor = "#1A2B3C"), requestSlot.captured.colorsLayers)
        assertEquals("Fake Partial", requestSlot.captured.username)
        assertNull(requestSlot.captured.email)
    }

    @Test
    fun `if uploadMapArt is called should send the image file as a png form data part named image`() = runTest {
        // Given
        val fakeMapArt = createFakeMapArt(id = "fake-image-upload-id").copy(
            imagePathLocation = "/fake/arts/fake-image-upload-id.png"
        )
        val fakeUserInfo = UserInfoEntity(id = "fake-image-user-id", username = null, email = null)
        val imageSlot = slot<MultipartBody.Part>()
        coEvery { martpApiService.saveMapArt(any(), capture(imageSlot)) } just runs

        // When
        repository.uploadMapArt(fakeMapArt, fakeUserInfo)
        advanceUntilIdle()

        // Then
        assertEquals(
            "form-data; name=\"image\"; filename=\"fake-image-upload-id.png\"",
            imageSlot.captured.headers?.get("Content-Disposition")
        )
        assertEquals("image/png", imageSlot.captured.body.contentType().toString())
    }

    @Test
    fun `if martp api throws an exception when uploading should log it without throwing`() = runTest {
        // Given
        coEvery { martpApiService.saveMapArt(any(), any()) } throws IOException("fake upload error")
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        repository.uploadMapArt(
            createFakeMapArt(id = "fake-failing-upload-id"),
            UserInfoEntity(id = "fake-failing-user-id", username = null, email = null)
        )
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { martpApiService.saveMapArt(any(), any()) }
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IOException>(), "MartpRepository.uploadMapArt") }
    }

    @Test
    fun `if uploading the map art is cancelled should not log the CancellationException`() = runTest {
        // Given
        coEvery { martpApiService.saveMapArt(any(), any()) } throws CancellationException("fake upload cancellation")

        // When
        repository.uploadMapArt(
            createFakeMapArt(id = "fake-cancelled-upload-id"),
            UserInfoEntity(id = "fake-cancelled-user-id", username = null, email = null)
        )
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { martpApiService.saveMapArt(any(), any()) }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region getColorsPalettes

    @Test
    fun `if colorPalettesDao has palettes should return them and refresh them from the martp api in the background`() = runTest {
        // Given
        val fakeLocalColorPalettes = listOf(
            ColorPaletteEntity(name = "Fake Local Palette", colors = listOf(0xFF010203.toInt()))
        )
        val fakeRemoteColorPalettesResponse = RemoteColorPalettesResponse(
            palettes = listOf(RemoteColorPalette(name = "Fake Remote Palette", colors = listOf("#0A0B0C")))
        )
        val replacedPalettesSlot = slot<List<ColorPaletteEntity>>()
        coEvery { colorPalettesDao.getAll() } returns fakeLocalColorPalettes
        coEvery { martpApiService.getColorPalettes() } returns fakeRemoteColorPalettesResponse
        coEvery { colorPalettesDao.replaceAll(capture(replacedPalettesSlot)) } just runs

        // When
        val result = repository.getColorsPalettes()

        // Then
        assertEquals(fakeLocalColorPalettes, result)
        coVerify(exactly = 0) { martpApiService.getColorPalettes() }
        advanceUntilIdle()
        coVerify(exactly = 1) { martpApiService.getColorPalettes() }
        assertEquals(
            listOf(ColorPaletteEntity(name = "Fake Remote Palette", colors = listOf(0xFF0A0B0C.toInt()))),
            replacedPalettesSlot.captured
        )
        coVerify(exactly = 1) { colorPalettesDao.replaceAll(any()) }
    }

    @Test
    fun `if colorPalettesDao has palettes and the background refresh fails should still return the local palettes`() = runTest {
        // Given
        val fakeLocalColorPalettes = listOf(
            ColorPaletteEntity(name = "Fake Kept Palette", colors = listOf(0xFF111213.toInt(), 0xFF141516.toInt()))
        )
        coEvery { colorPalettesDao.getAll() } returns fakeLocalColorPalettes
        coEvery { martpApiService.getColorPalettes() } throws IOException("fake background refresh error")
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.getColorsPalettes()
        advanceUntilIdle()

        // Then
        assertEquals(fakeLocalColorPalettes, result)
        coVerify(exactly = 0) { colorPalettesDao.replaceAll(any()) }
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IOException>(), "MartpRepository.fetchColorsPalettes") }
    }

    @Test
    fun `if colorPalettesDao has no palettes should return the palettes fetched from the martp api`() = runTest {
        // Given
        val fakeRemoteColorPalettesResponse = RemoteColorPalettesResponse(
            palettes = listOf(
                RemoteColorPalette(name = "Fake First Remote Palette", colors = listOf("#102030", "#405060")),
                RemoteColorPalette(name = "Fake Second Remote Palette", colors = listOf("#708090"))
            )
        )
        val fakeExpectedColorPalettes = listOf(
            ColorPaletteEntity(
                name = "Fake First Remote Palette",
                colors = listOf(0xFF102030.toInt(), 0xFF405060.toInt())
            ),
            ColorPaletteEntity(name = "Fake Second Remote Palette", colors = listOf(0xFF708090.toInt()))
        )
        coEvery { colorPalettesDao.getAll() } returns emptyList()
        coEvery { martpApiService.getColorPalettes() } returns fakeRemoteColorPalettesResponse
        coEvery { colorPalettesDao.replaceAll(any()) } just runs

        // When
        val result = repository.getColorsPalettes()
        advanceUntilIdle()

        // Then
        assertEquals(fakeExpectedColorPalettes, result)
        coVerify(exactly = 1) { martpApiService.getColorPalettes() }
        coVerify(exactly = 1) { colorPalettesDao.replaceAll(fakeExpectedColorPalettes) }
    }

    @Test
    fun `if colorPalettesDao has no palettes and the martp api fails should return the fallback Martp palette`() = runTest {
        // Given
        coEvery { colorPalettesDao.getAll() } returns emptyList()
        coEvery { martpApiService.getColorPalettes() } throws IOException("fake no palettes anywhere error")
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.getColorsPalettes()
        advanceUntilIdle()

        // Then
        assertEquals(listOf(fakeFallbackColorPalette), result)
        coVerify(exactly = 1) { martpApiService.getColorPalettes() }
        coVerify(exactly = 0) { colorPalettesDao.replaceAll(any()) }
    }

    @Test
    fun `if colorPalettesDao throws an exception when getting the palettes should log it and return the palettes fetched from the martp api`() = runTest {
        // Given
        val fakeRemoteColorPalettesResponse = RemoteColorPalettesResponse(
            palettes = listOf(RemoteColorPalette(name = "Fake Rescue Palette", colors = listOf("#ABCDEF")))
        )
        coEvery { colorPalettesDao.getAll() } throws IllegalStateException("fake get all palettes error")
        coEvery { martpApiService.getColorPalettes() } returns fakeRemoteColorPalettesResponse
        coEvery { colorPalettesDao.replaceAll(any()) } just runs
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.getColorsPalettes()
        advanceUntilIdle()

        // Then
        assertEquals(
            listOf(ColorPaletteEntity(name = "Fake Rescue Palette", colors = listOf(0xFFABCDEF.toInt()))),
            result
        )
        verify(exactly = 1) {
            errorLogger.logNonFatal(ofType<IllegalStateException>(), "MartpRepository.getColorsPalettes")
        }
        coVerify(exactly = 1) { martpApiService.getColorPalettes() }
    }

    @Test
    fun `if getting the local palettes is cancelled should rethrow the CancellationException without calling the martp api`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake get all palettes cancellation")
        coEvery { colorPalettesDao.getAll() } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.getColorsPalettes()
        }
        advanceUntilIdle()

        // Then
        assertSame(fakeCancellation, thrown)
        coVerify(exactly = 0) { martpApiService.getColorPalettes() }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region fetchColorsPalettes

    @Test
    fun `if martp api returns palettes should return them with the hex colors as opaque color ints and save them`() = runTest {
        // Given
        val fakeRemoteColorPalettesResponse = RemoteColorPalettesResponse(
            palettes = listOf(
                RemoteColorPalette(name = "Fake Hex Palette", colors = listOf("#f27ea9", "000000", "#FFFFFF"))
            )
        )
        val fakeExpectedColorPalettes = listOf(
            ColorPaletteEntity(
                name = "Fake Hex Palette",
                colors = listOf(0xFFF27EA9.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt())
            )
        )
        coEvery { martpApiService.getColorPalettes() } returns fakeRemoteColorPalettesResponse
        coEvery { colorPalettesDao.replaceAll(any()) } just runs

        // When
        val result = repository.fetchColorsPalettes()

        // Then
        assertEquals(fakeExpectedColorPalettes, result)
        coVerify(exactly = 1) { colorPalettesDao.replaceAll(fakeExpectedColorPalettes) }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    @Test
    fun `if martp api returns a palette without colors should return and save it with an empty color list`() = runTest {
        // Given
        val fakeRemoteColorPalettesResponse = RemoteColorPalettesResponse(
            palettes = listOf(RemoteColorPalette(name = "Fake Colorless Palette", colors = emptyList()))
        )
        coEvery { martpApiService.getColorPalettes() } returns fakeRemoteColorPalettesResponse
        coEvery { colorPalettesDao.replaceAll(any()) } just runs

        // When
        val result = repository.fetchColorsPalettes()

        // Then
        val fakeExpectedColorPalettes = listOf(ColorPaletteEntity(name = "Fake Colorless Palette", colors = emptyList()))
        assertEquals(fakeExpectedColorPalettes, result)
        coVerify(exactly = 1) { colorPalettesDao.replaceAll(fakeExpectedColorPalettes) }
    }

    @Test
    fun `if martp api returns an empty list should return the fallback Martp palette without saving it`() = runTest {
        // Given
        coEvery { martpApiService.getColorPalettes() } returns RemoteColorPalettesResponse(palettes = emptyList())

        // When
        val result = repository.fetchColorsPalettes()

        // Then
        assertEquals(listOf(fakeFallbackColorPalette), result)
        coVerify(exactly = 0) { colorPalettesDao.replaceAll(any()) }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    @Test
    fun `if martp api throws an exception should log it and return the fallback Martp palette without saving it`() = runTest {
        // Given
        coEvery { martpApiService.getColorPalettes() } throws IOException("fake color palettes api error")
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.fetchColorsPalettes()

        // Then
        assertEquals(listOf(fakeFallbackColorPalette), result)
        coVerify(exactly = 0) { colorPalettesDao.replaceAll(any()) }
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IOException>(), "MartpRepository.fetchColorsPalettes") }
    }

    @Test
    fun `if martp api returns a color that is not in the RRGGBB format should log it and return the fallback Martp palette`() = runTest {
        // Given
        val fakeRemoteColorPalettesResponse = RemoteColorPalettesResponse(
            palettes = listOf(
                RemoteColorPalette(name = "Fake Valid Palette", colors = listOf("#123456")),
                RemoteColorPalette(name = "Fake Short Hex Palette", colors = listOf("#FFF"))
            )
        )
        coEvery { martpApiService.getColorPalettes() } returns fakeRemoteColorPalettesResponse
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.fetchColorsPalettes()

        // Then
        assertEquals(listOf(fakeFallbackColorPalette), result)
        coVerify(exactly = 0) { colorPalettesDao.replaceAll(any()) }
        verify(exactly = 1) {
            errorLogger.logNonFatal(ofType<IllegalArgumentException>(), "MartpRepository.fetchColorsPalettes")
        }
    }

    @Test
    fun `if colorPalettesDao throws an exception when saving the palettes should log it and still return the fetched palettes`() = runTest {
        // Given
        val fakeRemoteColorPalettesResponse = RemoteColorPalettesResponse(
            palettes = listOf(RemoteColorPalette(name = "Fake Unsaved Palette", colors = listOf("#C0FFEE")))
        )
        coEvery { martpApiService.getColorPalettes() } returns fakeRemoteColorPalettesResponse
        coEvery { colorPalettesDao.replaceAll(any()) } throws IllegalStateException("fake replace palettes error")
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.fetchColorsPalettes()

        // Then
        assertEquals(
            listOf(ColorPaletteEntity(name = "Fake Unsaved Palette", colors = listOf(0xFFC0FFEE.toInt()))),
            result
        )
        verify(exactly = 1) {
            errorLogger.logNonFatal(ofType<IllegalStateException>(), "MartpRepository.fetchColorsPalettes")
        }
    }

    @Test
    fun `if fetching the palettes from the martp api is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake color palettes api cancellation")
        coEvery { martpApiService.getColorPalettes() } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.fetchColorsPalettes()
        }

        // Then
        assertSame(fakeCancellation, thrown)
        coVerify(exactly = 0) { colorPalettesDao.replaceAll(any()) }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    @Test
    fun `if saving the fetched palettes is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake replace palettes cancellation")
        coEvery { martpApiService.getColorPalettes() } returns RemoteColorPalettesResponse(
            palettes = listOf(RemoteColorPalette(name = "Fake Cancelled Save Palette", colors = listOf("#BADA55")))
        )
        coEvery { colorPalettesDao.replaceAll(any()) } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.fetchColorsPalettes()
        }

        // Then
        assertSame(fakeCancellation, thrown)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region getSelectedColorPalette

    @Test
    fun `if colorPalettesDao has the saved color palette should return it without loading all palettes`() = runTest {
        // Given
        val fakeColorPaletteName = "Fake Selected Palette"
        val fakeColorPalette = ColorPaletteEntity(name = fakeColorPaletteName, colors = listOf(0xFF212223.toInt()))
        every { artSettingsDataSore.data } returns flowOf(preferencesOf(fakeColorPaletteKey to fakeColorPaletteName))
        coEvery { colorPalettesDao.getByName(any()) } returns fakeColorPalette

        // When
        val result = repository.getSelectedColorPalette()
        advanceUntilIdle()

        // Then
        assertEquals(fakeColorPalette, result)
        coVerify(exactly = 1) { colorPalettesDao.getByName(fakeColorPaletteName) }
        coVerify(exactly = 0) { colorPalettesDao.getAll() }
        coVerify(exactly = 0) { martpApiService.getColorPalettes() }
    }

    @Test
    fun `if there is no saved color palette should look for the Martp palette`() = runTest {
        // Given
        val fakeMartpColorPalette = ColorPaletteEntity(name = "Martp", colors = listOf(0xFF313233.toInt()))
        every { artSettingsDataSore.data } returns flowOf(emptyPreferences())
        coEvery { colorPalettesDao.getByName(any()) } returns fakeMartpColorPalette

        // When
        val result = repository.getSelectedColorPalette()

        // Then
        assertEquals(fakeMartpColorPalette, result)
        coVerify(exactly = 1) { colorPalettesDao.getByName("Martp") }
    }

    @Test
    fun `if colorPalettesDao does not have the saved color palette should return the first local palette`() = runTest {
        // Given
        val fakeLocalColorPalettes = listOf(
            ColorPaletteEntity(name = "Fake First Local Palette", colors = listOf(0xFF414243.toInt())),
            ColorPaletteEntity(name = "Fake Second Local Palette", colors = listOf(0xFF444546.toInt()))
        )
        every { artSettingsDataSore.data } returns flowOf(preferencesOf(fakeColorPaletteKey to "Fake Deleted Palette"))
        coEvery { colorPalettesDao.getByName(any()) } returns null
        coEvery { colorPalettesDao.getAll() } returns fakeLocalColorPalettes
        coEvery { martpApiService.getColorPalettes() } returns RemoteColorPalettesResponse(
            palettes = listOf(RemoteColorPalette(name = "Fake Refreshed Palette", colors = listOf("#474849")))
        )
        coEvery { colorPalettesDao.replaceAll(any()) } just runs

        // When
        val result = repository.getSelectedColorPalette()
        advanceUntilIdle()

        // Then
        assertEquals(fakeLocalColorPalettes.first(), result)
        coVerify(exactly = 1) { colorPalettesDao.getByName("Fake Deleted Palette") }
        coVerify(exactly = 1) { colorPalettesDao.getAll() }
        coVerify(exactly = 1) { martpApiService.getColorPalettes() }
    }

    @Test
    fun `if colorPalettesDao does not have the saved color palette and has no palettes should return the first palette from the martp api`() = runTest {
        // Given
        every { artSettingsDataSore.data } returns flowOf(preferencesOf(fakeColorPaletteKey to "Fake Missing Palette"))
        coEvery { colorPalettesDao.getByName(any()) } returns null
        coEvery { colorPalettesDao.getAll() } returns emptyList()
        coEvery { martpApiService.getColorPalettes() } returns RemoteColorPalettesResponse(
            palettes = listOf(
                RemoteColorPalette(name = "Fake First Api Palette", colors = listOf("#515253")),
                RemoteColorPalette(name = "Fake Second Api Palette", colors = listOf("#545556"))
            )
        )
        coEvery { colorPalettesDao.replaceAll(any()) } just runs

        // When
        val result = repository.getSelectedColorPalette()
        advanceUntilIdle()

        // Then
        assertEquals(ColorPaletteEntity(name = "Fake First Api Palette", colors = listOf(0xFF515253.toInt())), result)
        coVerify(exactly = 1) { martpApiService.getColorPalettes() }
    }

    @Test
    fun `if no palette is found locally and the martp api fails should return the fallback Martp palette`() = runTest {
        // Given
        every { artSettingsDataSore.data } returns flowOf(preferencesOf(fakeColorPaletteKey to "Fake Offline Palette"))
        coEvery { colorPalettesDao.getByName(any()) } returns null
        coEvery { colorPalettesDao.getAll() } returns emptyList()
        coEvery { martpApiService.getColorPalettes() } throws IOException("fake offline palettes error")
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.getSelectedColorPalette()
        advanceUntilIdle()

        // Then
        assertEquals(fakeFallbackColorPalette, result)
    }

    @Test
    fun `if colorPalettesDao throws an exception when getting the saved color palette should log it and return the first local palette`() = runTest {
        // Given
        val fakeLocalColorPalettes = listOf(
            ColorPaletteEntity(name = "Fake Recovered Palette", colors = listOf(0xFF616263.toInt()))
        )
        every { artSettingsDataSore.data } returns flowOf(preferencesOf(fakeColorPaletteKey to "Fake Broken Palette"))
        coEvery { colorPalettesDao.getByName(any()) } throws IllegalStateException("fake get palette by name error")
        coEvery { colorPalettesDao.getAll() } returns fakeLocalColorPalettes
        coEvery { martpApiService.getColorPalettes() } returns RemoteColorPalettesResponse(palettes = emptyList())
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.getSelectedColorPalette()
        advanceUntilIdle()

        // Then
        assertEquals(fakeLocalColorPalettes.first(), result)
        verify(exactly = 1) {
            errorLogger.logNonFatal(ofType<IllegalStateException>(), "MartpRepository.getSelectedColorPalette")
        }
    }

    @Test
    fun `if getting the saved color palette is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake get palette by name cancellation")
        every { artSettingsDataSore.data } returns flowOf(preferencesOf(fakeColorPaletteKey to "Fake Cancelled Palette"))
        coEvery { colorPalettesDao.getByName(any()) } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.getSelectedColorPalette()
        }

        // Then
        assertSame(fakeCancellation, thrown)
        coVerify(exactly = 0) { colorPalettesDao.getAll() }
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

    // region getReversedAddressFromApi

    @Test
    fun `if martp api returns the address should return it and send the location and the device language`() = runTest {
        // Given
        val fakeLatitude = -12.9714
        val fakeLongitude = -38.5014
        val fakeReversedAddress = RemoteReversedAddress(
            street = "Fake Api Street",
            city = "Fake Api City",
            country = "Fake Api Country"
        )
        coEvery { martpApiService.reverseGeocode(any(), any(), any()) } returns fakeReversedAddress

        // When
        val result = repository.getReversedAddressFromApi(latitude = fakeLatitude, longitude = fakeLongitude)

        // Then
        assertEquals(fakeReversedAddress, result)
        coVerify(exactly = 1) {
            martpApiService.reverseGeocode(
                latitude = fakeLatitude,
                longitude = fakeLongitude,
                language = Locale.getDefault().language
            )
        }
    }

    @Test
    fun `if martp api returns an address without street, city and country should return it as it is`() = runTest {
        // Given
        val fakeEmptyReversedAddress = RemoteReversedAddress(street = null, city = null, country = null)
        coEvery { martpApiService.reverseGeocode(any(), any(), any()) } returns fakeEmptyReversedAddress

        // When
        val result = repository.getReversedAddressFromApi(latitude = 0.5, longitude = -160.5)

        // Then
        assertEquals(fakeEmptyReversedAddress, result)
    }

    @Test
    fun `if martp api throws an exception when reverse geocoding should log it and throw MartpApiErrorFailure`() = runTest {
        // Given
        val fakeErrorMessage = "fake reverse geocode error"
        coEvery { martpApiService.reverseGeocode(any(), any(), any()) } throws IOException(fakeErrorMessage)
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<MartpApiErrorFailure> {
            repository.getReversedAddressFromApi(latitude = 1.5, longitude = 2.5)
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IOException>(), "MartpRepository.getReversedAddressFromApi") }
    }

    @Test
    fun `if reverse geocoding is cancelled should rethrow the CancellationException`() = runTest {
        // Given
        val fakeCancellation = CancellationException("fake reverse geocode cancellation")
        coEvery { martpApiService.reverseGeocode(any(), any(), any()) } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.getReversedAddressFromApi(latitude = 3.5, longitude = 4.5)
        }

        // Then
        assertSame(fakeCancellation, thrown)
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

}
