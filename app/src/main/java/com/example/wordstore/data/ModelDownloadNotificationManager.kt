package com.example.wordstore.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.wordstore.R

class ModelDownloadNotificationManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        private const val CHANNEL_ID = "model_download_channel"
        private const val NOTIFICATION_ID = 1001
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model İndirmeleri",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Dil modeli indirme durumu"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showDownloadStarted(languageCode: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Dil Modeli İndiriliyor")
            .setContentText("${languageCode.uppercase()} dil modeli indiriliyor...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, 0, true) // Indeterminate progress
            .setOngoing(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    fun updateProgress(languageCode: String, progress: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Dil Modeli İndiriliyor")
            .setContentText("${languageCode.uppercase()}: %$progress")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    fun showDownloadComplete(languageCode: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("İndirme Tamamlandı")
            .setContentText("${languageCode.uppercase()} dil modeli hazır")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setProgress(0, 0, false)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    fun showDownloadError(languageCode: String, error: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("İndirme Başarısız")
            .setContentText("${languageCode.uppercase()}: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setProgress(0, 0, false)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
