package tkpm.com.crab.utils

class RatingFormatter {
    companion object {
        fun formatRating(rating: Double): String {
            return String.format("%.1f", rating)
        }
    }
}