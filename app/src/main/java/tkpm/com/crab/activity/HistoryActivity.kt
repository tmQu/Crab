package tkpm.com.crab.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tkpm.com.crab.R
import tkpm.com.crab.objects.Booking
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.utils.addressOverview
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale

class HistoryAdapter(val items: List<Booking>, val ctx: Context) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.historyItemDateTextView)
        val from: TextView = itemView.findViewById(R.id.historyItemFromTextView)
        val to: TextView = itemView.findViewById(R.id.historyItemToTextView)
        val price: TextView = itemView.findViewById(R.id.historyItemAmountTextView)
        val type: TextView = itemView.findViewById(R.id.historyItemTypeTextView)
        val overviewTextView by lazy { itemView.findViewById<TextView>(R.id.historyItemOverviewTextView) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_history_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(item.createdAt)
        holder.date.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(date)
        holder.from.text = item.info.pickup.address
        holder.to.text = item.info.destination.address
        val formatter = NumberFormat.getCurrencyInstance()
        formatter.currency = Currency.getInstance("VND")
        formatter.maximumFractionDigits = 0
        holder.price.text = formatter.format(item.info.fee)
        holder.type.text = item.service

        val str =
            addressOverview(item.info.pickup.address) + " to " + addressOverview(item.info.destination.address)
        holder.overviewTextView.text = str

        holder.itemView.setOnClickListener {
            val intent = Intent(ctx, HistoryDetailActivity::class.java)
            intent.putExtra("bookingId", item.id)
            ctx.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class HistoryActivity : AppCompatActivity() {

    private val recyclerView: RecyclerView by lazy {
        findViewById(R.id.historyRecyclerView)
    }
    private val list = mutableListOf<Booking>()
    private val adapter = HistoryAdapter(list, this)

    private val emptyTextView by lazy { findViewById<TextView>(R.id.emptyTextView) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        emptyTextView.visibility = View.GONE

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        APIService().doGet<List<Booking>>("accounts/${CredentialService().get()}/history",
            object : APICallback<Any> {
                override fun onSuccess(data: Any) {
                    data as List<Booking>

                    if (data.isEmpty()) {
                        emptyTextView.visibility = View.VISIBLE
                    }

                    list.clear()
                    list.addAll(data)
                    adapter.notifyDataSetChanged()
                }

                override fun onError(error: Throwable) {
                    Log.e("HistoryActivity", "Error fetching history", error)
                }
            })
    }
}