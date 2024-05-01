package tkpm.com.crab.utils

import android.util.Log

class Utils {
    companion object {
        fun formatVND(value: Int): String {

            return String.format("%,d", value)
        }
    }
}