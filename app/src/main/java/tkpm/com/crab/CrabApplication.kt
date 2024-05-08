package tkpm.com.crab

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class CrabApplication: Application() {
    companion object {
        val channel_id = "CrabNotification"
    }
    override fun onCreate() {
        super.onCreate()
        createChannelNotification()
    }

    private fun createChannelNotification() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channel_id,
                "Crab",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}