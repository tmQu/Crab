package tkpm.com.crab.objects

import com.google.gson.annotations.SerializedName

data class User (
    @SerializedName("_id")
    val id: String,
    val name: String,
    val phone: String,
    val role: String,
    val avatar: String,
    val firebaseUID: String
)