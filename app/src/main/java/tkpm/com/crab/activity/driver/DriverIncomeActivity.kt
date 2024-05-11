package tkpm.com.crab.activity.driver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import tkpm.com.crab.R
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.objects.Wallet
import tkpm.com.crab.utils.PriceDisplay


class DriverIncomeActivity : AppCompatActivity() {
    private lateinit var cardAmount: TextView
    private lateinit var cashAmount: TextView

    private lateinit var card_history_image_button: ImageButton
    private lateinit var topup_button: Button

    private lateinit var cash_history_image_button: ImageButton
    private lateinit var withdraw_button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_driver_income)

        // Set id for elements
        cardAmount = findViewById(R.id.driver_income_card)
        cashAmount = findViewById(R.id.driver_income_cash)

        card_history_image_button = findViewById(R.id.card_history_image_button)
        topup_button = findViewById(R.id.topup_button)

        cash_history_image_button = findViewById(R.id.cash_history_image_button)
        withdraw_button = findViewById(R.id.withdraw_button)

        // Set event for elements
        card_history_image_button.setOnClickListener {
            val intent = Intent(this, DriverTransactionsHistory::class.java).apply {
                putExtra("type", "credit")
            }
            startActivity(intent)
        }

        topup_button.setOnClickListener {
            val intent = Intent(this, DriverTopupPayoutActivity::class.java).apply {
                putExtra("type", "credit")
            }
            startActivity(intent)
        }

        cash_history_image_button.setOnClickListener {
            val intent = Intent(this, DriverTransactionsHistory::class.java).apply {
                putExtra("type", "cash")
            }
            startActivity(intent)
        }

        withdraw_button.setOnClickListener {
            val intent = Intent(this, DriverTopupPayoutActivity::class.java).apply {
                putExtra("type", "cash")
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        // Update card income
        APIService().doGet<Wallet>("/api/drivers/${CredentialService().get()}/wallets/credit",
            object : APICallback<Any> {
                override fun onSuccess(data: Any) {
                    data as Wallet
                    cardAmount.text = PriceDisplay.formatVND(data.balance)
                }

                override fun onError(error: Throwable) {
                    Log.e("API_SERVICE", "Error: ${error.message}")
                    Toast.makeText(
                        this@DriverIncomeActivity, "Lấy thông tin thất bại", Toast.LENGTH_SHORT
                    ).show()
                }
            })

        // Update cash income
        APIService().doGet<Wallet>("/api/drivers/${CredentialService().get()}/wallets/cash",
            object : APICallback<Any> {
                override fun onSuccess(data: Any) {
                    data as Wallet
                    cashAmount.text = PriceDisplay.formatVND(data.balance)
                }

                override fun onError(error: Throwable) {
                    Log.e("API_SERVICE", "Error: ${error.message}")
                    Toast.makeText(
                        this@DriverIncomeActivity, "Lấy thông tin thất bại", Toast.LENGTH_SHORT
                    ).show()
                }
            })

    }
}