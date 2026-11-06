package manoellribeiro.dev.martp.scenes.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.extensions.dp
import manoellribeiro.dev.martp.core.extensions.gone
import manoellribeiro.dev.martp.core.extensions.shareArtImage
import manoellribeiro.dev.martp.core.extensions.visible
import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.databinding.FragmentGalleryBinding
import manoellribeiro.dev.martp.scenes.main.MainViewModel

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
        setupViews()
        return binding.root
    }

    private fun setupViews() = with(binding) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            galleryHeaderLL.updatePadding(top = HEADER_TOP_PADDING_DP.dp(resources) + insets.top)
            statusScrimV.updateLayoutParams {
                height = insets.top + STATUS_SCRIM_FADE_DP.dp(resources)
            }
            WindowInsetsCompat.CONSUMED
        }
        galleryHeaderABL.addOnOffsetChangedListener { _, verticalOffset ->
            val scrimFadeDistance = STATUS_SCRIM_FADE_DP.dp(resources).toFloat()
            statusScrimV.alpha = (-verticalOffset / scrimFadeDistance).coerceIn(0f, 1f)
        }
    }

    private fun setupObservables() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.galleryState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .distinctUntilChanged()
                .collect { state ->
                    when(state) {
                        GalleryUiState.EmptyList -> setupEmptyListState()
                        is GalleryUiState.Error -> setupErrorState(state.failure)
                        GalleryUiState.Loading -> setupLoadingState()
                        is GalleryUiState.NotEmptyList -> setupNotEmptyListState(state.mapArts)
                    }
                }
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
            onShareButtonClickListener = { imagePath -> activity?.shareArtImage(imagePath) }
        )
    }

    private fun setupLoadingState() = with(binding) {
        galleryHeaderABL.setExpanded(true, false)
        mapArtsRV.gone()
        emptyListTV.gone()
        errorTV.gone()
        emptyGalleryImageIV.gone()
        loadingIndicatorPB.visible()
    }

    private fun setupErrorState(failure: Failure) = with(binding) {
        galleryHeaderABL.setExpanded(true, false)
        mapArtsRV.gone()
        emptyListTV.gone()
        errorTV.visible()
        emptyGalleryImageIV.gone()
        errorTV.text = getString(failure.messageToBeDisplayedToUserId)
        loadingIndicatorPB.gone()
    }

    private fun setupEmptyListState() = with(binding) {
        galleryHeaderABL.setExpanded(true, false)
        mapArtsRV.gone()
        emptyListTV.visible()
        emptyGalleryImageIV.visible()
        errorTV.gone()
        loadingIndicatorPB.gone()
    }

    private companion object {
        const val HEADER_TOP_PADDING_DP = 16
        const val STATUS_SCRIM_FADE_DP = 24
    }
}