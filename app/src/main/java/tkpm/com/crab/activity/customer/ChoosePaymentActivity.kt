package tkpm.com.crab.activity.customer

import android.app.Activity
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
import tkpm.com.crab.objects.PaymentMethodSerializable

class PaymentSelectionAdapter(
    private val paymentMethods: List<PaymentMethodRequest>,
    private val activity: Activity
) : androidx.recyclerview.widget.RecyclerView.Adapter<PaymentSelectionAdapter.PaymentViewHolder>() {

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
    ): PaymentSelectionAdapter.PaymentViewHolder {
        val itemView = android.view.LayoutInflater.from(parent.context).inflate(
            R.layout.item_payment_method,
            parent,
            false
        )
        return PaymentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PaymentSelectionAdapter.PaymentViewHolder, position: Int) {
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

        // Set onClickListener for payment methods
        holder.itemView.setOnClickListener {
            // Return data to previous activity
            val intent = Intent()

            // Put data with key "paymentMethod" using serializable
            val paymentMethod = PaymentMethodSerializable(
                currentItem.id,
                currentItem.name,
                currentItem.number,
                currentItem.exp,
                currentItem.cvv
            )
            intent.putExtra("paymentMethod", paymentMethod)

            activity.setResult(Activity.RESULT_OK, intent)
            activity.finish()
        }
    }

    override fun getItemCount(): Int {
        return paymentMethods.size
    }
}

class ChoosePaymentActivity : AppCompatActivity() {

    private lateinit var newPaymentMethodButton: Button
    private lateinit var paymentRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var paymentAdapter: PaymentSelectionAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_payment_method)

        // Set id for elements
        newPaymentMethodButton = findViewById(R.id.payment_method_new)

        // Set function to show the payment method button
        newPaymentMethodButton.setOnClickListener {
            val intent = Intent(this, NewPaymentMethodActivity::class.java)
            startActivity(intent)
        }

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

                paymentAdapter = PaymentSelectionAdapter(paymentMethods, this@ChoosePaymentActivity)
                paymentRecyclerView = findViewById(R.id.payment_method_recycler_view)
                paymentRecyclerView.adapter = paymentAdapter
                paymentRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ChoosePaymentActivity)

                Log.d("API_SERVICE", "Success: ${data.data.toString()}")
            }

            override fun onError(error: Throwable) {
                Log.e("API_SERVICE", "Error: ${error.message}")
            }
        })
    }
}