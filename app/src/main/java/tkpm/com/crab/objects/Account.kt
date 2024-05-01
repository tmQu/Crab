package tkpm.com.crab.objects

import com.google.gson.annotations.SerializedName

data class AccountRequest(
    val name: String = "",
    val phone: String = "",
    val password: String = "",
    @SerializedName("UID")
    val uid: String = "",
    val role: String = "",
)

data class AccountResponse(
    val token: String,
    val newUser: Boolean,
    val user: AccountRequest
)