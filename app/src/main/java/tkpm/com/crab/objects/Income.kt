package tkpm.com.crab.objects

import com.google.gson.annotations.SerializedName

data class Transaction(
    @SerializedName("_id")
    val id: String,
    val ref: String,
    val amount: Long,
    val type: String,
    val createdAt: String
)

data class Wallet(
    @SerializedName("_id")
    val id: String,
    val balance: Long,
    val transactions: List<Transaction>
)

data class Amount(
    val amount: Long
)