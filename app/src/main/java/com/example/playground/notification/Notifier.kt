package com.example.playground.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.playground.MainActivity
import com.example.playground.data.local.NotificationDao
import com.example.playground.data.local.NotificationEntity
import com.example.playground.data.model.MaStatus

class Notifier(
    private val context: Context,
    private val notificationDao: NotificationDao,
) {

    init {
        ensureChannels()
    }

    suspend fun notifyCrossover(
        symbol: String,
        name: String,
        newStatus: MaStatus,
        ma5: Double,
        ma20: Double,
        close: Double,
        market: String = "",
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val statusLabel = if (newStatus == MaStatus.BUY) "매수 전환" else "매도 전환"
        val title = "[MA $statusLabel] $name ($symbol)"
        val body = "5MA ${formatNumber(ma5)} · 20MA ${formatNumber(ma20)} · 종가 ${formatNumber(close)}"

        val notification = NotificationCompat.Builder(context, CHANNEL_MA)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(launchIntent())
            .build()

        nm.notify(symbol.hashCode(), notification)

        notificationDao.insert(
            NotificationEntity(
                symbol = symbol,
                name = name,
                type = "MA",
                status = if (newStatus == MaStatus.BUY) "BUY" else "SELL",
                market = market,
                detail = body,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun notifyQuantSignal(
        symbol: String,
        name: String,
        newStatus: MaStatus,
        rsi2: Double,
        sma200: Double,
        close: Double,
        market: String = "",
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val statusLabel = if (newStatus == MaStatus.BUY) "매수 신호" else "매도 신호"
        val title = "[RSI $statusLabel] $name ($symbol)"
        val body = "RSI(2) ${String.format(java.util.Locale.US, "%.1f", rsi2)} · SMA200 ${formatNumber(sma200)} · 종가 ${formatNumber(close)}"

        val notification = NotificationCompat.Builder(context, CHANNEL_MA)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(launchIntent())
            .build()

        nm.notify((symbol + "_quant").hashCode(), notification)

        notificationDao.insert(
            NotificationEntity(
                symbol = symbol,
                name = name,
                type = "RSI",
                status = if (newStatus == MaStatus.BUY) "BUY" else "SELL",
                market = market,
                detail = body,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    private fun launchIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_MA) == null) {
            val channel = NotificationChannel(
                CHANNEL_MA,
                "이평선 교차 알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "5일/20일 이동평균 교차 및 RSI 전략 알림"
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun formatNumber(value: Double): String =
        if (value >= 1000) String.format(java.util.Locale.US, "%,.0f", value)
        else String.format(java.util.Locale.US, "%.2f", value)

    companion object {
        const val CHANNEL_MA = "ma_crossover"
    }
}
