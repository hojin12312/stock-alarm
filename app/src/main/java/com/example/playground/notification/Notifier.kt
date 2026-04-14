package com.example.playground.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.playground.MainActivity
import com.example.playground.data.model.MaStatus

class Notifier(private val context: Context) {

    init {
        ensureChannel()
    }

    fun notifyCrossover(
        symbol: String,
        name: String,
        newStatus: MaStatus,
        ma5: Double,
        ma20: Double,
        close: Double,
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val statusLabel = if (newStatus == MaStatus.BUY) "매수 전환" else "매도 전환"
        val title = "[$statusLabel] $name ($symbol)"
        val body = "5MA ${formatNumber(ma5)} · 20MA ${formatNumber(ma20)} · 종가 ${formatNumber(close)}"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingFlags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(symbol.hashCode(), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "이평선 교차 알림",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "5일/20일 이동평균 대소 관계가 반전되면 알려줘"
        }
        nm.createNotificationChannel(channel)
    }

    private fun formatNumber(value: Double): String =
        if (value >= 1000) String.format(java.util.Locale.US, "%,.0f", value)
        else String.format(java.util.Locale.US, "%.2f", value)

    companion object {
        const val CHANNEL_ID = "ma_crossover"
    }
}
