package com.nimit.habits

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Alarms {
    const val CH = "habits"
    fun sp(c: Context) = c.getSharedPreferences("habits", Context.MODE_PRIVATE)
    fun code(id: String, slot: Int): Int = (id.hashCode() and 0xFFFFFF) * 2 + slot
    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun nextTime(time: String, days: List<Int>, from: Long): Long {
        val p = time.split(":")
        for (o in 0..7) {
            val c = Calendar.getInstance()
            c.timeInMillis = from
            c.add(Calendar.DAY_OF_YEAR, o)
            c.set(Calendar.HOUR_OF_DAY, p[0].toInt())
            c.set(Calendar.MINUTE, p[1].toInt())
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            if (c.timeInMillis > from && days.contains(c.get(Calendar.DAY_OF_WEEK) - 1)) return c.timeInMillis
        }
        return -1L
    }

    private fun pi(c: Context, id: String, slot: Int): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).putExtra("id", id)
        return PendingIntent.getBroadcast(c, code(id, slot), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun setAt(c: Context, id: String, slot: Int, at: Long) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val p = pi(c, id, slot)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
    }

    private fun cancel(c: Context, id: String) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi(c, id, 0)); am.cancel(pi(c, id, 1))
    }

    fun find(c: Context, id: String): JSONObject? {
        val a = JSONArray(sp(c).getString("list", "[]"))
        for (i in 0 until a.length()) { val o = a.getJSONObject(i); if (o.getString("id") == id) return o }
        return null
    }

    private fun days(o: JSONObject): List<Int> {
        val a = o.getJSONArray("days")
        return (0 until a.length()).map { a.getInt(it) }
    }

    fun scheduleHabit(c: Context, o: JSONObject) {
        val t = nextTime(o.getString("time"), days(o), System.currentTimeMillis())
        if (t > 0) setAt(c, o.getString("id"), 0, t)
    }

    fun scheduleAll(c: Context) {
        val a = JSONArray(sp(c).getString("list", "[]"))
        for (i in 0 until a.length()) scheduleHabit(c, a.getJSONObject(i))
    }

    fun replace(c: Context, json: String) {
        val old = JSONArray(sp(c).getString("list", "[]"))
        for (i in 0 until old.length()) cancel(c, old.getJSONObject(i).getString("id"))
        sp(c).edit().putString("list", json).apply()
        scheduleAll(c)
    }

    fun addPending(c: Context, id: String, v: Int) {
        val a = JSONArray(sp(c).getString("pending", "[]"))
        a.put(JSONObject().put("id", id).put("date", today()).put("v", v))
        sp(c).edit().putString("pending", a.toString()).apply()
    }

    fun doneToday(c: Context, id: String): Boolean {
        val a = JSONArray(sp(c).getString("pending", "[]"))
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            if (o.getString("id") == id && o.getString("date") == today()) return true
        }
        return false
    }
}
