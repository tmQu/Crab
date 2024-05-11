package tkpm.com.crab.utils

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class PriceDisplay {
    companion object {
        fun formatVND(price: Long): String {
            val formatter = NumberFormat.getCurrencyInstance()
            formatter.currency = Currency.getInstance("VND")
            formatter.maximumFractionDigits = 0

            return formatter.format(price)
        }
    }
}