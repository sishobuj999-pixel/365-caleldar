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
.yearnav{display:flex;align-items:center;gap:1.2cqw}
.top h1{font-size:17cqw;font-weight:800;letter-spacing:-.06em;line-height:.8}
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
.glass{background:rgba(24,39,63,.38);border:1px solid rgba(255,255,255,.30);box-shadow:0 18px 55px rgba(0,0,0,.32),inset 0 1px 0 rgba(255,255,255,.12);-webkit-backdrop-filter:blur(30px) saturate(175%);backdrop-filter:blur(30px) saturate(175%)}
.overlay{position:absolute;inset:0;background:rgba(3,9,20,.22);-webkit-backdrop-filter:blur(14px) saturate(130%);backdrop-filter:blur(14px) saturate(130%);display:flex;align-items:center;justify-content:center;padding:6cqw;z-index:5;opacity:0;visibility:hidden;transition:opacity .24s ease,visibility .24s ease}
.overlay.show{opacity:1;visibility:visible}
.modal{width:100%;max-width:440px;border-radius:5cqw;padding:5cqw;transform:translateY(14px) scale(.96);transition:transform .28s cubic-bezier(.2,.8,.2,1);max-height:88%;overflow:auto}
.overlay.show .modal{transform:translateY(0) scale(1)}
.modalhead{display:flex;justify-content:space-between;align-items:center;margin-bottom:3cqw}
.modal h2{font-size:5cqw;font-weight:800}
.close{border:1px solid var(--edge);background:rgba(255,255,255,.12);color:white;border-radius:50%;width:8cqw;height:8cqw;font-size:5cqw}
.datebig{font-size:3.2cqw;color:rgba(255,255,255,.76);margin-bottom:3cqw}
.countlabel{font-size:2.3cqw;color:#a9c9ff;text-transform:uppercase;letter-spacing:.1em;margin-bottom:1.2cqw}
.countdown{font-size:3.3cqw;font-weight:800;line-height:1.5;font-variant-numeric:tabular-nums}
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
<div id="calendarLayer"><div class="top"><div class="yearnav"><button class="nav" id="prev" aria-label="Previous year">‹</button><h1 id="year">2026</h1><button class="nav" id="next" aria-label="Next year">›</button></div><div class="live"><div id="clk">--:--:--</div><div id="dt"></div><div id="left"></div></div></div>
<div class="wk"><span>SAT</span><span>SUN</span><span>MON</span><span>TUE</span><span>WED</span><span>THU</span><span>FRI</span><span>SAT</span><span>SUN</span><span>MON</span><span>TUE</span><span>WED</span><span>THU</span><span>FRI</span></div>
<div class="g" id="grid"></div><div class="hint">SWIPE LEFT / RIGHT · SWIPE UP FOR MORE</div></div>
<div class="overlay" id="overlay"><section class="modal glass">
<div class="modalhead"><h2 id="modalTitle">Date</h2><button class="close" id="close">×</button></div>
<div class="datebig" id="dateBig"></div><div class="countlabel" id="countLabel">Time remaining</div><div class="countdown" id="dateCountdown"></div>
<div class="eventline" id="eventLine" style="display:none"></div>
<div class="actions"><button class="round" id="add" aria-label="Add event">＋</button><button class="round" id="alarm" aria-label="Set alarm">◷</button></div>
<div class="editor" id="editor"><label for="description">Event description</label><textarea id="description" placeholder="Write your event or reminder..."></textarea><button class="primary" id="eventOk">OK</button>
<div class="scopeWrap" id="scopeWrap"><label>Repeat this event (choose one)</label>
<label class="scope"><input type="radio" name="scope" value="month-year" checked> This month in this year</label>
<label class="scope"><input type="radio" name="scope" value="all-months-year"> Every month in this year</label>
<label class="scope"><input type="radio" name="scope" value="month-all-years"> This month in every year</label>
<label class="scope"><input type="radio" name="scope" value="all-months-all-years"> Every month in every year</label>
<button class="primary" id="saveEvent">Save event</button><button class="danger" id="deleteEvent">Remove event</button></div></div>
<div class="alarmEditor" id="alarmEditor"><label for="alarmTime">Choose alarm time</label><div class="digital" id="digitalTime">09:00</div><input id="alarmTime" type="time" value="09:00"><button class="primary" id="saveAlarm">Set device alarm</button><button class="secondary" id="cancelAlarm">Cancel</button><div id="alarmStatus" class="datebig"></div></div>
</section></div>
<section id="hub" class="glass">
<div class="hubhead"><h2 id="hubTitle">Calendar tools</h2><button class="close" id="hubBack">×</button></div>
<div id="homePage" class="page active"><div class="hubgrid">
<button class="tile" data-page="eventsPage"><span>▦</span>Events</button>
<button class="tile" data-page="notesPage"><span>▤</span>Notes</button>
<button class="tile" data-page="settingsPage"><span>⚙</span>Settings</button>
<button class="tile" data-page="aboutPage"><span>ⓘ</span>About</button>
</div><p class="emptyState">Swipe down to return to calendar</p></div>
<div id="eventsPage" class="page"><div class="pageTitle">All events</div><div id="eventsList"></div></div>
<div id="notesPage" class="page"><div class="pageTitle">Notebook</div><div id="noteEditor" class="noteEditor glass"><input id="noteTitle" placeholder="Note title"><textarea id="noteBody" placeholder="Write your note..."></textarea><button class="primary" id="saveNote">Save note</button><button class="secondary" id="cancelNote">Cancel</button></div><div id="notesList"></div><button class="pen" id="newNote" aria-label="New note">✎</button></div>
<div id="settingsPage" class="page"><div class="pageTitle">Settings</div><div class="settingrow"><span>24-hour clock</span><input type="checkbox" id="format24"></div><div class="settingrow"><span>Glass intensity</span><input type="range" id="glassLevel" min="10" max="42" value="30"></div><p class="emptyState">Settings are saved on this device.</p></div>
<div id="aboutPage" class="page"><div class="pageTitle">SED Calendar</div><div class="item"><strong>SED Calendar 2026–2040</strong><p>Calendar, recurring events, alarms and private notebook.</p></div></div>
</section></main>
<script>
const grid=document.getElementById('grid'),yearEl=document.getElementById('year');
const codes=['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];
const months=['January','February','March','April','May','June','July','August','September','October','November','December'];
const p=n=>String(n).padStart(2,'0');
let shownYear=2026,selectedDate=null,events={},notes=[],editingNoteId=null;
try{events=JSON.parse(localStorage.getItem('sedCalendarEvents')||'{}')||{};}catch(e){events={};}
try{notes=JSON.parse(localStorage.getItem('sedCalendarNotes')||'[]')||[];}catch(e){notes=[];}
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
 yearEl.textContent=shownYear;document.getElementById('prev').disabled=shownYear<=2026;document.getElementById('next').disabled=shownYear>=2040;
 grid.classList.remove('year-next','year-prev');void grid.offsetWidth;grid.classList.add(direction==='next'?'year-next':'year-prev');grid.innerHTML='';
 const first=new Date(shownYear,0,1),offset=(first.getDay()+1)%7,total=(new Date(shownYear+1,0,1)-first)/86400000;
 for(let i=0;i<offset;i++){const e=document.createElement('div');e.className='empty';grid.appendChild(e);}
 const today=new Date(),todayKey=keyFor(today);
 for(let i=0;i<total;i++){
  const d=new Date(shownYear,0,1+i),k=keyFor(d),cell=document.createElement('div');
  cell.className='day'+(d.getDate()===1?' month':'');
  if(k===todayKey)cell.classList.add('today');
  const matching=eventsForDate(d);if(matching.length)cell.classList.add('event');
  cell.dataset.date=k;cell.innerHTML='<b>'+(i+1)+'</b><i>'+codes[d.getMonth()]+' '+d.getDate()+'</i>';
  if(matching.length)cell.title=matching.map(x=>x.record.description).join(' · ');
  grid.appendChild(cell);
 }
 while(grid.children.length<378){const e=document.createElement('div');e.className='empty';grid.appendChild(e);}
 updateHeader();
}
function updateHeader(){
 const n=new Date(),clock=document.getElementById('clk'),use24=localStorage.getItem('sedCalendar24')==='true';
 clock.textContent=use24?p(n.getHours())+':'+p(n.getMinutes())+':'+p(n.getSeconds()):n.toLocaleTimeString([], {hour:'2-digit',minute:'2-digit',second:'2-digit'});
 document.getElementById('dt').textContent=n.getDate()+' '+months[n.getMonth()].slice(0,3)+' '+n.getFullYear();
 const left=document.getElementById('left');
 if(shownYear===2026){const ms=new Date(2027,0,1)-n;left.textContent=ms>0?parts(ms)+' left':'2026 has ended';}
 else if(shownYear>n.getFullYear()){left.textContent='COUNTDOWN · '+parts(new Date(shownYear,0,1)-n);}
 else left.textContent='PAST YEAR';
}
function changeYear(delta){const next=shownYear+delta;if(next<2026||next>2040)return;shownYear=next;renderYear(delta>0?'next':'prev');}
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
function updateDateCountdown(){if(!selectedDate)return;const ms=new Date(selectedDate.getFullYear(),selectedDate.getMonth(),selectedDate.getDate())-new Date(),label=document.getElementById('countLabel'),value=document.getElementById('dateCountdown');if(ms>0){label.textContent='TIME REMAINING';value.textContent=parts(ms);}else if(ms<0){label.textContent='TIME SINCE THIS DATE';value.textContent=parts(ms)+' ago';}else{label.textContent='TODAY';value.textContent='This date is today';}}
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
function closeModal(){document.getElementById('overlay').classList.remove('show');selectedDate=null;}
document.getElementById('close').addEventListener('click',closeModal);document.getElementById('overlay').addEventListener('click',e=>{if(e.target.id==='overlay')closeModal();});
function showHub(page){document.getElementById('hub').classList.add('show');showPage(page||'homePage');}
function hideHub(){document.getElementById('hub').classList.remove('show');}
function showPage(id){document.querySelectorAll('.page').forEach(p=>p.classList.remove('active'));document.getElementById(id).classList.add('active');document.getElementById('hubTitle').textContent=({homePage:'Calendar tools',eventsPage:'Events',notesPage:'Notebook',settingsPage:'Settings',aboutPage:'About'})[id]||'Calendar tools';if(id==='eventsPage')renderEvents();if(id==='notesPage')renderNotes();}
document.querySelectorAll('.tile').forEach(b=>b.addEventListener('click',()=>showPage(b.dataset.page)));
document.getElementById('hubBack').addEventListener('click',()=>{if(!document.getElementById('homePage').classList.contains('active'))showPage('homePage');else hideHub();});
function renderEvents(){
 const list=document.getElementById('eventsList');list.innerHTML='';const keys=Object.keys(events).sort();
 if(!keys.length){list.innerHTML='<div class="emptyState">No saved events yet.</div>';return;}
 keys.forEach(k=>{const rec=normalizeEvent(events[k]);if(!rec||!rec.description)return;const a=k.split('-').map(Number),d=new Date(a[0],a[1]-1,a[2]),el=document.createElement('button');el.className='item';el.style.width='100%';el.style.textAlign='left';el.innerHTML='<strong></strong><small></small><p></p>';el.querySelector('strong').textContent=rec.description;el.querySelector('small').textContent=d.toLocaleDateString('en-GB',{day:'numeric',month:'long',year:'numeric'})+' · '+scopeName(rec.scope);el.querySelector('p').textContent='Tap to open date';el.addEventListener('click',()=>{hideHub();openDate(d);});list.appendChild(el);});
}
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
 document.getElementById('calendarLayer').style.filter='blur('+(hubProgress*26).toFixed(2)+'px) saturate('+(1+hubProgress*.14)+')';
}
function settleHub(open){
 hubProgress=open?1:0;
 hub.classList.remove('dragging');sheet.classList.remove('dragging');
 hub.style.transform='';
 document.getElementById('calendarLayer').style.filter=open?'blur(26px) saturate(1.14)':'';
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