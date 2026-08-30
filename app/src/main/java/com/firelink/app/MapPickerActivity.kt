package com.firelink.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class MapPickerActivity : Activity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        val startLat = intent.getDoubleExtra("startLat", 32.4279)
        val startLon = intent.getDoubleExtra("startLon", 53.6880)
        val startZoom = if (intent.hasExtra("startLat")) 15 else 5
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(Bridge(), "Android")
        webView.loadDataWithBaseURL("https://www.openstreetmap.org/", html(startLat, startLon, startZoom), "text/html", "UTF-8", null)
    }

    inner class Bridge {
        @JavascriptInterface
        fun confirm(lat: Double, lon: Double) {
            runOnUiThread {
                setResult(RESULT_OK, Intent().putExtra("latitude", lat).putExtra("longitude", lon))
                finish()
            }
        }

        @JavascriptInterface
        fun noPoint() {
            runOnUiThread { Toast.makeText(this@MapPickerActivity, "ابتدا روی نقشه یک نقطه انتخاب کنید", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun html(lat: Double, lon: Double, zoom: Int) = """
<!doctype html><html dir="rtl"><head>
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"/>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<style>
html,body,#map{height:100%;margin:0} body{font-family:sans-serif;background:#fff}
#map{position:absolute;inset:0 0 82px 0}
#bar{position:absolute;bottom:0;left:0;right:0;height:82px;display:flex;align-items:center;gap:10px;padding:10px 12px;box-sizing:border-box;background:#fff;border-top:1px solid #ddd;z-index:9999}
#coord{flex:1;font-size:13px;line-height:1.4;color:#333;overflow:hidden}
button{border:0;border-radius:14px;background:#6f50b5;color:white;font-size:17px;padding:14px 20px;min-width:140px}
.tip{position:absolute;top:12px;left:50%;transform:translateX(-50%);z-index:9999;background:rgba(255,255,255,.95);padding:9px 14px;border-radius:12px;box-shadow:0 2px 8px #999;font-size:14px;white-space:nowrap}
</style></head><body>
<div class="tip">روی محل موردنظر بزنید</div><div id="map"></div>
<div id="bar"><div id="coord">هنوز نقطه‌ای انتخاب نشده</div><button id="send">ارسال برای تیم</button></div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script>
const map=L.map('map').setView([$lat,$lon],$zoom);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap'}).addTo(map);
let marker=null, selected=null;
map.on('click',e=>{ selected=e.latlng; if(marker) marker.setLatLng(selected); else marker=L.marker(selected).addTo(map); document.getElementById('coord').textContent='عرض: '+selected.lat.toFixed(6)+'  طول: '+selected.lng.toFixed(6); });
document.getElementById('send').addEventListener('click',()=>{ if(!selected){Android.noPoint();return;} Android.confirm(selected.lat,selected.lng); });
</script></body></html>
""".trimIndent()
}
