package com.cianjur.elogistik.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cianjur.elogistik.R
import com.cianjur.elogistik.databinding.FragmentOrdersBinding
import com.cianjur.elogistik.ui.jadwal.PesananItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class OrdersFragment : Fragment() {
    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var ordersAdapter: OrdersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        setupRecyclerView()
        loadOrders()

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        ordersAdapter = OrdersAdapter(
            onItemClick = { pesananId ->
                findNavController().navigate(
                    R.id.action_ordersFragment_to_detailPesananFragment,
                    Bundle().apply {
                        putString("pesananId", pesananId)
                        putBoolean("isAdmin", true)
                    }
                )
            }
        )
        
        binding.recyclerViewOrders.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ordersAdapter
        }
    }

    private fun loadOrders() {
        db.collection("pesanan")
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
                        description = "Toko: ${doc.getString("tokoNama")}\n" +
                                    "Lokasi: ${doc.getString("lokasi")}\n" +
                                    "Pesan: ${doc.getString("pesan") ?: "-"}",
                        date = doc.getString("jadwal") ?: "",
                        status = doc.getString("status") ?: ""
                    )
                } ?: emptyList()

                ordersAdapter.submitList(pesananList)
                
                binding.emptyView.visibility = 
                    if (pesananList.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerViewOrders.visibility = 
                    if (pesananList.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}