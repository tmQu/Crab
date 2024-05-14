package tkpm.com.crab.activity.driver

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import tkpm.com.crab.R
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.objects.Amount


class DriverTopupPayoutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_driver_topup_payout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val checkIcon = findViewById<ImageView>(R.id.transaction_completed_check)
        val drawable = AnimatedVectorDrawableCompat.create(this, R.drawable.avd_done)
        checkIcon.setImageDrawable(drawable)

        val topUpMaterialCardView = findViewById<MaterialCardView>(R.id.topUpMaterialCardView)
        topUpMaterialCardView.visibility = MaterialCardView.GONE

        val payoutMaterialCardView = findViewById<MaterialCardView>(R.id.payOutMaterialCardView)
        payoutMaterialCardView.visibility = MaterialCardView.GONE

        val transactionCompletedLinearLayout =
            findViewById<LinearLayout>(R.id.transactionCompletedLinearLayout)
        transactionCompletedLinearLayout.visibility = LinearLayout.GONE
        val transactionFinishButton = findViewById<Button>(R.id.transactionFinishButton)
        transactionFinishButton.setOnClickListener {
            finish()
        }

        val statusTextView = findViewById<TextView>(R.id.textView)

        val topUpTextInputLayout = findViewById<TextInputLayout>(R.id.topUpTextInputLayout)
        val topupButton = findViewById<Button>(R.id.topupButton)
        topupButton.setOnClickListener {
            val amount = topUpTextInputLayout.editText?.text.toString()
            if (amount.isEmpty()) {
                Toast.makeText(
                    this@DriverTopupPayoutActivity, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val data = Amount(amount.toLong())
            if (amount.toLong() <= 0) {
                Toast.makeText(
                    this@DriverTopupPayoutActivity, "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            APIService().doPost<Amount>("/api/drivers/${CredentialService().get()}/top-up",
                data,
                object : APICallback<Any> {
                    override fun onSuccess(data: Any) {
                        Log.d("DriverTopupPayoutActivity", "onSuccess: $data")
                        statusTextView.text = "Nạp tiền thành công"
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(topUpTextInputLayout.editText!!.windowToken, 0)
                        topUpMaterialCardView.visibility = MaterialCardView.GONE
                        transactionCompletedLinearLayout.visibility = LinearLayout.VISIBLE
                        drawable?.start()
                    }

                    override fun onError(error: Throwable) {
                        Log.e("API_SERVICE", "Error: ${error.message}")
                        statusTextView.text = "Nạp tiền thất bại"
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(topUpTextInputLayout.editText!!.windowToken, 0)
                        topUpMaterialCardView.visibility = MaterialCardView.GONE
                        transactionCompletedLinearLayout.visibility = LinearLayout.VISIBLE
                        drawable?.start()
                    }
                })
        }

        val payoutTextInputLayout = findViewById<TextInputLayout>(R.id.payoutTextInputLayout)
        val payoutButton = findViewById<Button>(R.id.payoutButton)
        payoutButton.setOnClickListener {
            val amount = payoutTextInputLayout.editText?.text.toString()
            if (amount.isEmpty()) {
                Toast.makeText(
                    this@DriverTopupPayoutActivity, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val data = Amount(amount.toLong())
            if (amount.toLong() <= 0) {
                Toast.makeText(
                    this@DriverTopupPayoutActivity, "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            APIService().doPost<Amount>("/api/drivers/${CredentialService().get()}/withdraw",
                data,
                object : APICallback<Any> {
                    override fun onSuccess(data: Any) {
                        Log.d("DriverTopupPayoutActivity", "onSuccess: $data")
                        statusTextView.text = "Rút tiền thành công"
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(payoutTextInputLayout.editText!!.windowToken, 0)
                        payoutMaterialCardView.visibility = MaterialCardView.GONE
                        transactionCompletedLinearLayout.visibility = LinearLayout.VISIBLE
                        drawable?.start()
                    }

                    override fun onError(error: Throwable) {
                        Log.e("API_SERVICE", "Error: ${error.message}")
                        statusTextView.text = "Rút tiền thất bại"
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(payoutTextInputLayout.editText!!.windowToken, 0)
                        payoutMaterialCardView.visibility = MaterialCardView.GONE
                        transactionCompletedLinearLayout.visibility = LinearLayout.VISIBLE
                        drawable?.start()
                    }
                })


        }

        val type = intent.getStringExtra("type")

        if (type == "credit") {
            topUpMaterialCardView.visibility = MaterialCardView.VISIBLE

        } else if (type == "cash") {
            payoutMaterialCardView.visibility = MaterialCardView.VISIBLE
        }
    }
}