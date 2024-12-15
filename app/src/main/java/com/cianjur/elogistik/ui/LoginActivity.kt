package com.cianjur.elogistik.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import android.view.animation.AnimationUtils
import android.view.animation.Animation
import com.cianjur.elogistik.R
import androidx.appcompat.app.AppCompatDelegate
import android.view.View

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val slideAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_animation)
        binding.logoImage.startAnimation(slideAnimation)

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

        supportActionBar?.hide()

        binding.loginButton.setOnClickListener {
            val email = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                setLoading(true)
                
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        authResult.user?.uid?.let { userId ->
                            Handler(Looper.getMainLooper()).postDelayed({
                                checkUserTypeAndRedirect(userId)
                            }, 500)
                        } ?: run {
                            setLoading(false)
                            showError("Error: User ID tidak ditemukan")
                        }
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        showError("Login gagal: ${e.message}")
                    }
            } else {
                showError("Mohon isi email dan password")
            }
        }

        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun checkUserTypeAndRedirect(userId: String) {
        db.collection("user").document(userId)
            .get()
            .addOnSuccessListener { document ->
                setLoading(false)
                
                if (document.exists()) {
                    try {
                        when (document.getString("type")) {
                            "petani" -> startActivity(Intent(this, MainActivity::class.java))
                            "toko" -> startActivity(Intent(this, TokoActivity::class.java))
                            "admin" -> startActivity(Intent(this, AdminActivity::class.java))
                            else -> {
                                showError("Tipe user tidak valid")
                                auth.signOut()
                                return@addOnSuccessListener
                            }
                        }
                        finish()
                    } catch (e: Exception) {
                        showError("Error: ${e.message}")
                        auth.signOut()
                    }
                } else {
                    showError("Data user tidak ditemukan")
                    auth.signOut()
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                showError("Error akses: ${e.message}")
                auth.signOut()
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.apply {
            loginButton.isEnabled = !isLoading
            loginProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            loginButton.text = if (isLoading) "" else "Masuk"
            usernameInput.isEnabled = !isLoading
            passwordInput.isEnabled = !isLoading
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}