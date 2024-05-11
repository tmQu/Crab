package tkpm.com.crab.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tkpm.com.crab.R
import tkpm.com.crab.objects.Message
import tkpm.com.crab.objects.Suggestion


class MessageAdapter(messageList: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.MyViewHolder>() {
    var messageList: List<Message>

    init {
        this.messageList = messageList
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val chatView: View =
            LayoutInflater.from(parent.context).inflate(R.layout.chat_item, null)
        return MyViewHolder(chatView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val message: Message = messageList[position]
        if (message.sentBy == (Message.SENT_BY_ME)) {
            holder.leftChatView.visibility = View.GONE
            holder.rightChatView.visibility = View.VISIBLE
            holder.rightTextView.setText(message.message)
        } else {
            holder.rightChatView.visibility = View.GONE
            holder.leftChatView.visibility = View.VISIBLE
            holder.leftTextView.setText(message.message)
            setSuggestionAdapter(holder.suggestionView, message)
        }
    }

    private fun setSuggestionAdapter(suggestsionView: RecyclerView, message: Message) {
        Log.i("SuggestionAdapter", "setSuggestionAdapter: " + message.listSuggestions[0])
        suggestsionView.visibility = View.VISIBLE
        val suggestionAdapter = SuggestionAdapter(message.listSuggestions)
        suggestsionView.adapter = suggestionAdapter
        suggestsionView.layoutManager = LinearLayoutManager(suggestsionView.context, LinearLayoutManager.HORIZONTAL, false)
        Log.i("SuggestionAdapter", "setSuggestionAdapter: " + message.listSuggestions.size)

    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var leftChatView: LinearLayout
        var rightChatView: LinearLayout
        var leftTextView: TextView
        var rightTextView: TextView
        var suggestionView: RecyclerView

        init {
            leftChatView = itemView.findViewById<LinearLayout>(R.id.left_chat_view)
            rightChatView = itemView.findViewById<LinearLayout>(R.id.right_chat_view)
            leftTextView = itemView.findViewById<TextView>(R.id.left_chat_text_view)
            rightTextView = itemView.findViewById<TextView>(R.id.right_chat_text_view)
            suggestionView = itemView.findViewById<RecyclerView>(R.id.suggestion)
        }
    }
}

