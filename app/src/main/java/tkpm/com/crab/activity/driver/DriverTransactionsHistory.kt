package tkpm.com.crab.activity.driver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import tkpm.com.crab.R
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.objects.Transaction
import tkpm.com.crab.utils.PriceDisplay
import tkpm.com.crab.utils.TimeFormatter
import java.text.SimpleDateFormat

class DriverTransactionsHistoryAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<DriverTransactionsHistoryAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val transactionRef: TextView = itemView.findViewById(R.id.transactionItemRefTextView)
        val transactionAmount: TextView = itemView.findViewById(R.id.transactionItemAmountTextView)
        val transactionType: TextView = itemView.findViewById(R.id.transactionItemTypeTextView)
        val transactionDate: TextView = itemView.findViewById(R.id.transactionItemTimeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.transactionAmount.text = PriceDisplay.formatVND(transaction.amount)
        holder.transactionType.text = transaction.type

        holder.transactionDate.text = TimeFormatter.GMT7Formatter(transaction.createdAt)
        holder.transactionRef.text = transaction.ref
    }

    override fun getItemCount(): Int {
        return transactions.size
    }
}

class DriverTransactionsHistory : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_driver_transactions_history)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.transactionHistoryLinearLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val type = intent.getStringExtra("type")

        val transactions = mutableListOf<Transaction>()
        val recyclerView = findViewById<RecyclerView>(R.id.transactionsRecyclerView)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        val adapter = DriverTransactionsHistoryAdapter(transactions)
        recyclerView.adapter = adapter
        APIService().doGet<List<Transaction>>("/api/drivers/${CredentialService().get()}/wallets/${type}/transactions",
            object : APICallback<Any> {
                override fun onSuccess(data: Any) {
                    data as List<Transaction>
                    transactions.addAll(data)
                    transactions.reverse()
                    adapter.notifyDataSetChanged()
                }

                override fun onError(error: Throwable) {
                    Log.e("API_SERVICE", "Error: ${error.message}")
                    Toast.makeText(
                        this@DriverTransactionsHistory,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}