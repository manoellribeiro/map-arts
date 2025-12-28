package manoellribeiro.dev.martp.core.data.repositories

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
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
    private lateinit var mapArtsDao: MapArtsDao
    private lateinit var connectivityService: ConnectivityService

    @BeforeEach
    fun setup() {
        mapboxApiService = mockk(relaxed = true)
        mapArtsDao = mockk(relaxed = true)
        connectivityService = mockk(relaxed = true)
        repository = MartpRepository(
            mapboxApiService, connectivityService, mapArtsDao
        )
    }

    @Test
    fun `fetchUserMapArts should forwards the call to mapArtDao and return the value`() = runTest {

        every { mapArtsDao.getAll() } returns arrayListOf(mockk(relaxed = true))

        val result = repository.fetchUserMapArts().await()

        verify { mapArtsDao.getAll() }
        assert(result.isNotEmpty())

    }

    @Test
    fun `fetchUserMapArts should throw Failure if mapArtDao throws Exception`() = runTest {

        every { mapArtsDao.getAll() } throws Exception()

        try {
            repository.fetchUserMapArts().await()
        } catch (e: Exception) {
            assert(e is Failure)
        }
    }

}