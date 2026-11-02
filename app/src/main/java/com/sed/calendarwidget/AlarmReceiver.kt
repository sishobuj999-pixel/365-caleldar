package com.sed.calendarwidget

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("sed_calendar_alarms", Context.MODE_PRIVATE)
            prefs.all.filterKeys { it.startsWith("alarm_") }.values.forEach { value ->
                val fields = (value as? String)?.split(",", limit = 6) ?: return@forEach
                if (fields.size == 6) {
                    schedule(context, fields[0].toInt(), fields[1].toInt(), fields[2].toInt(),
                        fields[3].toInt(), fields[4].toInt(), fields[5])
                }
            }
            return
        }

        val title = intent.getStringExtra("title") ?: "SED Calendar reminder"
        val description = intent.getStringExtra("description") ?: "You have an event today."
        val id = intent.getIntExtra("notificationId", 8102)
        context.getSharedPreferences("sed_calendar_alarms", Context.MODE_PRIVATE)
            .edit().remove("alarm_" + id).apply()

        val channelId = "sed_calendar_alarms"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(channelId, "Calendar alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Date and event alarms"
                setSound(alarmSound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
        val openIntent = PendingIntent.getActivity(context, 8101, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, channelId)
            else {
                @Suppress("DEPRECATION")
                Notification.Builder(context).setSound(alarmSound)
            }
        val notification = builder.setSmallIcon(R.mipmap.ic_launcher).setContentTitle(title)
            .setContentText(description).setStyle(Notification.BigTextStyle().bigText(description))
            .setContentIntent(openIntent).setAutoCancel(true).setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_MAX).setVibrate(longArrayOf(0, 500, 250, 500)).build()
        manager.notify(id, notification)
    }

    companion object {
        fun schedule(context: Context, year: Int, month: Int, day: Int, hour: Int, minute: Int, description: String): Boolean {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year); set(Calendar.MONTH, month - 1); set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= System.currentTimeMillis()) return false
            val id = year * 10000 + month * 100 + day
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.sed.calendarwidget.DATE_ALARM"
                putExtra("title", "SED Calendar · " + day + "/" + month + "/" + year)
                putExtra("description", description.ifBlank { "Your scheduled reminder." })
                putExtra("notificationId", id)
            }
            val pending = PendingIntent.getBroadcast(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val showIntent = PendingIntent.getActivity(context, id + 1, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return false
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(calendar.timeInMillis, showIntent), pending)
            val value = listOf(year, month, day, hour, minute).joinToString(",") + "," + description.replace(",", " ")
            context.getSharedPreferences("sed_calendar_alarms", Context.MODE_PRIVATE)
                .edit().putString("alarm_" + id, value).apply()
            return true
        }
    }
}