package com.cianjur.elogistik.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cianjur.elogistik.R
import com.cianjur.elogistik.databinding.ActivityRegisterBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupUserTypeSelection()
        setupRegisterButton()
    }

    private fun setupUserTypeSelection() {
        binding.userTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.petaniRadio -> {
                    userType = "petani"
                    showPetaniForm()
                }
                R.id.tokoRadio -> {
                    userType = "toko"
                    showTokoForm()
                }
            }
        }
    }

    private fun setupPhoneInput() {
        val phoneInput = binding.root.findViewById<TextInputEditText>(R.id.phoneInput)
        phoneInput?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && phoneInput.text?.toString().isNullOrEmpty()) {
                phoneInput.setText("62")
                phoneInput.setSelection(2) // Kursor di akhir "62"
            }
        }
    }

    private fun showPetaniForm() {
        // Inflater form petani
        binding.dynamicFormContainer.removeAllViews()
        layoutInflater.inflate(R.layout.form_petani, binding.dynamicFormContainer)
        setupPhoneInput()
    }

    private fun showTokoForm() {
        // Inflater form toko
        binding.dynamicFormContainer.removeAllViews()
        layoutInflater.inflate(R.layout.form_toko, binding.dynamicFormContainer)
        setupPhoneInput()
    }

    private fun setupRegisterButton() {
        binding.registerButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (validateInput()) {
                setLoading(true)
                registerUser(email, password)
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: return@addOnSuccessListener
                saveUserData(userId)
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Registrasi gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUserData(userId: String) {
        val userData = getUserData(userId)
        
        db.collection("user")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(this, "Registrasi berhasil", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun getUserData(userId: String): Map<String, Any> {
        val data = mutableMapOf<String, Any>(
            "uid" to userId,
            "email" to binding.emailInput.text.toString(),
            "type" to userType,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        val phoneNumber = binding.root.findViewById<TextInputEditText>(R.id.phoneInput).text.toString()
        val formattedPhone = if (!phoneNumber.startsWith("62")) {
            "62$phoneNumber"
        } else {
            phoneNumber
        }

        if (userType == "petani") {
            data.apply {
                put("nik", binding.root.findViewById<TextInputEditText>(R.id.nikInput).text.toString())
                put("nama", binding.root.findViewById<TextInputEditText>(R.id.namaInput).text.toString())
                put("phone", formattedPhone)
                put("alamat", binding.root.findViewById<TextInputEditText>(R.id.alamatInput).text.toString())
            }
        } else {
            data.apply {
                put("nik", binding.root.findViewById<TextInputEditText>(R.id.nikInput).text.toString())
                put("nama", binding.root.findViewById<TextInputEditText>(R.id.namaTokoInput).text.toString())
                put("phone", formattedPhone)
                put("alamat", binding.root.findViewById<TextInputEditText>(R.id.alamatInput).text.toString())
                put("jenisBarang", binding.root.findViewById<TextInputEditText>(R.id.jenisBarangInput).text.toString())
            }
        }

        return data
    }

    private fun validateInput(): Boolean {
        var isValid = true
        
        // Validasi email
        if (binding.emailInput.text.toString().isEmpty()) {
            binding.emailLayout.error = "Email tidak boleh kosong"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(binding.emailInput.text.toString()).matches()) {
            binding.emailLayout.error = "Format email tidak valid"
            isValid = false
        } else {
            binding.emailLayout.error = null
        }

        // Validasi password
        if (binding.passwordInput.text.toString().isEmpty()) {
            binding.passwordLayout.error = "Password tidak boleh kosong"
            isValid = false
        } else if (binding.passwordInput.text.toString().length < 6) {
            binding.passwordLayout.error = "Password minimal 6 karakter"
            isValid = false
        } else {
            binding.passwordLayout.error = null
        }

        // Validasi tipe user
        if (userType.isEmpty()) {
            Toast.makeText(this, "Pilih tipe user", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Validasi form dinamis
        if (userType.isNotEmpty()) {
            val nikInput = binding.root.findViewById<TextInputEditText>(R.id.nikInput)
            val phoneInput = binding.root.findViewById<TextInputEditText>(R.id.phoneInput)
            val alamatInput = binding.root.findViewById<TextInputEditText>(R.id.alamatInput)

            if (nikInput.text.toString().isEmpty()) {
                nikInput.error = "NIK tidak boleh kosong"
                isValid = false
            } else if (nikInput.text.toString().length != 16) {
                nikInput.error = "NIK harus 16 digit"
                isValid = false
            }

            if (phoneInput.text.toString().isEmpty()) {
                phoneInput.error = "Nomor HP tidak boleh kosong"
                isValid = false
            } else if (!phoneInput.text.toString().startsWith("62")) {
                phoneInput.error = "Nomor HP harus diawali dengan 62"
                isValid = false
            } else if (phoneInput.text.toString().length < 10) {
                phoneInput.error = "Nomor HP tidak valid"
                isValid = false
            }

            if (alamatInput.text.toString().isEmpty()) {
                alamatInput.error = "Alamat tidak boleh kosong"
                isValid = false
            }

            if (userType == "petani") {
                val namaInput = binding.root.findViewById<TextInputEditText>(R.id.namaInput)
                if (namaInput.text.toString().isEmpty()) {
                    namaInput.error = "Nama tidak boleh kosong"
                    isValid = false
                }
            } else {
                val namaTokoInput = binding.root.findViewById<TextInputEditText>(R.id.namaTokoInput)
                val jenisBarangInput = binding.root.findViewById<TextInputEditText>(R.id.jenisBarangInput)
                
                if (namaTokoInput.text.toString().isEmpty()) {
                    namaTokoInput.error = "Nama toko tidak boleh kosong"
                    isValid = false
                }
                
                if (jenisBarangInput.text.toString().isEmpty()) {
                    jenisBarangInput.error = "Jenis barang tidak boleh kosong"
                    isValid = false
                }
            }
        }

        return isValid
    }

    private fun setLoading(isLoading: Boolean) {
        binding.apply {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            registerButton.isEnabled = !isLoading
            emailInput.isEnabled = !isLoading
            passwordInput.isEnabled = !isLoading
            userTypeGroup.isEnabled = !isLoading
        }
    }
} 