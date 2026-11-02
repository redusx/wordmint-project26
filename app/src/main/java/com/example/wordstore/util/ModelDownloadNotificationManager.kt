package com.example.wordstore.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.wordstore.R
import com.example.wordstore.ui.format.languageName

class ModelDownloadNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_downloads),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    fun showDownloadStarted(languageCode: String) {
        post(languageCode, R.string.pack_notification_downloading, android.R.drawable.stat_sys_download, ongoing = true)
    }

    fun showDownloadComplete(languageCode: String) {
        post(languageCode, R.string.pack_notification_ready, android.R.drawable.stat_sys_download_done, ongoing = false)
    }

    fun showDownloadFailed(languageCode: String) {
        post(languageCode, R.string.pack_notification_failed, android.R.drawable.stat_notify_error, ongoing = false)
    }

    fun cancel() = notificationManager.cancel(NOTIFICATION_ID)

    private fun post(languageCode: String, textRes: Int, icon: Int, ongoing: Boolean) {
        val text = context.getString(textRes, languageName(languageCode))
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(text)
            .setSmallIcon(icon)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .apply { if (ongoing) setProgress(0, 0, true) }
            .build()
        // Without permission this does nothing, which is the intended behavior.
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "model_download_channel"
        const val NOTIFICATION_ID = 1001
    }
}
