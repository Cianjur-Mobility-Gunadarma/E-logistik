package com.cianjur.elogistik.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cianjur.elogistik.databinding.FragmentCreateUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateUserFragment : Fragment() {
    private var _binding: FragmentCreateUserBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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

        val userTypes = arrayOf("petani", "toko", "admin")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, userTypes)
        binding.spinnerUserType.adapter = adapter

        binding.buttonCreateUser.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()
            val userType = binding.spinnerUserType.selectedItem.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                binding.buttonCreateUser.isEnabled = false
                
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val userId = result.user?.uid
                        val userData = hashMapOf(
                            "email" to email,
                            "type" to userType
                        )

                        db.collection("user").document(userId!!)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(context, "User berhasil dibuat", Toast.LENGTH_SHORT).show()
                                binding.editTextEmail.text.clear()
                                binding.editTextPassword.text.clear()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        
                        binding.buttonCreateUser.isEnabled = true
                    }
                    .addOnFailureListener { e ->
                        binding.buttonCreateUser.isEnabled = true
                        Toast.makeText(context, "Gagal membuat user: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "Mohon isi semua field", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
