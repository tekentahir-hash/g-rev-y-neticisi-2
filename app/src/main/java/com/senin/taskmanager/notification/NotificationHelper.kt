package com.senin.taskmanager.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.senin.taskmanager.MainActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

const val CHANNEL_ID = "task_channel"
const val CHANNEL_NAME = "Görev Bildirimleri"

fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.createNotificationChannel(channel)
}

fun scheduleNotification(context: Context, taskId: Int, title: String, date: String, time: String) {
    val parts = time.split(":")
    val hour = parts[0].toIntOrNull() ?: return
    val min = parts[1].toIntOrNull() ?: return
    val localDate = LocalDate.parse(date)
    val triggerAt = LocalDateTime.of(localDate, LocalTime.of(hour, min))
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    if (triggerAt <= System.currentTimeMillis()) return

    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("task_id", taskId)
        putExtra("task_title", title)
    }
    val pi = PendingIntent.getBroadcast(
        context, taskId, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("task_id", 0)
        val taskTitle = intent.getStringExtra("task_title") ?: "Görev hatırlatması"

        val openIntent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, taskId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ Görev Zamanı!")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(taskId, notification)
    }
}
