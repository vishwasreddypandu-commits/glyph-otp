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

    const val SCROLL_MS = 110L
    private const val PAUSE_STEPS = 6

    /** Total scroll steps for a code: it enters from the right edge and leaves through the left edge. */
    fun steps(code: String, n: Int = 13) = n + code.length * 4 - 1 + 1

    /** One scroll frame. The matrix is round, so a single row in the middle is the widest usable strip. */
    fun bitmapFor(code: String, step: Int): Bitmap {
        val n = Common.getDeviceMatrixLength().coerceAtLeast(13)
        val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val y0 = (n - 5) / 2
        for ((i, ch) in code.withIndex()) {
            val x0 = n - step + i * 4
            val g = FONT[ch - '0']
            for (r in 0 until 5) for (c in 0 until 3) {
                val x = x0 + c
                if (x in 0 until n && g[r] and (4 shr c) != 0) bmp.setPixel(x, y0 + r, Color.WHITE)
            }
        }
        return bmp
    }

    /** Frame index for a running animation: scroll through, then pause blank briefly, repeat. */

    private var cacheCode: String? = null
    private var cacheFrames: List<IntArray> = emptyList()
    /** All scroll frames rendered up front, so nothing heavy happens between frames (that caused flicker). */
    @Synchronized fun framesFor(ctx: Context, code: String): List<IntArray> {
        if (cacheCode != code) {
            cacheFrames = (0 until steps(code) + PAUSE_STEPS).map { i ->
                val step = if (i < steps(code)) i else 0 // pause shows an empty strip
                frame(ctx.applicationContext, bitmapFor(code, step))
            }
            cacheCode = code
        }
        return cacheFrames
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

    private var job: Runnable? = null
    private var jobEnd: Runnable? = null

    private fun stopJob() {
        job?.let { main.removeCallbacks(it) }; jobEnd?.let { main.removeCallbacks(it) }
        job = null; jobEnd = null
    }

    /** From the notification listener: show the code for [ms], then hand the matrix back. */
    fun showApp(ctx: Context, code: String, ms: Long = 30_000) {
        val app = ctx.applicationContext
        main.post { stopJob() } // a newer code replaces any running one
        val frames = framesFor(app, code)
        val blank = frame(app, Bitmap.createBitmap(13, 13, Bitmap.Config.ARGB_8888))
        acquire(app) { m ->
            main.post {
                stopJob()
                val start = android.os.SystemClock.uptimeMillis()
                val tick = object : Runnable {
                    override fun run() {
                        if (job !== this) return
                        val n = (android.os.SystemClock.uptimeMillis() - start) / SCROLL_MS
                        try { m.setAppMatrixFrame(frames[(n % frames.size).toInt()]) } catch (e: Exception) { e.printStackTrace() }
                        main.postAtTime(this, start + (n + 1) * SCROLL_MS)
                    }
                }
                val end = Runnable {
                    stopJob()
                    try { m.setAppMatrixFrame(blank) } catch (_: Exception) {}
                    try { m.closeAppMatrix() } catch (_: Exception) {}
                    release()
                }
                job = tick; jobEnd = end
                main.post(tick)
                main.postAtTime(end, start + ms)
            }
        }
    }

    // Toy service helpers (always-on glyph toy).
    fun toyStart(ctx: Context) = acquire(ctx.applicationContext) {}
    fun toyStop() = release()
    fun toyShow(ctx: Context, code: String?, tick: Int = 0) {
        val m = gm ?: return
        if (!ready) return
        try {
            if (code != null) { val f = framesFor(ctx, code); m.setMatrixFrame(f[tick % f.size]) }
            else m.setMatrixFrame(frame(ctx.applicationContext, Bitmap.createBitmap(13, 13, Bitmap.Config.ARGB_8888)))
        } catch (e: Exception) { e.printStackTrace() }
    }
}
