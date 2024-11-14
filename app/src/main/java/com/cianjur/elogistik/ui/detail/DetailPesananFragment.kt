package com.cianjur.elogistik.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cianjur.elogistik.databinding.FragmentDetailPesananBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.navigation.fragment.findNavController
import com.cianjur.elogistik.R

class DetailPesananFragment : Fragment() {
    private var _binding: FragmentDetailPesananBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var pesananId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailPesananBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        db = FirebaseFirestore.getInstance()
        pesananId = arguments?.getString("pesananId")
        val isToko = arguments?.getBoolean("isToko") ?: false
        val isAdmin = arguments?.getBoolean("isAdmin") ?: false
        
        loadPesananDetail()
        
        when {
            isAdmin -> setupAdminView()
            isToko -> setupTokoView()
            else -> setupPetaniView()
        }
    }

    private fun setupTokoView() {
        // Sembunyikan semua tombol petani
        binding.editButton.visibility = View.GONE
        binding.batalkanButton.visibility = View.GONE
        binding.hubungiButton.visibility = View.GONE

        // Load status pesanan
        pesananId?.let { id ->
            db.collection("pesanan").document(id)
                .get()
                .addOnSuccessListener { document ->
                    val status = document.getString("status") ?: ""
                    
                    // Tampilkan tombol update status hanya jika status bukan dibatalkan atau selesai
                    binding.updateStatusButton.visibility = when (status) {
                        "dibatalkan", "selesai" -> View.GONE
                        else -> View.VISIBLE
                    }
                }
        }
        
        setupUpdateStatusButton()
    }

    private fun setupPetaniView() {
        binding.updateStatusButton.visibility = View.GONE
        
        pesananId?.let { id ->
            db.collection("pesanan").document(id)
                .get()
                .addOnSuccessListener { document ->
                    val status = document.getString("status") ?: ""
                    
                    when (status) {
                        "menunggu" -> {
                            // Jika status masih menunggu, tampilkan semua tombol termasuk edit
                            binding.hubungiButton.visibility = View.VISIBLE
                            binding.batalkanButton.visibility = View.VISIBLE
                            binding.editButton.visibility = View.VISIBLE
                        }
                        "dibatalkan" -> {
                            binding.hubungiButton.visibility = View.GONE
                            binding.batalkanButton.visibility = View.GONE
                            binding.editButton.visibility = View.GONE
                        }
                        else -> {
                            binding.hubungiButton.visibility = View.VISIBLE
                            binding.batalkanButton.visibility = View.GONE
                            binding.editButton.visibility = View.GONE
                        }
                    }
                }
        }
        
        setupHubungiButton()
        setupBatalkanButton()
        setupEditButton()
    }

    private fun setupUpdateStatusButton() {
        binding.updateStatusButton.setOnClickListener {
            // Ambil status saat ini
            pesananId?.let { id ->
                db.collection("pesanan").document(id)
                    .get()
                    .addOnSuccessListener { document ->
                        val currentStatus = document.getString("status") ?: ""
                        
                        // Tentukan opsi status yang bisa dipilih berdasarkan status saat ini
                        val options = when (currentStatus) {
                            "menunggu" -> arrayOf("diproses")
                            "diproses" -> arrayOf("dikirim")
                            "dikirim" -> arrayOf("selesai")
                            else -> arrayOf() // Tidak ada opsi jika status sudah selesai/dibatalkan
                        }
                        
                        if (options.isNotEmpty()) {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Update Status Pesanan")
                                .setItems(options) { _, which ->
                                    val newStatus = options[which]
                                    updatePesananStatus(newStatus)
                                }
                                .show()
                        }
                    }
            }
        }
    }

    private fun updatePesananStatus(newStatus: String) {
        pesananId?.let { id ->
            db.collection("pesanan").document(id)
                .update("status", newStatus)
                .addOnSuccessListener {
                    Toast.makeText(context, "Status berhasil diupdate", Toast.LENGTH_SHORT).show()
                    loadPesananDetail() // Refresh tampilan
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Gagal update status: ${e.message}", 
                        Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadPesananDetail() {
        pesananId?.let { id ->
            db.collection("pesanan").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        updateUI(document)
                    }
                }
        }
    }

    private fun updateUI(document: DocumentSnapshot) {
        binding.apply {
            tanggalText.text = document.getString("jadwal")
            alamatText.text = document.getString("lokasi")
            barangText.text = document.getString("kebutuhan")
            jumlahText.text = "${document.getLong("jumlah")} kg"
            tokoText.text = document.getString("tokoNama")
            statusText.text = "Status pesanan: ${document.getString("status")}"
        }
    }

    private fun setupHubungiButton() {
        binding.hubungiButton.setOnClickListener {
            // Implementasi untuk membuka WhatsApp
            val tokoNama = binding.tokoText.text.toString()
            db.collection("user")
                .whereEqualTo("nama", tokoNama)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val tokoPhone = documents.documents[0].getString("phone")
                        if (tokoPhone != null) {
                            openWhatsApp(tokoPhone)
                        }
                    }
                }
        }
    }

    private fun openWhatsApp(phone: String) {
        try {
            val url = "https://api.whatsapp.com/send?phone=$phone"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp tidak terpasang", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBatalkanButton() {
        binding.batalkanButton.setOnClickListener {
            // Tampilkan dialog konfirmasi
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Batalkan Pesanan")
                .setMessage("Apakah Anda yakin ingin membatalkan pesanan ini?")
                .setPositiveButton("Ya") { _, _ ->
                    pesananId?.let { id ->
                        db.collection("pesanan").document(id)
                            .update("status", "dibatalkan")
                            .addOnSuccessListener {
                                Toast.makeText(context, "Pesanan berhasil dibatalkan", Toast.LENGTH_SHORT).show()
                                // Kembali ke halaman sebelumnya
                                findNavController().navigateUp()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Gagal membatalkan pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .setNegativeButton("Tidak", null)
                .show()
        }
    }

    private fun setupEditButton() {
        binding.editButton.setOnClickListener {
            pesananId?.let { id ->
                val bundle = Bundle().apply {
                    putString("pesananId", id)
                    putBoolean("isEdit", true)
                }
                findNavController().navigate(
                    R.id.navigation_home,
                    bundle
                )
            }
        }
    }

    private fun setupAdminView() {
        // Tampilkan tombol-tombol untuk admin (tanpa tombol edit)
        binding.apply {
            editButton.visibility = View.GONE  // Sembunyikan tombol edit khusus untuk admin
            batalkanButton.visibility = View.VISIBLE
            updateStatusButton.visibility = View.VISIBLE
            hubungiButton.visibility = View.VISIBLE
        }
        
        // Setup tombol-tombol
        setupBatalkanButton()
        setupUpdateStatusButton()
        setupHubungiButton()

        // Modifikasi update status untuk admin (bisa ke semua status)
        binding.updateStatusButton.setOnClickListener {
            val statusOptions = arrayOf("menunggu", "diproses", "dikirim", "selesai", "dibatalkan")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update Status")
                .setItems(statusOptions) { _, which ->
                    updatePesananStatus(statusOptions[which])
                }
                .show()
        }

        // Modifikasi batalkan untuk admin (untuk menghapus pesanan)
        binding.batalkanButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Hapus Pesanan")
                .setMessage("Apakah Anda yakin ingin menghapus pesanan ini?")
                .setPositiveButton("Ya") { _, _ ->
                    pesananId?.let { id ->
                        db.collection("pesanan").document(id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Pesanan berhasil dihapus", Toast.LENGTH_SHORT).show()
                                findNavController().navigateUp()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Gagal menghapus pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .setNegativeButton("Tidak", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 