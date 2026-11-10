package com.gostream.app.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gostream.app.R
import com.gostream.app.data.model.ProviderItem

class ProviderAdapter(
    private val onProviderSelected: (ProviderItem) -> Unit
) : RecyclerView.Adapter<ProviderAdapter.ViewHolder>() {

    private val items = mutableListOf<ProviderItem>()
    private var selectedIndex = 0

    fun submitList(newItems: List<ProviderItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_provider_chip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position == selectedIndex)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvChipName)

        fun bind(item: ProviderItem, isSelected: Boolean) {
            tvName.text = item.name

            if (isSelected) {
                tvName.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_accent))
                tvName.isSelected = true
            } else {
                tvName.setTextColor(Color.WHITE)
                tvName.isSelected = false
            }

            itemView.setOnClickListener {
                val prev = selectedIndex
                selectedIndex = bindingAdapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(selectedIndex)
                onProviderSelected(item)
            }
        }
    }
}
