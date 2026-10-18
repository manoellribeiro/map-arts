package manoellribeiro.dev.martp.scenes.gallery

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.extensions.formatDateHour
import manoellribeiro.dev.martp.core.extensions.gone
import manoellribeiro.dev.martp.core.extensions.visible
import manoellribeiro.dev.martp.core.models.SketchArtType
import manoellribeiro.dev.martp.core.services.sensors.GravitySensor
import manoellribeiro.dev.martp.core.sketches.ColorfulMapSketch
import manoellribeiro.dev.martp.core.sketches.DefaultMartpSketch
import manoellribeiro.dev.martp.core.sketches.MartpSketch
import manoellribeiro.dev.martp.core.sketches.ParticleSystemSketch
import manoellribeiro.dev.martp.core.sketches.PointillismMartpSketch
import manoellribeiro.dev.martp.core.sketches.WaterFlowMartpSketch
import manoellribeiro.dev.martp.core.utils.ColorPaletteProvider
import manoellribeiro.dev.martp.databinding.ItemGalleryArtBinding
import processing.android.PFragment
import java.util.Calendar

class MapArtsRecyclerViewAdapter(
    private val fragmentManager: FragmentManager,
    private val onClickListener: (mapArt: MapArtEntity) -> Unit, // maybe we will have one screen with all the details of the art
    private val onShareButtonClickListener: (imagePathLocation: String) -> Unit
): ListAdapter<MapArtEntity, MapArtsRecyclerViewAdapter.MapArtViewHolder>(MapArtDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapArtViewHolder {
        val binding = ItemGalleryArtBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        // Every item inflates the same layout, so without a unique id all the sketches
        // would be added to the first mapArtsContainer found in the view tree
        binding.mapArtsContainer.id = View.generateViewId()
        return MapArtViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MapArtViewHolder, position: Int) {
        val mapArt = getItem(position)
        with(holder.binding) {

            val isOldEntitySavedAsBitmap = mapArt.imagePathLocation.isNotEmpty() && mapArt.sketchArtType == null
            val isNewEntityToBeRenderedWithProcessing = !mapArt.originalMapImageLocation.isNullOrEmpty() && mapArt.sketchArtType != null
            when {
                isOldEntitySavedAsBitmap -> {
                    holder.renderableMapArt = null
                    mapArtsContainer.gone()
                    artMiniatureCV.visible()
                    artMiniatureIV.visible()
                    val artBitmap = BitmapFactory.decodeFile(mapArt.imagePathLocation)
                    artMiniatureIV.setImageBitmap(artBitmap)
                    artTitleTV.text = mapArt.title
                    shareFAB.setOnClickListener {
                        onShareButtonClickListener.invoke(mapArt.imagePathLocation)
                    }
                    dateTV.text = Calendar.getInstance().apply { timeInMillis = mapArt.dateInMillis }.formatDateHour()
                    root.setOnClickListener { onClickListener.invoke(mapArt) }
                }

                isNewEntityToBeRenderedWithProcessing -> {
                    mapArtsContainer.visible()
                    artMiniatureCV.gone()
                    artMiniatureIV.gone()

                    // The sketch is attached in onViewAttachedToWindow, when the container is on screen
                    holder.renderableMapArt = mapArt
                    // A changed item can be rebound while it is still on screen
                    if (holder.itemView.isAttachedToWindow) attachSketch(holder)

                    artTitleTV.text = mapArt.title
                    shareFAB.setOnClickListener {
                        //onShareButtonClickListener.invoke(mapArt.imagePathLocation)
                    }
                    dateTV.text = Calendar.getInstance().apply { timeInMillis = mapArt.dateInMillis }.formatDateHour()
                    root.setOnClickListener { onClickListener.invoke(mapArt) }
                }
            }

        }
    }

    /**
     * The sketch fragment is only kept while the item is attached to the window. RecyclerView
     * binds items that are not in the view tree (prefetching, view cache), and a fragment
     * transaction executed while its container is detached crashes with "No view found for id".
     */
    override fun onViewAttachedToWindow(holder: MapArtViewHolder) {
        super.onViewAttachedToWindow(holder)
        attachSketch(holder)
    }

    private fun attachSketch(holder: MapArtViewHolder) {
        val mapArt = holder.renderableMapArt ?: return
        val container = holder.binding.mapArtsContainer
        // The container only has its real size after the layout pass
        container.doOnLayout {
            // Attaching happens in the middle of the RecyclerView layout pass, where the
            // requestLayout of the sketch's SurfaceView is ignored and it would stay 0x0 (gray).
            // Posting runs the transaction right after this layout pass finishes.
            container.post {
                val isStillShowingThisArt = container.isAttachedToWindow && holder.renderableMapArt === mapArt
                if (!isStillShowingThisArt || fragmentManager.isDestroyed) return@post
                fragmentManager.beginTransaction()
                    .replace(container.id, PFragment(createSketch(mapArt, container)))
                    // Executed right away, while the container is surely in the view tree
                    .commitNowAllowingStateLoss()
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: MapArtViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (fragmentManager.isDestroyed) return
        fragmentManager.findFragmentById(holder.binding.mapArtsContainer.id)?.let { sketchFragment ->
            fragmentManager.beginTransaction()
                .remove(sketchFragment)
                .commitNowAllowingStateLoss()
        }
    }

    private fun createSketch(mapArt: MapArtEntity, container: View): MartpSketch {
        val canvasWidth = container.width.toFloat()
        val canvasHeight = container.height.toFloat()
        val imagePath = mapArt.originalMapImageLocation.orEmpty()
        return when(mapArt.sketchArtType) {
            SketchArtType.POINTILLISM -> PointillismMartpSketch(
                horizontalTilesCount = 0,
                verticalTilesCount = 0,
                padding = 0,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                imagePath = imagePath,
            )
            SketchArtType.WATER_FLOW -> WaterFlowMartpSketch(
                padding = 20,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                imagePath = imagePath,
            )
            SketchArtType.PARTICLE_SYSTEM -> ParticleSystemSketch(
                padding = 20,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                imagePath = imagePath,
                gravitySensor = GravitySensor(context = container.context)
            )
            SketchArtType.COLORFUL_MAP -> ColorfulMapSketch(
                padding = 20,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                imagePath = imagePath,
                colorPalette = ColorPaletteProvider.byNameOrDefault(mapArt.colorPalette)
            ) {

            }
            SketchArtType.DEFAULT, null -> DefaultMartpSketch(
                horizontalTilesCount = 4,
                verticalTilesCount = 4,
                padding = 20,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                imagePath = imagePath,
            )
        }
    }

    class MapArtViewHolder(val binding: ItemGalleryArtBinding): RecyclerView.ViewHolder(binding.root) {
        // The art to render with a sketch when the item is attached, or null for old bitmap arts
        var renderableMapArt: MapArtEntity? = null
    }

    private object MapArtDiffCallback : DiffUtil.ItemCallback<MapArtEntity>() {
        override fun areItemsTheSame(oldItem: MapArtEntity, newItem: MapArtEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MapArtEntity, newItem: MapArtEntity): Boolean =
            oldItem == newItem
    }
}