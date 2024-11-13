package com.cianjur.elogistik.ui.profile

import User
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cianjur.elogistik.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import android.content.Intent
import android.app.Activity
import android.net.Uri
import com.cianjur.elogistik.R

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1
    private var isEditMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupUI()
        loadProfileData()
        setEditMode(false)

        return binding.root
    }

    private fun setupUI() {
        binding.apply {
            profileImage.setOnClickListener {
                if (isEditMode) openImagePicker()
            }

            btnSimpan.setOnClickListener {
                if (isEditMode) {
                    if (validateInput()) {
                        uploadImageAndSaveData()
                        setEditMode(false)
                    }
                } else {
                    setEditMode(true)
                }
            }
        }
    }

    private fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        binding.apply {
            nikInput.isEnabled = enabled
            namaInput.isEnabled = enabled
            alamatInput.isEnabled = enabled
            profileImage.isClickable = enabled
            
            btnSimpan.text = if (enabled) "Simpan Perubahan" else "Edit Profil"

            // Set background warna untuk mode tampilan
            nikInput.setBackgroundResource(if (enabled) R.drawable.edit_text_background else R.drawable.view_background)
            namaInput.setBackgroundResource(if (enabled) R.drawable.edit_text_background else R.drawable.view_background)
            alamatInput.setBackgroundResource(if (enabled) R.drawable.edit_text_background else R.drawable.view_background)
        }
    }

    private fun openImagePicker() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(Intent.createChooser(intent, "Pilih Foto"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            selectedImageUri = data.data
            binding.profileImage.setImageURI(selectedImageUri)
        }
    }

    private fun uploadImageAndSaveData() {
        val userId = auth.currentUser?.uid ?: return

        if (selectedImageUri != null) {
            val ref = storage.reference.child("profile_images/$userId.jpg")

            ref.putFile(selectedImageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    ref.downloadUrl.addOnSuccessListener { uri ->
                        saveProfileData(uri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Gagal mengupload foto", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveProfileData(null)
        }
    }

    private fun saveProfileData(photoUrl: String?) {
        val userId = auth.currentUser?.uid ?: return
        val user = User(
            id = userId,
            nik = binding.nikInput.text.toString(),
            nama = binding.namaInput.text.toString(),
            alamat = binding.alamatInput.text.toString(),
            photoUrl = photoUrl ?: "",
            updatedAt = System.currentTimeMillis()
        )

        firestore.collection("user")
            .document(userId)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(context, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
                loadProfileData()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProfileData() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("user")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    user?.let { updateUI(it) }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI(user: User) {
        binding.apply {
            welcomeText.text = "Halo, ${user.nama}"
            nikInput.setText(user.nik)
            namaInput.setText(user.nama)
            alamatInput.setText(user.alamat)

            if (user.photoUrl.isNotEmpty()) {
                Glide.with(this@ProfileFragment)
                    .load(user.photoUrl)
                    .placeholder(R.drawable.default_profile)
                    .into(profileImage)
            }
        }
    }

    private fun validateInput(): Boolean {
        var isValid = true
        
        if (binding.nikInput.text.toString().isEmpty()) {
            binding.nikLayout.error = "NIK tidak boleh kosong"
            isValid = false
        } else if (binding.nikInput.text.toString().length != 16) {
            binding.nikLayout.error = "NIK harus 16 digit"
            isValid = false
        } else {
            binding.nikLayout.error = null
        }

        if (binding.namaInput.text.toString().isEmpty()) {
            binding.namaLayout.error = "Nama tidak boleh kosong"
            isValid = false
        } else {
            binding.namaLayout.error = null
        }

        if (binding.alamatInput.text.toString().isEmpty()) {
            binding.alamatLayout.error = "Alamat tidak boleh kosong"
            isValid = false
        } else {
            binding.alamatLayout.error = null
        }

        return isValid
    }
}