package tkpm.com.crab.objects

//name: string;
//address: string;
//latitude: number;
//longitude: number;
//rating: number;
//user_ratings_total: number;
//imageUrl: string;
data class Suggestion (
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val user_ratings_total: Int,
    val imageUrl: String
)