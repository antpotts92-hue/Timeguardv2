package com.timeguard.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

/** Prototype local configuration. Profiles are device-local; no remote pairing. */
object FamilyStore {
    private const val PREF = "timeguard_family_v3"
    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    data class Child(val id: String, val name: String, val dailyMinutes: Int)
    data class Rule(val childId: String, val pkg: String, val minutes: Int, val blocked: Boolean)
    fun children(c: Context): List<Child> {
        val a = JSONArray(prefs(c).getString("children", "[]"))
        return (0 until a.length()).mapNotNull { a.optJSONObject(it) }.map { Child(it.optString("id"),it.optString("name"),it.optInt("minutes",120)) }
    }
    fun addChild(c: Context, name: String) {
        require(name.trim().length in 1..40)
        val a=JSONArray(prefs(c).getString("children","[]"))
        a.put(JSONObject().put("id",java.util.UUID.randomUUID().toString()).put("name",name.trim()).put("minutes",120))
        prefs(c).edit().putString("children",a.toString()).apply()
    }
    fun setDaily(c: Context, id: String, minutes: Int) {
        val a=JSONArray(prefs(c).getString("children","[]"))
        for(i in 0 until a.length()) if(a.getJSONObject(i).optString("id")==id) a.getJSONObject(i).put("minutes",minutes.coerceIn(0,1440))
        prefs(c).edit().putString("children",a.toString()).apply()
    }
    fun rules(c: Context): List<Rule> {
        val a=JSONArray(prefs(c).getString("rules","[]"))
        return (0 until a.length()).mapNotNull { a.optJSONObject(it) }.map { Rule(it.optString("child"),it.optString("pkg"),it.optInt("minutes",30),it.optBoolean("blocked")) }
    }
    fun saveRule(c: Context, rule: Rule) {
        val a=JSONArray()
        rules(c).filterNot { it.childId==rule.childId && it.pkg==rule.pkg }.forEach { a.put(JSONObject().put("child",it.childId).put("pkg",it.pkg).put("minutes",it.minutes).put("blocked",it.blocked)) }
        a.put(JSONObject().put("child",rule.childId).put("pkg",rule.pkg).put("minutes",rule.minutes.coerceIn(0,1440)).put("blocked",rule.blocked))
        prefs(c).edit().putString("rules",a.toString()).apply()
    }
    fun activeChild(c: Context) = prefs(c).getString("active", "") ?: ""
    fun setActiveChild(c: Context, id: String) = prefs(c).edit().putString("active",id).apply()
    fun hasPin(c: Context) = prefs(c).contains("pin_hash")
    private fun hash(pin: String, salt: ByteArray): String {
        val spec=PBEKeySpec(pin.toCharArray(),salt,120000,256)
        val bytes=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.encodeToString(bytes,Base64.NO_WRAP)
    }
    fun setPin(c: Context,pin: String) {
        require(pin.length in 6..12 && pin.all { it.isDigit() })
        val salt=ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs(c).edit().putString("pin_salt",Base64.encodeToString(salt,Base64.NO_WRAP)).putString("pin_hash",hash(pin,salt)).apply()
    }
    fun checkPin(c: Context,pin: String): Boolean {
        val salt=prefs(c).getString("pin_salt",null) ?: return false
        val expected=prefs(c).getString("pin_hash",null) ?: return false
        val actual=hash(pin,Base64.decode(salt,Base64.NO_WRAP))
        return java.security.MessageDigest.isEqual(actual.toByteArray(),expected.toByteArray())
    }
}
