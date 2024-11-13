package com.cianjur.elogistik.ui.admin

import User
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cianjur.elogistik.R
import com.cianjur.elogistik.databinding.ItemUserBinding

class UsersAdapter(
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {
    
    private var users = listOf<User>()

    fun setUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    inner class UserViewHolder(private val binding: ItemUserBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(user: User) {
            binding.apply {
                namaUser.text = user.nama
                nikUser.text = "NIK: ${user.nik}"
                phoneUser.text = "Telp: ${user.phone}"
                tipeUser.text = user.type

                // Load image using Glide
                if (user.photoUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(user.photoUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(imageUser)
                } else {
                    imageUser.setImageResource(R.drawable.ic_profile)
                }

                btnEdit.setOnClickListener { onEditClick(user) }
                btnDelete.setOnClickListener { onDeleteClick(user) }
            }
        }
    }
} 