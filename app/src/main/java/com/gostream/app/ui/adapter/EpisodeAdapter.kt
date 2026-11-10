package com.gostream.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gostream.app.R
import com.gostream.app.data.model.EpisodeItem

class EpisodeAdapter(
    private val onEpisodeClick: (EpisodeItem) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.ViewHolder>() {

    private val items = mutableListOf<EpisodeItem>()

    fun submitList(newItems: List<EpisodeItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_episode_chip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvEpisodeName)

        fun bind(item: EpisodeItem) {
            tvName.text = "Episode ${item.episodeNumber}"
            itemView.setOnClickListener {
                onEpisodeClick(item)
            }
        }
    }
}
