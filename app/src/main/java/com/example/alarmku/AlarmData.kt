
package com.example.alarmku

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class AlarmItem(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val daysOfWeek: Set<Int> = setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    ),
    val musicUri: String? = null
)

object AlarmData {
    private const val PREFS = "alarmku_prefs"
    private const val KEY = "alarms"

    fun load(context: Context): List<AlarmItem> {
        val prefs = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
        val array = JSONArray(prefs.getString(KEY, "[]"))

        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)

            val days = if (obj.has("daysOfWeek")) {
                val daysArray = obj.optJSONArray("daysOfWeek")
                if (daysArray != null) {
                    (0 until daysArray.length())
                        .map { daysArray.getInt(it) }
                        .filter { it in Calendar.SUNDAY..Calendar.SATURDAY }
                        .toSet()
                } else {
                    defaultDays()
                }
            } else {
                // Alarm lama tetap berbunyi setiap hari.
                defaultDays()
            }

            AlarmItem(
                id = obj.getInt("id"),
                hour = obj.getInt("hour"),
                minute = obj.getInt("minute"),
                enabled = obj.optBoolean("enabled", true),
                daysOfWeek = days,
                musicUri = obj.optString("musicUri")
                    .takeIf { it.isNotBlank() && it != "null" }
            )
        }
    }

    fun save(context: Context, alarms: List<AlarmItem>) {
        val array = JSONArray()

        alarms.forEach { alarm ->
            val daysArray = JSONArray()
            alarm.daysOfWeek.sorted().forEach { day ->
                daysArray.put(day)
            }

            array.put(
                JSONObject().apply {
                    put("id", alarm.id)
                    put("hour", alarm.hour)
                    put("minute", alarm.minute)
                    put("enabled", alarm.enabled)
                    put("daysOfWeek", daysArray)
                    put("musicUri", alarm.musicUri)
                }
            )
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, array.toString())
            .apply()
    }

    private fun defaultDays(): Set<Int> = setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    )
}
