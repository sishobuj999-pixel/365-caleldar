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

open class CalendarWidgetProvider : AppWidgetProvider() {
    protected open val layoutResource: Int = R.layout.calendar_widget

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val now = LocalDateTime.now()
        val target = LocalDateTime.of(2027, Month.JANUARY, 1, 0, 0)
        val duration = Duration.between(now, target)

        ids.forEach { id ->
            val views = RemoteViews(context.packageName, layoutResource)
            if (duration.isNegative || duration.isZero) {
                views.setTextViewText(R.id.widget_days, "00")
                views.setTextViewText(R.id.widget_hour_prefix, "00:")
                views.setChronometer(R.id.widget_timer, SystemClock.elapsedRealtime(), "%s", false)
                views.setChronometerCountDown(R.id.widget_timer, false)
            } else {
                val days = duration.toDays()
                val remainder = duration.minusDays(days).toMillis()
                val hours = Duration.ofMillis(remainder).toHours()
                val hourPrefix = when {
                    hours < 1 -> "00:"
                    hours < 10 -> "0"
                    else -> ""
                }
                views.setTextViewText(R.id.widget_days, days.toString().padStart(2, '0'))
                views.setTextViewText(R.id.widget_hour_prefix, hourPrefix)
                // Native Chronometer ticks every second; the day and zero-padding are static until widget refresh.
                views.setChronometer(R.id.widget_timer, SystemClock.elapsedRealtime() + remainder, "%s", true)
                views.setChronometerCountDown(R.id.widget_timer, true)
            }

            val intent = Intent(context, MainActivity::class.java)
            val pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            manager.updateAppWidget(id, views)
        }
    }
}

class WaterCalendarWidgetProvider : CalendarWidgetProvider() {
    override val layoutResource: Int = R.layout.calendar_widget_water
}

class BlurCalendarWidgetProvider : CalendarWidgetProvider() {
    override val layoutResource: Int = R.layout.calendar_widget_blur
}

class TransparentCalendarWidgetProvider : CalendarWidgetProvider() {
    override val layoutResource: Int = R.layout.calendar_widget_transparent
}