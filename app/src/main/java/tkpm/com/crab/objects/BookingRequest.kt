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
    val visa: String
)


data class MongoLocation(
    @SerializedName("_id") val id: String, val type: String, val coordinates: List<Double>
)

data class LocationRecord(
    @SerializedName("_id") val id: String, val address: String, val location: MongoLocation
)

data class BookingInfo(
    @SerializedName("_id") val id: String,
    val pickup: LocationRecord,
    val destination: LocationRecord,
    val phone: String,
    val name: String,
    val fee: Int
)

data class Booking(
    @SerializedName("_id") val id: String, val status: String, val info: BookingInfo, val service: String, val driver: User
)

