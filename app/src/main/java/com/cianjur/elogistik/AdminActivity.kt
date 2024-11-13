package com.cianjur.elogistik

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.cianjur.elogistik.databinding.ActivityAdminBinding
import com.cianjur.elogistik.ui.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Verifikasi apakah user adalah admin
        checkIsAdmin { isAdmin ->
            if (!isAdmin) {
                Toast.makeText(this, "Akses ditolak", Toast.LENGTH_SHORT).show()
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return@checkIsAdmin
            }
            
            // Lanjutkan setup activity jika user adalah admin
            binding = ActivityAdminBinding.inflate(layoutInflater)
            setContentView(binding.root)
            setupNavigation()
        }
    }

    private fun checkIsAdmin(callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false)
            return
        }

        db.collection("user").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.getString("type") == "admin") {
                    callback(true)
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
    }
}
