package tkpm.com.crab.notification


import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonObject
import tkpm.com.crab.CrabApplication
import tkpm.com.crab.R
import tkpm.com.crab.activity.customer.MapsActivity
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.constant.NOTIFICATION
class CustomerNotification: FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val notification = message.notification
        if(notification == null)
            return
        else {
            val title = notification.title
            val mess = notification.body
            val bookingId = message.data["booking_id"]
            val driverLat = message.data["driver_lat"]?.toDouble()
            val driverLng = message.data["driver_lng"]?.toDouble()
            sendNotification(title, mess)
            if (title == "Driver is coming") {
                updateUIWithLatLng(NOTIFICATION.DRIVER_COMMING, bookingId, driverLat, driverLng)
            }

            if (title == "Driver has arrived") {
                updateUI(NOTIFICATION.DRIVER_ARRIVED, bookingId)
            }

            if (title == "Pick up")
            {
                updateUI(NOTIFICATION.PICK_UP, bookingId)
            }

            if (title == "Trip finished") {
                updateUI(NOTIFICATION.FINISH_TRIP, bookingId)
            }
        }
    }

    private fun updateUIWithLatLng(message: String, bookingId: String?, driverLat: Double?, driverLng: Double?) {
        Log.i("Notification", "Send broad casst")
        val intent = Intent(NOTIFICATION.ACTION_NAME)
        intent.putExtra("message", message)
        intent.putExtra("booking_id", bookingId)
        intent.putExtra("driver_lat", driverLat)
        intent.putExtra("driver_lng", driverLng)
        sendBroadcast(intent)
    }

    private fun updateUI(message: String, bookingId: String?) {
        Log.i("Notification", "Send broad casst")
        val intent = Intent(NOTIFICATION.ACTION_NAME)
        intent.putExtra("message", message)
        intent.putExtra("booking_id", bookingId)
        sendBroadcast(intent)
    }

    private fun sendNotification(title: String?, mess: String?) {
        val intent = Intent(this, MapsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        Log.i("Notification, ", "title: $title, mess: $mess")
        val notificationBuilder =
            NotificationCompat.Builder(this, CrabApplication.channel_id)
                .setContentTitle(title)
                .setContentText(mess)
                .setSmallIcon(R.drawable.ic_grab)
                .setContentIntent(pendingIntent)


        val notification = notificationBuilder.build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)  as NotificationManager
        if (notificationManager != null)
        {
            notificationManager.notify(1, notification)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        val obj = JsonObject()
        obj.addProperty("user", CredentialService().getAll().id)
        obj.addProperty("token", token)

        APIService().doPost<Any>("notification/update-token", obj,  object : APICallback<Any> {
            override fun onSuccess(result: Any) {
                Log.i("Notification", "result: $result")
            }

            override fun onError(t: Throwable) {
                Log.i("Notification", "result: ${t.message}")

            }
        })
    }
}