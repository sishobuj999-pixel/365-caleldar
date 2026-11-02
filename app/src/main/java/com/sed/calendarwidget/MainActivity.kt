package com.sed.calendarwidget

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = 42 }
        }
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        val web = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            overScrollMode = View.OVER_SCROLL_NEVER
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            loadDataWithBaseURL("https://sed.calendar.local/", calendarHtml(), "text/html", "UTF-8", null)
        }
        setContentView(web)
    }

    private fun calendarHtml(): String = """
<!DOCTYPE html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>365 Calendar</title><meta name="theme-color" content="#172033">
<style>
:root{--ink:#fff;--line:rgba(255,255,255,.19)}
*{box-sizing:border-box;margin:0}
html,body{min-height:100%;width:100%;background:transparent;overflow:hidden}
body{display:flex;justify-content:center;align-items:center;font-family:"Helvetica Neue",Helvetica,Arial,sans-serif;color:var(--ink);padding:env(safe-area-inset-top) 0 env(safe-area-inset-bottom)}
.sheet{container-type:inline-size;position:relative;width:min(100vw,calc(100vh*9/16));height:min(177.7778vw,100vh);aspect-ratio:9/16;background:rgba(20,30,49,.38);-webkit-backdrop-filter:blur(28px);backdrop-filter:blur(28px);border:1px solid rgba(255,255,255,.24);border-radius:5cqw;padding:5cqw 3cqw 3cqw;display:flex;flex-direction:column;box-shadow:0 12px 42px rgba(0,0,0,.18);touch-action:pan-y}
.top{display:flex;justify-content:space-between;align-items:flex-end;padding:0 .4cqw 2.4cqw;min-height:19cqw}
.yearnav{display:flex;align-items:center;gap:1.2cqw}
.top h1{font-size:17cqw;font-weight:800;letter-spacing:-.06em;line-height:.8}
.nav{border:1px solid rgba(255,255,255,.25);background:rgba(255,255,255,.1);color:white;border-radius:50%;width:7cqw;height:7cqw;font-size:4cqw;display:flex;align-items:center;justify-content:center}
.nav:disabled{opacity:.15}
.live{text-align:right;font-variant-numeric:tabular-nums;max-width:57%;min-width:0}
#clk{font-size:5.2cqw;font-weight:800;letter-spacing:-.02em;line-height:1}
#dt,#left{font-size:1.9cqw;font-weight:600;margin-top:.9cqw;line-height:1.35}
#left{color:rgba(255,255,255,.88)}
.wk,.g{display:grid;grid-template-columns:repeat(14,1fr)}
.wk span{font-size:1.5cqw;font-weight:700;text-align:center;padding:1.2cqw 0;border-top:.5cqw solid rgba(255,255,255,.85);border-bottom:1px solid var(--line)}
.g{flex:1;min-height:0;grid-template-rows:repeat(27,minmax(0,1fr));border-left:1px solid var(--line)}
.g .day{border-right:1px solid var(--line);border-bottom:1px solid var(--line);display:flex;flex-direction:column;justify-content:center;align-items:center;gap:.25cqw;background:rgba(255,255,255,.035);min-width:0;overflow:hidden}
.g .empty{border-right:1px solid var(--line);border-bottom:1px solid var(--line);background:rgba(255,255,255,.018)}
.g b{font-size:2.7cqw;font-weight:800;letter-spacing:-.03em;line-height:1}
.g i{font-style:normal;font-size:1.3cqw;font-weight:500;color:rgba(255,255,255,.72);line-height:1;white-space:nowrap}
.g .month{background:rgba(0,0,0,.43)}.g .month b{color:#fff}.g .month i{color:#ddd}
.g .today{background:rgba(22,163,74,.82)!important}.g .today b,.g .today i{color:#fff}
.g .event{background:rgba(220,38,38,.88)!important}.g .event b,.g .event i{color:#fff}
.g .event.today{background:rgba(190,35,48,.96)!important}
.hint{text-align:center;font-size:1.6cqw;color:rgba(255,255,255,.55);padding-top:1.1cqw;letter-spacing:.08em}
.overlay{position:absolute;inset:0;background:rgba(4,10,20,.58);backdrop-filter:blur(8px);-webkit-backdrop-filter:blur(8px);display:none;align-items:center;justify-content:center;padding:6cqw;z-index:5}
.overlay.show{display:flex}
.modal{width:100%;max-width:420px;border:1px solid rgba(255,255,255,.28);border-radius:5cqw;padding:5cqw;background:rgba(27,40,62,.9);box-shadow:0 18px 55px rgba(0,0,0,.35)}
.modalhead{display:flex;justify-content:space-between;align-items:center;margin-bottom:3cqw}
.modal h2{font-size:5cqw;font-weight:800}
.close{border:0;background:rgba(255,255,255,.12);color:white;border-radius:50%;width:8cqw;height:8cqw;font-size:5cqw}
.datebig{font-size:3.2cqw;color:rgba(255,255,255,.72);margin-bottom:3cqw}
.countlabel{font-size:2.3cqw;color:#a9c9ff;text-transform:uppercase;letter-spacing:.1em;margin-bottom:1.2cqw}
.countdown{font-size:3.3cqw;font-weight:800;line-height:1.5;font-variant-numeric:tabular-nums}
.eventline{font-size:2.8cqw;margin-top:3cqw;padding:2.5cqw;border-radius:2cqw;background:rgba(255,255,255,.08);white-space:pre-wrap;overflow-wrap:anywhere}
.actions{display:flex;justify-content:flex-end;gap:2cqw;margin-top:3cqw}
.add{border:1px solid rgba(255,255,255,.25);background:rgba(255,255,255,.12);color:white;border-radius:50%;width:10cqw;height:10cqw;font-size:6cqw}
.editor{display:none;margin-top:3cqw}
.editor.show{display:block}
.editor label{display:block;font-size:2.5cqw;margin-bottom:1.5cqw;color:rgba(255,255,255,.8)}
.editor textarea{width:100%;min-height:24cqw;resize:vertical;border:1px solid rgba(255,255,255,.25);border-radius:2cqw;background:rgba(0,0,0,.18);color:white;padding:2.5cqw;font:inherit;font-size:2.8cqw;outline:none}
.editor textarea::placeholder{color:rgba(255,255,255,.45)}
.save{margin-top:2cqw;border:0;border-radius:2cqw;padding:2.5cqw 5cqw;background:#3b82f6;color:white;font-weight:700;font-size:2.7cqw}
.delete{margin-top:2cqw;margin-left:2cqw;border:1px solid rgba(255,255,255,.2);border-radius:2cqw;padding:2.5cqw 4cqw;background:transparent;color:#ffb4b4;font-size:2.7cqw}
@media(prefers-reduced-transparency:reduce){.sheet,.modal{background:rgba(20,30,49,.95)}}
</style></head><body><main class="sheet" id="sheet">
<div class="top"><div class="yearnav"><button class="nav" id="prev" aria-label="Previous year">‹</button><h1 id="year">2026</h1><button class="nav" id="next" aria-label="Next year">›</button></div><div class="live"><div id="clk">--:--:--</div><div id="dt"></div><div id="left"></div></div></div>
<div class="wk"><span>SAT</span><span>SUN</span><span>MON</span><span>TUE</span><span>WED</span><span>THU</span><span>FRI</span><span>SAT</span><span>SUN</span><span>MON</span><span>TUE</span><span>WED</span><span>THU</span><span>FRI</span></div>
<div class="g" id="grid"></div><div class="hint">SWIPE TO CHANGE YEAR</div>
<div class="overlay" id="overlay"><section class="modal">
<div class="modalhead"><h2 id="modalTitle">Date</h2><button class="close" id="close">×</button></div>
<div class="datebig" id="dateBig"></div><div class="countlabel" id="countLabel">Time remaining</div><div class="countdown" id="dateCountdown"></div>
<div class="eventline" id="eventLine" style="display:none"></div>
<div class="actions"><button class="add" id="add" aria-label="Add event">+</button></div>
<div class="editor" id="editor"><label for="description">What is happening on this date?</label><textarea id="description" placeholder="Write your event or reminder..."></textarea><div><button class="save" id="save">Save event</button><button class="delete" id="delete">Remove event</button></div></div>
</section></div></main>
<script>
const grid=document.getElementById('grid'),yearEl=document.getElementById('year');
const codes=['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];
const months=['January','February','March','April','May','June','July','August','September','October','November','December'];
const p=n=>String(n).padStart(2,'0');
let shownYear=2026,selectedDate=null,marked=-1;
let events={};try{events=JSON.parse(localStorage.getItem('sedCalendarEvents')||'{}')||{};}catch(e){events={};}
function keyFor(d){return d.getFullYear()+'-'+p(d.getMonth()+1)+'-'+p(d.getDate());}
function parts(ms){
 const sec=Math.max(0,Math.floor(Math.abs(ms)/1000)),days=Math.floor(sec/86400),hours=Math.floor(sec%86400/3600),mins=Math.floor(sec%3600/60),secs=sec%60;
 return days+' days  '+p(hours)+' hours  '+p(mins)+' minutes  '+p(secs)+' seconds';
}
function renderYear(){
 yearEl.textContent=shownYear;
 document.getElementById('prev').disabled=shownYear<=2026;
 document.getElementById('next').disabled=shownYear>=2040;
 grid.innerHTML='';
 const first=new Date(shownYear,0,1),offset=(first.getDay()+1)%7,total=(new Date(shownYear+1,0,1)-first)/86400000;
 for(let i=0;i<offset;i++){const e=document.createElement('div');e.className='empty';grid.appendChild(e);}
 const today=new Date(),todayKey=keyFor(today);
 for(let i=0;i<total;i++){
  const d=new Date(shownYear,0,1+i),k=keyFor(d),cell=document.createElement('div');
  cell.className='day'+(d.getDate()===1?' month':'');
  if(k===todayKey)cell.classList.add('today');
  if(events[k]&&String(events[k]).trim())cell.classList.add('event');
  cell.dataset.date=k;
  const title=events[k]?String(events[k]).replace(/</g,'&lt;').replace(/>/g,'&gt;'):'';
  cell.innerHTML='<b>'+(i+1)+'</b><i>'+codes[d.getMonth()]+' '+d.getDate()+'</i>';
  if(title)cell.title=title;
  grid.appendChild(cell);
 }
 while(grid.children.length<378){const e=document.createElement('div');e.className='empty';grid.appendChild(e);}
 updateHeader();
}
function updateHeader(){
 const n=new Date();
 document.getElementById('clk').textContent=p(n.getHours())+':'+p(n.getMinutes())+':'+p(n.getSeconds());
 document.getElementById('dt').textContent=n.getDate()+' '+months[n.getMonth()].slice(0,3)+' '+n.getFullYear();
 const left=document.getElementById('left');
 if(shownYear===2026){
  const target=new Date(2027,0,1),ms=target-n;
  left.textContent=ms>0?parts(ms)+' left':'2026 has ended';
 }else if(shownYear>n.getFullYear()){
  const ms=new Date(shownYear,0,1)-n;
  left.textContent='WILL COME AFTER · '+parts(ms);
 }else{
  left.textContent='PAST YEAR';
 }
}
function changeYear(delta){
 const next=shownYear+delta;
 if(next<2026||next>2040)return;
 shownYear=next;renderYear();
}
document.getElementById('prev').addEventListener('click',()=>changeYear(-1));
document.getElementById('next').addEventListener('click',()=>changeYear(1));
grid.addEventListener('click',e=>{
 const cell=e.target.closest('.day');if(!cell)return;
 const a=cell.dataset.date.split('-').map(Number);
 openDate(new Date(a[0],a[1]-1,a[2]));
});
function openDate(d){
 selectedDate=d;
 const k=keyFor(d);
 document.getElementById('modalTitle').textContent=d.getDate()+' '+months[d.getMonth()]+' '+d.getFullYear();
 document.getElementById('dateBig').textContent=d.toLocaleDateString('en-GB',{weekday:'long',day:'numeric',month:'long',year:'numeric'});
 document.getElementById('description').value=events[k]||'';
 document.getElementById('editor').classList.remove('show');
 const eventLine=document.getElementById('eventLine');
 if(events[k]){eventLine.textContent=events[k];eventLine.style.display='block';}else{eventLine.style.display='none';}
 updateDateCountdown();
 document.getElementById('overlay').classList.add('show');
}
function updateDateCountdown(){
 if(!selectedDate)return;
 const ms=new Date(selectedDate.getFullYear(),selectedDate.getMonth(),selectedDate.getDate())-new Date();
 const label=document.getElementById('countLabel'),value=document.getElementById('dateCountdown');
 if(ms>0){label.textContent='TIME REMAINING';value.textContent=parts(ms);}
 else if(ms<0){label.textContent='TIME SINCE THIS DATE';value.textContent=parts(ms)+' ago';}
 else{label.textContent='TODAY';value.textContent='This date is today';}
}
document.getElementById('add').addEventListener('click',()=>{
 const editor=document.getElementById('editor');editor.classList.toggle('show');
 if(editor.classList.contains('show'))document.getElementById('description').focus();
});
document.getElementById('save').addEventListener('click',()=>{
 if(!selectedDate)return;
 const k=keyFor(selectedDate),value=document.getElementById('description').value.trim();
 if(value)events[k]=value;else delete events[k];
 try{localStorage.setItem('sedCalendarEvents',JSON.stringify(events));}catch(e){}
 const line=document.getElementById('eventLine');
 if(value){line.textContent=value;line.style.display='block';}else{line.style.display='none';}
 document.getElementById('editor').classList.remove('show');
 renderYear();
});
document.getElementById('delete').addEventListener('click',()=>{
 if(!selectedDate)return;
 delete events[keyFor(selectedDate)];
 try{localStorage.setItem('sedCalendarEvents',JSON.stringify(events));}catch(e){}
 document.getElementById('description').value='';
 document.getElementById('eventLine').style.display='none';
 document.getElementById('editor').classList.remove('show');renderYear();
});
function closeModal(){document.getElementById('overlay').classList.remove('show');selectedDate=null;}
document.getElementById('close').addEventListener('click',closeModal);
document.getElementById('overlay').addEventListener('click',e=>{if(e.target.id==='overlay')closeModal();});
let startX=0,startY=0;
document.getElementById('sheet').addEventListener('touchstart',e=>{
 if(document.getElementById('overlay').classList.contains('show'))return;
 startX=e.changedTouches[0].screenX;startY=e.changedTouches[0].screenY;
},{passive:true});
document.getElementById('sheet').addEventListener('touchend',e=>{
 if(document.getElementById('overlay').classList.contains('show'))return;
 const dx=e.changedTouches[0].screenX-startX,dy=e.changedTouches[0].screenY-startY;
 if(Math.abs(dx)>55&&Math.abs(dx)>Math.abs(dy)*1.25)changeYear(dx<0?1:-1);
},{passive:true});
document.addEventListener('keydown',e=>{
 if(e.key==='ArrowLeft')changeYear(-1);
 if(e.key==='ArrowRight')changeYear(1);
 if(e.key==='Escape')closeModal();
});
renderYear();setInterval(()=>{updateHeader();updateDateCountdown();},1000);
</script></body></html>
    """.trimIndent()
}
