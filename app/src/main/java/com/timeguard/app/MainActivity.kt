
package com.timeguard.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.*

/** Local parent-dashboard prototype. Saved limits are NOT enforced on other apps. */
class MainActivity : Activity() {
    private val navy = Color.rgb(11, 19, 37)
    private val mint = Color.rgb(42, 211, 182)
    private lateinit var root: LinearLayout
    private var unlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!FamilyStore.hasPin(this)) setupPin() else unlock()
    }

    override fun onResume() { super.onResume(); if (unlocked) render() }

    private fun input(hint: String, numeric: Boolean = false, secret: Boolean = false): EditText =
        EditText(this).apply {
            this.hint = hint
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            if (numeric) inputType = if (secret) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD else InputType.TYPE_CLASS_NUMBER
        }

    private fun dialog(title: String, field: EditText, action: (String) -> Unit) {
        AlertDialog.Builder(this).setTitle(title).setView(field)
            .setPositiveButton("Save") { _, _ ->
                try { action(field.text.toString()); if (unlocked) render() }
                catch (e: Exception) { Toast.makeText(this, e.message ?: "Invalid input", Toast.LENGTH_LONG).show() }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun setupPin() {
        val field = input("Choose a 6–12 digit parent PIN", numeric = true, secret = true)
        AlertDialog.Builder(this).setTitle("Set parent PIN").setMessage("This PIN protects settings on this device. It is not a remote-control security system.")
            .setView(field).setCancelable(false).setPositiveButton("Continue") { _, _ ->
                val first = field.text.toString()
                if (first.length !in 6..12 || !first.all(Char::isDigit)) {
                    Toast.makeText(this, "Use 6–12 digits", Toast.LENGTH_LONG).show(); setupPin()
                } else {
                    val confirm = input("Repeat PIN", numeric = true, secret = true)
                    AlertDialog.Builder(this).setTitle("Confirm PIN").setView(confirm).setCancelable(false)
                        .setPositiveButton("Create") { _, _ ->
                            if (confirm.text.toString() == first) { FamilyStore.setPin(this, first); unlocked = true; render() }
                            else { Toast.makeText(this, "PINs do not match", Toast.LENGTH_LONG).show(); setupPin() }
                        }.show()
                }
            }.show()
    }

    private fun unlock() {
        val field = input("Parent PIN", numeric = true, secret = true)
        AlertDialog.Builder(this).setTitle("TimeGuard parent access").setView(field).setCancelable(false)
            .setPositiveButton("Unlock") { _, _ ->
                if (FamilyStore.checkPin(this, field.text.toString())) { unlocked = true; render() }
                else { Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_LONG).show(); unlock() }
            }.setNegativeButton("Exit") { _, _ -> finish() }.show()
    }

    private fun label(value: String, size: Float = 17f, color: Int = Color.WHITE) {
        root.addView(TextView(this).apply { text = value; textSize = size; setTextColor(color); setPadding(0, 12, 0, 12) })
    }

    private fun button(value: String, action: () -> Unit) {
        root.addView(Button(this).apply { text = value; setOnClickListener { action() } },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun selectedChild(): FamilyStore.Child? {
        val children = FamilyStore.children(this)
        return children.firstOrNull { it.id == FamilyStore.activeChild(this) } ?: children.firstOrNull()
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(navy)
            setPadding(24, 32, 24, 32)
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        label("◇ TIMEGUARD V3", 30f, mint)
        label("Family dashboard • Local prototype", 15f)
        label("CHILD PROFILES", 21f, mint)
        val children = FamilyStore.children(this)
        if (children.isEmpty()) label("No child profiles yet")
        children.forEach { child ->
            button("${if (selectedChild()?.id == child.id) "✓ " else ""}${child.name} — ${child.dailyMinutes} min/day") {
                FamilyStore.setActiveChild(this, child.id); render()
            }
        }
        button("ADD CHILD") {
            dialog("Child name", input("Name")) { FamilyStore.addChild(this, it) }
        }
        val child = selectedChild()
        if (child != null) {
            label("SCREEN-TIME SETTINGS • ${child.name}", 21f, mint)
            label("Daily configured limit: ${child.dailyMinutes} minutes")
            button("CHANGE DAILY LIMIT") {
                dialog("Daily minutes (0–1440)", input("Minutes", numeric = true)) {
                    FamilyStore.setDaily(this, child.id, it.toInt())
                }
            }
            label("APP RESTRICTIONS", 21f, mint)
            val rules = FamilyStore.rules(this).filter { it.childId == child.id }
            if (rules.isEmpty()) label("No app rules saved")
            rules.forEach { rule ->
                button("${rule.pkg} • ${if (rule.blocked) "Blocked" else "${rule.minutes} min/day"}") { editRule(child.id, rule.pkg, rule) }
            }
            button("ADD / EDIT APP RULE") {
                dialog("Android package name (e.g. com.instagram.android)", input("Package name")) { pkg ->
                    require(pkg.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) { "Enter a valid package name" }
                    editRule(child.id, pkg, FamilyStore.rules(this).firstOrNull { it.childId == child.id && it.pkg == pkg })
                }
            }
        }
        label("SCROLL TRACKING", 21f, mint)
        label("Your existing V2 scroll tracker remains separate from these child limits.", 14f)
        button("MANAGE SCROLL DETECTION") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        label("PARENT SECURITY", 21f, mint)
        button("LOCK PARENT DASHBOARD") { unlocked = false; unlock() }
        label("Prototype only: app blocks and time limits are stored but NOT enforced. No remote pairing or child-device control.", 13f, Color.LTGRAY)
    }

    private fun editRule(childId: String, pkg: String, old: FamilyStore.Rule?) {
        val field = input("Minutes per day (0–1440)", numeric = true).apply { setText((old?.minutes ?: 30).toString()) }
        val blocked = CheckBox(this).apply { text = "Block this app (saved preference only)"; setTextColor(Color.BLACK); isChecked = old?.blocked ?: false }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 0, 20, 0); addView(field); addView(blocked) }
        AlertDialog.Builder(this).setTitle(pkg).setView(layout)
            .setPositiveButton("Save rule") { _, _ ->
                val minutes = field.text.toString().toIntOrNull()
                if (minutes == null || minutes !in 0..1440) Toast.makeText(this, "Enter 0–1440 minutes", Toast.LENGTH_LONG).show()
                else { FamilyStore.saveRule(this, FamilyStore.Rule(childId, pkg, minutes, blocked.isChecked)); render() }
            }.setNegativeButton("Cancel", null).show()
    }
}
