package tkpm.com.crab.objects

data class BaseResponse<T>  (
    val success: Boolean,
    val message: String,
    val data: T
)