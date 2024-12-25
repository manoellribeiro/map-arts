package manoellribeiro.dev.martp.scenes.gallery

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.extensions.formatDateHour
import manoellribeiro.dev.martp.core.extensions.gone
import manoellribeiro.dev.martp.core.extensions.visible
import manoellribeiro.dev.martp.databinding.ItemGalleryArtBinding
import java.util.Calendar

class MapArtsRecyclerViewAdapter(
    private val mapArts: List<MapArtEntity>,
    private val onClickListener: (mapArt: MapArtEntity) -> Unit // we will have one screen with all the details of the art
): RecyclerView.Adapter<MapArtsRecyclerViewAdapter.MapArtViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapArtViewHolder {
        return MapArtViewHolder(
            ItemGalleryArtBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = mapArts.size

    override fun onBindViewHolder(holder: MapArtViewHolder, position: Int) {
        val mapArt = mapArts[position]
        with(holder.binding) {
            val artBitmap = BitmapFactory.decodeFile(mapArt.imagePathLocation)
            artMiniatureIV.setImageBitmap(artBitmap)
            artTitleTV.text = mapArt.title
            if(mapArt.description.isNullOrEmpty()) {
                artDescriptionTV.gone()
            } else {
                artDescriptionTV.visible()
                artDescriptionTV.text = mapArt.description
            }
            artDescriptionTV.text = mapArt.description
            dateTV.text = Calendar.getInstance().apply { timeInMillis = mapArt.dateInMillis }.formatDateHour()
            root.setOnClickListener { onClickListener.invoke(mapArt) }
        }
    }


    class MapArtViewHolder(val binding: ItemGalleryArtBinding): RecyclerView.ViewHolder(binding.root)
}