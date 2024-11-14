package com.cianjur.elogistik.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.cianjur.elogistik.R
import com.cianjur.elogistik.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var userAlamat: String = ""
    private var tokoList: List<TokoItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        loadUserAddress()
        setupJadwalPicker()
        loadTokoList()
        setupPesanButton()
    }

    private fun loadUserAddress() {
        auth.currentUser?.let { user ->
            db.collection("user").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userAlamat = document.getString("alamat") ?: ""
                        binding.lokasiInput.setText(userAlamat)
                    }
                }
        }
    }

    private fun setupJadwalPicker() {
        binding.jadwalInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    TimePickerDialog(
                        requireContext(),
                        { _, hour, minute ->
                            val jadwal = String.format(
                                "%02d-%02d-%04d %02d:%02d",
                                day, month + 1, year, hour, minute
                            )
                            binding.jadwalInput.setText(jadwal)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun loadTokoList() {
        db.collection("user")
            .whereEqualTo("type", "toko")
            .get()
            .addOnSuccessListener { documents ->
                val tokoList = documents.mapNotNull { doc -> 
                    doc.getString("nama")?.let { nama ->
                        TokoItem(doc.id, nama)
                    }
                }
                
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    tokoList
                )
                
                (binding.tokoDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
                
                this.tokoList = tokoList
            }
    }

    private fun setupPesanButton() {
        binding.pesanButton.setOnClickListener {
            val lokasi = binding.lokasiInput.text.toString()
            val kebutuhan = binding.kebutuhanInput.text.toString()
            val jumlah = binding.jumlahInput.text.toString()
            val jadwal = binding.jadwalInput.text.toString()
            val toko = binding.tokoDropdown.text.toString()
            val pesan = binding.pesanInput.text.toString()

            if (lokasi.isEmpty() || kebutuhan.isEmpty() || 
                jumlah.isEmpty() || jadwal.isEmpty() || toko.isEmpty()) {
                Toast.makeText(context, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedToko = tokoList.find { it.nama == toko }
            if (selectedToko == null) {
                Toast.makeText(context, "Mohon pilih toko yang valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pesanan = hashMapOf(
                "petaniId" to auth.currentUser?.uid,
                "tokoId" to selectedToko.id,
                "tokoNama" to selectedToko.nama,
                "lokasi" to lokasi,
                "kebutuhan" to kebutuhan,
                "jumlah" to jumlah.toInt(),
                "jadwal" to jadwal,
                "pesan" to pesan,
                "status" to "menunggu",
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("pesanan")
                .add(pesanan)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(context, "Pesanan berhasil dibuat", Toast.LENGTH_SHORT).show()
                    clearForm()
                    
                    // Navigate to detail
                    val bundle = Bundle().apply {
                        putString("pesananId", documentReference.id)
                    }
                    findNavController().navigate(
                        R.id.action_navigation_home_to_detailPesananFragment,
                        bundle
                    )
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun clearForm() {
        binding.apply {
            lokasiInput.setText(userAlamat)
            kebutuhanInput.setText("")
            jumlahInput.setText("")
            jadwalInput.setText("")
            tokoDropdown.setText("")
            pesanInput.setText("")
        }
    }

    data class TokoItem(val id: String, val nama: String) {
        override fun toString(): String = nama
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}