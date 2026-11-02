package com.sed.calendarwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import java.time.Duration
import java.time.LocalDateTime
import java.time.Month

class CalendarWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val now = LocalDateTime.now()
        val target = LocalDateTime.of(2027, Month.JANUARY, 1, 0, 0)
        val duration = Duration.between(now, target)

        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.calendar_widget)
            if (duration.isNegative || duration.isZero) {
                views.setTextViewText(R.id.widget_countdown, "2027 HAS ARRIVED")
                views.setChronometer(R.id.widget_timer, SystemClock.elapsedRealtime(), "%s", false)
            } else {
                val days = duration.toDays()
                val remainder = duration.minusDays(days).toMillis()
                views.setTextViewText(R.id.widget_countdown, "NEW YEAR 2027  •  $days DAYS LEFT")
                // Android's native Chronometer ticks every second without waking the app.
                views.setChronometer(R.id.widget_timer, SystemClock.elapsedRealtime() + remainder, "%s", true)
                views.setChronometerCountDown(R.id.widget_timer, true)
            }

            val intent = Intent(context, MainActivity::class.java)
            val pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            manager.updateAppWidget(id, views)
        }
    }
}