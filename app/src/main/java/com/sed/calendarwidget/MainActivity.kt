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
:root{--ink:#fff;--paper:rgba(20,30,49,.38);--line:rgba(255,255,255,.19)}
*{box-sizing:border-box;margin:0}
html,body{min-height:100%;background:transparent}
body{display:flex;justify-content:center;align-items:center;font-family:"Helvetica Neue",Helvetica,Arial,sans-serif;color:var(--ink);padding:env(safe-area-inset-top) 0 env(safe-area-inset-bottom)}
.sheet{container-type:inline-size;width:min(100vw,calc(100vh*9/16));height:min(177.7778vw,100vh);aspect-ratio:9/16;background:rgba(20,30,49,.38);-webkit-backdrop-filter:blur(28px);backdrop-filter:blur(28px);border:1px solid rgba(255,255,255,.24);border-radius:5cqw;padding:5cqw 3cqw 3cqw;display:flex;flex-direction:column;box-shadow:0 12px 42px rgba(0,0,0,.18)}
.top{display:flex;justify-content:space-between;align-items:flex-end;padding:0 .4cqw 2.4cqw}
.top h1{font-size:17cqw;font-weight:800;letter-spacing:-.06em;line-height:.8}
.top p{font-size:2.1cqw;font-weight:500;letter-spacing:.02em;text-align:right;line-height:1.5}
.wk,.g{display:grid;grid-template-columns:repeat(14,1fr)}
.wk span{font-size:1.5cqw;font-weight:700;text-align:center;padding:1.2cqw 0;border-top:.5cqw solid rgba(255,255,255,.85);border-bottom:1px solid var(--line)}
.g{flex:1;min-height:0;grid-template-rows:repeat(27,minmax(0,1fr));border-left:1px solid var(--line)}
.g div{border-right:1px solid var(--line);border-bottom:1px solid var(--line);display:flex;flex-direction:column;justify-content:center;align-items:center;gap:.25cqw;background:rgba(255,255,255,.035)}
.g div:nth-child(-n+14){border-top:0}
.g b{font-size:2.7cqw;font-weight:800;letter-spacing:-.03em;line-height:1}
.g i{font-style:normal;font-size:1.3cqw;font-weight:500;color:rgba(255,255,255,.72);line-height:1;white-space:nowrap}
.g .m{background:rgba(0,0,0,.43)}.g .m b{color:#fff}.g .m i{color:#ddd}
.g .e{background:rgba(255,255,255,.025)}
.g .t{background:rgba(22,163,74,.82)}.g .t b,.g .t i{color:#fff}
.live{text-align:right;font-variant-numeric:tabular-nums}
#clk{font-size:5.2cqw;font-weight:800;letter-spacing:-.02em;line-height:1}
#dt,#left{font-size:1.9cqw;font-weight:500;margin-top:.9cqw}
@media(prefers-reduced-transparency:reduce){.sheet{background:rgba(20,30,49,.9)}}
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