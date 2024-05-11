package tkpm.com.crab.objects


import com.google.gson.annotations.SerializedName

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
    val fee: Int,
    val visa: String,
    val distance: Long,
    val duration: Long
)
