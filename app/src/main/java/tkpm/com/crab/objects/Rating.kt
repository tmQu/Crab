package tkpm.com.crab.objects

import com.google.gson.annotations.SerializedName

data class PreRatingResponse (
    @SerializedName("_id")
    val id: String,
    val vehicle: String,
    val service: String,
    @SerializedName("pick_up")
    val pickUp: String,
    val destination: String
)

data class RatingRequest (
    val value: Int,
    val comment: String
)