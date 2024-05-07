package tkpm.com.crab.activity.driver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.sidesheet.SideSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocketListener
import okio.ByteString
import tkpm.com.crab.BuildConfig
import tkpm.com.crab.MainActivity
import tkpm.com.crab.R
import tkpm.com.crab.activity.ChangeInfoActivity
import tkpm.com.crab.activity.HistoryActivity
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.databinding.ActivityDriverMapsBinding
import tkpm.com.crab.objects.Vehicle
import tkpm.com.crab.utils.DirectionRequest
import tkpm.com.crab.utils.PriceDisplay

data class MongoLocation(
    @SerializedName("_id") val id: String, val type: String, val coordinates: List<Double>
)

data class LocationRecord(
    @SerializedName("_id") val id: String, val address: String, val location: MongoLocation
)

data class BookingInfo(
    @SerializedName("_id") val id: String,
    val pickup: LocationRecord,
    val destination: LocationRecord,
    val phone: String,
    val name: String,
    val fee: Int,
    val distance: Double,
)

data class Booking(
    @SerializedName("_id")
    val id: String,
    val status: String,
    val info: BookingInfo,
    val service: String,
    val createdAt: String,
)


class DriverMapActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        const val ONLINE = 1
        const val OFFLINE = 2
        const val BUSY = 3
    }

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityDriverMapsBinding

    private lateinit var locationProviderClient: FusedLocationProviderClient
    private lateinit var placeClient: PlacesClient

    private lateinit var bottomFunctionDriver: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomConnect: LinearLayout
    private lateinit var bottomDisconnect: LinearLayout
    private var driverStatus = OFFLINE

    private lateinit var currentLocation: LatLng

    private var currentLocationMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var pickupMarker: Marker? = null

    private lateinit var locationRequest: LocationRequest


    private val requests: MutableList<Booking> = ArrayList()
    private var currentBooking: Booking? = null
    private lateinit var requestRecyclerView: RecyclerView
//    private lateinit var requestAdapter: RequestAdapter


    private val polylines: MutableList<Polyline> = ArrayList()

    private lateinit var webSocket: WebSocket

    private fun setIncomeBooking(booking: Booking) {
        driverStatus = BUSY
        handleDriverStatus()
        val serviceName = findViewById<TextView>(R.id.service_name)
        val customerName = findViewById<TextView>(R.id.customer_name)
        val phoneBtn = findViewById<Button>(R.id.phoneBtn)
        val address = findViewById<TextView>(R.id.address)
        val priceView = findViewById<TextView>(R.id.price)
        val btnStep = findViewById<Button>(R.id.btn_step)
        val startBtnGroup = findViewById<LinearLayout>(R.id.starting_btn_group)


        btnStep.visibility = View.GONE

        priceView.text = PriceDisplay.formatVND(booking.info.fee.toLong())
        customerName.text = booking.info.name
        serviceName.text = booking.service
        address.text = booking.info.destination.address
        getDirection(
            LatLng(
                booking.info.pickup.location.coordinates[1],
                booking.info.pickup.location.coordinates[0]
            ), LatLng(
                booking.info.destination.location.coordinates[1],
                booking.info.destination.location.coordinates[0]
            ), true
        )
        clearMarkers()
        pickupMarker = mMap.addMarker(
            MarkerOptions().position(
                LatLng(
                    booking.info.pickup.location.coordinates[1],
                    booking.info.pickup.location.coordinates[0]
                )
            ).title("Pickup Location")
        )
        destinationMarker = mMap.addMarker(
            MarkerOptions().position(
                LatLng(
                    booking.info.destination.location.coordinates[1],
                    booking.info.destination.location.coordinates[0]
                )
            ).title("Destination Location")
        )
        findViewById<Button>(R.id.accept_btn).setOnClickListener {
            webSocket.acceptBooking(booking.id)
            startBtnGroup.visibility = View.GONE
            btnStep.visibility = View.VISIBLE
            getDirection(
                LatLng(currentLocation.latitude, currentLocation.longitude), LatLng(
                    booking.info.pickup.location.coordinates[1],
                    booking.info.pickup.location.coordinates[0]
                ), true
            )
            clearMarkers()
            pickupMarker = mMap.addMarker(
                MarkerOptions().position(
                    LatLng(
                        booking.info.pickup.location.coordinates[1],
                        booking.info.pickup.location.coordinates[0]
                    )
                ).title("Pickup Location")
            )
            currentLocationMarker = mMap.addMarker(
                MarkerOptions().position(currentLocation).title("Current Location")
            )

            updateBookingStatus(booking.id, "accepted")
        }
        findViewById<Button>(R.id.reject_btn).setOnClickListener {
            webSocket.rejectBooking(booking.id)
            startBtnGroup.visibility = View.GONE
            driverStatus = ONLINE
            handleDriverStatus()
            startBtnGroup.visibility = View.VISIBLE
            btnStep.visibility = View.GONE
            clearLines()
            clearMarkers()
        }
        btnStep.text = "Đã đón"

        btnStep.setOnClickListener {
            if (btnStep.text == "Đã đón") {
                getDirection(
                    LatLng(
                        booking.info.pickup.location.coordinates[1],
                        booking.info.pickup.location.coordinates[0]
                    ), LatLng(
                        booking.info.destination.location.coordinates[1],
                        booking.info.destination.location.coordinates[0]
                    ), true
                )
                clearMarkers()
                pickupMarker = mMap.addMarker(
                    MarkerOptions().position(
                        LatLng(
                            booking.info.pickup.location.coordinates[1],
                            booking.info.pickup.location.coordinates[0]
                        )
                    ).title("Pickup Location")
                )

                destinationMarker = mMap.addMarker(
                    MarkerOptions().position(
                        LatLng(
                            booking.info.destination.location.coordinates[1],
                            booking.info.destination.location.coordinates[0]
                        )
                    ).title("Destination Location")
                )

                btnStep.text = "Đã trả"
            } else if (btnStep.text == "Đã trả") {
                updateBookingStatus(booking.id, "completed")
            }
        }
        phoneBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + booking.info.phone))
            startActivity(intent)
        }
    }

    private fun updateBookingStatus(bookingId: String, status: String) {
        val driverId = CredentialService().getAll().id
        APIService().doPatch<Any>("bookings",
            mapOf("id" to bookingId, "status" to status, "driver" to driverId),
            object : APICallback<Any> {
                override fun onSuccess(data: Any) {

                }

                override fun onError(error: Throwable) {
                    Toast.makeText(
                        this@DriverMapActivity,
                        "Không thể cập nhật thông tin đơn hàng",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.i("DriverMapActivity", "${error.message}")
                }
            })
    }

    fun getBookingById(id: String) {
        APIService().doGet<Booking>("bookings/${id}", object : APICallback<Any> {
            override fun onSuccess(data: Any) {
                data as Booking
                currentBooking = data
                setIncomeBooking(data)
            }

            override fun onError(error: Throwable) {
                Toast.makeText(
                    this@DriverMapActivity,
                    "Không thể lấy thông tin đơn hàng",
                    Toast.LENGTH_SHORT
                ).show()
                Log.i("DriverMapActivity", "${error.message}")
            }
        })
    }

    private fun handleDriverStatus() {
        if (driverStatus == ONLINE) {
            bottomDisconnect.visibility = View.VISIBLE
            bottomConnect.visibility = View.GONE
            bottomFunctionDriver.state = BottomSheetBehavior.STATE_HIDDEN
        } else if (driverStatus == OFFLINE) {
            bottomDisconnect.visibility = View.GONE
            bottomConnect.visibility = View.VISIBLE
            bottomFunctionDriver.state = BottomSheetBehavior.STATE_HIDDEN
        } else if (driverStatus == BUSY) {
            bottomDisconnect.visibility = View.GONE
            bottomConnect.visibility = View.GONE
            bottomFunctionDriver.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_driver_maps)

        // // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bottomFunctionDriver = BottomSheetBehavior.from(findViewById(R.id.driver_function_card))
        bottomFunctionDriver.isHideable = true
        bottomFunctionDriver.isDraggable = false
        bottomFunctionDriver.state = BottomSheetBehavior.STATE_HIDDEN

        bottomConnect = findViewById(R.id.connection_bottom)
        bottomDisconnect = findViewById(R.id.disconnect_bottom)

        webSocket = WebSocket(this)

        driverStatus = OFFLINE
        handleDriverStatus()

        findViewById<Button>(R.id.connection_btn).setOnClickListener {
            APIService().doGet<Vehicle>("accounts/${CredentialService().get()}/vehicles",
                object : APICallback<Any> {
                    override fun onSuccess(data: Any) {
                        data as Vehicle
                        val isVehicleAvailable = (data.type != "")

                        if (!isVehicleAvailable) {
                            Toast.makeText(
                                this@DriverMapActivity,
                                "Hãy cập nhật thông tin phương tiện để nhận cuốc",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }

                        // Update driver status
                        driverStatus = ONLINE
                        webSocket.connectWebSocket(BuildConfig.BASE_URL_WS)
                        webSocket.driverOnline()
                        webSocket.updateVehicle(data.type)
                        webSocket.updateLocation(
                            currentLocation.latitude, currentLocation.longitude
                        )
                        handleDriverStatus()
                    }

                    override fun onError(error: Throwable) {
                        Log.i("DriverMapActivity", "Error fetching account")
                        Toast.makeText(
                            this@DriverMapActivity,
                            "Lấy thông tin tài khoản thất bại",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }

        findViewById<Button>(R.id.disconnect_btn).setOnClickListener {
            driverStatus = OFFLINE
            webSocket.driverOffline()
            webSocket.closeWebSocket()
            clearLines()
            clearMarkers()
            handleDriverStatus()
        }

        // Show left menu
        findViewById<Button>(R.id.left_menu_button).setOnClickListener {
            SideSheetBehavior.from(findViewById(R.id.left_driver_menu)).state =
                SideSheetBehavior.STATE_EXPANDED
        }

        // Set function to show the user information button
        findViewById<Button>(R.id.left_menu_user_info).setOnClickListener {
            val intent = Intent(this, ChangeInfoActivity::class.java)
            startActivity(intent)
        }

        // Set function to show the vehicle information button
        findViewById<Button>(R.id.left_menu_vehicle_info).setOnClickListener {
            val intent = Intent(this, ChangeVehicleInfo::class.java)
            startActivity(intent)
        }

        // Set function to show driver income
        findViewById<Button>(R.id.left_menu_income).setOnClickListener {
            val intent = Intent(this, DriverIncomeActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.left_menu_history).setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        // Set function for the logout button
        findViewById<Button>(R.id.left_menu_logout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            CredentialService().erase()

            // Clear all activities and start the login activity
            finishAffinity()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            currentLocation = LatLng(
                locationResult.lastLocation!!.latitude, locationResult.lastLocation!!.longitude
            )
            Log.i("SHIT!", "callback")

            currentLocationMarker?.position = currentLocation
            // webSocket.updateLocation(currentLocation.latitude, currentLocation.longitude)

        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        checkLocationPermission()

        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        locationProviderClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        ).continueWith {
            val lastLocation = locationProviderClient.lastLocation
            lastLocation.addOnSuccessListener {
                currentLocation = LatLng(it.latitude, it.longitude)
                currentLocationMarker = mMap.addMarker(
                    MarkerOptions().position(currentLocation).title("Current Location")
                )
                webSocket.updateLocation(currentLocation.latitude, currentLocation.longitude)
                centreCameraOnLocation(currentLocation)
            }
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            }
        }

    }

    // unchecked, used with caution
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 1
            )
        }
    }


    private fun centreCameraOnLocation(location: LatLng, zoom: Float = 15f) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoom))
    }

    private fun clearMarkers() {
        currentLocationMarker?.remove()
        destinationMarker?.remove()
        pickupMarker?.remove()
    }

    private fun clearLines() {
        for (line in polylines) {
            line.remove()
        }
        polylines.clear()
    }

    fun getDirection(origin: LatLng?, destination: LatLng?, clear: Boolean = false) {
        if (clear) {
            clearLines()
        }
//        centreCameraOnLocation(origin!!)
        mMap.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                LatLngBounds.Builder().include(origin!!).include(destination!!).build(), 50
            )
        )
        // val url =
        //     "https://maps.googleapis.com/maps/api/directions/json?origin=${origin?.latitude},${origin?.longitude}&destination=${destination?.latitude},${destination?.longitude}&key=${BuildConfig.MAPS_API_KEY}&mode=driving"

        val directionRequest = DirectionRequest(mMap, origin, destination, polylines)
        directionRequest.execute()
    }

    internal class WebSocket(activity: DriverMapActivity) {
        internal class BookingSocket(
            val bookingId: String, val response: String
        )

        internal class BookingRequest(
            val event: String, val bookingId: String
        )

        internal class BookingResponse(
            val event: String, val role: String, val booking: BookingSocket
        )

        private var webSocket: okhttp3.WebSocket? = null

        // WebSocket listener
        private val webSocketListener: WebSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
                Log.d(TAG, "Connected to WebSocket server")
                // Perform actions when connection is established
            }

            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
                // Handle incoming messages from the server
                val gson = Gson()
                val bookingRequest = gson.fromJson(text, BookingRequest::class.java)
                Log.d(
                    TAG, "Event: ${bookingRequest.event}, Booking ID: ${bookingRequest.bookingId}"
                )
                if (bookingRequest.event == "newBooking") {
                    // Handle new booking request
                    // Fetch booking details
                    // Display booking details to driver
                    // Show accept/reject buttons
                    // Send response to server
                    // Update UI

                    activity.getBookingById(bookingRequest.bookingId)

                } else if (bookingRequest.event == "bookingTimout") {
                    // Handle booking cancellation

                }
            }

            override fun onMessage(webSocket: okhttp3.WebSocket, bytes: ByteString) {
                Log.d(TAG, "Received bytes: " + bytes.hex())
                // Handle incoming bytes from the server
            }

            override fun onClosing(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closing connection")
                // Perform actions before connection is closed
            }

            override fun onFailure(
                webSocket: okhttp3.WebSocket, t: Throwable, response: Response?
            ) {
                Log.e(TAG, "Error: " + t.message)
                // Handle errors
            }
        }

        // Connect to WebSocket server
        fun connectWebSocket(serverUrl: String) {
            val client = OkHttpClient()
            val request: Request = Request.Builder().url(serverUrl).build()
            webSocket = client.newWebSocket(request, webSocketListener)
        }

        internal class DriverStatus(
            val event: String, val role: String
        )

        fun driverOnline() {
            val gson = Gson()
            val message = gson.toJson(DriverStatus("driverOnline", "driver"))
            sendMessage(message)
        }

        fun driverOffline() {
            val gson = Gson()
            val message = gson.toJson(DriverStatus("driverOffline", "driver"))
            sendMessage(message)
        }

        internal class VehicleType(
            val event: String,
            val vehicle: String
        )

        fun updateVehicle(vehicleType: String) {
            val gson = Gson()
            val message = gson.toJson(VehicleType("updateVehicle", vehicleType))
            sendMessage(message)
        }

        internal class LocationUpdate(
            val event: String, val role: String, val lat: Double, val lng: Double
        )

        fun updateLocation(lat: Double, lng: Double) {
            Log.i(TAG, "Update location: $lat, $lng")
            val gson = Gson()
            val bookingRequest = LocationUpdate("locationUpdate", "driver", lat, lng)
            val message = gson.toJson(bookingRequest)
            sendMessage(message)
        }


        fun acceptBooking(bookingId: String) {
            val gson = Gson()
            val bookingRequest =
                BookingResponse("bookingResponse", "driver", BookingSocket(bookingId, "accept"))
            val message = gson.toJson(bookingRequest)
            sendMessage(message)
        }

        fun rejectBooking(bookingId: String) {
            val gson = Gson()
            val bookingRequest =
                BookingResponse("bookingResponse", "driver", BookingSocket(bookingId, "reject"))
            val message = gson.toJson(bookingRequest)
            sendMessage(message)
        }

        // Send text message
        fun sendMessage(message: String?) {
            if (webSocket != null) {
                webSocket!!.send(message!!)
            }
        }

        // Send binary message
        fun sendBytes(bytes: ByteString?) {
            if (webSocket != null) {
                webSocket!!.send(bytes!!)
            }
        }

        // Close WebSocket connection
        fun closeWebSocket() {
            if (webSocket != null) {
                webSocket!!.close(1000, "Closing connection")
            }
        }

        companion object {
            private const val TAG = "WebSocketManager"
        }
    }


}