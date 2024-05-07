package tkpm.com.crab.activity.customer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import tkpm.com.crab.R
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.objects.PaymentMethodRequest
import tkpm.com.crab.objects.PaymentMethodResponse

class PaymentAdapter(
    private val paymentMethods: List<PaymentMethodRequest>,
) : androidx.recyclerview.widget.RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder>() {

    inner class PaymentViewHolder(itemView: android.view.View) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val paymentNumber = itemView.findViewById<android.widget.TextView>(R.id.item_payment_method_number)
        val paymentName = itemView.findViewById<android.widget.TextView>(R.id.item_payment_method_name)
        val paymentIcon = itemView.findViewById<android.widget.ImageView>(R.id.item_payment_method_icon)
    }

    fun getItem(position: Int): PaymentMethodRequest {
        return paymentMethods[position]
    }

    override fun onCreateViewHolder(
        parent: android.view.ViewGroup, viewType: Int
    ): PaymentAdapter.PaymentViewHolder {
        val itemView = android.view.LayoutInflater.from(parent.context).inflate(
            R.layout.item_payment_method_layout,
            parent,
            false
        )
        return PaymentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PaymentAdapter.PaymentViewHolder, position: Int) {
        val currentItem = paymentMethods[position]

        // Number == "" -> Cash
        if (currentItem.number == "") {
            holder.paymentNumber.text = ""
            holder.paymentName.text = "Tiền mặt"
            holder.paymentIcon.setImageResource(R.drawable.ic_cash)
        } else
        {
            val formattedNumber = '*' + currentItem.number.takeLast(4)
            holder.paymentNumber.text = formattedNumber
        }
    }

    override fun getItemCount(): Int {
        return paymentMethods.size
    }
}

class PaymentMethodActivity : AppCompatActivity() {

    private lateinit var newPaymentMethodButton: Button
    private lateinit var paymentRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var paymentAdapter: PaymentAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_payment_method)

        // Set id for elements
        newPaymentMethodButton = findViewById(R.id.payment_method_new)

        // Set onClickListener for newPaymentMethodButton
        newPaymentMethodButton.setOnClickListener {
            val intent = Intent(this, NewPaymentMethodActivity::class.java)
            startActivity(intent)
        }

        APIService().doGet<PaymentMethodResponse>("accounts/${CredentialService().get()}/payment-methods",  object:
            APICallback<Any> {
            override fun onSuccess(data: Any) {
                data as PaymentMethodResponse

                Log.d("API_SERVICE", "Success: $data")
            }

            override fun onError(error: Throwable) {
                Log.e("API_SERVICE", "Error: ${error.message}")
            }
        })

        updatePaymentMethods()
    }

    override fun onResume() {
        super.onResume()
        updatePaymentMethods()
    }

    private fun updatePaymentMethods() {
        APIService().doGet<PaymentMethodResponse>("accounts/${CredentialService().get()}/payment-methods",  object:
            APICallback<Any> {
            override fun onSuccess(data: Any) {
                data as PaymentMethodResponse

                // List of payment methods
                val paymentMethods = mutableListOf<PaymentMethodRequest>()
                paymentMethods.add(PaymentMethodRequest("", "", "", "", ""))
                paymentMethods.addAll(data.data)

                paymentAdapter = PaymentAdapter(paymentMethods)
                paymentRecyclerView = findViewById(R.id.payment_method_recycler_view)
                paymentRecyclerView.adapter = paymentAdapter
                paymentRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@PaymentMethodActivity)

                Log.d("API_SERVICE", "Success: ${data.data}")
            }

            override fun onError(error: Throwable) {
                Log.e("API_SERVICE", "Error: ${error.message}")
            }
        })
    }
}