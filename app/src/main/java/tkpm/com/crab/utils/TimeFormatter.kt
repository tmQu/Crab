package tkpm.com.crab.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class TimeFormatter {
    companion object {
        fun GMT7Formatter(originalTime: String): String {
            val originalFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            originalFormat.timeZone = TimeZone.getTimeZone("GMT+0")
            val targetFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
            targetFormat.timeZone = TimeZone.getTimeZone("GMT+7")
            val date = originalFormat.parse(originalTime)
            return targetFormat.format(date!!)
        }
    }

}