package tkpm.com.crab.activity.driver

import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import tkpm.com.crab.R

class CompleteOrderActivity : AppCompatActivity() {

    private lateinit var checkIcon: ImageView
    private lateinit var ratingButton: ImageButton
    private lateinit var continueButton: ImageButton
    private lateinit var stopButton: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_complete_order)

        // Set id for elements
        checkIcon = findViewById(R.id.complete_order_check)
        ratingButton = findViewById(R.id.complete_order_rating_btn)
        continueButton = findViewById(R.id.complete_order_continue_btn)
        stopButton = findViewById(R.id.complete_order_reject_btn)

        // Set Drawables
        val drawable = AnimatedVectorDrawableCompat.create(this, R.drawable.avd_done)
        checkIcon.setImageDrawable(drawable)
        drawable?.start()

        // Set return value for each button
        ratingButton.setOnClickListener {
            // Get booking id from intent
            val bookingId = intent.getStringExtra("booking_id")

            // Start RatingActivity
            val intent = Intent(this, DriverRatingActivity::class.java)
            intent.putExtra("booking_id", bookingId)
            startActivity(intent)

            setResult(RESULT_CANCELED)
            finish()
        }

        continueButton.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

        stopButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}