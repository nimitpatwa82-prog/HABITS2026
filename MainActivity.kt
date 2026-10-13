package com.nimit.habits

import android.app.Activity
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView

class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        val sp = Alarms.sp(this)
        if (Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms() && !sp.getBoolean("asked", false)) {
                sp.edit().putBoolean("asked", true).apply()
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            }
        }
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "Native")
        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)
    }

    override fun onResume() {
        super.onResume()
        if (::web.isInitialized) web.evaluateJavascript("if(window.pull)pull()", null)
    }

    inner class Bridge {
        @JavascriptInterface fun sync(json: String) { Alarms.replace(applicationContext, json) }
        @JavascriptInterface fun pending(): String = Alarms.sp(applicationContext).getString("pending", "[]") ?: "[]"
        @JavascriptInterface fun clear() { Alarms.sp(applicationContext).edit().putString("pending", "[]").apply() }
    }
}
