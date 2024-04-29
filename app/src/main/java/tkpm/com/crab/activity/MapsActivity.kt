package tkpm.com.crab.activity


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.LocationBias
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONException
import org.json.JSONObject
import tkpm.com.crab.BuildConfig
import tkpm.com.crab.R
import tkpm.com.crab.adapter.MapPredictionAdapter
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.objects.BookingRequest
import java.io.IOException
import java.net.URL


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    val REQUEST_CODE_PERMISSION = 1
    val TAG = "MapsActivity"

    private lateinit var mMap: GoogleMap

    private lateinit var locationProviderClient: FusedLocationProviderClient

    private var locationPermissionGranted = false
    private lateinit var placesClient: PlacesClient
    private lateinit var adapterMapPrediction: MapPredictionAdapter

    private val sessionToken = AutocompleteSessionToken.newInstance()

    private lateinit var autocomplete_district: AutoCompleteTextView
    private lateinit var autocomplete_addr: AutoCompleteTextView
    private lateinit var edt_city: TextInputLayout

    private val handler = Handler(Looper.getMainLooper())


    private lateinit var currentPosition: LatLng
    private lateinit var full_addr: String

    // Marker
    private var destinationMarker: Marker? = null
    private var currentMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_maps)

        checkLocationPermissions()

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
        autocomplete_district = findViewById(R.id.autocomplete_district)
        edt_city = findViewById(R.id.edt_city)

        handleAutocompleteAddr()
        handleAutocompleteDistrict()

        findViewById<ImageButton>(R.id.here_btn).setOnClickListener {
            setCurrentLocation()
        }

        findViewById<MaterialButton>(R.id.request_btn).setOnClickListener {
            createRequest()
        }
    }

    private fun createRequest() {
        val currentLocationAddress: Address? = Geocoder(this).getFromLocation(
            currentMarker?.position?.latitude ?: 0.0,
            currentMarker?.position?.longitude ?: 0.0,
            1
        )?.firstOrNull()

        val destinationLocationAddress: Address? = Geocoder(this).getFromLocation(
            destinationMarker?.position?.latitude ?: 0.0,
            destinationMarker?.position?.longitude ?: 0.0,
            1
        )?.firstOrNull()


        val data = BookingRequest(
            currentMarker?.position?.latitude ?: 0.0,
            currentMarker?.position?.longitude ?: 0.0,
            destinationMarker?.position?.latitude ?: 0.0,
            destinationMarker?.position?.longitude ?: 0.0,
            currentLocationAddress?.getAddressLine(0) ?: "",
            destinationLocationAddress?.getAddressLine(0) ?: "",
            "Nguyen Van A",
            "0123456789",
            "Nguyen Van B"
        )

        APIService().doPost<Any>("bookings", data, object : APICallback<Any> {
            override fun onSuccess(result: Any) {
                Toast.makeText(this@MapsActivity, "Success", Toast.LENGTH_SHORT).show()
            }

            override fun onError(t: Throwable) {
                Toast.makeText(this@MapsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun handleAutocompleteDistrict() {
//        val listDistrict = resources.getStringArray(R.array.district_hcm)
//        val districtAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listDistrict)
//        autocomplete_district.setAdapter(districtAdapter)
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


    class DirectionRequest(private val activity: MapsActivity, private val url: String) :
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
            activity.drawPath(result)
        }
    }

    private fun getDirection() {
        val origin = currentMarker?.position
        val destination = destinationMarker?.position

        val url =
            "https://maps.googleapis.com/maps/api/directions/json?origin=${origin?.latitude},${origin?.longitude}&destination=${destination?.latitude},${destination?.longitude}&key=${BuildConfig.MAPS_API_KEY}&mode=driving"

        val directionRequest = DirectionRequest(this, url)
        directionRequest.execute()

    }

    private fun getFullAddr(): String {
        full_addr =
            autocomplete_addr.text.toString() + " " + autocomplete_district.text.toString() + " " + edt_city.editText?.text.toString()
        return full_addr
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
            Log.i(TAG, "Place found: ${place.address}")
            var temp_addr = ""
            val components = place.addressComponents
            if (replace == true) {
                // get ward
                val ward = place.address
                val ward_split = ward.split(",")
                var wardName = ""
                for (i in ward_split) {
                    if (i.contains("Phường") || i.contains("phường")) {
                        wardName = i
                    }
                }

                for (component in components.asList()) {
                    Log.i(TAG, "Component: ${component.types} ${component.name}")
                    when {
                        "street_number" in component.types -> {
                            temp_addr += component.name + " "
                            Log.i(TAG, "Temp addr: $temp_addr")
                        }

                        "route" in component.types -> temp_addr += component.name + " "
                        "sublocality" in component.types -> temp_addr += component.name + " "
                        "locality" in component.types -> temp_addr += component.name
                        "administrative_area_level_2" in component.types -> autocomplete_district.setText(
                            component.name
                        )
                    }
                }
                if (wardName != "" && !temp_addr.contains("Phường") && !temp_addr.contains("phường")) {
                    temp_addr += ", $wardName"
                }
                autocomplete_addr.setText(temp_addr)
            }

            currentPosition = place.latLng
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

    private fun setCurrentLocationMarker(latLng: LatLng) {
        currentMarker?.remove()
        val markerOptions = MarkerOptions().position(latLng)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        currentMarker = mMap.addMarker(markerOptions)
    }

    private fun setDestinationLocationMarker(latLng: LatLng) {
        destinationMarker?.remove()
        val markerOptions = MarkerOptions().position(latLng)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
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
                        Log.i("Location", "Location: ${location.latitude}, ${location.longitude}")
                        currentPosition = LatLng(location.latitude, location.longitude)
                        setCurrentLocationMarker(currentPosition)
                        moveCamera(LatLng(location.latitude, location.longitude), 15f)
                    }
                }
            }
        } catch (error: SecurityException) {
            Log.e(TAG, "Error getting device location: ${error.message}")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show()
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        setCurrentLocation()


        mMap.setOnMarkerDragListener(object : OnMarkerDragListener {
            override fun onMarkerDrag(p0: Marker) {

            }

            override fun onMarkerDragEnd(p0: Marker) {
                Log.i(TAG, "Location: ${p0.position.latitude}, ${p0.position.longitude}")
            }

            override fun onMarkerDragStart(p0: Marker) {

            }
        })
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
        Log.i(TAG, "Query: $query")
        val bias: LocationBias = RectangularBounds.newInstance(
            LatLng(22.458744, 88.208162),  // SW lat, lng
            LatLng(22.730671, 88.524896) // NE lat, lng
        )

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
        val imm = this?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(searchBox.windowToken, 0)
    }

}