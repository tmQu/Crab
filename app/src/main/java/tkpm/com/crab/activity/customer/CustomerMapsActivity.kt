package tkpm.com.crab.activity.customer


import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.sidesheet.SideSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import org.json.JSONException
import org.json.JSONObject
import tkpm.com.crab.BuildConfig
import tkpm.com.crab.MainActivity
import tkpm.com.crab.R
import tkpm.com.crab.activity.ChangeInfoActivity
import tkpm.com.crab.activity.HistoryActivity
import tkpm.com.crab.activity.SuggestionActivity
import tkpm.com.crab.adapter.CustomWindowInfo
import tkpm.com.crab.adapter.MapPredictionAdapter
import tkpm.com.crab.adapter.TypeVehicleAdapter
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.constant.NOTIFICATION
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.objects.Booking
import tkpm.com.crab.objects.BookingRequest
import tkpm.com.crab.objects.BookingVehilce
import tkpm.com.crab.objects.Message
import tkpm.com.crab.objects.PaymentMethodSerializable
import tkpm.com.crab.objects.VehicleTypePrice
import tkpm.com.crab.objects.VehilceTypePriceResponse
import tkpm.com.crab.utils.DirectionRequest
import java.net.URL
import java.util.concurrent.Executors


class CustomerMapsActivity : AppCompatActivity(), OnMapReadyCallback {
    val REQUEST_CODE_PERMISSION = 1
    val TAG = "MapsActivity"

    companion object {
        const val CHOOSE_LOCATION = 1
        const val CHOOSE_VEHICLE = 2
        const val WAIT_DRIVER = 3
        const val DRIVER_COMING = 4
        const val DRIVER_ARRIVED = 5
        const val PICK_UP = 6
        const val FINISH_TRIP = 7
    }

    private var tripStatus = -1

    // map
    private lateinit var mMap: GoogleMap

    private lateinit var locationProviderClient: FusedLocationProviderClient

    private var locationPermissionGranted = false
    private lateinit var placesClient: PlacesClient
    private lateinit var adapterMapPrediction: MapPredictionAdapter

    private val sessionToken = AutocompleteSessionToken.newInstance()
    private lateinit var autocomplete_addr: AutoCompleteTextView


    // bottom sheet
    private lateinit var bottomChooseVehicle: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomChooseLocation: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomWaitDriver: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomDriverComing: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomDirverArrived: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomFinishTrip: BottomSheetBehavior<ConstraintLayout>

    private lateinit var leftUserMenu: SideSheetBehavior<ConstraintLayout>
    private lateinit var userMenuButton: Button

    // driver infor, booking info
    private lateinit var booking: Booking
    private var booking_id = ""
    private var driver_id = ""
    private var driverLat: Double = 0.0
    private var driverLng: Double = 0.0


    private val handler = Handler(Looper.getMainLooper())
    private var visaId = ""

    // Marker
    private var destinationMarker: Marker? = null
    private var currentMarker: Marker? = null
    private var driverMarker: Marker? = null

    private var suggestionLat = 0.0
    private var suggestionLng = 0.0

    private val polylines: MutableList<Polyline> = ArrayList()

    private var distance = 0L
    private var duration = 0L

    private var destinationAddress = ""
    private var currentAddress = ""

    private var vehicleType: VehicleTypePrice? = null

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private val myBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra("message")
            val bookingId = intent?.getStringExtra("booking_id")
            if (message == NOTIFICATION.DRIVER_COMMING) {
                tripStatus = DRIVER_COMING
                booking_id = bookingId ?: ""
                driverLat = intent.getDoubleExtra("driver_lat", 0.0)
                driverLng = intent.getDoubleExtra("driver_lng", 0.0)
                handleBottomSheet()
            }
            if (message == NOTIFICATION.DRIVER_ARRIVED) {
                tripStatus = DRIVER_ARRIVED
                handleBottomSheet()
            }
            if (message == NOTIFICATION.PICK_UP) {
                tripStatus = PICK_UP
                handleBottomSheet()
            }

            if (message == NOTIFICATION.FINISH_TRIP) {
                tripStatus = FINISH_TRIP
                handleBottomSheet()
            }
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
                // convert 16dp to px
                val dp = 16
                val px = (dp * resources.displayMetrics.density).toInt()
                return (resources.getDimensionPixelSize(resourceId))
            }
            return 0
        }

        val bottoms = listOf(
            findViewById<ConstraintLayout>(R.id.bottom_type_vehicle),
            findViewById<ConstraintLayout>(R.id.bottom_choose_location),
            findViewById<ConstraintLayout>(R.id.bottom_wait_driver),
            findViewById<ConstraintLayout>(R.id.bottom_driver_coming),
            findViewById<ConstraintLayout>(R.id.bottom_driver_arrived),
            findViewById<ConstraintLayout>(R.id.bottom_finish_trip),
        )

        for (bottom in bottoms) {
            bottom.setPadding(0, 0, 0, getNavBarHeight(this))
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_maps)

        userMenuButton = findViewById(R.id.left_menu_button)

        registerReceiver(
            myBroadcastReceiver,
            IntentFilter(NOTIFICATION.ACTION_NAME),
            RECEIVER_EXPORTED
        )

        checkLocationPermissions()

        val latSuggest = intent.getDoubleExtra("lat", 0.0)
        val longSuggest = intent.getDoubleExtra("long", 0.0)
        if(latSuggest != 0.0 && longSuggest != 0.0)
        {
            suggestionLat = latSuggest
            suggestionLng = longSuggest
        }

        // Get current booking


        // Set marker for current location
        setCurrentLocation()

        // Define a variable to hold the Places API key.
        val apiKey = BuildConfig.MAPS_API_KEY

        // Log an error if apiKey is not set.
        if (apiKey.isEmpty()) {
            Log.e("Places test", "No api key")
            finish()
            return
        }

        Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        placesClient = Places.createClient(this)
        adapterMapPrediction = MapPredictionAdapter(this)

        autocomplete_addr = findViewById(R.id.autocomplete_addr)
        bottomChooseVehicle = BottomSheetBehavior.from(findViewById(R.id.bottom_type_vehicle))
        bottomChooseLocation = BottomSheetBehavior.from(findViewById(R.id.bottom_choose_location))
        bottomWaitDriver = BottomSheetBehavior.from(findViewById(R.id.bottom_wait_driver))
        bottomDriverComing = BottomSheetBehavior.from(findViewById(R.id.bottom_driver_coming))
        bottomDirverArrived = BottomSheetBehavior.from(findViewById(R.id.bottom_driver_arrived))
        bottomFinishTrip = BottomSheetBehavior.from(findViewById(R.id.bottom_finish_trip))

        leftUserMenu = SideSheetBehavior.from(findViewById(R.id.left_menu))

        bottomChooseVehicle.isHideable = true
        bottomChooseLocation.isHideable = true
        bottomWaitDriver.isHideable = true
        bottomDriverComing.isHideable = true
        bottomDirverArrived.isHideable = true
        bottomFinishTrip.isHideable = true


        bottomChooseVehicle.state = BottomSheetBehavior.STATE_HIDDEN
        bottomChooseLocation.state = BottomSheetBehavior.STATE_HIDDEN
        bottomWaitDriver.state = BottomSheetBehavior.STATE_HIDDEN
        bottomDriverComing.state = BottomSheetBehavior.STATE_HIDDEN
        bottomDirverArrived.state = BottomSheetBehavior.STATE_HIDDEN
        bottomFinishTrip.state = BottomSheetBehavior.STATE_HIDDEN

        bottomWaitDriver.isDraggable = false
        bottomDriverComing.isDraggable = false
        bottomDirverArrived.isDraggable = false



        leftUserMenu.state = SideSheetBehavior.STATE_HIDDEN
        leftUserMenu.isDraggable = true


        bottomChooseVehicle.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (tripStatus == CHOOSE_VEHICLE) {
                        tripStatus = CHOOSE_LOCATION
                        handleBottomSheet()
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Do something for slide offset.
            }
        })


        handleAutocompleteAddr()
        val scheduleTaskExecutor = Executors.newScheduledThreadPool(5)



        findViewById<ImageButton>(R.id.here_btn).setOnClickListener {
            setCurrentLocation()
        }

        // Set button to show the left user menu
        userMenuButton.setOnClickListener {
            leftUserMenu.state = SideSheetBehavior.STATE_EXPANDED
        }

        // Set function to show the user information button
        findViewById<Button>(R.id.left_menu_user_info).setOnClickListener {
            val intent = Intent(this, ChangeInfoActivity::class.java)
            startActivity(intent)
        }

        // Set function to show payment method button
        findViewById<Button>(R.id.left_menu_payment_method).setOnClickListener {
            val intent = Intent(this, PaymentMethodActivity::class.java)
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

        // Init result launcher
        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // Handle the returned result here
                    val data: Intent? = result.data
                    val paymentMethod =
                        data?.getSerializableExtra("paymentMethod") as PaymentMethodSerializable

                    // Change text and icon of payment method
                    if (paymentMethod.number == "") {
                        findViewById<TextView>(R.id.bottom_type_vehicle_payment_name).text =
                            "Tiền mặt"
                        findViewById<ImageView>(R.id.bottom_type_vehicle_payment_icon).setImageResource(
                            R.drawable.ic_cash
                        )
                        visaId = ""
                    } else {
                        findViewById<TextView>(R.id.bottom_type_vehicle_payment_name).text =
                            "Visa*" + paymentMethod.number.takeLast(4)
                        findViewById<ImageView>(R.id.bottom_type_vehicle_payment_icon).setImageResource(
                            R.drawable.ic_visa
                        )
                        visaId = paymentMethod.id
                    }
                }
            }

        // Set function to choose payment method
        findViewById<LinearLayout>(R.id.bottom_type_vehicle_payment).setOnClickListener {
            val intent = Intent(this, ChoosePaymentActivity::class.java)
            resultLauncher.launch(intent)
        }

        // set function to call suggestion acitivity
        findViewById<Button>(R.id.left_suggestion).setOnClickListener {
            val intent = Intent(this, SuggestionActivity::class.java)
            startActivity(intent)
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
                override fun onSuccess(result: Any) {
                    val booking = result as Booking
                    booking_id = booking.id

                    getDirection(
                        LatLng(
                            booking.info.pickup.location.coordinates[1],
                            booking.info.pickup.location.coordinates[0]
                        ),
                        LatLng(
                            booking.info.destination.location.coordinates[1],
                            booking.info.destination.location.coordinates[0]
                        )
                    )
                    clearMarkers()
                    currentMarker = mMap.addMarker(
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

                    when (booking.status) {
                        "pending" -> {
                            tripStatus = WAIT_DRIVER
                            handleBottomSheet()
                        }

                        "accepted" -> {
                            tripStatus = DRIVER_COMING
                            handleBottomSheet()
                        }

                        "arrived-at-pick-up" -> {
                            tripStatus = DRIVER_ARRIVED
                            handleBottomSheet()
                        }

                        "pick-up" -> {
                            tripStatus = PICK_UP
                            handleBottomSheet()
                        }
                    }
                }

                override fun onError(error: Throwable) {
                    if (suggestionLat != 0.0 && suggestionLng != 0.0) {
                        val suggestionLatLng = LatLng(suggestionLat, suggestionLng)
                        setDestinationLocationMarker(suggestionLatLng)
                        moveCamera(suggestionLatLng, 15f)
                        getDirection()
                    }
                    Log.e("API_SERVICE", "${error.message}")
                }
            })
    }

    private fun createRequest() {
        val user = FirebaseAuth.getInstance().currentUser
        val phone = user?.phoneNumber ?: ""


        val data = BookingRequest(
            currentMarker!!.position.latitude,
            currentMarker!!.position.longitude,
            destinationMarker!!.position.latitude,
            destinationMarker!!.position.longitude,
            currentAddress,
            destinationAddress,
            CredentialService().getAll().name,
            phone,
            CredentialService().get(),
            vehicleType!!.typeVehicle,
            vehicleType!!.typeName,
            vehicleType!!.fee,
            visaId,
            distance,
            duration
        )

        APIService().doPost<BookingRequest>("bookings", data, object : APICallback<Any> {
            override fun onSuccess(result: Any) {
            }

            override fun onError(t: Throwable) {
                Toast.makeText(this@CustomerMapsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })

    }


    private var line: Polyline? = null

    fun drawPath(result: String?) {
        try {
            // Tranform the string into a json object
            val json = JSONObject(result!!)
            val routeArray = json.getJSONArray("routes")
            val routes = routeArray.getJSONObject(0)
            val overviewPolylines = routes.getJSONObject("overview_polyline")
            val encodedString = overviewPolylines.getString("points")
            val list = decodePoly(encodedString)
            if (line != null) line?.remove()
            line = mMap.addPolyline(
                PolylineOptions().addAll(list).width(12f)
                    .color(Color.parseColor("#05b1fb")) // Google maps blue color
                    .geodesic(true)
            )

        } catch (e: JSONException) {
        }
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly: MutableList<LatLng> = ArrayList()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(
                lat.toDouble() / 1E5, lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }


    class DirectionRequest(private val activity: CustomerMapsActivity, private val url: String) :
        AsyncTask<Void?, Void?, String?>() {
        override fun doInBackground(vararg params: Void?): String? {
            val result: String
            val handler = Handler(Looper.getMainLooper())
            try {
                val data = url.let { URL(it).readText() }
                result = data
            } catch (e: Exception) {
                handler.post {
                    Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                return null
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            // get the distance and time
            activity.getDistance(result)
            activity.getTime(result)

            activity.getDesAddress(result)
            if (activity.tripStatus == CHOOSE_LOCATION || activity.tripStatus == -1) {
                activity.tripStatus = CHOOSE_LOCATION
                activity.handleBottomSheet()
            }
            activity.drawPath(result)
        }


    }

    fun handleBottomSheet() {

        val bottoms = listOf(
            bottomChooseVehicle,
            bottomChooseLocation,
            bottomWaitDriver,
            bottomDriverComing,
            bottomDirverArrived,
            bottomFinishTrip
        )
        for (bottom in bottoms) {
            bottom.state = BottomSheetBehavior.STATE_HIDDEN
        }
        autocomplete_addr.isEnabled =
            !(tripStatus != CHOOSE_LOCATION && tripStatus != CHOOSE_VEHICLE)

        Log.i("HandleBotoomSheet", "Enabled: ${bottomChooseLocation.isHideable}")
        when(tripStatus)
        {
            CHOOSE_LOCATION -> {
                bottomChooseLocation.state = BottomSheetBehavior.STATE_EXPANDED
                showTheBottomLocation()
            }

            CHOOSE_VEHICLE -> {
                bottomChooseVehicle.state = BottomSheetBehavior.STATE_EXPANDED
                showTheBottomVehicle()
            }

            WAIT_DRIVER -> {
                bottomWaitDriver.state = BottomSheetBehavior.STATE_EXPANDED
                showTheBottomWaiting()
            }

            DRIVER_COMING -> {
                Log.i("Notification", "booking_id: $booking_id")
                bottomDriverComing.state = BottomSheetBehavior.STATE_EXPANDED
                showTheBottomDriverComing()
            }

            DRIVER_ARRIVED -> {
                bottomDirverArrived.state = BottomSheetBehavior.STATE_EXPANDED
                showTheBottomDriverArrived()
            }

            PICK_UP -> {
                bottomDirverArrived.state = BottomSheetBehavior.STATE_EXPANDED
                showTheBottomDriverArrived("Bạn đang trên đường")

            }

            FINISH_TRIP -> {
                bottomFinishTrip.state = BottomSheetBehavior.STATE_EXPANDED
                showTheBottomFinishTrip()
            }
        }

    }

    fun showTheBottomLocation() {
        val timeTv = findViewById<TextView>(R.id.time)
        val distanceTv = findViewById<TextView>(R.id.distance)
        val adressTv = findViewById<TextView>(R.id.address)
        val chooseBtn = findViewById<Button>(R.id.choose_location)


        timeTv.text = "(${duration / 60} mins)"
        distanceTv.text = "${distance / 1000} km"
        adressTv.text = destinationAddress

        bottomChooseLocation.state = BottomSheetBehavior.STATE_EXPANDED
        bottomChooseVehicle.state = BottomSheetBehavior.STATE_HIDDEN
        chooseBtn.setOnClickListener {
            tripStatus = CHOOSE_VEHICLE
            handleBottomSheet()
        }


    }

    fun showTheBottomVehicle() {
        val typeVehicleRv = findViewById<RecyclerView>(R.id.type_vehicle)
        val payBtn = findViewById<Button>(R.id.pay_now)

        payBtn.setOnClickListener {
            if (currentMarker == null || destinationMarker == null || currentAddress.isEmpty() || destinationAddress.isEmpty() || vehicleType == null) {
                Toast.makeText(this, "Do please, make your choices.", Toast.LENGTH_SHORT).show()

            }
            else {
                createRequest()
                tripStatus = WAIT_DRIVER
                handleBottomSheet()
            }

        }
        val data = mapOf("distance" to distance / 1000)
        vehicleType = null
        APIService().doPost<VehilceTypePriceResponse>(
            "fee/get-fee",
            data,
            object : APICallback<Any> {
                override fun onSuccess(result: Any) {

                    val data = (result as VehilceTypePriceResponse).fee
                    typeVehicleRv.adapter = TypeVehicleAdapter(data.toList()) {
                        vehicleType = data[it]
                    }
                    typeVehicleRv.layoutManager = LinearLayoutManager(
                        this@CustomerMapsActivity,
                        LinearLayoutManager.VERTICAL,
                        false
                    )
                }

                override fun onError(t: Throwable) {
                    Toast.makeText(
                        this@CustomerMapsActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.i("MapsActivity", t.message.toString())
                }
            })
    }

    fun showTheBottomWaiting() {

    }

    fun showTheBottomDriverComing() {
        val driverName = findViewById<TextView>(R.id.driver_name)
        val driverPhone = findViewById<TextView>(R.id.driver_phone)
        val driverAvatar = findViewById<ImageView>(R.id.driver_avatar)
        val driverVehicle = findViewById<TextView>(R.id.driver_vehicle)
        val driverVehicleDesc = findViewById<TextView>(R.id.driver_vehicle_desc)
        val driverRate = findViewById<TextView>(R.id.driver_rate)


        APIService().doGet<BookingVehilce>("bookings/${booking_id}/vehicle", object : APICallback<Any> {
            override fun onSuccess(result: Any) {
                result as BookingVehilce
                val vehilce = result.vehicleInfo
                val booking = result.booking
                val driver = booking.driver
                driverName.text = driver.name
                driverPhone.text = driver.phone

                var vehicleString = ""
                if (booking.vehicle == "car") {
                    vehicleString += "Xe hơi - biển số: <b>${vehilce.plate}</b>"
                } else if (booking.vehicle == "motorbike"){
                    vehicleString += "Xe máy - biển số: <b>${vehilce.plate}</b>"
                }
                driverVehicle.text = Html.fromHtml(vehicleString, Html.FROM_HTML_MODE_LEGACY)
                driverVehicleDesc.text = vehilce.description
                driverRate.text = result.rateDriver.toString()

                Picasso.get().load(driver.avatar).placeholder(R.drawable.ic_driver).into(driverAvatar)
                if(driverLat != 0.0 && driverLng != 0.0) {
                    getDirection(
                        LatLng(driverLat, driverLng),
                        LatLng(
                            booking.info.pickup.location.coordinates[1],
                            booking.info.pickup.location.coordinates[0]
                        )
                    )

                    clearMarkers()
                    currentMarker = mMap.addMarker(
                        MarkerOptions().position(
                            LatLng(
                                booking.info.pickup.location.coordinates[1],
                                booking.info.pickup.location.coordinates[0]
                            )
                        ).title("Pickup Location")
                            .snippet(booking.info.pickup.address)
                    )
                    driverMarker = mMap.addMarker(
                        MarkerOptions().position(
                            LatLng(driverLat, driverLng)
                        ).title("Driver Location")
                            .snippet("Driver is coming")
                    )
                }


            }

            override fun onError(t: Throwable) {
                Toast.makeText(this@CustomerMapsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun clearMarkers() {
        if (currentMarker != null) {
            currentMarker?.remove()
        }
        if (destinationMarker != null) {
            destinationMarker?.remove()
        }

        if (driverMarker != null) {
            driverMarker?.remove()
        }

    }

    fun showTheBottomDriverArrived(message: String = "")
    {
        val driverName = findViewById<TextView>(R.id.driver_arrived_name)
        val driverPhone = findViewById<TextView>(R.id.driver_arrived_phone)
        val driverAvatar = findViewById<ImageView>(R.id.driver_arrived_avatar)
        val driverVehicle = findViewById<TextView>(R.id.driver_arrived_vehicle)
        val driverVehicleDesc = findViewById<TextView>(R.id.driver_arrived_desc)
        val driverRate = findViewById<TextView>(R.id.driver_arrived_rate)
        if (message != "")
        {
            findViewById<TextView>(R.id.arrived_message).text = message
        }

        APIService().doGet<BookingVehilce>("bookings/${booking_id}/vehicle", object : APICallback<Any> {
            override fun onSuccess(result: Any) {
                result as BookingVehilce

                val vehilce = result.vehicleInfo
                val booking = result.booking
                val driver = booking.driver
                driverName.text = driver.name
                driverPhone.text = driver.phone


                var vehicleString = ""
                if (booking.vehicle == "car") {
                    vehicleString += "Xe hơi - biển số: <b>${vehilce.plate}</b>"
                } else if (booking.vehicle == "motorbike"){
                    vehicleString += "Xe máy - biển số: <b>${vehilce.plate}</b>"
                }
                driverVehicle.text = Html.fromHtml(vehicleString, Html.FROM_HTML_MODE_LEGACY)
                driverVehicleDesc.text = vehilce.description
                driverRate.text = result.rateDriver.toString()

                Picasso.get().load(driver.avatar).placeholder(R.drawable.ic_driver).into(driverAvatar)

                getDirection(
                    LatLng(
                        booking.info.pickup.location.coordinates[1],
                        booking.info.pickup.location.coordinates[0]
                    ),
                    LatLng(
                        booking.info.destination.location.coordinates[1],
                        booking.info.destination.location.coordinates[0]
                    )
                )
                clearMarkers()
                currentMarker = mMap.addMarker(
                    MarkerOptions().position(
                        LatLng(
                            booking.info.pickup.location.coordinates[1],
                            booking.info.pickup.location.coordinates[0]
                        )
                    ).title("Pickup Location")
                        .snippet(booking.info.pickup.address)
                )
                destinationMarker = mMap.addMarker(
                    MarkerOptions().position(
                        LatLng(
                            booking.info.destination.location.coordinates[1],
                            booking.info.destination.location.coordinates[0]
                        )
                    ).title("Destination Location")
                        .snippet(booking.info.destination.address)
                )

            }

            override fun onError(t: Throwable) {
                Toast.makeText(
                    this@CustomerMapsActivity,
                    "Error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.i("MapsActivity", t.message.toString())

            }
        })
    }

    fun showTheBottomFinishTrip()
    {

        clearLines()
        clearMarkers()
        autocomplete_addr.isEnabled = true

        // Set onClick for rating button
        findViewById<Button>(R.id.bottom_finish_trip_rating).setOnClickListener {
            val intent = Intent(this, CustomerRatingActivity::class.java)
            intent.putExtra("booking_id", booking_id)
            startActivity(intent)
        }
    }

    fun getDesAddress(result: String?) {
        try {
            // Tranform the string into a json object
            val json = JSONObject(result!!)
            val routeArray = json.getJSONArray("routes")
            val routes = routeArray.getJSONObject(0)
            val legs = routes.getJSONArray("legs")
            val origin = legs.getJSONObject(0)
            currentAddress = origin.getString("start_address")
            val legsObject = legs.getJSONObject(legs.length() - 1)
            destinationAddress = legsObject.getString("end_address")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun getDistance(result: String?): Any? {
        try {
            // Tranform the string into a json object
            val json = JSONObject(result!!)
            val routeArray = json.getJSONArray("routes")
            val routes = routeArray.getJSONObject(0)
            val legs = routes.getJSONArray("legs")
            val legsObject = legs.getJSONObject(0)
            val distance = legsObject.getJSONObject("distance")
            this.distance = distance.getLong("value")
            return distance
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    fun getTime(result: String?): Any? {
        try {
            // Tranform the string into a json object
            val json = JSONObject(result!!)
            val routeArray = json.getJSONArray("routes")
            val routes = routeArray.getJSONObject(0)
            val legs = routes.getJSONArray("legs")
            val legsObject = legs.getJSONObject(0)
            val time = legsObject.getJSONObject("duration")
            duration = time.getLong("value")
            return duration
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }


    private fun getDirection(origin: LatLng, destination: LatLng) {
        mMap.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                LatLngBounds.Builder().include(origin).include(destination).build(), 50
            )
        )
        clearLines()
        val url =
            "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&key=${BuildConfig.MAPS_API_KEY}&mode=driving"

        val directionRequest = DirectionRequest(mMap, origin!!, destination!!, polylines)
        directionRequest.execute()

    }
    private fun getDirection(setBound: Boolean = true) {
        val origin = currentMarker?.position
        val destination = destinationMarker?.position
        if(setBound)
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().include(origin!!).include(destination!!).build(), 50))


        clearLines()
        if(origin == null || destination == null)
            return

        val url =
            "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&key=${BuildConfig.MAPS_API_KEY}&mode=driving"

        val directionRequest = DirectionRequest(mMap, origin, destination, polylines)
        val result = directionRequest.execute().get()

        // get the distance and time
        getDistance(result)
        getTime(result)

        getDesAddress(result)
        showTheBottomLocation()
        // drawPath(result)

    }

    private fun getFullAddr(): String {
        return autocomplete_addr.text.toString()
    }

    private fun handleAutocompleteAddr() {
        autocomplete_addr.setAdapter(adapterMapPrediction)
        autocomplete_addr.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null) {    // Cancel any previous place prediction requests
                    handler.removeCallbacksAndMessages(null)

                    // Start a new place prediction request in 300 ms
                    handler.postDelayed({ getPlacePredictions(getFullAddr()) }, 300)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }
        })
        // catch enter key
        autocomplete_addr.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                // handle enter key

                searchText(getFullAddr())
                clearFocusAndHideKeyboard(autocomplete_addr)
                return@setOnKeyListener true
            }
            false
        }

        autocomplete_addr.setOnItemClickListener { parent, view, position, id ->
            val id = adapterMapPrediction.getItem(position).placeId
            clearFocusAndHideKeyboard(autocomplete_addr)

            searchById(id)

        }
    }

    private fun searchById(placeId: String, replace: Boolean = true) {
        val placeFields =
            listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.ADDRESS_COMPONENTS)


        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient.fetchPlace(request).addOnSuccessListener { response: FetchPlaceResponse ->
            val place = response.place

            setDestinationLocationMarker(place.latLng)
            moveCamera(place.latLng, 15f)
            getDirection()


        }.addOnFailureListener {
            Log.e(TAG, "Place not found: ${it.message}")
        }
    }


    private fun searchText(name: String) {
        val newRequest = FindAutocompletePredictionsRequest.builder().setCountries("VN")
            // Session Token only used to link related Place Details call. See https://goo.gle/paaln
            .setSessionToken(sessionToken).setQuery(name).build()

        // Perform autocomplete predictions request
        placesClient.findAutocompletePredictions(newRequest).addOnSuccessListener { response ->
            val predictions = response.autocompletePredictions
            searchById(predictions[0].placeId, false)
        }.addOnFailureListener { exception: Exception? ->
            if (exception is ApiException) {
                Log.e(TAG, "Place not found: ${exception.message}")
            }
        }
    }

    private fun moveCamera(latLng: LatLng, zoom: Float = 15f) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }

    private fun setCurrentLocationMarker(latLng: LatLng, address: String) {
        currentMarker?.remove()
        val markerOptions = MarkerOptions().position(latLng)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            .title("Pickup Location")
            .snippet(address)
            .draggable(true)
        currentMarker = mMap.addMarker(markerOptions)
    }

    private fun setDestinationLocationMarker(latLng: LatLng, address: String = "") {
        destinationMarker?.remove()
        val markerOptions = MarkerOptions().position(latLng)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            .draggable(true)
            .title("Destination Location")
            .snippet(address)
        destinationMarker = mMap.addMarker(markerOptions)
    }

    private fun initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Permission granted")

            initMap()
            locationPermissionGranted = true

        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), REQUEST_CODE_PERMISSION
            )
        }
    }

    private fun setCurrentLocation() {
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            val locationRequest = locationProviderClient.lastLocation
            locationRequest.addOnCompleteListener {
                if (it.isSuccessful) {
                    val location = it.result
                    if (location != null) {
                        setCurrentLocationMarker(LatLng(location.latitude, location.longitude), "")
                        if(destinationMarker != null)
                            getDirection(false)
                        moveCamera(LatLng(location.latitude, location.longitude), 15f)
                    }
                }
            }
        } catch (error: SecurityException) {
            Log.e(TAG, "Error getting device location: ${error.message}")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

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
        setCurrentLocation()

        mMap.setOnMarkerClickListener(object : GoogleMap.OnMarkerClickListener {
            override fun onMarkerClick(p0: Marker): Boolean {
                p0.showInfoWindow()
                if (p0 == destinationMarker) {
                    if (tripStatus == CHOOSE_LOCATION) {
                        tripStatus = CHOOSE_LOCATION
                        handleBottomSheet()
                    }
                    return true
                }
                return false
            }
        })

        mMap.setOnMarkerDragListener(object : OnMarkerDragListener {
            override fun onMarkerDrag(p0: Marker) {

            }

            override fun onMarkerDragEnd(p0: Marker) {
                if(destinationMarker != null)
                    getDirection(false)
            }

            override fun onMarkerDragStart(p0: Marker) {

            }
        })

        mMap.setOnMapLongClickListener {
            setDestinationLocationMarker(it)
            getDirection(false)
        }
        getBooking()

    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.isNotEmpty()) {
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Permission denied")
                        return
                    }
                }
                Log.d(TAG, "Permission granted")

                initMap()
                locationPermissionGranted = true

            }
        }
    }


    private fun getPlacePredictions(query: String) {
        // The value of 'bias' biases prediction results to the rectangular region provided
        // (currently Kolkata). Modify these values to get results for another area. Make sure to
        // pass in the appropriate value/s for .setCountries() in the
        // FindAutocompletePredictionsRequest.Builder object as well.


        // Create a new programmatic Place Autocomplete request in Places SDK for Android
        val newRequest = FindAutocompletePredictionsRequest.builder().setCountries("VN")
            // Session Token only used to link related Place Details call. See https://goo.gle/paaln
            .setSessionToken(sessionToken).setQuery(query).build()

        // Perform autocomplete predictions request
        placesClient.findAutocompletePredictions(newRequest).addOnSuccessListener { response ->
            val predictions = response.autocompletePredictions

            adapterMapPrediction.setPredictions(predictions)
        }.addOnFailureListener { exception: Exception? ->
            if (exception is ApiException) {
                Log.e(TAG, "Place not found: ${exception.message}")
            }
        }
    }

    private fun clearFocusAndHideKeyboard(searchBox: AutoCompleteTextView) {
        searchBox.clearFocus()
        val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(searchBox.windowToken, 0)
    }

    private fun clearLines() {
        for (line in polylines) {
            line.remove()
        }
        polylines.clear()
    }
}