package com.zag.bitterhabits.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zag.bitterhabits.MainActivity
import com.zag.bitterhabits.R
import com.zag.bitterhabits.data.DayState
import com.zag.bitterhabits.data.Habit
import com.zag.bitterhabits.data.Store
import com.zag.bitterhabits.data.state
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object Reminders {
    const val CHANNEL = "habit_reminders"
    private const val EXTRA_HABIT = "hid"
    private val lines = listOf("You know what to do.", "Still on the list.", "Ten minutes, then it's done.")

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    private fun alarmIntent(ctx: Context, habitId: String): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, habitId.hashCode(),
            Intent(ctx, ReminderReceiver::class.java).putExtra(EXTRA_HABIT, habitId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun cancel(ctx: Context, habitId: String) {
        ctx.getSystemService(AlarmManager::class.java)?.cancel(alarmIntent(ctx, habitId))
    }

    fun schedule(ctx: Context, h: Habit) {
        cancel(ctx, h.id)
        if (!h.reminderOn || h.archived) return
        val now = LocalDateTime.now()
        var at = now.toLocalDate().atStartOfDay().plusMinutes(h.reminderMinutes.toLong())
        if (!at.isAfter(now)) at = at.plusDays(1)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        ctx.getSystemService(AlarmManager::class.java)
            ?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, alarmIntent(ctx, h.id))
    }

    fun scheduleAll(ctx: Context) {
        Store.current(ctx).habits.forEach { schedule(ctx, it) }
    }

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, habitId: String) {
        val data = Store.current(ctx)
        val h = data.habit(habitId) ?: return
        val today = LocalDate.now()
        val st = data.state(h, today, today)
        if (st == DayState.DONE || st == DayState.REST) return
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val done = PendingIntent.getBroadcast(
            ctx, habitId.hashCode() + 1,
            Intent(ctx, MarkDoneReceiver::class.java).putExtra(EXTRA_HABIT, habitId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(h.name)
            .setContentText(lines[(today.dayOfYear + habitId.length) % lines.size])
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Mark done", done)
            .build()
        try {
            if (NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
                NotificationManagerCompat.from(ctx).notify(habitId.hashCode(), n)
            }
        } catch (e: SecurityException) {
            // Notifications not allowed; nothing to do.
        }
    }

    fun habitIdFrom(i: Intent): String? = i.getStringExtra(EXTRA_HABIT)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = Reminders.habitIdFrom(intent) ?: return
        Reminders.show(context, id)
        Store.current(context).habit(id)?.let { Reminders.schedule(context, it) }
    }
}

class MarkDoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = Reminders.habitIdFrom(intent) ?: return
        val e = LocalDate.now().toEpochDay()
        Store.update(context) { it.setRest(id, e, false).setDone(id, e, true) }
        NotificationManagerCompat.from(context).cancel(id.hashCode())
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.scheduleAll(context)
    }
}
