package com.sed.calendarwidget

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = 48 }
        }
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 410)
        }

        val web = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            overScrollMode = View.OVER_SCROLL_NEVER
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            webChromeClient = WebChromeClient()
            addJavascriptInterface(AlarmBridge(), "AndroidAlarm")
            loadDataWithBaseURL("https://sed.calendar.local/", calendarHtml(), "text/html", "UTF-8", null)
        }
        setContentView(web)
    }

    inner class AlarmBridge {
        @JavascriptInterface
        fun scheduleAlarm(year: Int, month: Int, day: Int, hour: Int, minute: Int, description: String): String {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Allow notifications for calendar alarms.", Toast.LENGTH_LONG).show()
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 410)
                }
                return "permission"
            }
            return try {
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(settingsIntent)
                    "exact_permission"
                } else if (AlarmReceiver.schedule(this@MainActivity, year, month, day, hour, minute, description)) "ok" else "past"
            } catch (_: Exception) { "error" }
        }
    }

    private fun calendarHtml(): String = """
<!DOCTYPE html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>SED Calendar</title>
<style>
:root{--ink:#fff;--line:rgba(255,255,255,.18);--glass:rgba(21,34,56,.34);--edge:rgba(255,255,255,.28)}
*{box-sizing:border-box;margin:0}
html,body{width:100%;height:100%;background:transparent;overflow:hidden}
body{display:flex;justify-content:center;align-items:center;font-family:"Helvetica Neue",Helvetica,Arial,sans-serif;color:var(--ink);padding:env(safe-area-inset-top) 0 env(safe-area-inset-bottom)}
button{font:inherit;cursor:pointer;color:white}
.sheet{container-type:inline-size;position:relative;width:min(100vw,calc(100vh*9/16));height:min(177.7778vw,100vh);background:rgba(20,30,49,.30);-webkit-backdrop-filter:blur(32px) saturate(165%);backdrop-filter:blur(32px) saturate(165%);border:1px solid var(--edge);border-radius:5cqw;padding:0;display:flex;flex-direction:column;box-shadow:0 12px 42px rgba(0,0,0,.2);touch-action:pan-y;overflow:hidden}
.top{display:flex;justify-content:space-between;align-items:flex-end;padding:0 .4cqw 2.4cqw;min-height:19cqw}
.yearnav{display:flex;align-items:center;gap:1.2cqw;min-width:0}.yearnav h1{cursor:ns-resize}.calendarSwitch{border:1px solid var(--edge);background:rgba(255,255,255,.12);border-radius:99px;padding:1.5cqw 2.2cqw;font-size:2.4cqw;font-weight:700;white-space:nowrap;color:#fff;max-width:25cqw}
.top h1{font-size:17cqw;font-weight:800;letter-spacing:-.06em;line-height:.8;touch-action:pan-y;user-select:none}
.nav,.iconbtn{border:1px solid var(--edge);background:rgba(255,255,255,.10);color:white;border-radius:50%;width:7cqw;height:7cqw;font-size:4cqw;display:flex;align-items:center;justify-content:center;backdrop-filter:blur(12px)}
.nav:disabled{opacity:.15}
.live{text-align:right;font-variant-numeric:tabular-nums;max-width:57%;min-width:0}
#clk{font-size:5.2cqw;font-weight:800;letter-spacing:-.02em;line-height:1}
#dt,#left{font-size:1.9cqw;font-weight:600;margin-top:.9cqw;line-height:1.35}
#left{color:rgba(255,255,255,.88)}
.wk,.g{display:grid;grid-template-columns:repeat(14,1fr)}
.wk span{font-size:1.5cqw;font-weight:700;text-align:center;padding:1.2cqw 0;border-top:.5cqw solid rgba(255,255,255,.85);border-bottom:1px solid var(--line)}
.g{flex:1;min-height:0;grid-template-rows:repeat(27,minmax(0,1fr));border-left:1px solid var(--line);will-change:transform,opacity}
.g.year-next{animation:yearNext .28s cubic-bezier(.2,.75,.25,1)}.g.year-prev{animation:yearPrev .28s cubic-bezier(.2,.75,.25,1)}
@keyframes yearNext{from{opacity:.25;transform:translateX(9%)}to{opacity:1;transform:translateX(0)}}
@keyframes yearPrev{from{opacity:.25;transform:translateX(-9%)}to{opacity:1;transform:translateX(0)}}
.g .day{border-right:1px solid var(--line);border-bottom:1px solid var(--line);display:flex;flex-direction:column;justify-content:center;align-items:center;gap:.25cqw;background:rgba(255,255,255,.035);min-width:0;overflow:hidden}
.g .empty{border-right:1px solid var(--line);border-bottom:1px solid var(--line);background:rgba(255,255,255,.018)}
.g b{font-size:2.7cqw;font-weight:800;letter-spacing:-.03em;line-height:1}
.g i{font-style:normal;font-size:1.3cqw;font-weight:500;color:rgba(255,255,255,.72);line-height:1;white-space:nowrap}
.g .month{background:rgba(0,0,0,.43)}.g .month b{color:#fff}.g .month i{color:#ddd}
.g .today{background:rgba(22,163,74,.82)!important}.g .today b,.g .today i{color:#fff}
.g .event{background:rgba(220,38,38,.88)!important}.g .event b,.g .event i{color:#fff}
.g .event.today{background:rgba(190,35,48,.96)!important}
.hint{text-align:center;font-size:1.6cqw;color:rgba(255,255,255,.58);padding-top:1.1cqw;letter-spacing:.08em}
.glass{background:rgba(24,39,63,.46);border:1px solid rgba(255,255,255,.30);box-shadow:0 18px 55px rgba(0,0,0,.32),inset 0 1px 0 rgba(255,255,255,.12);-webkit-backdrop-filter:blur(18px) saturate(145%);backdrop-filter:blur(18px) saturate(145%)}
.overlay{position:absolute;inset:0;background:rgba(3,9,20,.22);-webkit-backdrop-filter:blur(14px) saturate(130%);backdrop-filter:blur(14px) saturate(130%);display:flex;align-items:center;justify-content:center;padding:6cqw;z-index:5;opacity:0;visibility:hidden;transition:opacity .24s ease,visibility .24s ease}
.overlay.show{opacity:1;visibility:visible}
.historyOverlay{position:absolute;inset:0;z-index:10;padding:3cqw;background:rgba(3,9,20,.30);-webkit-backdrop-filter:blur(20px) saturate(135%);backdrop-filter:blur(20px) saturate(135%);display:flex;align-items:stretch;justify-content:center;opacity:0;visibility:hidden;transition:opacity .22s ease,visibility .22s ease}.historyOverlay.show{opacity:1;visibility:visible}.historyPanel{width:100%;height:100%;border-radius:4cqw;padding:4cqw;display:flex;flex-direction:column;min-height:0;transform:translateY(12px);transition:transform .24s ease}.historyOverlay.show .historyPanel{transform:translateY(0)}.historyHead{display:flex;justify-content:space-between;align-items:center;gap:2cqw;margin-bottom:2cqw}.historyHead h2{font-size:5cqw}.historyHead p{font-size:2.5cqw;color:rgba(255,255,255,.68);margin-top:1cqw}.historySummary{font-size:2.5cqw;color:rgba(255,255,255,.78);padding:2cqw;border-radius:2cqw;background:rgba(255,255,255,.07);margin-bottom:2cqw}.historyFilters{display:flex;gap:1.5cqw;overflow-x:auto;padding:0 0 2cqw;flex-shrink:0}.historyFilter{white-space:nowrap;border:1px solid rgba(255,255,255,.2);border-radius:99px;padding:1.8cqw 3cqw;background:rgba(255,255,255,.08);font-size:2.4cqw}.historyFilter.active{background:rgba(59,130,246,.72);border-color:rgba(255,255,255,.5)}.historyList{overflow:auto;min-height:0;flex:1;padding-right:1cqw;overscroll-behavior:contain}.historyCard{padding:2.7cqw;border:1px solid rgba(255,255,255,.18);border-radius:2.7cqw;background:rgba(255,255,255,.075);margin-bottom:2cqw}.historyMeta{font-size:2.2cqw;color:#a9c9ff;margin-bottom:1cqw}.historyCard strong{display:block;font-size:3cqw;line-height:1.35;margin-bottom:1cqw}.historyCard p{font-size:2.55cqw;line-height:1.48;color:rgba(255,255,255,.88);white-space:pre-wrap}.historyPages{font-size:2.15cqw;color:rgba(255,255,255,.58);margin-top:1.5cqw;overflow-wrap:anywhere}.historyFoot{font-size:2.1cqw;color:rgba(255,255,255,.48);padding-top:2cqw;text-align:center;flex-shrink:0}.historyMore{display:block;width:100%;padding:2.5cqw;border:1px solid rgba(255,255,255,.2);border-radius:2cqw;background:rgba(255,255,255,.1);font-size:2.5cqw;margin:1cqw 0 2cqw}
.modal{width:100%;max-width:440px;border-radius:5cqw;padding:5cqw;transform:translateY(14px) scale(.96);transition:transform .28s cubic-bezier(.2,.8,.2,1);max-height:88%;overflow:auto}
.overlay.show .modal{transform:translateY(0) scale(1)}
.modalhead{display:flex;justify-content:space-between;align-items:center;margin-bottom:3cqw}
.modal h2{font-size:5cqw;font-weight:800;letter-spacing:-.025em}.modal,.historyPanel{font-family:"Noto Sans Bengali","Noto Sans",system-ui,-apple-system,sans-serif}.datebig{line-height:1.55;white-space:pre-line}.modalhead{padding-bottom:2cqw;border-bottom:1px solid rgba(255,255,255,.14)}
.close{border:1px solid var(--edge);background:rgba(255,255,255,.12);color:white;border-radius:50%;width:8cqw;height:8cqw;font-size:5cqw}
.datebig{font-size:3.2cqw;color:rgba(255,255,255,.76);margin-bottom:3cqw}
.countlabel{font-size:2.3cqw;color:#a9c9ff;text-transform:uppercase;letter-spacing:.1em;margin-bottom:1.2cqw}
.countdown{font-size:3.3cqw;font-weight:800;line-height:1.5;font-variant-numeric:tabular-nums}\n.durationBreakdown{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:1.5cqw;margin-top:2.5cqw}\n.durationStat{padding:2.2cqw 1cqw;border:1px solid rgba(255,255,255,.18);border-radius:2.5cqw;background:rgba(255,255,255,.075);text-align:center;min-width:0}\n.durationStat strong{display:block;font-size:4cqw;font-weight:800;font-variant-numeric:tabular-nums}\n.durationStat span{display:block;margin-top:.7cqw;font-size:1.9cqw;color:rgba(255,255,255,.68);text-transform:uppercase;letter-spacing:.06em}
.eventline{font-size:2.8cqw;margin-top:3cqw;padding:2.5cqw;border-radius:2cqw;background:rgba(255,255,255,.10);white-space:pre-wrap;overflow-wrap:anywhere}
.actions{display:flex;justify-content:flex-end;gap:2cqw;margin-top:3cqw}
.round{border:1px solid var(--edge);background:rgba(255,255,255,.13);color:white;border-radius:50%;width:10cqw;height:10cqw;font-size:5.5cqw;display:grid;place-items:center}
.editor,.alarmEditor{display:none;margin-top:3cqw;padding:3cqw;border-radius:3cqw;background:rgba(0,0,0,.12);border:1px solid rgba(255,255,255,.16)}
.editor.show,.alarmEditor.show{display:block;animation:fadeUp .22s ease}
@keyframes fadeUp{from{opacity:0;transform:translateY(7px)}to{opacity:1;transform:translateY(0)}}
label{display:block;font-size:2.5cqw;margin-bottom:1.5cqw;color:rgba(255,255,255,.84)}
textarea,input{width:100%;border:1px solid rgba(255,255,255,.28);border-radius:2cqw;background:rgba(0,0,0,.22);color:white;padding:2.5cqw;font:inherit;font-size:2.8cqw;outline:none}
textarea{min-height:20cqw;resize:vertical}textarea::placeholder{color:rgba(255,255,255,.48)}
.primary,.secondary,.danger{margin-top:2cqw;border:1px solid rgba(255,255,255,.18);border-radius:2cqw;padding:2.5cqw 4cqw;font-weight:700;font-size:2.7cqw;background:rgba(59,130,246,.82)}
.secondary{background:rgba(255,255,255,.12)}.danger{background:rgba(185,28,28,.38);color:#ffd1d1}
.scope{display:flex;gap:2cqw;align-items:center;padding:2cqw;border-radius:2cqw;background:rgba(255,255,255,.055);margin-top:1.2cqw;font-size:2.5cqw}
.scope input{width:4cqw;height:4cqw;accent-color:#70aaff;padding:0;margin:0}
.scopeWrap{display:none;margin-top:3cqw}.scopeWrap.show{display:block}
.digital{font-size:10cqw;font-weight:800;letter-spacing:.08em;text-align:center;font-variant-numeric:tabular-nums;padding:2cqw}
input[type=time]{font-size:5cqw;letter-spacing:.12em;text-align:center;color-scheme:dark}
#sheet{transform:translateZ(0);touch-action:pan-y;user-select:none;-webkit-user-select:none}
#calendarLayer{position:absolute;inset:0;display:flex;flex-direction:column;padding:5cqw 3cqw 3cqw;filter:blur(0) saturate(1);will-change:filter;transform:translateZ(0);transition:filter .28s cubic-bezier(.22,.75,.18,1)}
#hub{position:absolute;inset:0;z-index:8;border-radius:5cqw;padding:5cqw 4cqw;display:flex;flex-direction:column;transform:translate3d(0,105%,0);transition:transform .42s cubic-bezier(.22,.75,.18,1);overflow:hidden;will-change:transform;touch-action:none}
#hub.show{transform:translate3d(0,0,0)}
#hub.dragging,#sheet.dragging{transition:none!important}
#hub .page{touch-action:pan-y;overscroll-behavior:contain}
#hub .page.active{animation:none}
@media(prefers-reduced-motion:reduce){#hub,#sheet{transition-duration:.01ms!important}}
.hubhead{display:flex;align-items:center;justify-content:space-between;margin-bottom:5cqw}
.hubhead h2{font-size:7cqw}
.hubgrid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:3cqw}
.tile{min-height:25cqw;border:1px solid rgba(255,255,255,.23);border-radius:4cqw;background:rgba(255,255,255,.10);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2cqw;font-size:3cqw;font-weight:700;backdrop-filter:blur(12px)}
.tile span{font-size:8cqw}
.page{display:none;min-height:0;overflow:auto;flex:1;padding-bottom:4cqw}
.page.active{display:block;animation:fadeUp .25s ease}
.pageTitle{font-size:5cqw;font-weight:800;margin-bottom:3cqw}
.item{padding:3cqw;border:1px solid rgba(255,255,255,.2);border-radius:3cqw;background:rgba(255,255,255,.08);margin-bottom:2cqw}
.item strong{display:block;font-size:3.1cqw;margin-bottom:1cqw}
.item small{display:block;color:rgba(255,255,255,.65);font-size:2.3cqw;margin-bottom:1.5cqw}
.item p{font-size:2.7cqw;white-space:pre-wrap;overflow-wrap:anywhere}
.emptyState{text-align:center;padding:10cqw 4cqw;color:rgba(255,255,255,.58);font-size:3cqw}
.noteEditor{display:none;padding:3cqw;border-radius:3cqw;margin-bottom:3cqw}
.noteEditor.show{display:block}
.noteEditor input{margin-bottom:2cqw}
.pen{position:absolute;right:5cqw;bottom:5cqw;width:13cqw;height:13cqw;border-radius:50%;font-size:6cqw;background:rgba(59,130,246,.82);border:1px solid rgba(255,255,255,.35);box-shadow:0 8px 22px rgba(0,0,0,.25)}
.settingrow{display:flex;justify-content:space-between;align-items:center;padding:3cqw 0;border-bottom:1px solid var(--line);font-size:3cqw}
.settingrow input{width:5cqw;height:5cqw}
@media(prefers-reduced-transparency:reduce){.sheet,.modal,#hub{background:rgba(20,30,49,.96)}}
</style></head><body><main class="sheet" id="sheet">
<div id="calendarLayer"><div class="top"><div class="yearnav"><button class="nav" id="prev" aria-label="Previous year">‹</button><h1 id="year">2026</h1><button class="calendarSwitch" id="calendarType">Gregorian ▾</button><button class="nav" id="next" aria-label="Next year">›</button></div><div class="live"><div id="clk">--:--:--</div><div id="dt"></div><div id="left"></div></div></div>
<div class="wk"><span>SAT</span><span>SUN</span><span>MON</span><span>TUE</span><span>WED</span><span>THU</span><span>FRI</span><span>SAT</span><span>SUN</span><span>MON</span><span>TUE</span><span>WED</span><span>THU</span><span>FRI</span></div>
<div class="g" id="grid"></div><div class="hint">SWIPE LEFT / RIGHT · SWIPE UP FOR MORE</div></div>
<div class="overlay" id="overlay"><section class="modal glass">
<div class="modalhead"><h2 id="modalTitle">Date</h2><button class="close" id="close">×</button></div>
<div class="datebig" id="dateBig"></div><div class="countlabel" id="countLabel">Time remaining</div><div class="countdown" id="dateCountdown"></div><div class="durationBreakdown" aria-label="Calendar duration"><div class="durationStat"><strong id="yearsPart">0</strong><span>Years</span></div><div class="durationStat"><strong id="monthsPart">0</strong><span>Months</span></div><div class="durationStat"><strong id="daysPart">0</strong><span>Days</span></div></div><div class="countlabel" style="margin-top:2.5cqw">TOTAL DURATION</div><div class="durationBreakdown"><div class="durationStat"><strong id="totalDays">0</strong><span>Total days</span></div><div class="durationStat"><strong id="totalHours">0</strong><span>Total hours</span></div><div class="durationStat"><strong id="totalMinutes">0</strong><span>Total minutes</span></div></div>
<div class="eventline" id="eventLine" style="display:none"></div>
<div class="actions"><button class="round" id="historyOpen" aria-label="Explore history for this date">🌍</button><button class="round" id="add" aria-label="Add event">＋</button><button class="round" id="alarm" aria-label="Set alarm">◷</button></div>
<div class="editor" id="editor"><label for="description">Event description</label><textarea id="description" placeholder="Write your event or reminder..."></textarea><button class="primary" id="eventOk">OK</button>
<div class="scopeWrap" id="scopeWrap"><label>Repeat this event (choose one)</label>
<label class="scope"><input type="radio" name="scope" value="month-year" checked> This month in this year</label>
<label class="scope"><input type="radio" name="scope" value="all-months-year"> Every month in this year</label>
<label class="scope"><input type="radio" name="scope" value="month-all-years"> This month in every year</label>
<label class="scope"><input type="radio" name="scope" value="all-months-all-years"> Every month in every year</label>
<button class="primary" id="saveEvent">Save event</button><button class="danger" id="deleteEvent">Remove event</button></div></div>
<div class="alarmEditor" id="alarmEditor"><label for="alarmTime">Choose alarm time</label><div class="digital" id="digitalTime">09:00</div><input id="alarmTime" type="time" value="09:00"><button class="primary" id="saveAlarm">Set device alarm</button><button class="secondary" id="cancelAlarm">Cancel</button><div id="alarmStatus" class="datebig"></div></div>
</section></div>
<div class="historyOverlay" id="historyOverlay"><section class="historyPanel glass"><div class="historyHead"><div><h2>History Explorer</h2><p id="historyDate"></p></div><button class="close" id="historyClose">×</button></div><div class="historySummary" id="historySummary">Choose a date to explore its history.</div><div class="historyFilters" id="historyFilters"><button class="historyFilter active" data-kind="all">All</button><button class="historyFilter" data-kind="events">Events</button><button class="historyFilter" data-kind="births">Births</button><button class="historyFilter" data-kind="deaths">Deaths</button><button class="historyFilter" data-kind="holidays">Holidays</button><button class="historyFilter" data-kind="selected">Selected</button></div><div class="historyList" id="historyList" aria-live="polite"><div class="emptyState">Loading historical records…</div></div><div class="historyFoot" id="historyFoot">Source: English Wikipedia · On this day</div></section></div>
<section id="hub" class="glass">
<div class="hubhead"><h2 id="hubTitle">Calendar tools</h2><button class="close" id="hubBack">×</button></div>
<div id="homePage" class="page active"><div class="hubgrid">
<button class="tile" data-page="eventsPage"><span>▦</span>Events</button>
<button class="tile" data-page="notesPage"><span>▤</span>Notes</button>
<button class="tile" data-page="settingsPage"><span>⚙</span>Settings</button>
<button class="tile" data-page="aboutPage"><span>ⓘ</span>About</button>
<button class="tile" data-page="calculatorPage"><span>🧮</span>Date calculator</button>
</div><p class="emptyState">Swipe down to return to calendar</p></div>
<div id="eventsPage" class="page"><div class="pageTitle">All events</div><div id="eventsList"></div></div>
<div id="notesPage" class="page"><div class="pageTitle">Notebook</div><div id="noteEditor" class="noteEditor glass"><input id="noteTitle" placeholder="Note title"><textarea id="noteBody" placeholder="Write your note..."></textarea><button class="primary" id="saveNote">Save note</button><button class="secondary" id="cancelNote">Cancel</button></div><div id="notesList"></div><button class="pen" id="newNote" aria-label="New note">✎</button></div>
<div id="settingsPage" class="page"><div class="pageTitle">Settings</div><div class="settingrow"><span>24-hour clock</span><input type="checkbox" id="format24"></div><div class="settingrow"><span>Glass intensity</span><input type="range" id="glassLevel" min="10" max="42" value="30"></div><p class="emptyState">Settings are saved on this device.</p></div>
<div id="aboutPage" class="page"><div class="pageTitle">SED Calendar</div><div class="item"><strong>SED Calendar 1900–2100</strong><p>Calendar, recurring events, alarms and private notebook.</p></div></div>
<div id="calculatorPage" class="page"><div class="pageTitle">Date calculator</div><div class="item"><label for="calcStart">From date</label><input id="calcStart" type="date"><label for="calcEnd" style="margin-top:2.5cqw">To date</label><input id="calcEnd" type="date"><button class="primary" id="calculateDates" style="width:100%">Calculate duration</button></div><div id="calcResult" class="item" aria-live="polite"><strong>Select two dates</strong><p>See the exact calendar difference in years, months and days.</p></div></div>
</section></main>
<script>
const grid=document.getElementById('grid'),yearEl=document.getElementById('year');
const codes=['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];
const MIN_YEAR=1900,MAX_YEAR=2100;
const CALENDARS=["gregorian","hijri","bangla"];const CALENDAR_NAMES={gregorian:"Gregorian",hijri:"Hijri",bangla:"Bangla"};const BMONTHS=["Boishakh","Joishtho","Ashar","Srabon","Bhadro","Ashwin","Kartik","Agrahayan","Poush","Magh","Falgun","Chaitra"];const HMONTHS=["Muharram","Safar","Rabi al-Awwal","Rabi al-Thani","Jumada al-Awwal","Jumada al-Thani","Rajab","Shaban","Ramadan","Shawwal","Dhul Qadah","Dhul Hijjah"];let activeCalendar="gregorian";const hijriFmt=new Intl.DateTimeFormat("en-u-ca-islamic-umalqura-nu-latn",{year:"numeric",month:"numeric",day:"numeric"});
const months=['January','February','March','April','May','June','July','August','September','October','November','December'];
const p=n=>String(n).padStart(2,'0');
let shownYear=new Date().getFullYear(),selectedDate=null,events={},notes=[],editingNoteId=null;
try{events=JSON.parse(localStorage.getItem('sedCalendarEvents')||'{}')||{};}catch(e){events={};}
try{notes=JSON.parse(localStorage.getItem('sedCalendarNotes')||'[]')||[];}catch(e){notes=[];}

function calendarParts(d,cal=activeCalendar){
 if(cal==='gregorian')return {year:d.getFullYear(),month:d.getMonth()+1,day:d.getDate(),monthName:months[d.getMonth()]};
 if(cal==='hijri'){const parts=hijriFmt.formatToParts(d),v={};parts.forEach(x=>{if(x.type==='year'||x.type==='month'||x.type==='day')v[x.type]=parseInt(x.value,10);});return {year:v.year,month:v.month,day:v.day,monthName:HMONTHS[v.month-1]};}
 const start=new Date(d.getFullYear(),3,14);let by=d.getFullYear()-593;if(d<start)by--;
 const ys=new Date(by+593,3,14),offset=Math.floor((d-ys)/86400000),lens=banglaMonthLengths(by);let rem=offset,m=0;while(m<11&&rem>=lens[m]){rem-=lens[m];m++;}return {year:by,month:m+1,day:rem+1,monthName:BMONTHS[m]};
}
function banglaMonthLengths(by){const gy=by+593,leap=(gy%4===0&&(gy%100!==0||gy%400===0));return [31,31,31,31,31,31,30,30,30,30,leap?30:29,30];}
function dateForCalendarParts(y,m,day,cal=activeCalendar){
 if(cal==='gregorian'){const d=new Date(y,m-1,day);return d.getFullYear()===y&&d.getMonth()===m-1&&d.getDate()===day?d:null;}
 if(cal==='bangla'){const lens=banglaMonthLengths(y);if(m<1||m>12||day<1||day>lens[m-1])return null;const d=new Date(y+593,3,14);let off=day-1;for(let i=0;i<m-1;i++)off+=lens[i];d.setDate(d.getDate()+off);return d;}
 const estimate=Math.floor(622+(y-1)*354.367/365.2425),from=new Date(estimate-1,0,1),to=new Date(estimate+2,0,1);for(let d=new Date(from);d<to;d.setDate(d.getDate()+1)){const q=calendarParts(d,'hijri');if(q.year===y&&q.month===m&&q.day===day)return new Date(d);}return null;
}
function calendarYearStart(y,cal=activeCalendar){if(cal==='gregorian')return new Date(y,0,1);if(cal==='bangla')return new Date(y+593,3,14);return dateForCalendarParts(y,1,1,'hijri')||new Date(Math.floor(622+(y-1)*354.367/365.2425),0,1);}
function calendarYearRange(cal=activeCalendar){const n=calendarParts(new Date(),cal).year;return cal==='gregorian'?{min:1900,max:2100}:{min:n-120,max:n+120};}
function calendarMonthShort(q,cal=activeCalendar){return cal==='gregorian'?codes[q.month-1]:cal==='bangla'?BMONTHS[q.month-1].slice(0,3).toUpperCase():HMONTHS[q.month-1].slice(0,3).toUpperCase();}

function keyFor(d){return d.getFullYear()+'-'+p(d.getMonth()+1)+'-'+p(d.getDate());}
function normalizeEvent(v){return typeof v==='string'?{description:v,scope:'month-year'}:v;}
function parts(ms){const sec=Math.max(0,Math.floor(Math.abs(ms)/1000)),days=Math.floor(sec/86400),hours=Math.floor(sec%86400/3600),mins=Math.floor(sec%3600/60),secs=sec%60;return days+'d  '+p(hours)+'h  '+p(mins)+'m  '+p(secs)+'s';}
function eventApplies(base,record,target){
 const scope=record.scope||'month-year';
 if(scope==='month-year')return base.getFullYear()===target.getFullYear()&&base.getMonth()===target.getMonth()&&base.getDate()===target.getDate();
 if(scope==='all-months-year')return base.getFullYear()===target.getFullYear()&&base.getDate()===target.getDate();
 if(scope==='month-all-years')return base.getMonth()===target.getMonth()&&base.getDate()===target.getDate();
 if(scope==='all-months-all-years')return base.getDate()===target.getDate();
 return keyFor(base)===keyFor(target);
}
function eventsForDate(date){const found=[];Object.keys(events).forEach(k=>{const rec=normalizeEvent(events[k]);const a=k.split('-').map(Number);const base=new Date(a[0],a[1]-1,a[2]);if(rec&&rec.description&&eventApplies(base,rec,date))found.push({key:k,record:rec});});return found;}

function renderYear(direction){
 const range=calendarYearRange();yearEl.textContent=shownYear;document.getElementById('calendarType').textContent=CALENDAR_NAMES[activeCalendar]+' ▾';document.getElementById('prev').disabled=shownYear<=range.min;document.getElementById('next').disabled=shownYear>=range.max;
 grid.classList.remove('year-next','year-prev');void grid.offsetWidth;grid.classList.add(direction==='next'?'year-next':'year-prev');grid.innerHTML='';
 const first=calendarYearStart(shownYear),next=calendarYearStart(shownYear+1),offset=(first.getDay()+1)%7,total=Math.round((next-first)/86400000);
 for(let i=0;i<offset;i++){const e=document.createElement('div');e.className='empty';grid.appendChild(e);}
 const today=new Date(),todayKey=keyFor(today);
 for(let i=0;i<total;i++){const d=new Date(first);d.setDate(first.getDate()+i);const q=calendarParts(d),k=keyFor(d),cell=document.createElement('div');cell.className='day'+(q.day===1?' month':'');if(k===todayKey)cell.classList.add('today');const matching=eventsForDate(d);if(matching.length)cell.classList.add('event');cell.dataset.date=k;cell.innerHTML='<b>'+q.day+'</b><i>'+calendarMonthShort(q)+' '+q.month+'</i>';if(matching.length)cell.title=matching.map(x=>x.record.description).join(' · ');grid.appendChild(cell);}
 while(grid.children.length<378){const e=document.createElement('div');e.className='empty';grid.appendChild(e);}updateHeader();
}

function updateHeader(){
 const n=new Date(),clock=document.getElementById('clk'),use24=localStorage.getItem('sedCalendar24')==='true',q=calendarParts(n);
 clock.textContent=use24?p(n.getHours())+':'+p(n.getMinutes())+':'+p(n.getSeconds()):n.toLocaleTimeString([], {hour:'2-digit',minute:'2-digit',second:'2-digit'});
 document.getElementById('dt').textContent=q.day+' '+q.monthName+' '+q.year+' · '+n.getDate()+' '+months[n.getMonth()].slice(0,3)+' '+n.getFullYear();
 const left=document.getElementById('left'),start=calendarYearStart(shownYear),end=calendarYearStart(shownYear+1),today=new Date(n.getFullYear(),n.getMonth(),n.getDate()),remain=Math.max(0,Math.ceil((end-today)/86400000));
 left.textContent=shownYear===q.year?(remain+' days left in '+CALENDAR_NAMES[activeCalendar]+' year'):shownYear>q.year?('COUNTDOWN · '+Math.max(0,Math.ceil((start-today)/86400000))+' days'):'PAST '+CALENDAR_NAMES[activeCalendar].toUpperCase()+' YEAR';
}
function changeYear(delta){const range=calendarYearRange(),next=shownYear+delta;if(next<range.min||next>range.max)return;shownYear=next;renderYear(delta>0?'next':'prev');}
function switchCalendar(){const i=CALENDARS.indexOf(activeCalendar);activeCalendar=CALENDARS[(i+1)%CALENDARS.length];shownYear=calendarParts(new Date(),activeCalendar).year;renderYear('next');updateDateCountdown();}
document.getElementById('calendarType').addEventListener('click',switchCalendar);
let yearTouchY=null,yearWheelLock=0;
yearEl.addEventListener('touchstart',e=>{e.stopPropagation();yearTouchY=e.touches[0].clientY;},{passive:true});
yearEl.addEventListener('touchend',e=>{e.stopPropagation();if(yearTouchY===null)return;const dy=e.changedTouches[0].clientY-yearTouchY;yearTouchY=null;if(Math.abs(dy)>12)changeYear(dy<0?1:-1);},{passive:true});
yearEl.addEventListener('wheel',e=>{e.preventDefault();if(yearWheelLock)return;yearWheelLock=1;changeYear(e.deltaY>0?1:-1);setTimeout(()=>yearWheelLock=0,100);},{passive:false});
document.getElementById('prev').addEventListener('click',()=>changeYear(-1));document.getElementById('next').addEventListener('click',()=>changeYear(1));
grid.addEventListener('click',e=>{const cell=e.target.closest('.day');if(!cell)return;const a=cell.dataset.date.split('-').map(Number);openDate(new Date(a[0],a[1]-1,a[2]));});
function openDate(d){
 selectedDate=d;const matches=eventsForDate(d),k=keyFor(d),own=normalizeEvent(events[k]);
 document.getElementById('modalTitle').textContent=d.getDate()+' '+months[d.getMonth()]+' '+d.getFullYear();
 document.getElementById('dateBig').textContent=d.toLocaleDateString('en-GB',{weekday:'long',day:'numeric',month:'long',year:'numeric'});
 document.getElementById('description').value=own&&own.description?own.description:'';
 document.getElementById('editor').classList.remove('show');document.getElementById('scopeWrap').classList.remove('show');
 document.getElementById('alarmEditor').classList.remove('show');document.querySelector('input[name="scope"][value="month-year"]').checked=true;
 const line=document.getElementById('eventLine');if(matches.length){line.textContent=matches.map(x=>x.record.description).join('\n');line.style.display='block';}else{line.style.display='none';}
 updateDateCountdown();document.getElementById('overlay').classList.add('show');
}
function updateDateCountdown(){if(!selectedDate)return;const target=new Date(selectedDate.getFullYear(),selectedDate.getMonth(),selectedDate.getDate()),now=new Date(),nowDay=new Date(now.getFullYear(),now.getMonth(),now.getDate()),future=target>=nowDay,start=future?nowDay:target,end=future?target:nowDay,ms=Math.abs(target-now),label=document.getElementById('countLabel'),value=document.getElementById('dateCountdown');if(ms>0){label.textContent=future?'TIME REMAINING':'TIME SINCE THIS DATE';value.textContent=parts(ms)+(future?'':' ago');}else{label.textContent='TODAY';value.textContent='This date is today';}let y=end.getFullYear()-start.getFullYear(),anchor=new Date(start);anchor.setFullYear(start.getFullYear()+y);if(anchor>end){y--;anchor=new Date(start);anchor.setFullYear(start.getFullYear()+y);}let m=(end.getFullYear()-anchor.getFullYear())*12+end.getMonth()-anchor.getMonth();let monthAnchor=new Date(anchor);monthAnchor.setDate(1);monthAnchor.setMonth(monthAnchor.getMonth()+m);const lastDay=new Date(monthAnchor.getFullYear(),monthAnchor.getMonth()+1,0).getDate();monthAnchor.setDate(Math.min(anchor.getDate(),lastDay));if(monthAnchor>end){m--;monthAnchor=new Date(anchor);monthAnchor.setDate(1);monthAnchor.setMonth(monthAnchor.getMonth()+m);monthAnchor.setDate(Math.min(anchor.getDate(),new Date(monthAnchor.getFullYear(),monthAnchor.getMonth()+1,0).getDate()));}const d=Math.floor((end-monthAnchor)/86400000),sec=Math.floor(ms/1000);document.getElementById('yearsPart').textContent=y;document.getElementById('monthsPart').textContent=m;document.getElementById('daysPart').textContent=d;document.getElementById('totalDays').textContent=Math.floor(sec/86400).toLocaleString();document.getElementById('totalHours').textContent=Math.floor(sec/3600).toLocaleString();document.getElementById('totalMinutes').textContent=Math.floor(sec/60).toLocaleString();}

// Date History Explorer — keep every source record; do not deduplicate.
const historyState={data:null,kind:'all',visible:50};
const historyKinds=['events','births','deaths','holidays','selected'];
function historyDateKey(d){return p(d.getMonth()+1)+'/'+p(d.getDate());}
function historyCacheKey(d){return 'sedHistory_'+historyDateKey(d);}
function historyRecords(){
 if(!historyState.data)return [];
 const kinds=historyState.kind==='all'?historyKinds:[historyState.kind],out=[];
 kinds.forEach(kind=>(Array.isArray(historyState.data[kind])?historyState.data[kind]:[]).forEach((record,index)=>out.push({kind:kind,record:record,index:index})));
 return out;
}
function historyLabel(kind){return ({events:'Historical event',births:'Born on this day',deaths:'Died on this day',holidays:'Holiday / observance',selected:'Featured anniversary'})[kind]||kind;}
function renderHistory(){
 const list=document.getElementById('historyList'),summary=document.getElementById('historySummary');
 if(!historyState.data){list.innerHTML='<div class="emptyState">No historical records loaded.</div>';return;}
 const all=historyKinds.reduce((n,k)=>n+(Array.isArray(historyState.data[k])?historyState.data[k].length:0),0),records=historyRecords(),shown=records.slice(0,historyState.visible);
 summary.textContent=all.toLocaleString()+' records available · '+(historyState.kind==='all'?'All categories':historyLabel(historyState.kind))+' · Repeated entries are kept as supplied by the source.';
 list.innerHTML='';
 if(!records.length){list.innerHTML='<div class="emptyState">No records were returned for this category and date.</div>';return;}
 shown.forEach(item=>{
  const r=item.record||{},card=document.createElement('article');card.className='historyCard';
  const meta=document.createElement('div');meta.className='historyMeta';meta.textContent=historyLabel(item.kind)+(r.year?' · '+r.year:'');
  const pages=Array.isArray(r.pages)?r.pages:[],title=document.createElement('strong');title.textContent=pages[0]&&pages[0].title?pages[0].title.replace(/_/g,' '):'Historical record';
  const body=document.createElement('p');body.textContent=r.text||r.description||r.title||'No additional description is available in this record.';
  card.appendChild(meta);card.appendChild(title);card.appendChild(body);
  if(pages.length){const related=document.createElement('div');related.className='historyPages';related.textContent='Related pages: '+pages.map(x=>(x.title||'').replace(/_/g,' ')).filter(Boolean).join(' · ');card.appendChild(related);}
  list.appendChild(card);
 });
 if(records.length>shown.length){const more=document.createElement('button');more.className='historyMore';more.textContent='Show more ('+(records.length-shown.length).toLocaleString()+' remaining)';more.addEventListener('click',()=>{historyState.visible+=50;renderHistory();});list.appendChild(more);}
}
async function loadDateHistory(d,force){
 const list=document.getElementById('historyList'),summary=document.getElementById('historySummary'),key=historyCacheKey(d);
 historyState.kind='all';historyState.visible=50;historyState.data=null;
 document.querySelectorAll('.historyFilter').forEach(b=>b.classList.toggle('active',b.dataset.kind==='all'));
 document.getElementById('historyDate').textContent=d.toLocaleDateString('en-GB',{weekday:'long',day:'numeric',month:'long'});
 list.innerHTML='<div class="emptyState">Loading all available records…</div>';summary.textContent='Connecting to historical archive…';
 if(!force){try{const cached=localStorage.getItem(key);if(cached){const saved=JSON.parse(cached);if(saved&&saved.data){historyState.data=saved.data;renderHistory();summary.textContent+=' · Cached copy';}}}catch(e){}}
 if(!navigator.onLine){if(historyState.data){renderHistory();summary.textContent+=' · Offline copy';}else list.innerHTML='<div class="emptyState">Internet connection is unavailable. Connect to load this date’s history.</div>';return;}
 try{
  const response=await fetch('https://en.wikipedia.org/api/rest_v1/feed/onthisday/all/'+historyDateKey(d),{method:'GET',headers:{'Accept':'application/json'}});
  if(!response.ok)throw new Error('Archive returned HTTP '+response.status);
  const data=await response.json();historyState.data=data;
  try{localStorage.setItem(key,JSON.stringify({savedAt:Date.now(),data:data}));}catch(e){}
  renderHistory();
 }catch(error){
  if(historyState.data){renderHistory();summary.textContent+=' · Showing saved copy';}
  else{list.innerHTML='<div class="emptyState">Could not load historical records. '+String(error.message||error)+'<br><button class="secondary" id="historyRetry">Try again</button></div>';const retry=document.getElementById('historyRetry');if(retry)retry.addEventListener('click',()=>loadDateHistory(d,true));}
 }
}
document.getElementById('historyOpen').addEventListener('click',()=>{if(!selectedDate)return;document.getElementById('historyOverlay').classList.add('show');loadDateHistory(selectedDate,false);});
document.getElementById('historyClose').addEventListener('click',()=>document.getElementById('historyOverlay').classList.remove('show'));
document.getElementById('historyOverlay').addEventListener('click',e=>{if(e.target.id==='historyOverlay')document.getElementById('historyOverlay').classList.remove('show');});
document.getElementById('historyFilters').addEventListener('click',e=>{const b=e.target.closest('.historyFilter');if(!b)return;historyState.kind=b.dataset.kind;historyState.visible=50;document.querySelectorAll('.historyFilter').forEach(x=>x.classList.toggle('active',x===b));renderHistory();});

document.getElementById('add').addEventListener('click',()=>{document.getElementById('alarmEditor').classList.remove('show');document.getElementById('editor').classList.toggle('show');document.getElementById('scopeWrap').classList.remove('show');if(document.getElementById('editor').classList.contains('show'))document.getElementById('description').focus();});
document.getElementById('eventOk').addEventListener('click',()=>{if(!document.getElementById('description').value.trim()){document.getElementById('description').focus();return;}document.getElementById('scopeWrap').classList.add('show');});
document.getElementById('saveEvent').addEventListener('click',()=>{
 if(!selectedDate)return;const value=document.getElementById('description').value.trim();if(!value)return;
 const scope=document.querySelector('input[name="scope"]:checked').value;
 events[keyFor(selectedDate)]={description:value,scope:scope};
 localStorage.setItem('sedCalendarEvents',JSON.stringify(events));document.getElementById('editor').classList.remove('show');document.getElementById('scopeWrap').classList.remove('show');
 const line=document.getElementById('eventLine');line.textContent=eventsForDate(selectedDate).map(x=>x.record.description).join('\n');line.style.display='block';renderYear();
});
document.getElementById('deleteEvent').addEventListener('click',()=>{if(!selectedDate)return;delete events[keyFor(selectedDate)];localStorage.setItem('sedCalendarEvents',JSON.stringify(events));document.getElementById('description').value='';document.getElementById('eventLine').style.display='none';document.getElementById('editor').classList.remove('show');document.getElementById('scopeWrap').classList.remove('show');renderYear();});
document.getElementById('alarm').addEventListener('click',()=>{document.getElementById('editor').classList.remove('show');document.getElementById('alarmEditor').classList.toggle('show');document.getElementById('alarmStatus').textContent='';});
document.getElementById('alarmTime').addEventListener('input',e=>{document.getElementById('digitalTime').textContent=e.target.value||'--:--';});
document.getElementById('saveAlarm').addEventListener('click',()=>{
 if(!selectedDate)return;const t=document.getElementById('alarmTime').value;if(!t)return;
 const a=t.split(':').map(Number),matches=eventsForDate(selectedDate),desc=matches.map(x=>x.record.description).join(' / ');
 const result=window.AndroidAlarm?window.AndroidAlarm.scheduleAlarm(selectedDate.getFullYear(),selectedDate.getMonth()+1,selectedDate.getDate(),a[0],a[1],desc):'error';
 const status=document.getElementById('alarmStatus');
 status.textContent=result==='ok'?'Alarm set · '+t+' · default device alarm sound':result==='past'?'Choose a future time.':result==='permission'?'Allow notification permission, then set the alarm again.':result==='exact_permission'?'Allow Alarms & reminders access, return here and set it again.':'Could not set alarm.';
});
document.getElementById('cancelAlarm').addEventListener('click',()=>document.getElementById('alarmEditor').classList.remove('show'));
function closeModal(){document.getElementById('overlay').classList.remove('show');document.getElementById('historyOverlay').classList.remove('show');selectedDate=null;}
document.getElementById('close').addEventListener('click',closeModal);document.getElementById('overlay').addEventListener('click',e=>{if(e.target.id==='overlay')closeModal();});
function showHub(page){document.getElementById('hub').classList.add('show');showPage(page||'homePage');}
function hideHub(){document.getElementById('hub').classList.remove('show');}
function showPage(id){document.querySelectorAll('.page').forEach(p=>p.classList.remove('active'));document.getElementById(id).classList.add('active');document.getElementById('hubTitle').textContent=({homePage:'Calendar tools',eventsPage:'Events',notesPage:'Notebook',settingsPage:'Settings',aboutPage:'About',calculatorPage:'Date calculator'})[id]||'Calendar tools';if(id==='eventsPage')renderEvents();if(id==='notesPage')renderNotes();if(id==='calculatorPage')initDateCalculator();}
document.querySelectorAll('.tile').forEach(b=>b.addEventListener('click',()=>showPage(b.dataset.page)));
document.getElementById('hubBack').addEventListener('click',()=>{if(!document.getElementById('homePage').classList.contains('active'))showPage('homePage');else hideHub();});
function renderEvents(){
 const list=document.getElementById('eventsList');list.innerHTML='';const keys=Object.keys(events).sort();
 if(!keys.length){list.innerHTML='<div class="emptyState">No saved events yet.</div>';return;}
 keys.forEach(k=>{const rec=normalizeEvent(events[k]);if(!rec||!rec.description)return;const a=k.split('-').map(Number),d=new Date(a[0],a[1]-1,a[2]),el=document.createElement('button');el.className='item';el.style.width='100%';el.style.textAlign='left';el.innerHTML='<strong></strong><small></small><p></p>';el.querySelector('strong').textContent=rec.description;el.querySelector('small').textContent=d.toLocaleDateString('en-GB',{day:'numeric',month:'long',year:'numeric'})+' · '+scopeName(rec.scope);el.querySelector('p').textContent='Tap to open date';el.addEventListener('click',()=>{hideHub();openDate(d);});list.appendChild(el);});
}
function initDateCalculator(){const a=document.getElementById('calcStart'),b=document.getElementById('calcEnd');if(!a.value||!b.value){const t=new Date(),v=t.getFullYear()+'-'+p(t.getMonth()+1)+'-'+p(t.getDate());if(!a.value)a.value=v;if(!b.value)b.value=v;}}
function parseLocalDate(value){if(!value)return null;const a=value.split('-').map(Number);return new Date(a[0],a[1]-1,a[2]);}
function calendarDifference(a,b){let start=a<=b?new Date(a):new Date(b),end=a<=b?new Date(b):new Date(a);let years=end.getFullYear()-start.getFullYear(),anchor=new Date(start);anchor.setFullYear(start.getFullYear()+years);if(anchor>end){years--;anchor=new Date(start);anchor.setFullYear(start.getFullYear()+years);}let months=(end.getFullYear()-anchor.getFullYear())*12+end.getMonth()-anchor.getMonth(),monthAnchor=new Date(anchor);monthAnchor.setDate(1);monthAnchor.setMonth(monthAnchor.getMonth()+months);monthAnchor.setDate(Math.min(anchor.getDate(),new Date(monthAnchor.getFullYear(),monthAnchor.getMonth()+1,0).getDate()));if(monthAnchor>end){months--;monthAnchor=new Date(anchor);monthAnchor.setDate(1);monthAnchor.setMonth(monthAnchor.getMonth()+months);monthAnchor.setDate(Math.min(anchor.getDate(),new Date(monthAnchor.getFullYear(),monthAnchor.getMonth()+1,0).getDate()));}return {years,months,days:Math.round((end-monthAnchor)/86400000),forward:a<=b};}
document.getElementById('calculateDates').addEventListener('click',()=>{const a=parseLocalDate(document.getElementById('calcStart').value),b=parseLocalDate(document.getElementById('calcEnd').value),out=document.getElementById('calcResult');if(!a||!b){out.innerHTML='<strong>Choose both dates</strong><p>Please select a start date and an end date.</p>';return;}const d=calendarDifference(a,b),from=a.toLocaleDateString('en-GB',{day:'numeric',month:'short',year:'numeric'}),to=b.toLocaleDateString('en-GB',{day:'numeric',month:'short',year:'numeric'});out.innerHTML='<strong>'+(d.forward?'Time elapsed / remaining':'Date difference')+'</strong><p>'+from+' → '+to+'</p><div class="durationBreakdown"><div class="durationStat"><strong>'+d.years+'</strong><span>Years</span></div><div class="durationStat"><strong>'+d.months+'</strong><span>Months</span></div><div class="durationStat"><strong>'+d.days+'</strong><span>Days</span></div></div>';});
function scopeName(s){return ({'month-year':'This month in this year','all-months-year':'Every month in this year','month-all-years':'This month in every year','all-months-all-years':'Every month in every year'})[s]||'One date';}
function persistNotes(){localStorage.setItem('sedCalendarNotes',JSON.stringify(notes));}
function renderNotes(){
 const list=document.getElementById('notesList');list.innerHTML='';
 if(!notes.length){list.innerHTML='<div class="emptyState">Your notebook is empty. Tap the pen to write a note.</div>';return;}
 notes.slice().sort((a,b)=>b.updated-a.updated).forEach(n=>{const el=document.createElement('button');el.className='item';el.style.width='100%';el.style.textAlign='left';el.innerHTML='<strong></strong><small></small><p></p>';el.querySelector('strong').textContent=n.title||'Untitled note';el.querySelector('small').textContent=new Date(n.updated).toLocaleString();el.querySelector('p').textContent=n.body;el.addEventListener('click',()=>editNote(n.id));list.appendChild(el);});
}
function editNote(id){const n=notes.find(x=>x.id===id);if(!n)return;editingNoteId=id;document.getElementById('noteTitle').value=n.title;document.getElementById('noteBody').value=n.body;document.getElementById('noteEditor').classList.add('show');}
document.getElementById('newNote').addEventListener('click',()=>{editingNoteId=null;document.getElementById('noteTitle').value='';document.getElementById('noteBody').value='';document.getElementById('noteEditor').classList.add('show');document.getElementById('noteTitle').focus();});
document.getElementById('cancelNote').addEventListener('click',()=>document.getElementById('noteEditor').classList.remove('show'));
document.getElementById('saveNote').addEventListener('click',()=>{
 const title=document.getElementById('noteTitle').value.trim(),body=document.getElementById('noteBody').value.trim();if(!title&&!body)return;
 if(editingNoteId){const n=notes.find(x=>x.id===editingNoteId);if(n){n.title=title;n.body=body;n.updated=Date.now();}}
 else notes.push({id:String(Date.now()),title:title,body:body,updated:Date.now()});
 persistNotes();document.getElementById('noteEditor').classList.remove('show');renderNotes();
});
const fmt=document.getElementById('format24');fmt.checked=localStorage.getItem('sedCalendar24')==='true';fmt.addEventListener('change',()=>{localStorage.setItem('sedCalendar24',String(fmt.checked));updateHeader();});
const glass=document.getElementById('glassLevel');glass.value=localStorage.getItem('sedGlassLevel')||'30';glass.addEventListener('input',()=>{localStorage.setItem('sedGlassLevel',glass.value);document.querySelector('.sheet').style.backdropFilter='blur('+glass.value+'px) saturate(165%)';});
// Interactive, finger-tracked page transitions. The dashboard is a real draggable sheet.
const sheet=document.getElementById('sheet'),hub=document.getElementById('hub');
let gesture=null,lastY=0,lastT=0,velocityY=0,hubProgress=0;
const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
function paintHub(progress){
 hubProgress=clamp(progress,0,1);
 hub.classList.add('dragging');sheet.classList.add('dragging');
 hub.style.transform='translate3d(0,'+((1-hubProgress)*100)+'%,0)';
}
function settleHub(open){
 hubProgress=open?1:0;
 hub.classList.remove('dragging');sheet.classList.remove('dragging');
 hub.style.transform='';
 document.getElementById('calendarLayer').style.filter=open?'blur(18px) saturate(1.08)':'';
 if(open){hub.classList.add('show');}
 else{hub.classList.remove('show');}
}
function startDrag(e,source){
 if(document.getElementById('overlay').classList.contains('show'))return;
 const t=e.touches[0];
 if(source==='hub'){
  const page=hub.querySelector('.page.active');
  if(page&&page.scrollTop>0)return;
  if(!hub.classList.contains('show'))return;
 }else if(hub.classList.contains('show'))return;
 gesture={source,startX:t.clientX,startY:t.clientY,mode:null};
 lastY=t.clientY;lastT=performance.now();velocityY=0;
}
function moveDrag(e,source){
 if(!gesture||gesture.source!==source)return;
 const t=e.touches[0],dx=t.clientX-gesture.startX,dy=t.clientY-gesture.startY,now=performance.now();
 velocityY=(t.clientY-lastY)/Math.max(1,now-lastT);lastY=t.clientY;lastT=now;
 if(!gesture.mode){
  if(Math.max(Math.abs(dx),Math.abs(dy))<5)return;
  if(Math.abs(dx)>Math.abs(dy)*1.12)gesture.mode='horizontal';
  else if(Math.abs(dy)>Math.abs(dx)*1.12)gesture.mode=source==='sheet'?(dy<0?'open':'none'):(dy>0?'close':'none');
  else return;
 }
 if(gesture.mode==='open'){
  paintHub(-dy/Math.max(1,hub.clientHeight||sheet.clientHeight));
  e.preventDefault();
 }else if(gesture.mode==='close'){
  const page=hub.querySelector('.page.active');
  if(page&&page.scrollTop>0){gesture.mode='none';return;}
  paintHub(1-dy/Math.max(1,hub.clientHeight||sheet.clientHeight));
  e.preventDefault();
 }else if(gesture.mode==='horizontal'&&source==='sheet'){
  const width=Math.max(1,sheet.clientWidth),distance=clamp(dx,-width,width);
  grid.style.transition='none';
  grid.style.transform='translate3d('+distance+'px,0,0)';
  grid.style.opacity=String(1-Math.min(.22,Math.abs(distance)/width*.22));
  e.preventDefault();
 }
}
function finishDrag(source){
 if(!gesture||gesture.source!==source)return;
 const mode=gesture.mode,v=velocityY,transform=hub.style.transform;
 gesture=null;
 if(mode==='open'){
  const m=transform.match(/,([-\d.]+)%/),remaining=m?Number(m[1]):100,progress=1-remaining/100;
  settleHub(progress>=.10||v<-.28);
 }else if(mode==='close'){
  const m=transform.match(/,([-\d.]+)%/),remaining=m?Number(m[1]):0,progress=1-remaining/100;
  settleHub(!(progress<.90||v>.28));
 }else if(mode==='horizontal'&&source==='sheet'){
  const m=grid.style.transform.match(/translate3d\(([-\d.]+)px/),distance=m?Number(m[1]):0;
  grid.style.transition='transform .30s cubic-bezier(.22,.75,.18,1),opacity .30s ease';
  grid.style.transform='';grid.style.opacity='';
  if(Math.abs(distance)>Math.min(76,sheet.clientWidth*.18))changeYear(distance<0?1:-1);
  setTimeout(()=>{grid.style.transition='';},320);
 }
}
sheet.addEventListener('touchstart',e=>startDrag(e,'sheet'),{passive:true});
sheet.addEventListener('touchmove',e=>moveDrag(e,'sheet'),{passive:false});
sheet.addEventListener('touchend',()=>finishDrag('sheet'),{passive:true});
sheet.addEventListener('touchcancel',()=>finishDrag('sheet'),{passive:true});
hub.addEventListener('touchstart',e=>startDrag(e,'hub'),{passive:true});
hub.addEventListener('touchmove',e=>moveDrag(e,'hub'),{passive:false});
hub.addEventListener('touchend',()=>finishDrag('hub'),{passive:true});
hub.addEventListener('touchcancel',()=>finishDrag('hub'),{passive:true});
showHub=function(page){showPage(page||'homePage');settleHub(true);};
hideHub=function(){settleHub(false);};
document.addEventListener('keydown',e=>{if(e.key==='ArrowLeft')changeYear(-1);if(e.key==='ArrowRight')changeYear(1);if(e.key==='Escape'){closeModal();hideHub();}});
renderYear();setInterval(()=>{updateHeader();updateDateCountdown();},1000);
</script></body></html>
    """.trimIndent()
}