package com.timeguard.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val navy = Color.rgb(11, 19, 37)
    private val mint = Color.rgb(42, 211, 182)
    private lateinit var root: LinearLayout
    private fun label(s: String, size: Float = 16f, color: Int = Color.WHITE): TextView = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); setPadding(0, 9, 0, 9)
    }
    private fun button(s: String, action: () -> Unit) = Button(this).apply {
        text = s; setOnClickListener { action() }; root.addView(this, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }
    override fun onResume() { super.onResume(); render() }
    private fun render() {
        val scroll = ScrollView(this).apply { setBackgroundColor(navy) }
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 36, 24, 30) }
        scroll.addView(root); setContentView(scroll)
        root.addView(label("◇ TIMEGUARD", 30f, mint))
        root.addView(label("Smart Scroll + Screen Coach V2", 20f))
        root.addView(label("Local prototype • No account • No cloud upload", 13f))
        button("ENABLE / MANAGE SCROLL DETECTION") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        val data = ScrollStore.today(this)
        root.addView(label("TODAY'S ACTIVITY", 19f, mint))
        root.addView(label("${data.events} scroll events", 27f))
        root.addView(label("${data.sessions} estimated sessions  •  ${data.activeMinutesEstimate} estimated active minutes", 16f))
        root.addView(label("Session estimate: consecutive events in the same app within 60 seconds. This is NOT actual screen time; single-event sessions contribute 0 minutes.", 12f))
        data.perApp.entries.toList().take(10).forEach { entry ->
    root.addView(label("${friendly(entry.key)} • ${entry.value} events", 15f))
        }
        button("REFRESH DASHBOARD") { render() }
        root.addView(label("✦ SCREEN COACH", 23f, mint))
        val goal = ScrollStore.goal(this)
        val coaching = when {
            data.events == 0 -> "Start by enabling detection and scrolling in another app."
            data.activeMinutesEstimate >= goal -> "Your estimated active scrolling has reached your $goal-minute goal. Consider taking a break."
            data.sessions >= 5 -> "You've had several scrolling sessions. Are you opening apps intentionally? A short pause may help."
            else -> "Nice start. Check your activity periodically and decide whether a break would help."
        }
        root.addView(label(coaching, 17f))
        root.addView(label("Local rules, not live AI. Estimates can undercount or overcount actual usage.", 12f))
        button("SET DAILY ESTIMATED-MINUTES GOAL ($goal)") {
            val options = arrayOf("10", "20", "30", "45", "60")
            AlertDialog.Builder(this).setTitle("Daily estimated-minutes goal")
                .setItems(options) { _, index -> ScrollStore.setGoal(this, options[index].toInt()); render() }.show()
        }
        button("DELETE LOCAL ACTIVITY") {
            AlertDialog.Builder(this).setTitle("Delete recorded activity?")
                .setMessage("This removes TimeGuard's locally saved scroll events.")
                .setPositiveButton("Delete") { _, _ -> ScrollStore.clear(this); render() }
                .setNegativeButton("Cancel", null).show()
        }
        root.addView(label("Test build only. Accessibility scroll events vary by app. Disable the service in Android Settings at any time.", 12f))
    }
    private fun friendly(pkg: String): String = when (pkg) {
        "com.zhiliaoapp.musically" -> "TikTok"
        "com.instagram.android" -> "Instagram"
        "com.facebook.katana" -> "Facebook"
        "com.openai.chatgpt" -> "ChatGPT"
        "com.android.settings" -> "Android Settings"
        "com.sec.android.app.launcher" -> "Samsung Launcher"
        else -> pkg
    }
}
