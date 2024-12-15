import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

@Keep
data class User(
    @DocumentId
    val id: String = "",
    val nik: String = "",
    val nama: String = "",
    val phone: String = "",
    val alamat: String = "",
    val photoUrl: String = "",
    val type: String = "",
    val jenisBarang: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)