package com.glyphotp

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject

object GlyphDisplay {
    // 3x5 pixel digits, one row per Int (3 bits).
    private val FONT = arrayOf(
        intArrayOf(7,5,5,5,7), intArrayOf(2,6,2,2,7), intArrayOf(7,1,7,4,7), intArrayOf(7,1,7,1,7), intArrayOf(5,5,7,1,1),
        intArrayOf(7,4,7,1,7), intArrayOf(7,4,7,5,7), intArrayOf(7,1,1,1,1), intArrayOf(7,5,7,5,7), intArrayOf(7,5,7,1,7)
    )
    private val main = Handler(Looper.getMainLooper())
    private var users = 0
    private var gm: GlyphMatrixManager? = null
    private var ready = false
    private val pending = ArrayList<(GlyphMatrixManager) -> Unit>()

    private fun target() = if (Common.is25111p()) Glyph.DEVICE_25111p else Glyph.DEVICE_23112

    /** Square bitmap with the code as pixels: up to 3 digits per row, two rows. */
    fun bitmapFor(code: String): Bitmap {
        val n = Common.getDeviceMatrixLength().coerceAtLeast(13)
        val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val rows = if (code.length <= 3) listOf(code) else listOf(code.take((code.length + 1) / 2), code.drop((code.length + 1) / 2))
        val totalH = rows.size * 5 + (rows.size - 1) * 2
        var y0 = (n - totalH) / 2
        for (row in rows) {
            val w = row.length * 4 - 1
            var x0 = (n - w) / 2
            for (ch in row) {
                val g = FONT[ch - '0']
                for (r in 0 until 5) for (c in 0 until 3)
                    if (g[r] and (4 shr c) != 0) bmp.setPixel(x0 + c, y0 + r, Color.WHITE)
                x0 += 4
            }
            y0 += 7
        }
        return bmp
    }

    private fun frame(ctx: Context, bmp: Bitmap): IntArray {
        val obj = GlyphMatrixObject.Builder().setImageSource(bmp).setPosition(0, 0).setBrightness(255).build()
        return GlyphMatrixFrame.Builder().addTop(obj).build(ctx).render()
    }

    @Synchronized private fun acquire(ctx: Context, then: (GlyphMatrixManager) -> Unit) {
        users++
        if (ready) { then(gm!!); return }
        pending.add(then)
        if (gm != null) return
        val m = GlyphMatrixManager.getInstance(ctx.applicationContext)
        gm = m
        m.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: ComponentName) {
                m.register(target())
                ready = true
                val todo = synchronized(this@GlyphDisplay) { pending.toList().also { pending.clear() } }
                todo.forEach { it(m) }
            }
            override fun onServiceDisconnected(name: ComponentName) { ready = false }
        })
    }

    @Synchronized private fun release() {
        users--
        if (users <= 0) { users = 0; try { gm?.unInit() } catch (_: Exception) {}; gm = null; ready = false; pending.clear() }
    }

    /** From the notification listener: show the code for [ms], then hand the matrix back. */
    fun showApp(ctx: Context, code: String, ms: Long = 30_000) {
        val app = ctx.applicationContext
        acquire(app) { m ->
            try { m.setAppMatrixFrame(frame(app, bitmapFor(code))) } catch (e: Exception) { e.printStackTrace() }
            main.postDelayed({
                try { m.closeAppMatrix() } catch (_: Exception) {}
                release()
            }, ms)
        }
    }

    // Toy service helpers (always-on glyph toy).
    fun toyStart(ctx: Context) = acquire(ctx.applicationContext) {}
    fun toyStop() = release()
    fun toyShow(ctx: Context, code: String?) {
        val m = gm ?: return
        if (!ready) return
        try {
            val bmp = if (code != null) bitmapFor(code) else Bitmap.createBitmap(13, 13, Bitmap.Config.ARGB_8888)
            m.setMatrixFrame(frame(ctx.applicationContext, bmp))
        } catch (e: Exception) { e.printStackTrace() }
    }
}
