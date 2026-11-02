package com.sed.calendarwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.Duration
import java.time.LocalDateTime
import java.time.Month

class CalendarWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val now = LocalDateTime.now()
        val target = LocalDateTime.of(2027, Month.JANUARY, 1, 0, 0)
        val duration = Duration.between(now, target)
        val days = duration.toDays().coerceAtLeast(0)
        val hours = duration.toHours().rem(24).coerceAtLeast(0)
        val minutes = duration.toMinutes().rem(60).coerceAtLeast(0)
        val text = if (duration.isNegative) "2027 HAS ARRIVED"
            else "NEW YEAR 2027  •  %dD %02dH %02dM LEFT".format(days, hours, minutes)

        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.calendar_widget)
            views.setTextViewText(R.id.widget_countdown, text)
            val intent = Intent(context, MainActivity::class.java)
            val pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            manager.updateAppWidget(id, views)
        }
    }
}
