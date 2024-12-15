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
import androidx.activity.OnBackPressedCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.cianjur.elogistik.ui.LoginActivity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.view.inputmethod.EditorInfo


class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1
    private var isEditMode = false
    private var backPressedCallback: OnBackPressedCallback? = null
    private val PICK_LOCATION_REQUEST = 1001

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
            phoneInput.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && phoneInput.text?.toString().isNullOrEmpty()) {
                    phoneInput.setText("62")
                    phoneInput.text?.let { editable ->
                        phoneInput.setSelection(editable.length)
                    }
                }
            }

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

            logoutButton.setOnClickListener {
                showLogoutConfirmation()
            }

            // Tambahkan ini untuk menyembunyikan keyboard saat klik di luar input
            root.setOnClickListener {
                hideKeyboard()
            }

            // Tambahkan listener untuk setiap EditText
            nikInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard()
                    true
                } else false
            }
            
            namaInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard()
                    true
                } else false
            }
            
            phoneInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard()
                    true
                } else false
            }
            
            alamatInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard()
                    true
                } else false
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        binding.apply {
            nikInput.isEnabled = enabled
            namaInput.isEnabled = enabled
            phoneInput.isEnabled = enabled
            alamatInput.isEnabled = enabled
            jenisBarangInput.isEnabled = enabled
            profileImage.isClickable = enabled
            
            btnSimpan.text = if (enabled) "Simpan Perubahan" else "Edit Profil"

            // Set background warna untuk mode tampilan
            nikInput.setBackgroundResource(if (enabled) R.drawable.edit_text_background else R.drawable.view_background)
            namaInput.setBackgroundResource(if (enabled) R.drawable.edit_text_background else R.drawable.view_background)
            phoneInput.setBackgroundResource(if (enabled) R.drawable.edit_text_background else R.drawable.view_background)
            alamatInput.setBackgroundResource(if (enabled) R.drawable.edit_text_background else R.drawable.view_background)
            jenisBarangInput.setBackgroundResource(if (enabled) R.drawable.edit_text_background else R.drawable.view_background)
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
        } else if (requestCode == PICK_LOCATION_REQUEST && resultCode == Activity.RESULT_OK) {
            val address = data?.getStringExtra("address")
            binding.alamatInput.setText(address)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.apply {
            loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnSimpan.isEnabled = !isLoading
            // Disable semua input saat loading
            profileImage.isEnabled = !isLoading
            nikInput.isEnabled = !isLoading
            namaInput.isEnabled = !isLoading
            phoneInput.isEnabled = !isLoading
            alamatInput.isEnabled = !isLoading
            jenisBarangInput.isEnabled = !isLoading
        }


        if (isLoading) {

            if (backPressedCallback == null) {
                backPressedCallback = object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
        
                    }
                }
                requireActivity().onBackPressedDispatcher.addCallback(
                    viewLifecycleOwner,
                    backPressedCallback!!
                )
            }
        } else {
            // Hapus callback jika loading selesai
            backPressedCallback?.remove()
            backPressedCallback = null
        }
    }

    private fun uploadImageAndSaveData() {
        val userId = auth.currentUser?.uid ?: return

        if (selectedImageUri != null) {
            setLoading(true) // Tampilkan loading overlay

            val ref = storage.reference.child("profile_images/$userId/profile.jpg")

            ref.putFile(selectedImageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    ref.downloadUrl.addOnSuccessListener { uri ->
                        saveProfileData(uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    setLoading(false) // Sembunyikan loading overlay
                    Toast.makeText(context, "Gagal mengupload foto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    binding.progressBar.progress = progress.toInt()
                }
        } else {
            saveProfileData(null)
        }
    }

    private fun saveProfileData(photoUrl: String?) {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("user")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val existingUser = document.toObject(User::class.java)
                
                val updates = hashMapOf<String, Any>(
                    "nik" to binding.nikInput.text.toString(),
                    "nama" to binding.namaInput.text.toString(),
                    "phone" to binding.phoneInput.text.toString(),
                    "alamat" to binding.alamatInput.text.toString(),
                    "updatedAt" to System.currentTimeMillis()
                )

                if (existingUser?.type == "toko") {
                    updates["jenisBarang"] = binding.jenisBarangInput.text.toString()
                }

                if (photoUrl != null) {
                    updates["photoUrl"] = photoUrl
                }

                existingUser?.type?.let {
                    updates["type"] = it
                }

                firestore.collection("user")
                    .document(userId)
                    .update(updates)
                    .addOnSuccessListener {
                        setLoading(false) // Sembunyikan loading overlay
                        Toast.makeText(context, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
                        loadProfileData()
                        setEditMode(false)
                    }
                    .addOnFailureListener { e ->
                        setLoading(false) // Sembunyikan loading overlay
                        Toast.makeText(context, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
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
            phoneInput.setText(user.phone)
            alamatInput.setText(user.alamat)

            if (user.photoUrl.isNotEmpty()) {
                Glide.with(this@ProfileFragment)
                    .load(user.photoUrl)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.default_profile)
            }

            if (user.type == "toko") {
                jenisBarangLayout.visibility = View.VISIBLE
                jenisBarangHint.visibility = View.VISIBLE
                jenisBarangInput.setText(user.jenisBarang)
            } else {
                jenisBarangLayout.visibility = View.GONE
                jenisBarangHint.visibility = View.GONE
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

        if (binding.phoneInput.text.toString().isEmpty()) {
            binding.phoneLayout.error = "Nomor HP tidak boleh kosong"
            isValid = false
        } else if (binding.phoneInput.text.toString().length < 10) {
            binding.phoneLayout.error = "Nomor HP tidak valid"
            isValid = false
        } else {
            binding.phoneLayout.error = null
        }

        if (binding.alamatInput.text.toString().isEmpty()) {
            binding.alamatLayout.error = "Alamat tidak boleh kosong"
            isValid = false
        } else {
            binding.alamatLayout.error = null
        }

        return isValid
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun performLogout() {
        auth.signOut()
        // Redirect ke LoginActivity
        startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        backPressedCallback?.remove()
        backPressedCallback = null
        _binding = null
    }
}