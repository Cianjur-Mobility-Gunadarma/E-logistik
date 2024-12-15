package com.cianjur.elogistik.ui.toko

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cianjur.elogistik.R
import com.cianjur.elogistik.databinding.FragmentPesananTokoBinding
import com.cianjur.elogistik.ui.jadwal.PesananItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class PesananTokoFragment : Fragment() {
    private var _binding: FragmentPesananTokoBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: PesananTokoAdapter
    
    private var pesananListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPesananTokoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        
        setupRecyclerView()
        loadPesanan()

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        adapter = PesananTokoAdapter { pesananId ->
            findNavController().navigate(
                R.id.action_navigation_pesanan_to_detailPesananFragment,
                Bundle().apply {
                    putString("pesananId", pesananId)
                    putBoolean("isToko", true)
                }
            )
        }
        
        binding.recyclerViewPesanan.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@PesananTokoFragment.adapter
        }
    }

    private fun loadPesanan() {
        val currentUser = auth.currentUser?.uid ?: return
        
        pesananListener = db.collection("pesanan")
            .whereEqualTo("tokoId", currentUser)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (_binding == null) return@addSnapshotListener
                
                binding.progressBar.visibility = View.GONE
                
                if (error != null) {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val pesananList = snapshots?.documents?.map { doc ->
                    val kebutuhanText = try {
                        when (val kebutuhanData = doc.get("kebutuhanList")) {
                            is List<*> -> {
                                kebutuhanData.joinToString(", ") { item ->
                                    when (item) {
                                        is Map<*, *> -> "${item["nama"]}: ${item["jumlah"]}"
                                        is String -> item
                                        else -> item.toString()
                                    }
                                }
                            }
                            is String -> kebutuhanData
                            else -> ""
                        }
                    } catch (e: Exception) {
                        doc.getString("kebutuhan") ?: ""
                    }

                    PesananItem(
                        id = doc.id,
                        title = kebutuhanText,
                        description = "Lokasi: ${doc.getString("lokasi")}\n" +
                                    "Pesan: ${doc.getString("pesan") ?: "-"}",
                        date = doc.getString("jadwal") ?: "",
                        status = doc.getString("status") ?: ""
                    )
                } ?: emptyList()

                if (_binding != null) {
                    adapter.submitList(pesananList)
                    
                    binding.emptyView.visibility = 
                        if (pesananList.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerViewPesanan.visibility = 
                        if (pesananList.isEmpty()) View.GONE else View.VISIBLE
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pesananListener?.remove()
        _binding = null
    }
}