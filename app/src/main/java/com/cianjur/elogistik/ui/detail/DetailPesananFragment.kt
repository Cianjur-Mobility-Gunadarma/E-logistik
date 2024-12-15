package com.cianjur.elogistik.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
    private var latitude: Double? = null
    private var longitude: Double? = null

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

        // Sembunyikan tombol update status secara default
        binding.updateStatusButton.visibility = View.GONE

        loadPesananDetail()
        setupLihatLokasiButton()

        when {
            isAdmin -> setupAdminView()
            isToko -> setupTokoView()
            else -> setupPetaniView()
        }
    }

    private fun setupTokoView() {
        binding.apply {
            // Sembunyikan tombol yang tidak diperlukan toko
            editButton.visibility = View.GONE
            
            // Ubah text untuk menunjukkan ini adalah alamat petani
            alamatTokoLabel.text = "Alamat Petani:"
            
            // Tampilkan tombol hubungi petani
            hubungiButton.text = "Hubungi petani melalui WA"
            hubungiButton.visibility = View.VISIBLE
            
            // Setup tombol batalkan untuk toko
            batalkanButton.visibility = View.VISIBLE
            batalkanButton.text = "Batalkan Pesanan"
            batalkanButton.setOnClickListener {
                pesananId?.let { id ->
                    db.collection("pesanan").document(id)
                        .get()
                        .addOnSuccessListener { document ->
                            val status = document.getString("status") ?: ""
                            
                            // Bisa dibatalkan jika status menunggu, diproses, atau dikirim
                            if (status in listOf("menunggu", "diproses", "dikirim")) {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Batalkan Pesanan")
                                    .setMessage("Apakah Anda yakin ingin membatalkan pesanan ini?")
                                    .setPositiveButton("Ya") { _, _ ->
                                        updatePesananStatus("dibatalkan")
                                    }
                                    .setNegativeButton("Tidak", null)
                                    .show()
                            } else {
                                Toast.makeText(context, 
                                    "Pesanan tidak dapat dibatalkan", 
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
            
            // Update onClickListener untuk hubungi petani
            hubungiButton.setOnClickListener {
                pesananId?.let { id ->
                    db.collection("pesanan")
                        .document(id)
                        .get()
                        .addOnSuccessListener { document ->
                            val petaniId = document.getString("petaniId")
                            if (petaniId != null) {
                                db.collection("user")
                                    .document(petaniId)
                                    .get()
                                    .addOnSuccessListener { petaniDoc ->
                                        val phone = petaniDoc.getString("phone")
                                        if (phone != null) {
                                            openWhatsApp(phone)
                                        }
                                    }
                            }
                        }
                }
            }

            // Setup tombol update status
            setupUpdateStatusButton()
            
            // Update visibility tombol berdasarkan status
            pesananId?.let { id ->
                db.collection("pesanan").document(id)
                    .get()
                    .addOnSuccessListener { document ->
                        val status = document.getString("status") ?: ""
                        
                        when (status) {
                            "menunggu", "diproses", "dikirim" -> {
                                updateStatusButton.visibility = View.VISIBLE
                                batalkanButton.visibility = View.VISIBLE
                            }
                            "dibatalkan", "selesai" -> {
                                updateStatusButton.visibility = View.GONE
                                batalkanButton.visibility = View.GONE
                            }
                            else -> {
                                updateStatusButton.visibility = View.VISIBLE
                                batalkanButton.visibility = View.VISIBLE
                            }
                        }
                    }
            }
        }
    }

    private fun setupPetaniView() {
        binding.apply {
            // Pastikan tombol update status GONE untuk petani
            updateStatusButton.visibility = View.GONE
            
            alamatTokoLabel.text = "Alamat Toko:"
            hubungiButton.text = "Hubungi toko melalui WA"
            
            // Setup tombol lainnya untuk petani
            pesananId?.let { id ->
                db.collection("pesanan").document(id)
                    .get()
                    .addOnSuccessListener { document ->
                        val status = document.getString("status") ?: ""
                        
                        when (status) {
                            "menunggu" -> {
                                hubungiButton.visibility = View.VISIBLE
                                batalkanButton.visibility = View.VISIBLE
                                editButton.visibility = View.VISIBLE
                            }
                            "dibatalkan" -> {
                                hubungiButton.visibility = View.GONE
                                batalkanButton.visibility = View.GONE
                                editButton.visibility = View.GONE
                            }
                            else -> {
                                hubungiButton.visibility = View.VISIBLE
                                batalkanButton.visibility = View.GONE
                                editButton.visibility = View.GONE
                            }
                        }
                    }
            }
            
            setupHubungiButton()
            setupBatalkanButton()
            setupEditButton()
        }
    }

    private fun setupUpdateStatusButton() {
        binding.updateStatusButton.setOnClickListener {
            pesananId?.let { id ->
                db.collection("pesanan").document(id)
                    .get()
                    .addOnSuccessListener { document ->
                        val currentStatus = document.getString("status") ?: ""
                        
                        // Tentukan status berikutnya berdasarkan status saat ini
                        val nextStatus = when (currentStatus) {
                            "menunggu" -> "diproses"
                            "diproses" -> "dikirim"
                            "dikirim" -> "selesai"
                            else -> ""
                        }
                        
                        // Tampilkan dialog dengan status berikutnya
                        if (nextStatus.isNotEmpty()) {
                            val displayStatus = getStatusText(nextStatus)
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Update Status Pesanan")
                                .setMessage("Ubah status menjadi \"$displayStatus\"?")
                                .setPositiveButton("Ya") { _, _ ->
                                    updatePesananStatus(nextStatus)
                                }
                                .setNegativeButton("Tidak", null)
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
                    if (document.exists()) {
                        updateUI(document)
                        
                        // Update visibility tombol update status hanya jika user adalah toko
                        val isToko = arguments?.getBoolean("isToko") ?: false
                        val isAdmin = arguments?.getBoolean("isAdmin") ?: false
                        val currentStatus = document.getString("status") ?: ""
                        
                        binding.updateStatusButton.visibility = when {
                            isAdmin -> View.VISIBLE
                            isToko && canUpdateStatus(currentStatus) -> View.VISIBLE
                            else -> View.GONE
                        }
                    }
                }
        }
    }

    private fun updateUI(document: DocumentSnapshot) {
        binding.apply {
            // Update judul sesuai dengan nama petani
            titleText.text = document.getString("petaniNama") ?: "Petani"

            // Update informasi lainnya seperti biasa
            tanggalText.text = document.getString("jadwal")
            alamatText.text = document.getString("lokasi")
            petaniText.text = document.getString("petaniNama")

            val petaniId = document.getString("petaniId")
            val tokoNama = document.getString("tokoNama")
            val isToko = arguments?.getBoolean("isToko") ?: false

            // Ambil data petani
            if (petaniId != null) {
                db.collection("user")
                    .document(petaniId)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        if (userDoc.exists()) {
                            petaniText.text = userDoc.getString("nama")
                            titleText.text = userDoc.getString("nama")

                            // Jika user adalah toko, tampilkan alamat petani
                            if (isToko) {
                                alamatTokoText.text = userDoc.getString("alamat")
                            }
                        }
                    }
            }

            // Ambil data toko
            if (tokoNama != null) {
                db.collection("user")
                    .whereEqualTo("nama", tokoNama)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val tokoDoc = documents.documents[0]
                            tokoText.text = tokoDoc.getString("nama")

                            // Jika user adalah petani, tampilkan alamat toko
                            if (!isToko) {
                                alamatTokoText.text = tokoDoc.getString("alamat")
                            }
                        }
                    }
            }

            // Update data lainnya
            barangText.text = getKebutuhanText(document)
            val rawStatus = document.getString("status") ?: ""
            statusText.text = getStatusText(rawStatus)
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
                // Ambil data pesanan terlebih dahulu
                db.collection("pesanan").document(id)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val bundle = Bundle().apply {
                                putString("pesananId", id)
                                putString("lokasi", document.getString("lokasi"))
                                putString("tokoNama", document.getString("tokoNama"))
                                putString("jadwal", document.getString("jadwal"))
                                putString("pesan", document.getString("pesan"))
                                putString("kebutuhanList", document.get("kebutuhanList").toString())
                                putBoolean("isEdit", true)
                            }
                            findNavController().navigate(
                                R.id.navigation_home,
                                bundle
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
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

        // Khusus untuk admin, bisa update ke semua status
        binding.updateStatusButton.setOnClickListener {
            val statusList = listOf(
                "menunggu" to "Menunggu konfirmasi toko",
                "diproses" to "Sedang diproses toko",
                "dikirim" to "Sedang dikirim",
                "selesai" to "Pesanan selesai",
                "dibatalkan" to "Pesanan dibatalkan"
            )
            
            val displayOptions = statusList.map { it.second }.toTypedArray()
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update Status")
                .setItems(displayOptions) { _, which ->
                    val newStatus = statusList[which].first
                    updatePesananStatus(newStatus)
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

    private fun setupLihatLokasiButton() {
        binding.buttonLihatLokasi.setOnClickListener {
            val alamat = if (arguments?.getBoolean("isToko") == true) {
                binding.alamatText.text.toString() // Alamat pengiriman untuk toko
            } else {
                binding.alamatTokoText.text.toString() // Alamat toko untuk petani
            }

            if (alamat.isNotEmpty()) {
                val mapsUrl = "https://www.google.com/maps/search/${Uri.encode(alamat)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Tidak dapat membuka maps: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Alamat tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPesananStatus() {
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
    }

    private fun getKebutuhanText(document: DocumentSnapshot): String {
        return try {
            when (val kebutuhanData = document.get("kebutuhanList")) {
                is List<*> -> {
                    kebutuhanData.joinToString("\n") { item ->
                        when (item) {
                            is Map<*, *> -> "${item["nama"]}: ${item["jumlah"]}"
                            is String -> item
                            else -> item.toString()
                        }
                    }
                }
                is String -> kebutuhanData
                else -> document.getString("kebutuhan") ?: ""
            }
        } catch (e: Exception) {
            document.getString("kebutuhan") ?: ""
        }
    }

    private fun getStatusText(status: String): String {
        return when (status.lowercase()) {
            "menunggu" -> "Menunggu konfirmasi toko"
            "diproses" -> "Sedang diproses toko"
            "dikirim" -> "Sedang dikirim"
            "selesai" -> "Pesanan selesai"
            "dibatalkan" -> "Pesanan dibatalkan"
            else -> status
        }
    }

    // Tambahkan fungsi untuk mengecek apakah status bisa diupdate
    private fun canUpdateStatus(currentStatus: String): Boolean {
        return when (currentStatus) {
            "menunggu", "diproses", "dikirim" -> true
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 