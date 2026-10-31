package com.glyphotp

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.nothing.ketchum.Common

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48) }
        status = TextView(this).apply { textSize = 16f }
        l.addView(status)
        fun btn(t: String, f: () -> Unit) = l.addView(Button(this).apply { text = t; setOnClickListener { f() } })
        btn("1. Allow notification access") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        btn("2. Test: show 482913") { GlyphDisplay.showApp(this, "482913") }
        btn("3. Add OTP toy to Always-on Glyph") {
            try {
                startActivity(Intent().setComponent(ComponentName("com.nothing.thirdparty",
                    "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity")))
            } catch (e: Exception) { status.text = "Open Settings > Glyph Interface > Glyph Toys manually.\n${e.message}" }
        }
        setContentView(l)
    }

    override fun onResume() {
        super.onResume()
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true
        val device = try { if (Common.is25111p()) "Phone (4a) Pro, 13x13" else if (Common.is23112()) "Phone (3), 25x25" else "unsupported device" } catch (e: Throwable) { "unknown" }
        status.text = "Device: $device\nNotification access: ${if (enabled) "ON" else "OFF"}\n\n" +
            "Codes are read from notifications, scrolled 3 times, then the normal glyphs take back over,, and kept in memory/prefs for 5 min. Nothing leaves the phone."
    }
}
