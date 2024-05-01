package tkpm.com.crab.objects

class VehicleTypePrice {
    val typeVehicle: String = ""
    val typeName: String = ""
    val numSeat: Int = 0
    val fee: Int = 0
}

class VehilceTypePriceResponse {
    val fee: List<VehicleTypePrice> = emptyList()
}