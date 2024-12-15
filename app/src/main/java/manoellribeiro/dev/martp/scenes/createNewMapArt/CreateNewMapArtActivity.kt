package manoellribeiro.dev.martp.scenes.createNewMapArt

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.core.sketches.DefaultMartpSketch
import manoellribeiro.dev.martp.databinding.ActivityCreateNewMapArtBinding
import manoellribeiro.dev.martp.scenes.gallery.GalleryViewModel
import processing.android.PFragment

@AndroidEntryPoint
class CreateNewMapArtActivity: AppCompatActivity() {

    private lateinit var binding: ActivityCreateNewMapArtBinding
    private val viewModel: CreateNewMapArtViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateNewMapArtBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupObservables()
    }

    override fun onResume() {
        super.onResume()
        binding.mapArtsContainer.doOnLayout {
            lifecycleScope.launch {
                viewModel.startToGenerateMapArt(
                    directory = this@CreateNewMapArtActivity.filesDir,
                    canvasToDrawArtWidth = binding.mapArtsContainer.width,
                    canvasToDrawArtHeight = binding.mapArtsContainer.height
                )
            }
        }
    }

    private fun setupObservables() {
        viewModel.state.observe(this@CreateNewMapArtActivity) { state ->
            when(state) {
                is CreateNewMapArtUiState.Error -> {

                }
                is CreateNewMapArtUiState.ImageDownloaded -> handleImageDownloadedState(state.imagePath)
                CreateNewMapArtUiState.Loading -> {

                }
            }
        }
    }

    private fun handleImageDownloadedState(imagePath: String) {
        val sketch = DefaultMartpSketch(
            horizontalTilesCount = 0,
            verticalTilesCount = 0,
            padding = 0,
            canvasWidth = binding.mapArtsContainer.width.toFloat(),
            canvasHeight = binding.mapArtsContainer.height.toFloat(),
            imagePath = imagePath
        )

        val processingFragment = PFragment(sketch)
        processingFragment.setView(binding.mapArtsContainer, this@CreateNewMapArtActivity)
    }
}