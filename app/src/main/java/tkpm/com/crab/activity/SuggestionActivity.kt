package tkpm.com.crab.activity

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonObject
import tkpm.com.crab.R
import tkpm.com.crab.adapter.MessageAdapter
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.objects.Booking
import tkpm.com.crab.objects.Message
import tkpm.com.crab.objects.Suggestion

class SuggestionActivity : AppCompatActivity() {
    private lateinit var chatRv: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageButton

    private lateinit var messageAdapter: MessageAdapter
    private var listMessage: MutableList<Message> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suggestion)


        chatRv = findViewById(R.id.chat_rv)
        messageBox = findViewById(R.id.message_box)
        sendButton = findViewById(R.id.send_button)

        messageAdapter = MessageAdapter(listMessage, this)
        chatRv.adapter = messageAdapter
        chatRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        sendButton.setOnClickListener {
            val question: String = messageBox.getText().toString().trim()

            addToChat(Message(question, Message.SENT_BY_ME), Message.SENT_BY_ME)
            getSuggestion()

            messageBox.setText("")

        }
    }


    fun addToChat(message: Message, sentBy: String?) {
        listMessage.add(message)
        messageAdapter.notifyDataSetChanged()
        chatRv.smoothScrollToPosition(messageAdapter.itemCount)
    }



    private fun addResponse()
    {

    }
    private fun getSuggestion()
    {
        val obj = JsonObject()
        val message = listMessage[listMessage.size - 1]
        var jsonString = "[{\"content\": \"${message.message}\", \"role\": \"${message.sentBy}\"}]"

        Log.i("SuggestionActivity", jsonString)
        obj.addProperty("prompt", jsonString)

        // Get suggestion from server

        APIService().doPost<Message>("suggestions", obj, object : APICallback<Any> {
            override fun onSuccess(data: Any) {
                var listSuggestion = mutableListOf<Suggestion>()
                val gson = Gson()
//                [
//                    {
//                        "name": "Lô Tô NGÔI SAO VIỆT",
//                        "address": "youtube@gmail.com",
//                        "latitude": 10.8230989,
//                        "longitude": 106.6296638,
                //                        "rating": 4.5,
                //         "user_rating_total:100,
//                        "imageUrl": null
//                    }
//                ]
                data as Message
                addToChat(data, Message.SENT_BY_CHATGPT)

            }

            override fun onError(error: Throwable) {
                Log.i("DriverMapActivity", "${error.message}")
                val message = Message("Xin lỗi, tôi không thể tìm thấy thông tin bạn yêu cầu", Message.SENT_BY_CHATGPT)
                addToChat(message, Message.SENT_BY_CHATGPT)
            }
        })
    }
}