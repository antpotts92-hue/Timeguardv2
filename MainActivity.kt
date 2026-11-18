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

/** Parent settings require local PIN. This is not tamper-proof device management. */
class MainActivity : Activity() {
    private val navy=Color.rgb(11,19,37)
    private val mint=Color.rgb(42,211,182)
    private lateinit var column: LinearLayout
    private var unlocked=false
    private fun label(s:String, size:Float=17f, color:Int=Color.WHITE)=TextView(this).apply { text=s;textSize=size;setTextColor(color);setPadding(0,12,0,12) }
    private fun button(s:String,action:()->Unit)=Button(this).apply { text=s;setOnClickListener { action() } }
    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);render() }
    private fun ask(title:String,hint:String,numeric:Boolean=false,done:(String)->Unit) {
        val input=EditText(this).apply { this.hint=hint; inputType=if(numeric) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT }
        AlertDialog.Builder(this).setTitle(title).setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save") { _,_->done(input.text.toString()) }.show()
    }
    private fun requireParent(action:()->Unit) {
        if(unlocked){action();return}
        if(!FamilyStore.hasPin(this)) {
            ask("Create parent PIN (6–12 digits)","Parent PIN",true) { pin ->
                if(pin.length in 6..12) { FamilyStore.setPin(this,pin);unlocked=true;action() } else Toast.makeText(this,"Use 6–12 digits",Toast.LENGTH_LONG).show()
            }
        } else ask("Unlock parent controls","Parent PIN",true) { pin ->
            if(FamilyStore.checkPin(this,pin)){unlocked=true;action()} else Toast.makeText(this,"Incorrect PIN",Toast.LENGTH_LONG).show()
        }
    }
    private fun render() {
        val scroll=ScrollView(this).apply { setBackgroundColor(navy) }
        column=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(24,28,24,36) }
        scroll.addView(column);setContentView(scroll)
        column.addView(label("◇ TIMEGUARD V3",32f,mint))
        column.addView(label("Family Controls • Local prototype",19f))
        val activity=ScrollStore.today(this)
        column.addView(label("Today: ${activity.events} scroll events • ${activity.activeMinutesEstimate} estimated scroll minutes"))
        column.addView(button("ENABLE / MANAGE SCROLL DETECTION") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        column.addView(button("GRANT USAGE ACCESS") { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) })
        column.addView(label(if(UsageReader.permitted(this)) "Usage access enabled" else "Usage access not enabled — screen-time totals unavailable",14f,mint))
        column.addView(button("REFRESH DASHBOARD") { render() })
        column.addView(label("PARENT CONTROLS",24f,mint))
        column.addView(button(if(unlocked) "PARENT SETTINGS (UNLOCKED)" else "UNLOCK PARENT SETTINGS") { requireParent { render() } })
        if(unlocked) parentPanel() else column.addView(label("Unlock to manage child profiles, daily limits and app rules.",15f))
        column.addView(label("Important: restrictions below are advisory in this prototype; Android Accessibility and Usage Access do not provide tamper-proof blocking. No remote pairing or cloud upload.",13f))
    }
    private fun parentPanel() {
        column.addView(button("ADD CHILD PROFILE") { ask("New child profile","Nickname") { name ->
            if(name.isNotBlank()) { FamilyStore.addChild(this,name);render() }
        } })
        val children=FamilyStore.children(this)
        if(children.isEmpty()) column.addView(label("No child profiles yet."))
        val usage=UsageReader.today(this)
        for(child in children) {
            column.addView(label("${child.name} • daily goal ${child.dailyMinutes} min",20f,mint))
            column.addView(button("SET DAILY LIMIT — ${child.name}") { ask("Daily limit (minutes)","0–1440",true) { value ->
                value.toIntOrNull()?.let { FamilyStore.setDaily(this,child.id,it);render() }
            } })
            column.addView(button("SELECT AS THIS DEVICE'S PROFILE") { FamilyStore.setActiveChild(this,child.id);render() })
            column.addView(button("ADD / EDIT APP RULE") { chooseApp(child.id) })
            FamilyStore.rules(this).filter { it.childId==child.id }.forEach { rule ->
                val minutes=(usage[rule.pkg] ?: 0L)/60000L
                column.addView(label("${rule.pkg}: ${minutes}m observed / ${rule.minutes}m limit${if(rule.blocked) " • marked restricted" else ""}",14f))
            }
        }
        val active=FamilyStore.activeChild(this)
        column.addView(label("Active device profile: ${children.firstOrNull { it.id==active }?.name ?: "none"}",15f))
        if(active.isNotEmpty()) {
            val child=children.firstOrNull { it.id==active }
            if(child!=null) {
                val total=usage.filterKeys { it!=packageName }.values.sum()/60000L
                column.addView(label("Observed device usage today: ${total}m / ${child.dailyMinutes}m daily goal",17f))
                if(total>=child.dailyMinutes) column.addView(label("Daily goal reached. Advisory alert only.",16f,mint))
            }
        }
        column.addView(button("LOCK PARENT SETTINGS") { unlocked=false;render() })
    }
    private fun chooseApp(childId:String) {
        val usage=UsageReader.today(this)
        val packages=(usage.keys + listOf("com.zhiliaoapp.musically","com.instagram.android","com.google.android.youtube","com.facebook.katana")).distinct().sorted()
        AlertDialog.Builder(this).setTitle("Choose an app").setItems(packages.toTypedArray()) { _,index ->
            val pkg=packages[index]
            val choices=arrayOf("Mark restricted (advisory)","15 minute goal","30 minute goal","60 minute goal","120 minute goal")
            AlertDialog.Builder(this).setTitle(pkg).setItems(choices) { _,choice ->
                FamilyStore.saveRule(this,FamilyStore.Rule(childId,pkg,when(choice){1->15;2->30;3->60;4->120;else->0},choice==0));render()
            }.show()
        }.show()
    }
}
