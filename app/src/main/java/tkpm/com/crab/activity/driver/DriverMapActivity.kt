package tkpm.com.crab.activity.driver

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocketListener
import okio.ByteString
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import tkpm.com.crab.BuildConfig
import tkpm.com.crab.MainActivity
import tkpm.com.crab.R
import tkpm.com.crab.activity.ChangeInfoActivity
import tkpm.com.crab.activity.HistoryActivity
import tkpm.com.crab.activity.customer.CustomerMapsActivity
import tkpm.com.crab.adapter.CustomWindowInfo
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.databinding.ActivityDriverMapsBinding
import tkpm.com.crab.dialog.TimeOutDialog
import tkpm.com.crab.objects.Booking
import tkpm.com.crab.objects.Vehicle
import tkpm.com.crab.utils.DirectionRequest
import tkpm.com.crab.utils.PriceDisplay


class DriverMapActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        const val ONLINE = 1
        const val OFFLINE = 2
        const val BUSY = 3

        const val CHOOSE_LOCATION = 1
        const val CHOOSE_VEHICLE = 2
        const val WAIT_DRIVER = 3
        const val DRIVER_COMING = 4
        const val DRIVER_ARRIVED = 5
        const val PICK_UP = 6
        const val FINISH_TRIP = 7

    }
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object: Runnable {
        override fun run() {
            Log.i("timeout", timeOut.toString())
            findViewById<TextView>(R.id.timeout).text = "${timeOut}s"
            timeOut -= 1
            if (timeOut == 0) {
                findViewById<LinearLayout>(R.id.timeout_group).visibility = View.GONE
                bottomFunctionDriver.state = BottomSheetBehavior.STATE_HIDDEN
                handler.removeCallbacksAndMessages(null)
            }
            else
                handler.postDelayed(this, 1000)
        }
    }
    private var tripStatus = -1
    private var timeOut = 0
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityDriverMapsBinding

    private lateinit var locationProviderClient: FusedLocationProviderClient
    private lateinit var placeClient: PlacesClient

    private lateinit var bottomFunctionDriver: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomConnect: LinearLayout
    private lateinit var bottomDisconnect: LinearLayout
    private var driverStatus = OFFLINE

    private lateinit var currentLocation: LatLng

    //    private var currentLocationMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var pickupMarker: Marker? = null
    private var driverMarker: Marker?=null
    private lateinit var locationRequest: LocationRequest


    private val requests: MutableList<Booking> = ArrayList()
    private var currentBooking: Booking? = null
    private lateinit var requestRecyclerView: RecyclerView
    private lateinit var vehicleType: String


    private val polylines: MutableList<Polyline> = ArrayList()

    private lateinit var webSocket: WebSocket

    private val startCompleteOrderForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Handle the returned result
                // Close all fragments and back to default fragment to wait for new booking
                driverStatus = ONLINE
                webSocket.connectWebSocket(BuildConfig.BASE_URL_WS)
                webSocket.driverOnline()
                webSocket.updateVehicle(vehicleType)
                webSocket.updateLocation(
                    currentLocation.latitude, currentLocation.longitude
                )
                handleDriverStatus()
                clearLines()
                clearMarkers()
                tripStatus = -1

                // Clear the current booking
                currentBooking = null
            }

            if (result.resultCode == Activity.RESULT_CANCELED) {
                // Handle the cancelled result
                // Close all fragments, back to default fragment and set the driver status to offline
                driverStatus = OFFLINE
                webSocket.driverOffline()
                webSocket.closeWebSocket()
                handleDriverStatus()

                // Clear the current booking
                currentBooking = null
                tripStatus = -1

                // Clear the map
                clearLines()
                clearMarkers()
            }
        }

    private fun getBooking() {
        val user = CredentialService().getAll()
        val obj = JsonObject()
        obj.addProperty("id", user.id)
        obj.addProperty("role", user.role)
        APIService().doPost<Booking>(
            "bookings/check-progress-booking",
            obj,
            object : APICallback<Any> {
                override fun onSuccess(data: Any) {
                    currentBooking = data as Booking
                    driverStatus = BUSY
                    vehicleType = currentBooking?.vehicle ?: ""
                    when (currentBooking?.status) {
                        "accepted" -> {
                            tripStatus = DRIVER_COMING
                            handleDriverStatus()
                        }

                        "arrived-at-pick-up" -> {
                            tripStatus = DRIVER_ARRIVED
                            handleDriverStatus()
                        }

                        "pick-up" -> {
                            tripStatus = CustomerMapsActivity.PICK_UP
                            handleDriverStatus()
                        }
                    }
                    getDeviceLocation()
                }

                override fun onError(error: Throwable) {
                    Log.e("API_SERVICE", "${error.message}")

                }
            })
    }

    private fun getDeviceLocation() {
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            val locationRequest = locationProviderClient.lastLocation
            locationRequest.addOnCompleteListener {
                if (it.isSuccessful) {
                    val location = it.result
                    if (location != null) {
                        currentLocation = LatLng(location.latitude, location.longitude)
                    }
                }

                setIncomeBooking(currentBooking!!, false)

            }
        } catch (error: SecurityException) {
            Log.e("SET_CURRENT_LOCATION", "Error getting device location: ${error.message}")
        }
    }

    private fun setIncomeBooking(booking: Booking, newBooking: Boolean = true) {
        driverStatus = BUSY
        handleDriverStatus()
        val serviceName = findViewById<TextView>(R.id.service_name)
        val customerName = findViewById<TextView>(R.id.customer_name)
        val phoneBtn = findViewById<Button>(R.id.phoneBtn)
        val address = findViewById<TextView>(R.id.address)
        val priceView = findViewById<TextView>(R.id.price)
        val btnStep = findViewById<Button>(R.id.btn_step)
        val startBtnGroup = findViewById<LinearLayout>(R.id.starting_btn_group)
        Log.i("timeout", "status " + bottomFunctionDriver.isHideable)

        btnStep.visibility = View.GONE
        val payment = if(booking.info.payment_method == "cash") "Tiền mặt" else "Ví điện tử"
        priceView.text = "${PriceDisplay.formatVND(booking.info.fee.toLong())} - ${payment}"
        customerName.text = booking.info.name
        serviceName.text = booking.service
        address.text = booking.info.destination.address


        if (newBooking)
        {
            tripStatus = -1
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
                ).title("Pickup Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).snippet(booking.info.pickup.address)
            )
            destinationMarker = mMap.addMarker(
                MarkerOptions().position(
                    LatLng(
                        booking.info.destination.location.coordinates[1],
                        booking.info.destination.location.coordinates[0]
                    )
                ).title("Destination Location").snippet(booking.info.destination.address)
            )
        }

        findViewById<Button>(R.id.accept_btn).setOnClickListener {
            handler.removeCallbacks(runnable)
            webSocket.acceptBooking(booking.id)
            startBtnGroup.visibility = View.GONE
            btnStep.visibility = View.VISIBLE
            drawDriverToPickup(booking)
            tripStatus = DRIVER_COMING
            handleTripStatus(booking, btnStep, startBtnGroup)
            updateBookingStatusWithLatLng(
                booking.id,
                "accepted",
                currentLocation.latitude,
                currentLocation.longitude
            )
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

        handleTripStatus(booking, btnStep, startBtnGroup)
        if(tripStatus == -1)
        {
            startBtnGroup.visibility = View.VISIBLE
        }
        else {
            startBtnGroup.visibility = View.GONE
        }
        val headerStatus = findViewById<TextView>(R.id.status_header)
        val navigateBtn = findViewById<Button>(R.id.rideRequestNavButton)
        btnStep.setOnClickListener {
            if (tripStatus == DRIVER_COMING) {
                tripStatus = DRIVER_ARRIVED
                updateBookingStatus(booking.id, "arrived-at-pick-up")
                btnStep.text = "Đã đón"
                headerStatus.text = "Đón khách"
                navigateBtn.setOnClickListener {
                    navigateMap(LatLng(booking.info.pickup.location.coordinates[1], booking.info.pickup.location.coordinates[0]))
                }
            } else if (tripStatus == DRIVER_ARRIVED) {
                drawPickUpToDes(booking)
                tripStatus = PICK_UP
                updateBookingStatus(booking.id, "pick-up")
                btnStep.text = "Đã trả"
                headerStatus.text = "Chở khách"
                navigateBtn.setOnClickListener {
                    navigateMap(LatLng(booking.info.destination.location.coordinates[1], booking.info.destination.location.coordinates[0]))
                }
            } else if (tripStatus == PICK_UP) {
                updateBookingStatus(booking.id, "completed")



                // Change to default status
                btnStep.text = "Đã tới"
                startBtnGroup.visibility = View.GONE
                tripStatus = FINISH_TRIP
            }
        }
        phoneBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + booking.info.phone))
            startActivity(intent)
        }
    }

    private fun navigateMap(location: LatLng)
    {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=${location.latitude},${location.longitude}")
            )
            intent.setPackage("com.google.android.apps.maps")
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("DriverMapActivity", "Error opening map: ${e.message}")
            Toast.makeText(this, "Tải google map để sử dụng chức năng này", Toast.LENGTH_SHORT).show()
        }
    }
    private fun handleTripStatus(booking: Booking, btnStep: Button, startBtnGroup: LinearLayout)
    {
        val navigateBtn = findViewById<Button>(R.id.rideRequestNavButton)
        val timeOutGroup = findViewById<LinearLayout>(R.id.timeout_group)
        val headerStatus = findViewById<TextView>(R.id.status_header)
        Log.i("timeout", "status " + tripStatus.toString())
        timeOutGroup.visibility = View.GONE
        when(tripStatus)
        {
            WAIT_DRIVER -> {
                btnStep.visibility = View.GONE
                startBtnGroup.visibility = View.VISIBLE
            }

            DRIVER_COMING -> {
                Log.i("DriverMapActivity", "Driver coming")
                drawDriverToPickup(booking)
                Log.i("DriverMapActivity", "Driver coming")
                btnStep.text = "Đã tới"
                btnStep.visibility = View.VISIBLE
                startBtnGroup.visibility = View.GONE
                navigateBtn.visibility = View.VISIBLE
                navigateBtn.setOnClickListener {
                    navigateMap(LatLng(booking.info.pickup.location.coordinates[1], booking.info.pickup.location.coordinates[0]))
                }
                headerStatus.text = "Đón khách"
            }

            DRIVER_ARRIVED -> {
                drawPickUpToDes(booking)
                btnStep.text = "Đã đón"
                headerStatus.text = "Đón khách"
                btnStep.visibility = View.VISIBLE
                startBtnGroup.visibility = View.GONE
                navigateBtn.visibility = View.VISIBLE
                navigateBtn.setOnClickListener {
                    navigateMap(LatLng(booking.info.destination.location.coordinates[1], booking.info.destination.location.coordinates[0]))
                }
            }

            PICK_UP -> {
                drawPickUpToDes(booking)
                btnStep.text = "Đã trả"
                headerStatus.text = "Chở khách"
                btnStep.visibility = View.VISIBLE
                startBtnGroup.visibility = View.GONE
                navigateBtn.visibility = View.VISIBLE
                navigateBtn.setOnClickListener {
                    navigateMap(LatLng(booking.info.destination.location.coordinates[1], booking.info.destination.location.coordinates[0]))
                }
            }

            FINISH_TRIP -> {
                btnStep.visibility = View.GONE
                startBtnGroup.visibility = View.VISIBLE
            }
            else -> {
                btnStep.visibility = View.GONE
                startBtnGroup.visibility = View.VISIBLE
                if(timeOut != 0)
                {
                    Log.i("timeout", timeOut.toString())
                    timeOutGroup.visibility = View.VISIBLE
                    val gifDrawable = GifDrawable(resources, R.drawable.ic_wait)
                    findViewById<GifImageView>(R.id.gif_timeout).setImageDrawable(gifDrawable)
                    gifDrawable.start()
                    Log.i("timeout", gifDrawable.duration.toString())

                    handler.postDelayed(runnable, 1000)
                }
                navigateBtn.visibility = View.INVISIBLE
                navigateBtn.setOnClickListener {
                    // cancel listener
                }
                headerStatus.text = "Cuốc mới"
            }
        }
    }

    private fun drawPickUpToDes(booking: Booking) {
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
            ).title("Pickup Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).snippet(booking.info.pickup.address)

        )

        destinationMarker = mMap.addMarker(
            MarkerOptions().position(
                LatLng(
                    booking.info.destination.location.coordinates[1],
                    booking.info.destination.location.coordinates[0]
                )
            ).title("Destination Location").snippet(booking.info.destination.address)
        )
    }

    private fun drawDriverToPickup(booking: Booking) {
        getDirection(
            LatLng(currentLocation.latitude, currentLocation.longitude), LatLng(
                booking.info.pickup.location.coordinates[1],
                booking.info.pickup.location.coordinates[0]
            ), true
        )
        clearMarkers()
        driverMarker = mMap.addMarker(
            MarkerOptions().position(
                LatLng(currentLocation.latitude, currentLocation.longitude)
            ).title("Your position").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        pickupMarker = mMap.addMarker(
            MarkerOptions().position(
                LatLng(
                    booking.info.pickup.location.coordinates[1],
                    booking.info.pickup.location.coordinates[0]
                )
            ).title("Pickup Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).snippet(booking.info.pickup.address)

        )
    }

    private fun updateBookingStatusWithLatLng(
        bookingId: String,
        status: String,
        lat: Double = 0.0,
        lng: Double = 0.0
    ) {
        val driverId = CredentialService().getAll().id
        val obj = JsonObject()
        obj.addProperty("id", bookingId)
        obj.addProperty("status", status)
        obj.addProperty("driver", driverId)
        obj.addProperty("driverLat", lat)
        obj.addProperty("driverLng", lng)
        APIService().doPatch<Any>("bookings", obj, object : APICallback<Any> {
            override fun onSuccess(data: Any) {

            }

            override fun onError(error: Throwable) {
//                Toast.makeText(this@DriverMapActivity, "Error updating booking", Toast.LENGTH_SHORT).show()
//                Log.i("DriverMapActivity", "${error.message}")
            }
        })
    }

    private fun updateBookingStatus(bookingId: String, status: String) {
        val driverId = CredentialService().getAll().id
        APIService().doPatch<Any>("bookings",
            mapOf("id" to bookingId, "status" to status, "driver" to driverId),
            object : APICallback<Any> {
                override fun onSuccess(data: Any) {
                    // Open complete order activity
                    if(tripStatus == FINISH_TRIP)
                    {
                        val intent = Intent(this@DriverMapActivity, CompleteOrderActivity::class.java)
                        intent.putExtra("booking_id", currentBooking?.id)
                        startCompleteOrderForResult.launch(intent)
                    }

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
        Log.i("timeout", "status driver" + driverStatus.toString())
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
            BottomSheetBehavior.STATE_SETTLING
            Log.i("timeout", "status ub " + bottomFunctionDriver.state)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        fun getNavBarHeight(context: Context): Int {
            val resources: Resources = context.resources
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                Log.i(
                    "DriverMapActivity",
                    "NavBar height: ${resources.getDimensionPixelSize(resourceId)}"
                )
                return (resources.getDimensionPixelSize(resourceId) * 1.25).toInt()
            }
            return 0
        }

        findViewById<ConstraintLayout>(R.id.driver_function_card).setPadding(
            0,
            0,
            0,
            getNavBarHeight(this)
        )

        findViewById<LinearLayout>(R.id.connection_bottom).setPadding(
            0,
            0,
            0,
            getNavBarHeight(this)
        )

        findViewById<LinearLayout>(R.id.disconnect_bottom).setPadding(
            0,
            0,
            0,
            getNavBarHeight(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_driver_maps)

        checkLocationPermission()

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
            // Check if there is no current location -> Do not allow to connect
            if (!::currentLocation.isInitialized) {
                Toast.makeText(
                    this@DriverMapActivity,
                    "Không thể kết nối khi chưa bật vị trí",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

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

                        // Update vehicle type
                        vehicleType = data.type

                        // Update driver status
                        driverStatus = ONLINE
                        webSocket.connectWebSocket(BuildConfig.BASE_URL_WS)
                        webSocket.driverOnline()
                        webSocket.updateVehicle(vehicleType)
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

        // Set function for here button
        findViewById<ImageButton>(R.id.here_btn).setOnClickListener {
            // Zoom in current location if available
            locationProviderClient = LocationServices.getFusedLocationProviderClient(this)

            try {
                val locationRequest = locationProviderClient.lastLocation
                locationRequest.addOnCompleteListener {
                    if (it.isSuccessful) {
                        val location = it.result
                        if (location != null) {
                            val latLng = LatLng(location.latitude, location.longitude)
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        }
                    }
                }
            } catch (error: SecurityException) {
                Log.e("SET_CURRENT_LOCATION", "Error getting device location: ${error.message}")
            }
        }
    }


    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            currentLocation = LatLng(
                locationResult.lastLocation!!.latitude, locationResult.lastLocation!!.longitude
            )
            Log.i("DEBUG", "callback")

//            currentLocationMarker?.position = currentLocation
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

        // Disable zoom controls
        mMap.uiSettings.isZoomControlsEnabled = false

        // Disable my location button
        mMap.uiSettings.isMyLocationButtonEnabled = false

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        mMap.setInfoWindowAdapter(CustomWindowInfo(this))

        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isCompassEnabled = false

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
                    if (it != null) {
                        currentLocation = LatLng(it.latitude, it.longitude)
//                        currentLocationMarker = mMap.addMarker(
//                            MarkerOptions().position(currentLocation).title("Current Location")
//                        )
                        webSocket.updateLocation(
                            currentLocation.latitude,
                            currentLocation.longitude
                        )
                        centreCameraOnLocation(currentLocation)
                    } else {
                        val defaultLocation = LatLng(15.941288938934974, 107.7343606079183)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f))
                    }
            }
        }

        getBooking()

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
//        currentLocationMarker?.remove()
        driverMarker?.remove()
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
            val event: String, val bookingId: String, val timeOut: Int = 0
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
                    activity.timeOut = bookingRequest.timeOut / 1000
                    activity.getBookingById(bookingRequest.bookingId)

                } else if (bookingRequest.event == "bookingTimeout") {
                    // Handle booking cancellation
                    Log.i("timeout", "handle timeout")
                    activity.handleTimeOut()
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

    fun handleTimeOut() {
        runOnUiThread{
            driverStatus = OFFLINE
            webSocket.driverOffline()
            webSocket.closeWebSocket()
            handleDriverStatus()

            // Clear the current booking
            currentBooking = null
            Log.i("timeout", "handle timout funct")
            // Clear the map
            clearLines()
            clearMarkers()

            val timeOutDialog = TimeOutDialog()
            timeOutDialog.show(supportFragmentManager, "timeout")
        }


    }


}