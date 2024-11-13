//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.cianjur.elogistik.databinding.FragmentPesananTokoBinding
//import com.google.firebase.database.core.view.View
//import com.google.firebase.firestore.FirebaseFirestore
//
//class PesananTokoFragment : Fragment() {
//    private var _binding: FragmentPesananTokoBinding? = null
//    private val binding get() = _binding!!
//    private lateinit var pesananAdapter: PesananTokoAdapter
//    private lateinit var firestore: FirebaseFirestore
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
//        _binding = FragmentPesananTokoBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        setupRecyclerView()
//        loadPesanan()
//    }
//
//    private fun setupRecyclerView() {
//        pesananAdapter = PesananTokoAdapter()
//        binding.recyclerViewPesanan.apply {
//            layoutManager = LinearLayoutManager(context)
//            adapter = pesananAdapter
//        }
//    }
//
//    private fun loadPesanan() {
//        firestore.collection("pesanan")
//            .orderBy("tanggal", Query.Direction.DESCENDING)
//            .addSnapshotListener { snapshot, e ->
//                if (e != null) {
//                    return@addSnapshotListener
//                }
//
//                val pesananList = snapshot?.toObjects(Pesanan::class.java) ?: emptyList()
//                pesananAdapter.submitList(pesananList)
//
//                binding.emptyView.visibility = if (pesananList.isEmpty()) View.VISIBLE else View.GONE
//            }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}