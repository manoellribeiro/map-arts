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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
import manoellribeiro.dev.martp.core.data.local.daos.UserInfoDao
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity
import manoellribeiro.dev.martp.core.data.network.geoapify.GeoapifyApiService
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.data.network.mapbox.models.MapboxMapStyle
import manoellribeiro.dev.martp.core.logging.ErrorLogger
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import manoellribeiro.dev.martp.core.models.failures.MapboxApiErrorFailure
import manoellribeiro.dev.martp.core.models.failures.NoInternetConnectionFailure
import manoellribeiro.dev.martp.core.models.SketchArtType
import manoellribeiro.dev.martp.core.services.ConnectivityService
import manoellribeiro.dev.martp.infra.MainDispatcherExtension
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class MartpRepositoryTest {

    private lateinit var repository: MartpRepository
    private lateinit var mapboxApiService: MapboxApiService
    private lateinit var geoapifyApiService: GeoapifyApiService
    private lateinit var mapArtsDao: MapArtsDao
    private lateinit var connectivityService: ConnectivityService
    private lateinit var userInfoDao: UserInfoDao
    private lateinit var artSettingsDataSore: DataStore<Preferences>
    private lateinit var errorLogger: ErrorLogger

    // Same names as the private constants of MartpRepository
    private val fakeMapArtStyleKey = stringPreferencesKey("MAP_ART_STYLE_KEY")
    private val fakeMapZoomKey = floatPreferencesKey("MAP_ZOOM_KEY")

    @BeforeEach
    fun setup() {
        mapboxApiService = mockk()
        geoapifyApiService = mockk()
        mapArtsDao = mockk()
        connectivityService = mockk()
        userInfoDao = mockk()
        artSettingsDataSore = mockk()
        errorLogger = mockk()

        repository = MartpRepository(
            mapboxApiService = mapboxApiService,
            geoapifyApiService = geoapifyApiService,
            connectivityService = connectivityService,
            mapArtDao = mapArtsDao,
            userInfoDao = userInfoDao,
            artSettingsDataSore = artSettingsDataSore,
            ioDispatcher = UnconfinedTestDispatcher(),
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
        imagePathLocation = "/fake/path/image.png",
        originalMapImageLocation = "/fake/path/map-arts/fake-map-art-id.png",
        sketchArtType = SketchArtType.COLORFUL_MAP,
        colorPalette = "fake palette",
        colorsLayers = null
    )

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
    fun `if data store has no saved art style should return the DEFAULT SketchArtType`() = runTest {
        // Given
        every { artSettingsDataSore.data } returns flowOf(emptyPreferences())

        // When
        val result = repository.getArtStylePreference()

        // Then
        assertEquals(SketchArtType.DEFAULT, result)
    }

    @Test
    fun `if data store has an unknown art style name should return the DEFAULT SketchArtType`() = runTest {
        // Given
        val fakeUnknownArtStyleName = "FAKE_UNKNOWN_ART_STYLE"
        every { artSettingsDataSore.data } returns flowOf(preferencesOf(fakeMapArtStyleKey to fakeUnknownArtStyleName))
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.getArtStylePreference()

        // Then
        assertEquals(SketchArtType.DEFAULT, result)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalArgumentException>(), "MartpRepository.getArtStylePreference") }
    }

    @Test
    fun `if data store throws an exception when reading the art style should return the DEFAULT SketchArtType`() = runTest {
        // Given
        every { artSettingsDataSore.data } returns flow { throw IOException("fake art style read error") }
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.getArtStylePreference()

        // Then
        assertEquals(SketchArtType.DEFAULT, result)
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
    fun `if data store has no saved map zoom should return the default zoom of 15`() = runTest {
        // Given
        every { artSettingsDataSore.data } returns flowOf(emptyPreferences())

        // When
        val result = repository.getMapZoomPreference()

        // Then
        assertEquals(15f, result)
    }

    @Test
    fun `if data store throws an exception when reading the map zoom should return the default zoom of 15`() = runTest {
        // Given
        every { artSettingsDataSore.data } returns flow { throw IOException("fake map zoom read error") }
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val result = repository.getMapZoomPreference()

        // Then
        assertEquals(15f, result)
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
    fun `if there is no saved map zoom should request the mapbox image with the default zoom of 15`(
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
            mapboxApiService.getStaticMapImage(any(), any(), any(), any(), any(), mapZoom = 15f)
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

    // region saveMapArt

    private fun createFakeStaticImage(dir: File, bytes: ByteArray = byteArrayOf(3, 1, 4, 1, 5)): File {
        return File(dir, "image.png").apply { writeBytes(bytes) }
    }

    @Test
    fun `if mapArtDao inserts the map art should move the static image to the arts directory named with the art id`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        val fakeImageBytes = byteArrayOf(2, 7, 1, 8)
        val fakeStaticImage = createFakeStaticImage(fakeDir, fakeImageBytes)
        val fakeMapArt = createFakeMapArt(id = "fake-map-art-id-to-move")
        coEvery { mapArtsDao.insert(any()) } just runs

        // When
        repository.saveMapArt(fakeMapArt, fakeStaticImage.path, fakeDir)

        // Then
        val expectedArtImage = File(File(fakeDir, "map-arts"), "fake-map-art-id-to-move.png")
        assertArrayEquals(fakeImageBytes, expectedArtImage.readBytes())
        assertFalse(fakeStaticImage.exists())
    }

    @Test
    fun `if mapArtDao inserts the map art should save the entity with the moved image path`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        val fakeStaticImage = createFakeStaticImage(fakeDir)
        val fakeMapArt = createFakeMapArt(id = "fake-map-art-id-to-save")
        val mapArtSlot = slot<MapArtEntity>()
        coEvery { mapArtsDao.insert(capture(mapArtSlot)) } just runs

        // When
        repository.saveMapArt(fakeMapArt, fakeStaticImage.path, fakeDir)

        // Then
        val expectedArtImagePath = File(File(fakeDir, "map-arts"), "fake-map-art-id-to-save.png").path
        assertEquals(fakeMapArt.copy(originalMapImageLocation = expectedArtImagePath), mapArtSlot.captured)
        coVerify(exactly = 1) { mapArtsDao.insert(any()) }
    }

    @Test
    fun `if the arts directory already has images should keep them when saving a new map art`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        val fakeArtsDirectory = File(fakeDir, "map-arts").apply { mkdirs() }
        val fakeExistingArtImage = File(fakeArtsDirectory, "fake-existing-art-id.png").apply { writeBytes(byteArrayOf(7, 7)) }
        val fakeStaticImage = createFakeStaticImage(fakeDir)
        coEvery { mapArtsDao.insert(any()) } just runs

        // When
        repository.saveMapArt(createFakeMapArt(id = "fake-new-art-id"), fakeStaticImage.path, fakeDir)

        // Then
        assertArrayEquals(byteArrayOf(7, 7), fakeExistingArtImage.readBytes())
        assertTrue(File(fakeArtsDirectory, "fake-new-art-id.png").exists())
    }

    @Test
    fun `if the static image does not exist should throw LocalStorageErrorFailure without inserting the map art`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        val fakeMissingStaticImagePath = File(fakeDir, "fake-missing-image.png").path
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        assertThrows<LocalStorageErrorFailure> {
            repository.saveMapArt(createFakeMapArt(id = "fake-art-id-without-image"), fakeMissingStaticImagePath, fakeDir)
        }

        // Then
        assertFalse(File(File(fakeDir, "map-arts"), "fake-art-id-without-image.png").exists())
        coVerify(exactly = 0) { mapArtsDao.insert(any()) }
        verify(exactly = 1) {
            errorLogger.logNonFatal(any(), "MartpRepository.moveStaticMapImageToArtsDirectory")
        }
    }

    @Test
    fun `if mapArtDao throws an exception when inserting should throw LocalStorageErrorFailure with the original message`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        val fakeErrorMessage = "fake insert map art error"
        val fakeStaticImage = createFakeStaticImage(fakeDir)
        coEvery { mapArtsDao.insert(any()) } throws IllegalStateException(fakeErrorMessage)
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<LocalStorageErrorFailure> {
            repository.saveMapArt(createFakeMapArt(), fakeStaticImage.path, fakeDir)
        }

        // Then
        assertEquals(fakeErrorMessage, failure.message)
        verify(exactly = 1) { errorLogger.logNonFatal(ofType<IllegalStateException>(), "MartpRepository.saveMapArtEntity") }
    }

    @Test
    fun `if mapArtDao throws an exception without message when inserting should throw LocalStorageErrorFailure with null message`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        val fakeStaticImage = createFakeStaticImage(fakeDir)
        coEvery { mapArtsDao.insert(any()) } throws IllegalStateException()
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        val failure = assertThrows<LocalStorageErrorFailure> {
            repository.saveMapArt(createFakeMapArt(), fakeStaticImage.path, fakeDir)
        }

        // Then
        assertNull(failure.message)
    }

    @Test
    fun `if mapArtDao throws an exception when inserting should delete the moved image`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        val fakeStaticImage = createFakeStaticImage(fakeDir)
        coEvery { mapArtsDao.insert(any()) } throws IllegalStateException("fake insert error to clean up")
        every { errorLogger.logNonFatal(any(), any()) } just runs

        // When
        assertThrows<LocalStorageErrorFailure> {
            repository.saveMapArt(createFakeMapArt(id = "fake-art-id-to-clean-up"), fakeStaticImage.path, fakeDir)
        }

        // Then
        assertFalse(File(File(fakeDir, "map-arts"), "fake-art-id-to-clean-up.png").exists())
        assertFalse(fakeStaticImage.exists())
    }

    @Test
    fun `if inserting the map art is cancelled should rethrow the CancellationException and keep the moved image`(
        @TempDir fakeDir: File
    ) = runTest {
        // Given
        val fakeCancellation = CancellationException("fake insert map art cancellation")
        val fakeStaticImage = createFakeStaticImage(fakeDir)
        coEvery { mapArtsDao.insert(any()) } throws fakeCancellation

        // When
        val thrown = assertThrows<CancellationException> {
            repository.saveMapArt(createFakeMapArt(id = "fake-cancelled-art-id"), fakeStaticImage.path, fakeDir)
        }

        // Then
        assertSame(fakeCancellation, thrown)
        assertTrue(File(File(fakeDir, "map-arts"), "fake-cancelled-art-id.png").exists())
        verify(exactly = 0) { errorLogger.logNonFatal(any(), any()) }
    }

    // endregion

}
