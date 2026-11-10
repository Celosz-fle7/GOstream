package com.gostream.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.gostream.app.R
import com.gostream.app.data.model.VideoItem

class TvVideoAdapter(
    private val onItemClick: (VideoItem) -> Unit,
    private val onItemFocused: (VideoItem) -> Unit
) : RecyclerView.Adapter<TvVideoAdapter.ViewHolder>() {

    private val items = mutableListOf<VideoItem>()

    fun submitList(newItems: List<VideoItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_card_tv, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPoster: ImageView = itemView.findViewById(R.id.ivTvPoster)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTvTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvTvSubtitle)

        fun bind(item: VideoItem) {
            tvTitle.text = item.title
            tvSubtitle.text = item.status ?: "Tersedia"

            ivPoster.load(item.posterUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_card_focus)
                error(R.drawable.bg_card_focus)
            }

            itemView.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    // Android TV 10-foot smooth pop-out animation
                    view.animate()
                        .scaleX(1.08f)
                        .scaleY(1.08f)
                        .translationZ(8f)
                        .setDuration(180)
                        .start()

                    onItemFocused(item)
                } else {
                    view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .translationZ(0f)
                        .setDuration(180)
                        .start()
                }
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
