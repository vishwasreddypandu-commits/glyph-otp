package com.glyphotp

import android.content.Context

/** Keeps the last code for a few minutes only, then forgets it. */
object OtpStore {
    const val TTL_MS = 5 * 60 * 1000L
    private fun prefs(c: Context) = c.getSharedPreferences("otp", Context.MODE_PRIVATE)

    fun save(c: Context, code: String) =
        prefs(c).edit().putString("code", code).putLong("at", System.currentTimeMillis()).apply()

    fun fresh(c: Context): String? {
        val p = prefs(c)
        val code = p.getString("code", null) ?: return null
        if (System.currentTimeMillis() - p.getLong("at", 0) > TTL_MS) { p.edit().clear().apply(); return null }
        return code
    }

    fun clear(c: Context) = prefs(c).edit().clear().apply()
}
