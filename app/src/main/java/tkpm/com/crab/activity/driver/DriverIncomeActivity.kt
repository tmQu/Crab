package tkpm.com.crab.activity.driver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import tkpm.com.crab.R
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.objects.IncomeValueResponse
import tkpm.com.crab.utils.PriceDisplay

class DriverIncomeActivity : AppCompatActivity() {
    private lateinit var cardAmount: TextView
    private lateinit var cashAmount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_driver_income)

        // Set id for elements
        cardAmount = findViewById(R.id.driver_income_card)
        cashAmount = findViewById(R.id.driver_income_cash)

        // Update card income
        APIService().doGet<IncomeValueResponse>("/api/drivers/${CredentialService().get()}/card-incomes", object:
            APICallback<Any> {
            override fun onSuccess(data: Any) {
                data as IncomeValueResponse
                cardAmount.text = PriceDisplay.formatVND(data.total)
            }

            override fun onError(error: Throwable) {
                Log.e("API_SERVICE", "Error: ${error.message}")
                Toast.makeText(this@DriverIncomeActivity, "Lấy thông tin thất bại", Toast.LENGTH_SHORT).show()
            }
        })

        // Update cash income
        APIService().doGet<IncomeValueResponse>("/api/drivers/${CredentialService().get()}/cash-incomes", object:
            APICallback<Any> {
            override fun onSuccess(data: Any) {
                data as IncomeValueResponse
                cashAmount.text = PriceDisplay.formatVND(data.total)
            }

            override fun onError(error: Throwable) {
                Log.e("API_SERVICE", "Error: ${error.message}")
                Toast.makeText(this@DriverIncomeActivity, "Lấy thông tin thất bại", Toast.LENGTH_SHORT).show()
            }
        })


    }
}