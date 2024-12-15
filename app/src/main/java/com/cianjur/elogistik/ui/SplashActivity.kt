package com.cianjur.elogistik.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.cianjur.elogistik.AdminActivity
import com.cianjur.elogistik.MainActivity
import com.cianjur.elogistik.TokoActivity
import com.cianjur.elogistik.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        supportActionBar?.hide()

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserAndRedirect()
        }, 2000) // Delay 2 detik
    }

    private fun checkUserAndRedirect() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User sudah login, cek tipe user
            db.collection("user").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        when (document.getString("type")) {
                            "petani" -> startActivity(Intent(this, MainActivity::class.java))
                            "toko" -> startActivity(Intent(this, TokoActivity::class.java))
                            "admin" -> startActivity(Intent(this, AdminActivity::class.java))
                            else -> startActivity(Intent(this, LoginActivity::class.java))
                        }
                    } else {
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
        } else {
            // User belum login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
} 