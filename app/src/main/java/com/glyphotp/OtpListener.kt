package com.glyphotp

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class OtpListener : NotificationListenerService() {
    companion object { private var lastCode: String? = null; private var lastAt = 0L }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val e = sbn.notification.extras
        val text = listOfNotNull(
            e.getCharSequence(Notification.EXTRA_TITLE),
            e.getCharSequence(Notification.EXTRA_TEXT),
            e.getCharSequence(Notification.EXTRA_BIG_TEXT)
        ).joinToString("\n")
        val code = OtpParser.extract(text) ?: return
        // Messaging apps re-post/update notifications; don't restart the animation for the same code.
        val now = System.currentTimeMillis()
        if (code == lastCode && now - lastAt < 120_000) return
        lastCode = code; lastAt = now
        OtpStore.save(this, code)
        GlyphDisplay.showApp(this, code)
    }
}
