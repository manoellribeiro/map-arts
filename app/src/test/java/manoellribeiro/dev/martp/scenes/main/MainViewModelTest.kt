package manoellribeiro.dev.martp.scenes.main

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import manoellribeiro.dev.martp.infra.InstantTaskExecutorRuleForJUnit5
import manoellribeiro.dev.martp.scenes.gallery.GalleryUiState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(InstantTaskExecutorRuleForJUnit5::class)
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var repository: MartpRepository

    @BeforeEach
    fun setup() {
        repository = mockk(relaxed = true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.collectGalleryStates(): List<GalleryUiState> {
        viewModel = MainViewModel(
            repository = repository
        )
        val states = mutableListOf<GalleryUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.galleryState.toList(states)
        }
        return states
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `if repository returns an empty list should emit EmptyList ui state`() = runTest {
        every { repository.observeUserMapArts() } returns flowOf(arrayListOf())

        val states = collectGalleryStates()

        advanceUntilIdle()

        assertEquals(
            listOf(GalleryUiState.Loading, GalleryUiState.EmptyList),
            states
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `if repository throws an exception should emit Error ui state`() = runTest {

        every { repository.observeUserMapArts() } returns flow { throw LocalStorageErrorFailure("error") }

        val states = collectGalleryStates()

        advanceUntilIdle()

        assertEquals(2, states.size)
        assertEquals(GalleryUiState.Loading, states[0])
        assert(states[1] is GalleryUiState.Error)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `if repository returns not empty list should emit NotEmptyList ui state`() = runTest {

        every { repository.observeUserMapArts() } returns flowOf(arrayListOf(mockk(relaxed = true)))

        val states = collectGalleryStates()

        advanceUntilIdle()

        assertEquals(2, states.size)
        assertEquals(GalleryUiState.Loading, states[0])
        assert(states[1] is GalleryUiState.NotEmptyList)
    }

}