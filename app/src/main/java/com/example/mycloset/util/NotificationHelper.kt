package com.example.mycloset.util
import com.example.mycloset.R
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(context: Context) {


    private val appContext = context.applicationContext

    // IDs
    private val CHANNEL_EVENTS = "events_channel"

    // Call this in MainActivity onCreate
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Events"
            val descriptionText = "Notifications for general app events"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_EVENTS, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    fun sendNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(appContext, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_notification) // Make sure this icon exists
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Removes notification when tapped

        with(NotificationManagerCompat.from(appContext)) {
            // Check permission (Required for API 33+)
            if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {

                // notify(id, builder.build())
                // Using current time as ID ensures notifications don't overwrite each other
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        }
    }
}