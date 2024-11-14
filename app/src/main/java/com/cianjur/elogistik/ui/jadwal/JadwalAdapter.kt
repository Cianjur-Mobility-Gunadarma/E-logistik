package com.cianjur.elogistik.ui.jadwal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cianjur.elogistik.databinding.ItemJadwalBinding

class JadwalAdapter(private val onItemClick: (String) -> Unit) : 
    ListAdapter<PesananItem, JadwalAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemJadwalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemJadwalBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position).id)
                }
            }
        }

        fun bind(item: PesananItem) {
            binding.apply {
                todoTitle.text = item.title
                todoDescription.text = item.description
                todoDate.text = item.date
                todoCategory.text = item.status
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PesananItem>() {
        override fun areItemsTheSame(oldItem: PesananItem, newItem: PesananItem) = 
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PesananItem, newItem: PesananItem) = 
            oldItem == newItem
    }
}

data class PesananItem(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val status: String
) 