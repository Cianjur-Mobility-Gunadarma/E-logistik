package com.cianjur.elogistik.ui.admin

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cianjur.elogistik.databinding.FragmentCreateUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.cianjur.elogistik.ui.LoginActivity

class CreateUserFragment : Fragment() {
    private var _binding: FragmentCreateUserBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        checkAdminRole()
        setupUI()
    }

    private fun checkAdminRole() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        db.collection("user").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document == null || document.data == null || 
                    document.getString("type") != "admin") {
                    showErrorMessage(Exception("Anda tidak memiliki akses admin"))
                    performLogout()
                }
            }
            .addOnFailureListener { e ->
                showErrorMessage(e)
                performLogout()
            }
    }

    private fun setupUI() {
        val userTypes = arrayOf("petani", "toko", "admin")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, userTypes)
        (binding.spinnerUserType as? AutoCompleteTextView)?.setAdapter(adapter)

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.buttonCreateUser.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()
            val userType = binding.spinnerUserType.text.toString()

            if (validateInput(email, password, userType)) {
                createUser(email, password, userType)
            }
        }
    }

    private fun validateInput(email: String, password: String, userType: String): Boolean {
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email tidak boleh kosong"
            return false
        }
        if (password.isEmpty()) {
            binding.passwordLayout.error = "Password tidak boleh kosong"
            return false
        }
        if (userType.isEmpty()) {
            binding.userTypeLayout.error = "Tipe user harus dipilih"
            return false
        }
        return true
    }

    private fun createUser(email: String, password: String, userType: String) {
        setLoading(true)
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid
                val userData = hashMapOf(
                    "email" to email,
                    "type" to userType,
                    "createdAt" to System.currentTimeMillis(),
                    "photoUrl" to "",
                    "nama" to "",
                    "phone" to "",
                    "alamat" to "",
                    "nik" to ""
                )

                db.collection("user").document(userId!!)
                    .set(userData)
                    .addOnSuccessListener {
                        setLoading(false)
                        showSuccessMessage("User baru berhasil dibuat")
                        clearInputs()
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        showErrorMessage(e)
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                showErrorMessage(e)
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.buttonCreateUser.isEnabled = !isLoading
        binding.editTextEmail.isEnabled = !isLoading
        binding.editTextPassword.isEnabled = !isLoading
        binding.spinnerUserType.isEnabled = !isLoading
    }

    private fun showSuccessMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun showErrorMessage(e: Exception) {
        val message = when {
            e.message?.contains("network") == true -> 
                "Koneksi internet bermasalah. Silakan coba lagi"
            e.message?.contains("password") == true -> 
                "Password harus minimal 6 karakter"
            e.message?.contains("email") == true -> 
                "Format email tidak valid"
            e.message?.contains("already in use") == true -> 
                "Email sudah terdaftar"
            e.message?.contains("permission") == true ->
                "Anda tidak memiliki izin untuk melakukan operasi ini"
            else -> "Terjadi kesalahan: ${e.message}"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun clearInputs() {
        binding.apply {
            editTextEmail.text?.clear()
            editTextPassword.text?.clear()
            spinnerUserType.text?.clear()
            emailLayout.error = null
            passwordLayout.error = null
            userTypeLayout.error = null
        }
    }

    private fun performLogout() {
        try {
            // Nonaktifkan UI terlebih dahulu
            _binding?.let {
                it.buttonCreateUser.isEnabled = false
                it.logoutButton.isEnabled = false
            }
            
            // Clear semua data
            activity?.let { fragmentActivity ->
                fragmentActivity.viewModelStore.clear()
            }
            
            // Logout dari Firebase Auth
            auth.signOut()
            
            // Tampilkan toast berhasil logout
            if (isAdded) {
                Toast.makeText(requireContext(), "Berhasil logout", Toast.LENGTH_SHORT).show()
            }
            
            // Navigasi ke LoginActivity dengan delay singkat
            Handler(Looper.getMainLooper()).postDelayed({
                if (isAdded) {
                    startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    requireActivity().finish()
                }
            }, 500) // Delay 500ms agar toast terlihat
            
        } catch (e: Exception) {
            // Silent catch, langsung navigasi ke login
            auth.signOut()
            if (isAdded) {
                Toast.makeText(requireContext(), "Berhasil logout", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                requireActivity().finish()
            }
        }
    }

    private fun navigateToLogin() {
        if (!isAdded) return  // Cek apakah fragment masih attached
        
        try {
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showLogoutConfirmation() {
        if (!isAdded) return
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                // Langsung logout tanpa menampilkan pesan
                performLogout()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            // Clear semua referensi
            db.clearPersistence()
            _binding = null
        } catch (e: Exception) {
            _binding = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
