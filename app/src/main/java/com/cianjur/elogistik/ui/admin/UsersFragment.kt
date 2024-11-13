package com.cianjur.elogistik.ui.admin

import User
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cianjur.elogistik.databinding.DialogEditUserBinding
import com.cianjur.elogistik.databinding.FragmentUsersBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class UsersFragment : Fragment() {
    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var usersAdapter: UsersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        db = FirebaseFirestore.getInstance()
        setupRecyclerView()
        loadUsers()
    }

    private fun setupRecyclerView() {
        usersAdapter = UsersAdapter(
            onEditClick = { user -> 
                showEditDialog(user)
            },
            onDeleteClick = { user ->
                showDeleteConfirmation(user)
            }
        )
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = usersAdapter
        }
    }

    private fun loadUsers() {
        db.collection("user")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val usersList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                } ?: listOf()

                usersAdapter.setUsers(usersList)
            }
    }

    private fun showEditDialog(user: User) {
        val dialog = AlertDialog.Builder(requireContext())
        val dialogBinding = DialogEditUserBinding.inflate(layoutInflater)
        
        // Pre-fill existing data
        dialogBinding.apply {
            editTextNama.setText(user.nama)
            editTextNik.setText(user.nik)
            editTextPhone.setText(user.phone)
            editTextAlamat.setText(user.alamat)
            
            val userTypes = arrayOf("admin", "petani", "toko")
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, userTypes)
            spinnerUserType.adapter = adapter
            spinnerUserType.setSelection(userTypes.indexOf(user.type))
        }

        dialog.setView(dialogBinding.root)
            .setTitle("Edit User")
            .setPositiveButton("Simpan") { _, _ ->
                val updatedUser = user.copy(
                    nama = dialogBinding.editTextNama.text.toString(),
                    nik = dialogBinding.editTextNik.text.toString(),
                    phone = dialogBinding.editTextPhone.text.toString(),
                    alamat = dialogBinding.editTextAlamat.text.toString(),
                    type = dialogBinding.spinnerUserType.selectedItem.toString(),
                    updatedAt = System.currentTimeMillis()
                )
                updateUser(updatedUser)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateUser(user: User) {
        db.collection("user").document(user.id)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(context, "User berhasil diupdate", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmation(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus User")
            .setMessage("Apakah Anda yakin ingin menghapus user ${user.nama}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteUser(user)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteUser(user: User) {
        // Hapus dari Authentication
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { currentUser ->
            if (currentUser.uid != user.id) { // Mencegah admin menghapus dirinya sendiri
                db.collection("user").document(user.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "User berhasil dihapus", Toast.LENGTH_SHORT).show()
                        
                        // Hapus foto profil jika ada
                        if (user.photoUrl.isNotEmpty()) {
                            val storage = FirebaseStorage.getInstance()
                            val photoRef = storage.getReferenceFromUrl(user.photoUrl)
                            photoRef.delete().addOnFailureListener { e ->
                                Log.e("DeletePhoto", "Error deleting photo: ${e.message}")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "Tidak dapat menghapus akun sendiri", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 