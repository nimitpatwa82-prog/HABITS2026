package com.nimit.habits

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getStringExtra("id") ?: return
        val h = Alarms.find(c, id) ?: return
        Alarms.scheduleHabit(c, h)
        if (Alarms.doneToday(c, id)) return
        val name = h.getString("name")
        val mini = h.optString("mini", "")
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(Alarms.CH, "Habit reminders", NotificationManager.IMPORTANCE_HIGH))
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val base = Alarms.code(id, 0)
        fun act(action: String, k: Int): PendingIntent =
            PendingIntent.getBroadcast(c, base + k * (1 shl 26), Intent(c, ActionReceiver::class.java).setAction(action).putExtra("id", id), flags)
        val open = PendingIntent.getActivity(c, base + 4 * (1 shl 26), Intent(c, MainActivity::class.java), flags)
        val icon = Icon.createWithResource(c, R.drawable.ic_notif)
        val b = Notification.Builder(c, Alarms.CH)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(name)
            .setContentText("Time for your habit · " + h.getString("time"))
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(icon, "Done", act("done", 1)).build())
            .addAction(Notification.Action.Builder(icon, "Snooze 10 min", act("snooze", 2)).build())
        if (mini.isNotEmpty()) b.addAction(Notification.Action.Builder(icon, "2-min", act("mini", 3)).build())
        nm.notify(base, b.build())
    }
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getStringExtra("id") ?: return
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(Alarms.code(id, 0))
        when (i.action) {
            "done" -> Alarms.addPending(c, id, 1)
            "mini" -> Alarms.addPending(c, id, 2)
            "snooze" -> Alarms.setAt(c, id, 1, System.currentTimeMillis() + 10 * 60000L)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Alarms.scheduleAll(c)
    }
}
