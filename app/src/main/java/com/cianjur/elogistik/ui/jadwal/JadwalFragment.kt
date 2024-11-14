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


class JadwalFragment : Fragment() {

    private var _binding: FragmentJadwalBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: JadwalAdapter

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
        db.collection("pesanan")
            .orderBy("jadwal", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val pesananList = snapshots?.documents?.map { doc ->
                    PesananItem(
                        id = doc.id,
                        title = doc.getString("kebutuhan") ?: "",
                        description = doc.getString("lokasi") ?: "",
                        date = doc.getString("jadwal") ?: "",
                        status = doc.getString("status") ?: ""
                    )
                } ?: emptyList()

                adapter.submitList(pesananList)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}