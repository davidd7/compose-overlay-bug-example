package com.example.composeoverlaybugexample

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat


private const val NOTIFICATION_CHANNEL_ID = "OverlayService.Notifications"
private const val EXTRA_TYPE = "type"
private const val TYPE_SHOW_OVERLAY = 0
private const val TYPE_HIDE_OVERLAY = 1


class OverlayService() : Service() {
    private var overlayWindowHolder: OverlayWindowHolder? = null

    override fun onCreate() {
        super.onCreate()
        initNotificationChannels(this)
        startForeground(1, getForegroundNotification(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.getIntExtra(EXTRA_TYPE, -1)) {
            TYPE_SHOW_OVERLAY -> {
                overlayWindowHolder = overlayWindowHolder ?: OverlayWindowHolder(this@OverlayService)
            }
            TYPE_HIDE_OVERLAY -> {
                stopSelf()
            }
            else -> { /* do nothing */ }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        overlayWindowHolder?.hideOverlay(this)
        super.onDestroy()
    }

    companion object {
        fun showOverlay(context: Context) {
            startService(context, TYPE_SHOW_OVERLAY)
        }

        fun hideOverlay(context: Context) {
            startService(context, TYPE_HIDE_OVERLAY)
        }

        private fun startService(context: Context, type: Int) {
            val myIntent = Intent(context, OverlayService::class.java)
            myIntent.putExtra(EXTRA_TYPE, type)
            context.startForegroundService(myIntent)
        }
    }

    override fun onBind(p0: Intent?): IBinder? = null
}


fun initNotificationChannels(context: Context) {
    val newChannel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        "Displaying over other apps notification",
        NotificationManager.IMPORTANCE_LOW,
    )
    newChannel.description = ""
    newChannel.setSound(null, null)
    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(newChannel)
}


fun getForegroundNotification(context: Context): Notification {
    return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setPriority(NotificationManager.IMPORTANCE_MIN).setShowWhen(false).setSilent(true)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFAULT)
        .setCategory(Notification.CATEGORY_SERVICE)
        .build()
}






