package com.sed.calendarwidget

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clock: TextView
    private lateinit var dateLine: TextView
    private lateinit var countdown: TextView
    private lateinit var calendarHost: LinearLayout
    private var shownMonth = YearMonth.now()
    private val dark = Color.rgb(23, 25, 35)
    private val panel = Color.rgb(35, 39, 52)
    private val accent = Color.rgb(74, 222, 128)
    private val white = Color.WHITE
    private val muted = Color.rgb(190, 195, 208)

    private val ticker = object : Runnable {
        override fun run() {
            val now = LocalDateTime.now()
            clock.text = now.format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH))
            dateLine.text = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))
            val target = LocalDateTime.of(2027, 1, 1, 0, 0)
            val d = Duration.between(now, target)
            countdown.text = if (d.isNegative) "2027 HAS ARRIVED" else
                "%d DAYS  %02d:%02d:%02d LEFT".format(
                    d.toDays(), d.toHours().rem(24), d.toMinutes().rem(60), d.seconds.rem(60))
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = dark
        window.navigationBarColor = dark
        window.decorView.systemUiVisibility = 0
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
            setPadding(dp(18), dp(22), dp(18), dp(12))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val year = TextView(this).apply {
            text = "2026"; textSize = 42f; setTextColor(white); typeface = Typeface.DEFAULT_BOLD
        }
        clock = TextView(this).apply {
            textSize = 32f; setTextColor(white); typeface = Typeface.DEFAULT_BOLD
        }
        dateLine = TextView(this).apply { textSize = 15f; setTextColor(muted) }
        countdown = TextView(this).apply {
            textSize = 16f; setTextColor(accent); typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(12), 0, dp(18))
        }
        content.addView(year)
        content.addView(clock)
        content.addView(dateLine)
        content.addView(countdown)
        val heading = TextView(this).apply {
            text = "FULL CALENDAR"; textSize = 13f; setTextColor(muted)
            typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(8), 0, dp(10))
        }
        content.addView(heading)
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }
        val prev = Button(this).apply { text = "‹"; setOnClickListener { shownMonth = shownMonth.minusMonths(1); renderMonth() } }
        val next = Button(this).apply { text = "›"; setOnClickListener { shownMonth = shownMonth.plusMonths(1); renderMonth() } }
        val monthTitle = TextView(this).apply {
            tag = "monthTitle"; textSize = 20f; setTextColor(white); gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        nav.addView(prev, LinearLayout.LayoutParams(dp(52), dp(48)))
        nav.addView(monthTitle, LinearLayout.LayoutParams(0, dp(48), 1f))
        nav.addView(next, LinearLayout.LayoutParams(dp(52), dp(48)))
        content.addView(nav)
        calendarHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(calendarHost)
        val todayButton = Button(this).apply {
            text = "TODAY"; setOnClickListener { shownMonth = YearMonth.now(); renderMonth() }
        }
        content.addView(todayButton)
        scroll.addView(content)
        page.addView(scroll)
        setContentView(page)
        renderMonth()
        handler.post(ticker)
    }

    private fun renderMonth() {
        calendarHost.removeAllViews()
        val monthLabel = findTextViewByTag(window.decorView, "monthTitle")
        monthLabel?.text = shownMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
        val weekdays = GridLayout(this).apply { columnCount = 7 }
        listOf("S","M","T","W","T","F","S").forEach {
            val tv = TextView(this).apply {
                text = it; gravity = Gravity.CENTER; setTextColor(muted); textSize = 13f
                setPadding(0, dp(10), 0, dp(10))
            }
            weekdays.addView(tv, GridLayout.LayoutParams().apply {
                width = 0; height = dp(38); columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            })
        }
        calendarHost.addView(weekdays)
        val grid = GridLayout(this).apply { columnCount = 7 }
        val first = shownMonth.atDay(1)
        val offset = first.dayOfWeek.value % 7
        val count = shownMonth.lengthOfMonth()
        val rows = (offset + count + 6) / 7
        repeat(rows * 7) { index ->
            val day = index - offset + 1
            val valid = day in 1..count
            val today = valid && shownMonth.atDay(day) == LocalDate.now()
            val tv = TextView(this).apply {
                text = if (valid) day.toString() else ""
                gravity = Gravity.CENTER
                textSize = 15f
                setTextColor(if (today) Color.BLACK else white)
                if (today) { setBackgroundColor(accent); typeface = Typeface.DEFAULT_BOLD }
                else if (valid) setBackgroundColor(panel)
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0; height = dp(48)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
            grid.addView(tv, lp)
        }
        calendarHost.addView(grid)
    }

    private fun findTextViewByTag(v: View, tagValue: String): TextView? {
        if (v is TextView && v.tag == tagValue) return v
        if (v is android.view.ViewGroup) {
            for (i in 0 until v.childCount) {
                val found = findTextViewByTag(v.getChildAt(i), tagValue)
                if (found != null) return found
            }
        }
        return null
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }
}
