package tkpm.com.crab.utils

import java.text.NumberFormat
import java.util.Locale

class PriceDisplay {
    companion object {
        fun formatVND(price: Long): String {
            return NumberFormat.getNumberInstance(Locale.US)
                .format(price) + " VNĐ"
        }
    }
}