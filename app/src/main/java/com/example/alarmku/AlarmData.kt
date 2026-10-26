
package com.example.alarmku

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AlarmItem(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true
)

object AlarmData {
    private const val PREFS = "alarmku_prefs"
    private const val KEY = "alarms"

    fun load(context: Context): List<AlarmItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = JSONArray(prefs.getString(KEY, "[]"))
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            AlarmItem(
                id = obj.getInt("id"),
                hour = obj.getInt("hour"),
                minute = obj.getInt("minute"),
                enabled = obj.optBoolean("enabled", true)
            )
        }
    }

    fun save(context: Context, alarms: List<AlarmItem>) {
        val array = JSONArray()
        alarms.forEach { alarm ->
            array.put(
                JSONObject().apply {
                    put("id", alarm.id)
                    put("hour", alarm.hour)
                    put("minute", alarm.minute)
                    put("enabled", alarm.enabled)
                }
            )
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, array.toString())
            .apply()
    }
}
