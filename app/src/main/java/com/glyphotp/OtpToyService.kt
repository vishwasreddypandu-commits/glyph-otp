package com.glyphotp

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.nothing.ketchum.GlyphToy

/** Always-on Glyph Toy: shows the latest code (for 5 minutes) while selected. */
class OtpToyService : Service() {
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == GlyphToy.MSG_GLYPH_TOY) {
                val ev = msg.data.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                if (ev == GlyphToy.EVENT_AOD) startLoop()
            } else super.handleMessage(msg)
        }
    }
    private var running = false
    private var tick = 0
    private val loop = object : Runnable {
        override fun run() {
            val code = OtpStore.fresh(this@OtpToyService)
            GlyphDisplay.toyShow(this@OtpToyService, code, tick++)
            if (code != null) handler.postDelayed(this, GlyphDisplay.SCROLL_MS) else running = false
        }
    }
    private fun startLoop() { if (!running) { running = true; handler.post(loop) } }
    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent): IBinder {
        GlyphDisplay.toyStart(this)
        handler.postDelayed({ startLoop() }, 800)
        return messenger.binder
    }

    override fun onUnbind(intent: Intent): Boolean { handler.removeCallbacks(loop); running = false; GlyphDisplay.toyStop(); return false }
}
