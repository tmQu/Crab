package tkpm.com.crab.activity.driver

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import tkpm.com.crab.R
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.dialog.LoadingDialog
import tkpm.com.crab.objects.PreRatingResponse
import tkpm.com.crab.objects.RatingRequest

class DriverRatingActivity : AppCompatActivity() {
    private var bookingId: String = ""

    private lateinit var bookingField: TextView
    private lateinit var vehicle: TextView
    private lateinit var service: TextView
    private lateinit var pickUp: TextView
    private lateinit var destination: TextView

    private lateinit var ratingBar: RatingBar
    private lateinit var ratingText: TextInputEditText
    private lateinit var ratingBtn: Button

    private lateinit var loadingDialog: LoadingDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rating)

        // Set id for elements
        bookingField = findViewById(R.id.rating_booking_id)
        vehicle = findViewById(R.id.rating_vehicle)
        service = findViewById(R.id.rating_service)
        pickUp = findViewById(R.id.rating_pick_up)
        destination = findViewById(R.id.rating_destination)
        ratingBar = findViewById(R.id.rating_rating_bar)
        ratingText = findViewById(R.id.rating_rating_text)
        ratingBtn = findViewById(R.id.rating_submit)
        loadingDialog = LoadingDialog(this@DriverRatingActivity)

        // Get string extra from intent
        bookingId = intent.getStringExtra("booking_id").toString()

        // Set default rating value to 5 stars
        ratingBar.rating = 5f

        // Do not allow user rating 0 star
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            if (rating == 0f) {
                ratingBar.rating = 1f
            }
        }

        APIService().doGet<PreRatingResponse>("bookings/$bookingId/rating/pre-rating-info", object :
            APICallback<Any> {
            override fun onSuccess(data: Any) {
                Log.i("Notification", "result: $data")

                data as PreRatingResponse

                // Set data for elements
                bookingField.text = data.id
                vehicle.text =
                    if (data.vehicle == "motorbike") "Xe máy" else "Xe hơi"
                service.text = data.service
                pickUp.text = data.pickUp
                destination.text = data.destination

                // Submit rating
                ratingBtn = findViewById(R.id.rating_submit)
                ratingBtn.setOnClickListener {
                    val ratingRequest = RatingRequest(
                        value = ratingBar.rating.toInt(),
                        comment = ratingText.text.toString()
                    )

                    loadingDialog.startLoadingDialog()

                    APIService().doPost<Any>("bookings/$bookingId/driver-rating", ratingRequest, object :
                        APICallback<Any> {
                        override fun onSuccess(data: Any) {
                            Log.i("Notification", "result: $data")
                            Toast.makeText(this@DriverRatingActivity, "Đánh giá thành công", Toast.LENGTH_SHORT).show()
                            loadingDialog.dismissDialog()
                            finish()
                        }

                        override fun onError(error: Throwable) {
                            Log.i("Notification", "result: ${error.message}")
                            Toast.makeText(this@DriverRatingActivity, "Đánh giá thất bại", Toast.LENGTH_SHORT).show()
                            loadingDialog.dismissDialog()
                        }
                    })
                }
            }

            override fun onError(error: Throwable) {
                Log.i("Notification", "result: ${error.message}")
                Toast.makeText(this@DriverRatingActivity, "Đánh giá thất bại", Toast.LENGTH_SHORT).show()
            }
        })
    }
}