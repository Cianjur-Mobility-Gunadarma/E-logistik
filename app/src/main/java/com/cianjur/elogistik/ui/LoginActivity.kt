package com.cianjur.elogistik.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cianjur.elogistik.AdminActivity
import com.cianjur.elogistik.MainActivity
import com.cianjur.elogistik.TokoActivity
import com.cianjur.elogistik.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        db = Firebase.firestore

        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (availability.isUserResolvableError(resultCode)) {
                availability.getErrorDialog(this, resultCode, 9000)?.show()
            }
            return
        }

        binding.loginButton.setOnClickListener {
            val email = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                binding.loginButton.isEnabled = false
                
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        authResult.user?.uid?.let { userId ->
                            checkUserTypeAndRedirect(userId)
                        } ?: run {
                            binding.loginButton.isEnabled = true
                            Toast.makeText(this, "Error: User ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        binding.loginButton.isEnabled = true
                        Toast.makeText(this, "Login gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Mohon isi email dan password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUserTypeAndRedirect(userId: String) {
        db.collection("user").document(userId)
            .get()
            .addOnSuccessListener { document ->
                binding.loginButton.isEnabled = true
                
                if (document.exists()) {
                    try {
                        when (document.getString("type")) {
                            "petani" -> {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            "toko" -> {
                                startActivity(Intent(this, TokoActivity::class.java))
                                finish()
                            }
                            "admin" -> {
                                startActivity(Intent(this, AdminActivity::class.java))
                                finish()
                            }
                            else -> {
                                Toast.makeText(this, "Tipe user tidak valid", Toast.LENGTH_SHORT).show()
                                auth.signOut()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                    }
                } else {
                    Toast.makeText(this, "Data user tidak ditemukan", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            }
            .addOnFailureListener { e ->
                binding.loginButton.isEnabled = true
                Toast.makeText(this, "Error akses: ${e.message}", Toast.LENGTH_SHORT).show()
                auth.signOut()
            }
    }
}