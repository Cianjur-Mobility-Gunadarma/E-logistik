//package com.cianjur.elogistik.ui.admin
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.cianjur.elogistik.databinding.FragmentOrdersBinding
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.Query
//
//class OrdersFragment : Fragment() {
//    private var _binding: FragmentOrdersBinding? = null
//    private val binding get() = _binding!!
//    private lateinit var db: FirebaseFirestore
//    private lateinit var ordersAdapter: OrdersAdapter
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        db = FirebaseFirestore.getInstance()
//        setupRecyclerView()
//        loadOrders()
//    }
//
//    private fun setupRecyclerView() {
//        ordersAdapter = OrdersAdapter()
//        binding.recyclerViewOrders.apply {
//            layoutManager = LinearLayoutManager(context)
//            adapter = ordersAdapter
//        }
//    }
//
//    private fun loadOrders() {
//        db.collection("orders")
//            .orderBy("createdAt", Query.Direction.DESCENDING)
//            .addSnapshotListener { snapshot, e ->
//                if (e != null) {
//                    return@addSnapshotListener
//                }
//
//                val ordersList = snapshot?.documents?.mapNotNull { doc ->
//                    doc.toObject(Order::class.java)?.apply {
//                        id = doc.id
//                    }
//                } ?: listOf()
//
//                ordersAdapter.submitList(ordersList)
//            }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}