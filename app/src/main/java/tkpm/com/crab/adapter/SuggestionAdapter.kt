package tkpm.com.crab.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import tkpm.com.crab.R
import tkpm.com.crab.objects.Suggestion

class SuggestionAdapter(suggestionList: List<Suggestion>, onClickItem: (Double, Double) -> Unit): RecyclerView.Adapter<SuggestionAdapter.ItemViewHolder>() {
    var suggestionList: List<Suggestion>
    var onClickItem: (lat: Double, long: Double) -> Unit
    init {
        this.suggestionList = suggestionList
        this.onClickItem = onClickItem
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView
        var address: TextView
        var rating: RatingBar
        var userRatingsTotal: TextView
        var imageView: ImageView



        init {
            name = itemView.findViewById(R.id.suggestion_name)
            address = itemView.findViewById(R.id.suggestion_address)
            rating = itemView.findViewById(R.id.suggestion_rating)
            userRatingsTotal = itemView.findViewById(R.id.suggestion_user_ratings_total)
            imageView = itemView.findViewById(R.id.suggestion_image)

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.suggestion_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val suggestion: Suggestion = suggestionList[position]

        holder.name.text = suggestion.name
        holder.address.text = suggestion.address
        holder.rating.rating = suggestion.rating.toFloat()
        holder.userRatingsTotal.text = "Dựa trên  ${suggestion.user_ratings_total} đánh giá"
        if(suggestion.imageUrl != "")
            Picasso.get().load(suggestion.imageUrl).into(holder.imageView)
        else {
            Picasso.get().load(R.drawable.no_img).into(holder.imageView)
        }

        holder.itemView.setOnClickListener {
            val suggestion = suggestionList[position]
            onClickItem.invoke(suggestion.latitude, suggestion.longitude)
        }


    }

    override fun getItemCount(): Int {
        return suggestionList.size
    }
}