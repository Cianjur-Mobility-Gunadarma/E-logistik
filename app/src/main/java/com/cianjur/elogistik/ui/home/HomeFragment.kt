package com.cianjur.elogistik.ui.home

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cianjur.elogistik.R
import com.cianjur.elogistik.databinding.FragmentHomeBinding
import com.cianjur.elogistik.databinding.ItemKebutuhanBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

data class KebutuhanItem(
    var nama: String = "",
    var jumlah: String = ""
)

data class TokoItem(
    val id: String = "",
    val nama: String = "",
    val alamat: String = "",
    val phone: String = "",
    val jenisBarang: String = ""
)

class KebutuhanAdapter(
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<KebutuhanAdapter.ViewHolder>() {
    
    private val items = mutableListOf<KebutuhanItem>()

    inner class ViewHolder(private val binding: ItemKebutuhanBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: KebutuhanItem, position: Int) {
            binding.apply {
                namaKebutuhanInput.setText(item.nama)
                jumlahInput.setText(item.jumlah)
                deleteButton.setOnClickListener { onDelete(position) }

                // Update item ketika text berubah
                namaKebutuhanInput.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        items[position].nama = namaKebutuhanInput.text.toString()
                    }
                }

                jumlahInput.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        items[position].jumlah = jumlahInput.text.toString()
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemKebutuhanBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: KebutuhanItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(position: Int) {
        if (position in 0 until items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }
    }

    fun getItems(): List<KebutuhanItem> = items.toList()
}

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var kebutuhanAdapter: KebutuhanAdapter
    private var userAlamat: String = ""
    private var selectedToko: TokoItem? = null
    private val tokoList = mutableListOf<TokoItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupKebutuhanRecyclerView()
        loadUserAddress()
        setupJadwalPicker()
        loadTokoList()
        setupPesanButton()

        binding.tambahKebutuhanButton.setOnClickListener {
            kebutuhanAdapter.addItem(KebutuhanItem())
        }

        // Cek apakah ini mode edit
        arguments?.let { args ->
            if (args.getBoolean("isEdit", false)) {
                // Load data untuk edit
                loadDataForEdit(args)
            }
        }
    }

    private fun setupKebutuhanRecyclerView() {
        kebutuhanAdapter = KebutuhanAdapter(
            onDelete = { position ->
                kebutuhanAdapter.removeItem(position)
            }
        )
        
        binding.kebutuhanRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = kebutuhanAdapter
        }

        // Tambahkan item pertama
        kebutuhanAdapter.addItem(KebutuhanItem())
    }

    private fun setupPesanButton() {
        binding.pesanButton.setOnClickListener {
            val lokasi = binding.lokasiInput.text.toString()
            val jadwal = binding.jadwalInput.text.toString()
            val pesan = binding.pesanInput.text.toString()
            val kebutuhanList = kebutuhanAdapter.getItems()

            if (lokasi.isEmpty() || jadwal.isEmpty() || selectedToko == null || 
                kebutuhanList.isEmpty() || kebutuhanList.any { it.nama.isEmpty() || it.jumlah.isEmpty() }) {
                Toast.makeText(context, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convert kebutuhanList menjadi List<Map<String, String>>
            val kebutuhanMapList = kebutuhanList.map { kebutuhan ->
                mapOf(
                    "nama" to kebutuhan.nama,
                    "jumlah" to kebutuhan.jumlah
                )
            }

            val pesanan = hashMapOf(
                "petaniId" to auth.currentUser?.uid,
                "tokoId" to (selectedToko?.id ?: ""),
                "tokoNama" to (selectedToko?.nama ?: ""),
                "lokasi" to lokasi,
                "kebutuhanList" to kebutuhanMapList,
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
            jadwalInput.setText("")
            tokoDropdown.setText("")
            pesanInput.setText("")
            jenisBarangInfo.visibility = View.GONE
        }
        // Reset kebutuhan list
        kebutuhanAdapter = KebutuhanAdapter(
            onDelete = { position -> kebutuhanAdapter.removeItem(position) }
        )
        binding.kebutuhanRecyclerView.adapter = kebutuhanAdapter
        kebutuhanAdapter.addItem(KebutuhanItem())
    }

    private fun loadUserAddress() {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("user").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (isAdded && _binding != null) {
                    if (document.exists()) {
                        userAlamat = document.getString("alamat") ?: ""
                        binding.lokasiInput.setText(userAlamat)
                    }
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun setupTokoDropdown() {
        // Format: "Nama Toko - Jenis Barang"
        val tokoItems = tokoList.map { "${it.nama} - ${it.jenisBarang}" }
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            tokoItems
        )
        
        binding.tokoDropdown.setAdapter(adapter)

        binding.tokoDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedToko = tokoList[position]
            updateTokoInfo()
        }
    }

    private fun updateTokoInfo() {
        selectedToko?.let { toko ->
            binding.jenisBarangInfo.apply {
                text = "Alamat: ${toko.alamat}\nTelepon: ${toko.phone}"
                visibility = View.VISIBLE
            }
        }
    }

    private fun loadTokoList() {
        db.collection("user")
            .whereEqualTo("type", "toko")
            .get()
            .addOnSuccessListener { documents ->
                if (isAdded && _binding != null) {
                    tokoList.clear()
                    for (document in documents) {
                        val toko = TokoItem(
                            id = document.id,
                            nama = document.getString("nama") ?: "",
                            alamat = document.getString("alamat") ?: "",
                            phone = document.getString("phone") ?: "",
                            jenisBarang = document.getString("jenisBarang") ?: ""
                        )
                        tokoList.add(toko)
                    }
                    setupTokoDropdown()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadDataForEdit(args: Bundle) {
        val pesananId = args.getString("pesananId")
        
        // Set data yang sudah ada di arguments
        binding.apply {
            lokasiInput.setText(args.getString("lokasi"))
            jadwalInput.setText(args.getString("jadwal"))
            pesanInput.setText(args.getString("pesan"))
        }

        // Load data toko dan set selectedToko
        pesananId?.let { id ->
            db.collection("pesanan").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val tokoId = document.getString("tokoId") ?: ""
                        val tokoNama = document.getString("tokoNama") ?: ""
                        
                        // Load data toko lengkap
                        db.collection("user").document(tokoId)
                            .get()
                            .addOnSuccessListener { tokoDoc ->
                                if (tokoDoc.exists()) {
                                    selectedToko = TokoItem(
                                        id = tokoId,
                                        nama = tokoNama,
                                        alamat = tokoDoc.getString("alamat") ?: "",
                                        phone = tokoDoc.getString("phone") ?: "",
                                        jenisBarang = tokoDoc.getString("jenisBarang") ?: ""
                                    )
                                    
                                    // Set toko dropdown dan info
                                    binding.tokoDropdown.setText("${selectedToko?.nama} - ${selectedToko?.jenisBarang}")
                                    updateTokoInfo()
                                }
                            }

                        // Load kebutuhan list
                        when (val kebutuhanData = document.get("kebutuhanList")) {
                            is List<*> -> {
                                // Clear existing kebutuhan
                                kebutuhanAdapter = KebutuhanAdapter(
                                    onDelete = { position -> kebutuhanAdapter.removeItem(position) }
                                )
                                binding.kebutuhanRecyclerView.adapter = kebutuhanAdapter

                                kebutuhanData.forEach { item ->
                                    when (item) {
                                        is Map<*, *> -> {
                                            kebutuhanAdapter.addItem(
                                                KebutuhanItem(
                                                    nama = (item["nama"] as? String) ?: "",
                                                    jumlah = (item["jumlah"] as? String) ?: ""
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }

        // Update button text dan behavior
        binding.pesanButton.text = "Update Pesanan"
        binding.pesanButton.setOnClickListener {
            updatePesanan(pesananId)
        }
    }

    private fun updatePesanan(pesananId: String?) {
        if (pesananId == null) return

        val lokasi = binding.lokasiInput.text.toString()
        val jadwal = binding.jadwalInput.text.toString()
        val pesan = binding.pesanInput.text.toString()
        val kebutuhanList = kebutuhanAdapter.getItems()

        if (lokasi.isEmpty() || jadwal.isEmpty() || selectedToko == null || 
            kebutuhanList.isEmpty() || kebutuhanList.any { it.nama.isEmpty() || it.jumlah.isEmpty() }) {
            Toast.makeText(context, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show()
            return
        }

        val kebutuhanMapList = kebutuhanList.map { kebutuhan ->
            mapOf(
                "nama" to kebutuhan.nama,
                "jumlah" to kebutuhan.jumlah
            )
        }

        val updateData = hashMapOf(
            "lokasi" to lokasi,
            "jadwal" to jadwal,
            "pesan" to pesan,
            "kebutuhanList" to kebutuhanMapList,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("pesanan").document(pesananId)
            .update(updateData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(context, "Pesanan berhasil diupdate", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}