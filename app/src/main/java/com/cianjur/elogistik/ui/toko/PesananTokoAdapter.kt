package com.cianjur.elogistik.ui.toko

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cianjur.elogistik.R
import com.cianjur.elogistik.databinding.ItemPesananTokoBinding
import com.cianjur.elogistik.ui.jadwal.PesananItem

class PesananTokoAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<PesananItem, PesananTokoAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPesananTokoBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemPesananTokoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
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
                todoCategory.text = when(item.status) {
                    "menunggu" -> "Menunggu"
                    "diproses" -> "Diproses"
                    "dikirim" -> "Dikirim"
                    "selesai" -> "Selesai"
                    "dibatalkan" -> "Dibatalkan"
                    else -> item.status
                }

                // Set warna status berdasarkan statusnya
                val color = when(item.status) {
                    "menunggu" -> R.color.status_waiting
                    "diproses" -> R.color.status_process
                    "dikirim" -> R.color.status_shipped
                    "selesai" -> R.color.status_completed
                    "dibatalkan" -> R.color.status_cancelled
                    else -> R.color.primary
                }
                todoCategory.setBackgroundColor(itemView.context.getColor(color))
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