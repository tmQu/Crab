package tkpm.com.crab.objects

data class BookingRequest (
    val pLat: Double,
    val pLng: Double,
    val dLat: Double,
    val dLng: Double,
    val pAddress: String,
    val dAddress: String,
    val name: String,
    val phone: String,
    val ordered_by: String,
    val vehicle: String,
    val service: String,
    val fee: Int
)

