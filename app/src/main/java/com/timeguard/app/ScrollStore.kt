package com.timeguard.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Local-only event log. Sessions are estimates, not exact app foreground time. */
object ScrollStore {
    private const val PREF = "timeguard_v2"
    private const val KEY = "events"
    private const val MAX_EVENTS = 20000
    private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
    private const val SESSION_GAP_MS = 60_000L
    data class Event(val pkg: String, val time: Long)
    data class Summary(val events: Int, val sessions: Int, val activeMinutesEstimate: Int, val perApp: Map<String, Int>)

    @Synchronized fun record(context: Context, pkg: String, now: Long) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val array = try { JSONArray(prefs.getString(KEY, "[]")) } catch (_: Exception) { JSONArray() }
        val fresh = JSONArray()
        for (i in maxOf(0, array.length() - MAX_EVENTS + 1) until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (obj.optLong("t") >= now - MAX_AGE_MS) fresh.put(obj)
        }
        fresh.put(JSONObject().put("p", pkg).put("t", now))
        prefs.edit().putString(KEY, fresh.toString()).apply()
    }
    @Synchronized fun today(context: Context): Summary {
        val start = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val array = try { JSONArray(context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]")) } catch (_: Exception) { JSONArray() }
        val events = mutableListOf<Event>()
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val time = obj.optLong("t")
            val pkg = obj.optString("p")
            if (pkg.isNotBlank() && dateFormat.format(Date(time)) == start) events.add(Event(pkg, time))
        }
        val sorted = events.sortedBy { it.time }
        var sessions = 0
        var activeMs = 0L
        var last: Event? = null
        for (item in sorted) {
            val prev = last
            if (prev == null || prev.pkg != item.pkg || item.time - prev.time > SESSION_GAP_MS) sessions++
            else if (item.time > prev.time) activeMs += item.time - prev.time
            last = item
        }
        return Summary(sorted.size, sessions, (activeMs / 60000).toInt(), sorted.groupingBy { it.pkg }.eachCount().toList().sortedByDescending { it.second }.toMap())
    }
    fun goal(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("goal", 30)
    fun setGoal(context: Context, minutes: Int) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt("goal", minutes).apply()
    fun clear(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).apply()
}
