
package com.example.alarmku

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, alarm: AlarmItem) {
        // Batalkan jadwal lama agar perubahan hari/jam berlaku.
        cancel(context, alarm)

        if (!alarm.enabled || alarm.daysOfWeek.isEmpty()) return

        val manager = context.getSystemService(Context.ALARM_SERVICE)
                as AlarmManager

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !manager.canScheduleExactAlarms()
        ) {
            return
        }

        val now = System.currentTimeMillis()
        var nextTime: Long? = null

        // Cari jadwal terdekat dalam tujuh hari ke depan.
        for (offset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                add(Calendar.DAY_OF_YEAR, offset)
            }

            val day = candidate.get(Calendar.DAY_OF_WEEK)

            if (
                day in alarm.daysOfWeek &&
                candidate.timeInMillis > now
            ) {
                nextTime = candidate.timeInMillis
                break
            }
        }

        val triggerTime = nextTime ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("hour", alarm.hour)
            putExtra("minute", alarm.minute)
            putExtra("music_uri", alarm.musicUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        manager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun cancel(context: Context, alarm: AlarmItem) {
        val intent = Intent(context, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val manager = context.getSystemService(Context.ALARM_SERVICE)
                as AlarmManager

        manager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
