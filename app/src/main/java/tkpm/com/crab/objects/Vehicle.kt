package tkpm.com.crab.objects

data class Vehicle (
    val user: String,
    val type: String,
    val plate: String,
    val description: String,
)

data class VehicleValidation (
    val data: Boolean
)