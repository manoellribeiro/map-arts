package manoellribeiro.dev.martp.core.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.coVerify
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
    fun `fetchUserMapArts should forwards the call to mapArtDao and return the value`() = runTest {

        coEvery { mapArtsDao.getAll() } returns arrayListOf(mockk(relaxed = true))

        val result = repository.fetchUserMapArts()

        coVerify { mapArtsDao.getAll() }
        assert(result.isNotEmpty())

    }

    @Test
    fun `fetchUserMapArts should throw Failure if mapArtDao throws Exception`() = runTest {

        coEvery { mapArtsDao.getAll() } throws Exception()

        try {
            repository.fetchUserMapArts()
        } catch (e: Exception) {
            assert(e is Failure)
        }
    }

}