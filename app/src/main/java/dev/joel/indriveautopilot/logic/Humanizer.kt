package dev.joel.indriveautopilot.logic

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.preference.PreferenceManager
import de.robv.android.xposed.XposedBridge
import kotlin.math.max
import kotlin.random.Random

object Humanizer {
    private var lastClickAt = 0L

    fun humanClick(v: View, ctx: Context) {
        val p = PreferenceManager.getDefaultSharedPreferences(ctx)
        val min = p.getString("delayMinMs", "700")!!.toLong()
        val maxD = p.getString("delayMaxMs", "1800")!!.toLong()
        val jitter = p.getString("jitterPct", "15")!!.toInt()
        val rate = p.getString("rateLimitSec", "20")!!.toLong()

        val now = System.currentTimeMillis()
        if (now - lastClickAt < rate * 1000) {
            XposedBridge.log("[Humanizer] Skip: rate-limit")
            return
        }

        val base = if (maxD > min) Random.nextLong(min, maxD) else min
        val jitterMs = (base * (jitter / 100.0)).toLong()
        val finalDelay = base + Random.nextLong(-jitterMs, jitterMs + 1)

        Handler(Looper.getMainLooper()).postDelayed({
            clickView(v)
            lastClickAt = System.currentTimeMillis()
        }, max(0L, finalDelay))
    }

    fun clickView(v: View) {
        runCatching { v.performClick() }
            .onFailure { XposedBridge.log("[Humanizer] performClick fallo: ${it.message}") }
    }
}