package manoellribeiro.dev.martp.scenes.main

import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import manoellribeiro.dev.martp.core.data.repositories.MartpRepository
import manoellribeiro.dev.martp.core.models.failures.LocalStorageErrorFailure
import manoellribeiro.dev.martp.infra.InstantTaskExecutorRuleForJUnit5
import manoellribeiro.dev.martp.scenes.gallery.GalleryUiState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(InstantTaskExecutorRuleForJUnit5::class)
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var repository: MartpRepository

    private lateinit var observer: Observer<GalleryUiState>


    @BeforeEach
    fun setup() {
        repository = mockk(relaxed = true)
        viewModel = MainViewModel(
            repository = repository
        )
        observer =  mockk<Observer<GalleryUiState>>(relaxed = true)
        viewModel.galleryState.observeForever(observer)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `if repository returns an empty list should emit EmptyList ui state`() = runTest {
        coEvery { repository.fetchUserMapArts() } returns arrayListOf()

        viewModel.getUserMapArts()

        advanceUntilIdle()

        verifySequence {
            observer.onChanged(GalleryUiState.Loading)
            observer.onChanged(GalleryUiState.EmptyList)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `if repository throws an exception should emit Error ui state`() = runTest {

        coEvery { repository.fetchUserMapArts() } throws LocalStorageErrorFailure("error")

        viewModel.getUserMapArts()

        advanceUntilIdle()

        verifySequence {
            observer.onChanged(GalleryUiState.Loading)
            observer.onChanged(any<GalleryUiState.Error>())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `if repository returns not empty list should emit NotEmptyList ui state`() = runTest {

        coEvery { repository.fetchUserMapArts() } returns arrayListOf(mockk(relaxed = true))

        viewModel.getUserMapArts()

        advanceUntilIdle()

        verifySequence {
            observer.onChanged(GalleryUiState.Loading)
            observer.onChanged(any<GalleryUiState.NotEmptyList>())
        }
    }

}