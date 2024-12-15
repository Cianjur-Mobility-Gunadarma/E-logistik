package com.cianjur.elogistik.ui.jadwal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cianjur.elogistik.databinding.FragmentJadwalBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import com.cianjur.elogistik.R
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.auth.FirebaseAuth
import android.util.Log


class JadwalFragment : Fragment() {

    private var _binding: FragmentJadwalBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: JadwalAdapter
    private var snapshotListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJadwalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        setupRecyclerView()
        loadPesanan()
    }

    private fun setupRecyclerView() {
        adapter = JadwalAdapter { pesananId ->
            // Navigate to detail when item clicked
            val bundle = Bundle().apply {
                putString("pesananId", pesananId)
            }
            findNavController().navigate(
                R.id.action_navigation_jadwal_to_detailPesananFragment,
                bundle
            )
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@JadwalFragment.adapter
        }
    }

    private fun loadPesanan() {
        snapshotListener?.remove()
        
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showToast("User tidak terautentikasi")
            return
        }

        // Sederhanakan query dulu - hanya filter berdasarkan petaniId
        val query = db.collection("pesanan")
            .whereEqualTo("petaniId", userId)
            // Hapus orderBy sementara untuk mengisolasi masalah
            //.orderBy("createdAt", Query.Direction.DESCENDING)

        snapshotListener = query.addSnapshotListener { snapshots, e ->
            if (!isAdded || _binding == null) return@addSnapshotListener

            if (e != null) {
                context?.let { safeContext ->
                    if (e.message?.contains("failed precondition") == true) {
                        // Log error untuk debugging
                        Log.e("JadwalFragment", "Error: ${e.message}")
                        Toast.makeText(safeContext, 
                            "Sedang memuat data...", 
                            Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(safeContext, "Error: ${e.message}", 
                            Toast.LENGTH_SHORT).show()
                    }
                }
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                val pesananList = snapshots.documents.mapNotNull { doc ->
                    try {
                        val kebutuhanText = when (val kebutuhanData = doc.get("kebutuhanList")) {
                            is List<*> -> {
                                kebutuhanData.mapNotNull { item ->
                                    when (item) {
                                        is Map<*, *> -> "${item["nama"]}: ${item["jumlah"]}"
                                        is String -> item
                                        else -> null
                                    }
                                }.joinToString(", ")
                            }
                            is String -> kebutuhanData
                            else -> ""
                        }

                        PesananItem(
                            id = doc.id,
                            title = kebutuhanText.ifEmpty { "Pesanan" },
                            description = doc.getString("lokasi") ?: "",
                            date = doc.getString("jadwal") ?: "",
                            status = getStatusText(doc.getString("status") ?: "")
                        )
                    } catch (e: Exception) {
                        Log.e("JadwalFragment", "Error parsing document: ${e.message}")
                        null
                    }
                }
                adapter.submitList(pesananList)
            } else {
                adapter.submitList(emptyList())
            }
        }
    }

    // Tambahkan fungsi untuk format status
    private fun getStatusText(status: String): String {
        return when (status.lowercase()) {
            "menunggu" -> "Menunggu konfirmasi"
            "diproses" -> "Sedang diproses"
            "dikirim" -> "Sedang dikirim"
            "selesai" -> "Selesai"
            "dibatalkan" -> "Dibatalkan"
            else -> status
        }
    }

    private fun showToast(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        snapshotListener?.remove()
        snapshotListener = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        snapshotListener?.remove()
        snapshotListener = null
        super.onDestroy()
    }
}