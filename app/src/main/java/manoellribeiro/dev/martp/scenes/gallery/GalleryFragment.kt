package manoellribeiro.dev.martp.scenes.gallery

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.extensions.gone
import manoellribeiro.dev.martp.core.extensions.visible
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.databinding.FragmentGalleryBinding
import manoellribeiro.dev.martp.scenes.main.MainViewModel
import java.io.File

@AndroidEntryPoint
class GalleryFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentGalleryBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentGalleryBinding.inflate(layoutInflater)
        setupObservables()
        viewModel.getUserMapArts()
        return binding.root
    }

    private fun setupObservables() {
        viewModel.galleryState.observe(viewLifecycleOwner) { state ->
            when(state) {
                GalleryUiState.EmptyList -> setupEmptyListState()
                is GalleryUiState.Error -> setupErrorState(state.failure)
                GalleryUiState.Loading -> setupLoadingState()
                is GalleryUiState.NotEmptyList -> setupNotEmptyListState(state.mapArts)
            }
        }
    }

    private fun shareArtImage(imagePathLocation: String) {
        activity?.applicationContext?.let {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            sharingIntent.type = "image/*"
            val uri = FileProvider.getUriForFile(
                it,
                it.packageName + ".provider",
                File(imagePathLocation)
            )
            sharingIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_art_message))
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
            it.startActivities(arrayOf(Intent.createChooser(sharingIntent, "Share with")))
        }
    }

    private fun setupNotEmptyListState(mapArts: List<MapArtEntity>) = with(binding) {
        emptyListTV.gone()
        errorTV.gone()
        emptyGalleryImageIV.gone()
        loadingIndicatorPB.gone()
        mapArtsRV.visible()
        mapArtsRV.layoutManager = LinearLayoutManager(activity)
        mapArtsRV.adapter = MapArtsRecyclerViewAdapter(
            mapArts = mapArts,
            onClickListener = { mapArt ->  },
            onShareButtonClickListener = { imagePath -> shareArtImage(imagePath)}
        )
    }

    private fun setupLoadingState() = with(binding) {
        mapArtsRV.gone()
        emptyListTV.gone()
        errorTV.gone()
        emptyGalleryImageIV.gone()
        loadingIndicatorPB.visible()
    }

    private fun setupErrorState(failure: Failure) = with(binding) {
        mapArtsRV.gone()
        emptyListTV.gone()
        errorTV.visible()
        emptyGalleryImageIV.gone()
        errorTV.text = getString(failure.messageToBeDisplayedToUserId)
        loadingIndicatorPB.gone()
    }

    private fun setupEmptyListState() = with(binding) {
        mapArtsRV.gone()
        emptyListTV.visible()
        emptyGalleryImageIV.visible()
        errorTV.gone()
        loadingIndicatorPB.gone()
    }
}