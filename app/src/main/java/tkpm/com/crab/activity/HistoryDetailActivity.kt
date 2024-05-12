package tkpm.com.crab.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.squareup.picasso.Picasso
import tkpm.com.crab.BuildConfig
import tkpm.com.crab.R
import tkpm.com.crab.activity.customer.CustomerRatingActivity
import tkpm.com.crab.activity.driver.DriverRatingActivity
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.objects.Booking
import tkpm.com.crab.utils.DirectionRequest
import tkpm.com.crab.utils.PriceDisplay
import tkpm.com.crab.utils.addressOverview

class HistoryDetailActivity : AppCompatActivity(), OnMapReadyCallback {
    private val bookingId: String by lazy { intent.getStringExtra("bookingId")!! }
    private var map: GoogleMap? = null
    private var booking: Booking? = null

    private var isDriver = false

    private val mapView: MapView by lazy { findViewById<MapView>(R.id.map) }
    private val pickupTextView by lazy { findViewById<TextView>(R.id.historyDetailPickupTextView) }
    private val destinationTextView by lazy { findViewById<TextView>(R.id.historyDetailDestinationTextView) }
    private val amountTextView by lazy { findViewById<TextView>(R.id.historyDetailAmountTextView) }
    private val typeTextView by lazy { findViewById<TextView>(R.id.historyDetailTypeTextView) }
    private val distanceTextView by lazy { findViewById<TextView>(R.id.historyDetailDistanceTextView) }

    private val customerLinearLayout by lazy { findViewById<LinearLayout>(R.id.historyDetailCustomerLinearLayout) }
    private val customerAvatar by lazy { findViewById<ImageView>(R.id.historyDetailCustomerAvatar) }
    private val customerName by lazy { findViewById<TextView>(R.id.historyDetailCustomerNameTextView) }
    private val customerRating by lazy { findViewById<RatingBar>(R.id.historyDetailCustomerRatingBar) }
    private val customerRateButton by lazy { findViewById<TextView>(R.id.historyDetailCustomerRateButton) }

    private val driverLinearLayout by lazy { findViewById<LinearLayout>(R.id.historyDetailDriverLinearLayout) }
    private val driverAvatar by lazy { findViewById<ImageView>(R.id.historyDetailDriverAvatar) }
    private val driverName by lazy { findViewById<TextView>(R.id.historyDetailDriverNameTextView) }
    private val driverRating by lazy { findViewById<RatingBar>(R.id.historyDetailDriverRatingBar) }
    private val driverRateButton by lazy { findViewById<TextView>(R.id.historyDetailDriverRateButton) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        getBooking()
    }

    override fun onResume() {
        super.onResume()
        getBooking()
    }

    private fun getBooking() {
        APIService().doGet<Booking>("bookings/$bookingId", object : APICallback<Any> {
            override fun onSuccess(data: Any) {
                data as Booking

                booking = data

                if (data.driver.id == CredentialService().get()) {
                    isDriver = true
                }

                val mapFragment =
                    supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this@HistoryDetailActivity)

                pickupTextView.text = "Từ: ${addressOverview(data.info.pickup.address)}"
                destinationTextView.text = "Đến: ${addressOverview(data.info.destination.address)}"
                amountTextView.text = PriceDisplay.formatVND(data.info.fee.toLong())
                typeTextView.text = data.service
                distanceTextView.text = "${data.info.distance / 1000} km"

                if (isDriver) {
                    Log.i("HistoryDetailActivity", "Driver ${data.orderedBy.avatar}")
                    if (data.orderedBy.avatar == "") Picasso.get().load(R.drawable.grab_splash)
                        .into(customerAvatar)
                    else Picasso.get().load(BuildConfig.BASE_URL + "files/" + data.orderedBy.avatar).placeholder(R.drawable.grab_splash)
                        .into(customerAvatar)

                    customerName.text = data.orderedBy.name
                    if (data.info.driver_rating == null) {
                        customerRating.visibility = View.GONE
                        customerRateButton.visibility = View.VISIBLE
                    } else {
                        customerRating.rating = data.info.driver_rating.value
                        customerRating.visibility = View.VISIBLE
                        customerRateButton.visibility = View.GONE
                    }

                    customerRateButton.setOnClickListener {
                        val intent = android.content.Intent(
                            this@HistoryDetailActivity,
                            DriverRatingActivity::class.java
                        )
                        intent.putExtra("booking_id", bookingId)
                        startActivity(intent)
                    }

                    customerLinearLayout.visibility = View.VISIBLE
                    driverLinearLayout.visibility = View.GONE

                } else {
                    if (data.driver.avatar == "") Picasso.get().load(R.drawable.grab_splash)
                        .into(driverAvatar)
                    else Picasso.get().load(BuildConfig.BASE_URL + "files/" + data.driver.avatar).placeholder(R.drawable.grab_splash)
                        .into(driverAvatar)

                    driverName.text = data.driver.name

                    if (data.info.customer_rating == null) {
                        driverRating.visibility = View.GONE
                        driverRateButton.visibility = View.VISIBLE
                    } else {
                        driverRating.rating = data.info.customer_rating.value
                        driverRating.visibility = View.VISIBLE
                        driverRateButton.visibility = View.GONE
                    }

                    driverRateButton.setOnClickListener {
                        val intent = android.content.Intent(
                            this@HistoryDetailActivity,
                            CustomerRatingActivity::class.java
                        )
                        intent.putExtra("booking_id", bookingId)
                        startActivity(intent)
                    }

                    customerLinearLayout.visibility = View.GONE
                    driverLinearLayout.visibility = View.VISIBLE
                }


            }

            override fun onError(error: Throwable) {
                error.printStackTrace()
            }
        })

    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map?.uiSettings?.isMapToolbarEnabled = false
        map?.uiSettings?.setAllGesturesEnabled(false)

        map.let {
            val pickup = LatLng(
                booking?.info?.pickup?.location?.coordinates?.get(1)!!,
                booking?.info?.pickup?.location?.coordinates?.get(0)!!
            )
            val destination = LatLng(
                booking?.info?.destination?.location?.coordinates?.get(1)!!,
                booking?.info?.destination?.location?.coordinates?.get(0)!!
            )
            val bounds = LatLngBounds.builder().include(pickup).include(destination).build()
            it?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 125))

            it?.addMarker(
                com.google.android.gms.maps.model.MarkerOptions().position(pickup).title("Pickup")
                    .icon(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                            com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
                        )
                    )
            )

            it?.addMarker(
                com.google.android.gms.maps.model.MarkerOptions().position(destination)
                    .title("Destination").icon(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                            com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                        )
                    )
            )
            DirectionRequest(it!!, pickup, destination).execute()
        }
    }
}
