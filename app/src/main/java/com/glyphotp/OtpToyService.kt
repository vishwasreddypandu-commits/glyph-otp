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
                if (ev == GlyphToy.EVENT_AOD) GlyphDisplay.toyShow(this@OtpToyService, OtpStore.fresh(this@OtpToyService))
            } else super.handleMessage(msg)
        }
    }
    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent): IBinder {
        GlyphDisplay.toyStart(this)
        handler.postDelayed({ GlyphDisplay.toyShow(this, OtpStore.fresh(this)) }, 800)
        return messenger.binder
    }

    override fun onUnbind(intent: Intent): Boolean { GlyphDisplay.toyStop(); return false }
}
