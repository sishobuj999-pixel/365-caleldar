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

object WidgetNavigation {
    fun pending(context: Context, page: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).putExtra("widget_page", page).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

open class CalendarWidgetProvider : AppWidgetProvider() {
    protected open val layoutResource: Int = R.layout.calendar_widget

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        WeatherService.refreshIfStale(context)
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

            val weather = runCatching { org.json.JSONObject(context.getSharedPreferences("sed_weather", Context.MODE_PRIVATE).getString("weather_json", "{}") ?: "{}") }.getOrNull()
            val temperature = weather?.optDouble("temperature", Double.NaN) ?: Double.NaN
            val description = weather?.optString("description", "Weather") ?: "Weather"
            views.setTextViewText(R.id.widget_weather_temp, if (temperature.isFinite()) "${temperature.toInt()}°" else "--°")
            views.setTextViewText(R.id.widget_weather_desc, description)
            // Only bind IDs present in the classic layouts; root intentionally has no click action.
            views.setOnClickPendingIntent(R.id.widget_date, WidgetNavigation.pending(context, "date", id * 100 + 1))
            views.setOnClickPendingIntent(R.id.widget_clock, WidgetNavigation.pending(context, "time", id * 100 + 2))
            views.setOnClickPendingIntent(R.id.widget_weather_panel, WidgetNavigation.pending(context, "weather", id * 100 + 3))
            views.setOnClickPendingIntent(R.id.widget_days, WidgetNavigation.pending(context, "countdown", id * 100 + 4))
            views.setOnClickPendingIntent(R.id.widget_timer, WidgetNavigation.pending(context, "countdown", id * 100 + 5))
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

open class AdvancedCalendarWidgetProvider:AppWidgetProvider(){
 protected open val style:Int=0
 private val bgs=intArrayOf(R.drawable.widget_glass,R.drawable.widget_water,R.drawable.widget_blur,R.drawable.widget_transparent)
 private fun show(v:RemoteViews,id:Int,on:Boolean){v.setViewVisibility(id,if(on)android.view.View.VISIBLE else android.view.View.GONE)}
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){
  WeatherService.refreshIfStale(context)
  val now=java.time.LocalDateTime.now();val start=now.withDayOfYear(1).toLocalDate().atStartOfDay();val next=now.plusYears(1).withDayOfYear(1).toLocalDate().atStartOfDay()
  val left=java.time.Duration.between(now,next).coerceAtLeast(java.time.Duration.ZERO);val days=left.toDays();val rem=left.minusDays(days).toMillis()
  val total=java.time.Duration.between(start,next).seconds.coerceAtLeast(1);val elapsed=java.time.Duration.between(start,now).seconds.coerceAtLeast(0);val pct=((elapsed*100.0)/total).toInt().coerceIn(0,100)
  val loc=java.util.Locale.getDefault();val month=now.month.getDisplayName(java.time.format.TextStyle.FULL,loc).uppercase(loc);val wd=now.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL,loc);val week=now.get(java.time.temporal.WeekFields.of(loc).weekOfWeekBasedYear());val yd=now.toLocalDate().lengthOfYear()
  ids.forEach{id->val v=RemoteViews(context.packageName,R.layout.calendar_widget_multi)
   v.setInt(R.id.widget_root,"setBackgroundResource",bgs[style%4]);v.setTextViewText(R.id.widget_days,days.toString().padStart(2,'0'));v.setChronometer(R.id.widget_timer,android.os.SystemClock.elapsedRealtime()+rem,"%s",true);v.setChronometerCountDown(R.id.widget_timer,true)
   v.setTextViewText(R.id.widget_year,now.year.toString());v.setTextViewText(R.id.widget_percent,"$pct%");v.setProgressBar(R.id.widget_progress,100,pct,false);v.setTextViewText(R.id.widget_dayofyear,"Day ${now.dayOfYear} of $yd");v.setTextViewText(R.id.widget_passed,"${now.dayOfYear-1} days passed");v.setTextViewText(R.id.widget_remaining,"$days days until New Year");v.setTextViewText(R.id.widget_daynum,now.dayOfMonth.toString());v.setTextViewText(R.id.widget_month,month);v.setTextViewText(R.id.widget_week,"WEEK $week · $wd".uppercase(loc))
   val clock=style in listOf(0,1,4,5,6,7,8,9,11);val date=style in listOf(1,4,5,6,7,8,9,11);val count=style in listOf(3,6,8,9);val progress=style in listOf(2,7,9,10,11);val stats=style in listOf(7,9,10);val tile=style==4;val weekInfo=style==5||style==9
   show(v,R.id.widget_header,clock);show(v,R.id.widget_clock,clock);show(v,R.id.widget_year,style in listOf(1,7,9));show(v,R.id.widget_date,date);show(v,R.id.widget_countdown_row,count);show(v,R.id.widget_progress_row,progress);show(v,R.id.widget_dayofyear,weekInfo||style in listOf(2,10,11));show(v,R.id.widget_passed,stats);show(v,R.id.widget_remaining,stats||style in listOf(3,6,8));show(v,R.id.widget_daynum,tile);show(v,R.id.widget_month,tile);show(v,R.id.widget_week,weekInfo)
   if(style==0)v.setTextViewTextSize(R.id.widget_clock,android.util.TypedValue.COMPLEX_UNIT_SP,27f);if(style==8)v.setTextViewTextSize(R.id.widget_days,android.util.TypedValue.COMPLEX_UNIT_SP,42f)
   val weather=runCatching{org.json.JSONObject(context.getSharedPreferences("sed_weather",Context.MODE_PRIVATE).getString("weather_json","{}")?: "{}")}.getOrNull()
   val temp=weather?.optDouble("temperature",Double.NaN)?:Double.NaN
   val description=weather?.optString("description","Weather")?:"Weather"
   v.setTextViewText(R.id.widget_weather_temp,if(temp.isFinite()) "${temp.toInt()}°" else "--°")
   v.setTextViewText(R.id.widget_weather_desc,description)
   // Keep the root inert so individual data areas receive their own page intents.
   v.setOnClickPendingIntent(R.id.widget_clock,WidgetNavigation.pending(context,"time",id*100+21))
   v.setOnClickPendingIntent(R.id.widget_date,WidgetNavigation.pending(context,"date",id*100+22))
   v.setOnClickPendingIntent(R.id.widget_weather_panel,WidgetNavigation.pending(context,"weather",id*100+23))
   v.setOnClickPendingIntent(R.id.widget_countdown_row,WidgetNavigation.pending(context,"countdown",id*100+24))
   v.setOnClickPendingIntent(R.id.widget_days,WidgetNavigation.pending(context,"countdown",id*100+25))
   v.setOnClickPendingIntent(R.id.widget_timer,WidgetNavigation.pending(context,"countdown",id*100+26))
   v.setOnClickPendingIntent(R.id.widget_progress_row,WidgetNavigation.pending(context,"progress",id*100+27))
   v.setOnClickPendingIntent(R.id.widget_percent,WidgetNavigation.pending(context,"progress",id*100+28))
   v.setOnClickPendingIntent(R.id.widget_progress,WidgetNavigation.pending(context,"progress",id*100+29))
   v.setOnClickPendingIntent(R.id.widget_dayofyear,WidgetNavigation.pending(context,"yearInfo",id*100+30))
   v.setOnClickPendingIntent(R.id.widget_passed,WidgetNavigation.pending(context,"yearInfo",id*100+31))
   v.setOnClickPendingIntent(R.id.widget_remaining,WidgetNavigation.pending(context,"countdown",id*100+32))
   v.setOnClickPendingIntent(R.id.widget_daynum,WidgetNavigation.pending(context,"date",id*100+33))
   v.setOnClickPendingIntent(R.id.widget_month,WidgetNavigation.pending(context,"date",id*100+34))
   v.setOnClickPendingIntent(R.id.widget_week,WidgetNavigation.pending(context,"yearInfo",id*100+35))
   manager.updateAppWidget(id,v)
  }
 }
}

class MiniClockWidgetProvider:AdvancedCalendarWidgetProvider(){override val style=0}

class ClockStripWidgetProvider:AdvancedCalendarWidgetProvider(){override val style=1}

class YearProgressLineWidgetProvider:AdvancedCalendarWidgetProvider(){override val style=2}

class CountdownStripWidgetProvider:AdvancedCalendarWidgetProvider(){override val style=3}

class DateSquareWidgetProvider:AdvancedCalendarWidgetProvider(){override val style=4}

class WeekDayCardWidgetProvider:AdvancedCalendarWidgetProvider(){override val style=5}

class CountdownCardWidgetProvider:AdvancedCalendarWidgetProvider(){override val style=6}

class YearProgressCardWidgetProvider:AdvancedCalendarWidgetProvider(){override val style=7}

class VerticalCountdownWidgetProvider:AdvancedCalendarWidgetProvider(){override val style=8}

class YearStatisticsWidgetProvider:AdvancedCalendarWidgetProvider(){override val style=9}

class MinimalProgressWidgetProvider:AdvancedCalendarWidgetProvider(){override val style=10}

class DualInfoWidgetProvider:AdvancedCalendarWidgetProvider(){override val style=11}
