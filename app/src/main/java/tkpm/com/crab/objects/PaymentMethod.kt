package tkpm.com.crab.objects

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PaymentMethodRequest(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val number: String,
    val exp: String,
    val cvv: String
)

data class PaymentMethodResponse(
    val data: List<PaymentMethodRequest>
)

data class PaymentMethodSerializable(
    val id: String,
    val name: String,
    val number: String,
    val exp: String,
    val cvv: String
) : Serializable