package manoellribeiro.dev.martp.core.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
import manoellribeiro.dev.martp.core.data.local.daos.UserInfoDao
import manoellribeiro.dev.martp.core.data.network.geoapify.GeoapifyApiService
import manoellribeiro.dev.martp.core.data.network.mapbox.MapboxApiService
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.core.services.ConnectivityService
import manoellribeiro.dev.martp.infra.InstantTaskExecutorRuleForJUnit5
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(InstantTaskExecutorRuleForJUnit5::class)
class MartpRepositoryTest {

    private lateinit var repository: MartpRepository
    private lateinit var mapboxApiService: MapboxApiService
    private lateinit var geoapifyApiService: GeoapifyApiService
    private lateinit var mapArtsDao: MapArtsDao
    private lateinit var connectivityService: ConnectivityService
    private lateinit var userInfoDao: UserInfoDao
    private lateinit var artSettingsDataSore: DataStore<Preferences>

    @BeforeEach
    fun setup() {
        mapboxApiService = mockk(relaxed = true)
        mapArtsDao = mockk(relaxed = true)
        connectivityService = mockk(relaxed = true)
        geoapifyApiService = mockk(relaxed = true)
        userInfoDao = mockk(relaxed = true)
        artSettingsDataSore = mockk(relaxed = true)
        repository = MartpRepository(
            mapboxApiService, geoapifyApiService, connectivityService, mapArtsDao, userInfoDao, artSettingsDataSore, UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `observeUserMapArts should forwards the call to mapArtDao and return the value`() = runTest {

        every { mapArtsDao.getAll() } returns flowOf(arrayListOf(mockk(relaxed = true)))

        val result = repository.observeUserMapArts().first()

        verify { mapArtsDao.getAll() }
        assert(result.isNotEmpty())

    }

    @Test
    fun `observeUserMapArts should throw Failure if mapArtDao throws Exception`() = runTest {

        every { mapArtsDao.getAll() } returns flow { throw Exception() }

        try {
            repository.observeUserMapArts().first()
        } catch (e: Exception) {
            assert(e is Failure)
        }
    }

}