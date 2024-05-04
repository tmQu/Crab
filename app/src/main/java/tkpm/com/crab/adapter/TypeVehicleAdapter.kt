package tkpm.com.crab.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tkpm.com.crab.R
import tkpm.com.crab.objects.VehicleTypePrice
import tkpm.com.crab.utils.PriceDisplay
import kotlin.properties.Delegates

class TypeVehicleAdapter(val listVehicle: List<VehicleTypePrice>, val onItemClickListener:  (Int) -> Unit): RecyclerView.Adapter<TypeVehicleAdapter.ViewHolder>(){

    var selectedList = BooleanArray(listVehicle.size)
    var lastSelected by Delegates.observable(-1) { property, oldPos, newPos ->

        if(oldPos != -1)
        {
            selectedList[oldPos] = false
            notifyItemChanged(oldPos)
        }
        if (newPos in listVehicle.indices) {
            selectedList[newPos] = true
            notifyItemChanged(newPos)
        }
    }


    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        init {
            itemView.setOnClickListener {
                onItemClickListener(adapterPosition)
                lastSelected = adapterPosition
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.type_vehicle_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageVehicle = holder.itemView.findViewById<ImageView>(R.id.image_vehicle)
        val nameVehicle = holder.itemView.findViewById<TextView>(R.id.vehicle_type)
        val priceVehicle = holder.itemView.findViewById<TextView>(R.id.price)
        val vehicleSeat = holder.itemView.findViewById<TextView>(R.id.vehicle_seat)

        val vehicle = listVehicle[position]

        priceVehicle.text = PriceDisplay.formatVND(vehicle.fee.toLong())
        if (vehicle.typeVehicle.contains("Bike", true))
        {
            imageVehicle.setImageResource(R.drawable.ic_bike)
            nameVehicle.text = vehicle.typeName
            vehicleSeat.text = vehicle.numSeat.toString()
        }
        else if (vehicle.typeVehicle.contains("Car", true))
        {
            imageVehicle.setImageResource(R.drawable.ic_car)
            nameVehicle.text = vehicle.typeName
            vehicleSeat.text = vehicle.numSeat.toString()
        }
        else {
            nameVehicle.text = ""
            vehicleSeat.text = ""
        }
        if(selectedList[position])
        {
            holder.itemView.setBackgroundColor(Color.parseColor("#F0F9F8"))
        }
        else
        {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"))

        }
    }

    override fun getItemCount(): Int {
        return listVehicle.size
    }
}