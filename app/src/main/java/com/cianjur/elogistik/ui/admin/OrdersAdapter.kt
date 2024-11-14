package com.cianjur.elogistik.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cianjur.elogistik.databinding.ItemPesananTokoBinding
import com.cianjur.elogistik.ui.jadwal.PesananItem

class OrdersAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<PesananItem, OrdersAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPesananTokoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
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
                todoCategory.text = item.status
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PesananItem>() {
            override fun areItemsTheSame(oldItem: PesananItem, newItem: PesananItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PesananItem, newItem: PesananItem): Boolean {
                return oldItem == newItem
            }
        }
    }
} 