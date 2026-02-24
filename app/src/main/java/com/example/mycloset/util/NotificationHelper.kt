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
    private val CHANNEL_POPUP = "popup_channel"
    private val CHANNEL_SILENT = "silent_channel"

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val popupChannel = NotificationChannel(
                CHANNEL_POPUP,
                "Popup Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "PopUp notification"
            }

            val silentChannel = NotificationChannel(
                CHANNEL_SILENT,
                "Icon alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = " Icon tray notifications"
            }
            notificationManager.createNotificationChannels(listOf(popupChannel, silentChannel))
        }
    }


    fun sendNotification(title: String, message: String, isUrgent: Boolean = true) {
        val channelId = if(isUrgent) CHANNEL_POPUP else CHANNEL_SILENT
        val priority = if(isUrgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT

        val builder = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setAutoCancel(true) // Removes notification when tapped

        with(NotificationManagerCompat.from(appContext)) {
            // Check permission (Required for API 33+)
            if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {

                val notificationId = if(isUrgent) {
                    System.currentTimeMillis().toInt()
                } else{
                    1001 // Fixed id to replace the old silent one
                }
                notify(notificationId, builder.build())
            }
        }
    }
}