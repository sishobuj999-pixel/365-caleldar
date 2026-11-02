package com.sed.calendarwidget

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val web = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
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
<title>2026 Calendar</title><meta name="theme-color" content="#ffffff">
<style>
:root{--ink:#000;--paper:#fff;--line:#d4d4d4}
*{box-sizing:border-box;margin:0}
html,body{height:100%;background:#fff}
body{display:flex;justify-content:center;align-items:center;font-family:"Helvetica Neue",Helvetica,Arial,sans-serif;color:var(--ink)}
.sheet{container-type:inline-size;width:min(100vw,calc(100vh*9/16));height:min(177.7778vw,100vh);aspect-ratio:9/16;background:var(--paper);padding:5cqw 3cqw 3cqw;display:flex;flex-direction:column}
.top{display:flex;justify-content:space-between;align-items:flex-end;padding:0 .4cqw 2.4cqw}
.top h1{font-size:17cqw;font-weight:800;letter-spacing:-.06em;line-height:.8}
.top p{font-size:2.1cqw;font-weight:500;letter-spacing:.02em;text-align:right;line-height:1.5}
.wk,.g{display:grid;grid-template-columns:repeat(14,1fr)}
.wk span{font-size:1.5cqw;font-weight:700;text-align:center;padding:1.2cqw 0;border-top:.5cqw solid var(--ink);border-bottom:1px solid var(--ink)}
.g{flex:1;min-height:0;grid-template-rows:repeat(27,minmax(0,1fr));border-left:1px solid var(--line)}
.g div{border-right:1px solid var(--line);border-bottom:1px solid var(--line);display:flex;flex-direction:column;justify-content:center;align-items:center;gap:.25cqw}
.g div:nth-child(-n+14){border-top:0}
.g b{font-size:2.7cqw;font-weight:800;letter-spacing:-.03em;line-height:1}
.g i{font-style:normal;font-size:1.3cqw;font-weight:500;color:#555;line-height:1;white-space:nowrap}
.g .m{background:var(--ink)}.g .m b{color:#fff}.g .m i{color:#bbb}
.g .e{background:#fff}
.g .t{background:#16a34a}.g .t b,.g .t i{color:#fff}
.live{text-align:right;font-variant-numeric:tabular-nums}
#clk{font-size:5.2cqw;font-weight:800;letter-spacing:-.02em;line-height:1}
#dt,#left{font-size:1.9cqw;font-weight:500;margin-top:.9cqw}
</style></head><body><main class="sheet">
<div class="top"><h1>2026</h1><div class="live"><div id="clk">--:--:--</div><div id="dt"></div><div id="left"></div></div></div>
<div class="wk"><span>SAT</span><span>SUN</span><span>MON</span><span>TUE</span><span>WED</span><span>THU</span><span>FRI</span><span>SAT</span><span>SUN</span><span>MON</span><span>TUE</span><span>WED</span><span>THU</span><span>FRI</span></div>
<div class="g" id="grid"></div></main><script>
const grid=document.getElementById('grid');
const codes=['JN','FB','MR','AP','MY','JU','JL','AU','SE','OC','NO','DE'];
const first=new Date(2026,0,1), offset=(first.getDay()+1)%7;
for(let i=0;i<offset;i++){const e=document.createElement('div');e.className='e';grid.appendChild(e)}
for(let i=0;i<365;i++){
 const d=new Date(2026,0,1+i), cell=document.createElement('div');
 if(d.getDate()===1)cell.className='m';
 cell.innerHTML='<b>'+(i+1)+'</b><i>'+codes[d.getMonth()]+' '+d.getDate()+'</i>';
 grid.appendChild(cell);
}
while(grid.children.length<378){const e=document.createElement('div');e.className='e';grid.appendChild(e)}
const cells=[...grid.children].filter(c=>c.querySelector('b'));
const p=n=>String(n).padStart(2,'0');
const M=['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
let marked=-1;
function tick(){
 const n=new Date();
 document.getElementById('clk').textContent=p(n.getHours())+':'+p(n.getMinutes())+':'+p(n.getSeconds());
 document.getElementById('dt').textContent=n.getDate()+' '+M[n.getMonth()]+' '+n.getFullYear();
 const ms=new Date(2027,0,1)-n,L=document.getElementById('left');
 if(ms>0&&n.getFullYear()===2026){const s=Math.floor(ms/1000);L.textContent=Math.floor(s/86400)+'d '+p(Math.floor(s%86400/3600))+':'+p(Math.floor(s%3600/60))+':'+p(s%60)+' left';
 const doy=Math.floor((new Date(2026,n.getMonth(),n.getDate())-first)/864e5)+1;
 if(doy!==marked){cells.forEach(c=>c.classList.remove('t'));if(cells[doy-1])cells[doy-1].classList.add('t');marked=doy;}
 }else{L.textContent=n.getFullYear()>2026?'2026 has ended':'';cells.forEach(c=>c.classList.remove('t'));}
}
tick();setInterval(tick,1000);
</script></body></html>
    """.trimIndent()
}