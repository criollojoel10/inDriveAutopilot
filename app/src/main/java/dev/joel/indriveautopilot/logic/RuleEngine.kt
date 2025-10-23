package dev.joel.indriveautopilot.logic

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import de.robv.android.xposed.XSharedPreferences
import kotlin.math.roundToInt

data class Verdict(
    val accept: Boolean,
    val reason: String? = null,
    val dRounded: Int = -1,
    val tier: String = "",            // "A","B","C"
    val minRequired: Double = -1.0,
    val toleranceApplied: Double = 0.0,
    val source: String = "SN"         // opcional
)

data class RuleConfig(
    val minRating: Double,
    val minReviews: Int,
    val maxPickupKm: Double,
    val dMaxKm: Double,

    val minFixedUnder3: Double,       // A: <3.0 -> $1.00
    val minFixed3to34: Double,        // B: 3.0..3.4 -> $1.20
    val perKmFrom35: Double,          // C: >=3.5 -> $/km (con redondeo)
    val tolFrom35: Double,            // C: tolerancia $0.10
    val tolUnder3Enabled: Boolean,
    val tolUnder3: Double,
    val roundHalfUp: Boolean,

    val allowUnknownReviewsCount: Boolean,
    val allowUnknownStopsCount: Boolean,

    // Comportamiento
    val autoOpenFeedItems: Boolean,
    val showRejectToast: Boolean,
    val autoCloseOnReject: Boolean,
    val useAccessibilityFallback: Boolean,
    val nightPause: Boolean,
    val panicGesture: Boolean,
    val batterySaver: Boolean,

    // Humanizer
    val delayMinMs: Long,
    val delayMaxMs: Long,
    val jitterPct: Int,
    val rateLimitSec: Long,

    // Logs
    val logEnabled: Boolean,
    val logTotals: Boolean
)

class RuleEngine(private val cfg: RuleConfig) {

    // Prefiltro en feed (sin pickup ni paradas)
    fun preFilterFeed(rating: Double?, reviews: Int?, price: Double?, dReal: Double?): Boolean {
        if (rating == null || price == null || dReal == null) return false
        if (rating < cfg.minRating) return false
        if (reviews == null && !cfg.allowUnknownReviewsCount) return false
        if (reviews != null && reviews < cfg.minReviews) return false
        if (dReal > cfg.dMaxKm) return false
        val (minReq, tol, _, tier) = minPriceForD(dReal)
        return price + tol >= minReq
    }

    // Evaluación final
    fun evaluate(
        rating: Double?, reviews: Int?, pickupKm: Double?, price: Double?, dReal: Double?,
        hasMultipleStops: Boolean, stopsKnown: Boolean
    ): Verdict {
        // Datos críticos
        if (price == null || dReal == null || pickupKm == null) {
            return Verdict(accept = false, reason = "Datos incompletos")
        }

        // Límite de distancia real
        if (dReal > cfg.dMaxKm) {
            return Verdict(accept = false, reason = "D > ${cfg.dMaxKm}")
        }

        // Paradas
        if (stopsKnown && hasMultipleStops && !cfg.allowUnknownStopsCount) {
            return Verdict(accept = false, reason = "Múltiples paradas")
        } else if (!stopsKnown && !cfg.allowUnknownStopsCount) {
            return Verdict(accept = false, reason = "Paradas desconocidas")
        }

        // Pickup
        if (pickupKm > cfg.maxPickupKm) {
            return Verdict(accept = false, reason = "Pickup > ${cfg.maxPickupKm} km")
        }

        // Rating / reviews
        if (rating == null || rating < cfg.minRating) {
            return Verdict(accept = false, reason = "Rating bajo")
        }
        if (reviews == null && !cfg.allowUnknownReviewsCount) {
            return Verdict(accept = false, reason = "Sin conteo de evaluaciones")
        }
        if (reviews != null && reviews < cfg.minReviews) {
            return Verdict(accept = false, reason = "Pocas evaluaciones")
        }

        // Precio por tramo
        val (minReq, tol, dRounded, tier) = minPriceForD(dReal)
        val ok = price + tol >= minReq
        return if (ok) {
            Verdict(
                accept = true, reason = null,
                dRounded = dRounded, tier = tier, minRequired = minReq, toleranceApplied = tol
            )
        } else {
            Verdict(
                accept = false, reason = "Precio insuficiente",
                dRounded = dRounded, tier = tier, minRequired = minReq, toleranceApplied = tol
            )
        }
    }

    private fun minPriceForD(dReal: Double): Quad {
        // Tier A: < 3.0
        return if (dReal < 3.0) {
            val min = cfg.minFixedUnder3
            val tol = if (cfg.tolUnder3Enabled) -cfg.tolUnder3 else 0.0
            Quad(min, tol, dRounded = dReal.roundHalfUpInt(cfg.roundHalfUp), tier = "A")
        }
        // Tier B: 3.0..3.4
        else if (dReal <= 3.4) {
            val min = cfg.minFixed3to34
            val tol = if (cfg.tolUnder3Enabled) -cfg.tolUnder3 else 0.0
            Quad(min, tol, dRounded = dReal.roundHalfUpInt(cfg.roundHalfUp), tier = "B")
        }
        // Tier C: >= 3.5
        else {
            val dR = dReal.roundHalfUpInt(cfg.roundHalfUp)
            val min = cfg.perKmFrom35 * dR
            val tol = -cfg.tolFrom35
            Quad(min, tol, dRounded = dR, tier = "C")
        }
    }

    data class Quad(val min: Double, val tol: Double, val dRounded: Int, val tier: String)

    private fun Double.roundHalfUpInt(enabled: Boolean): Int {
        if (!enabled) return this.toInt()
        val x = (this * 10).roundToInt() // .5 hacia arriba
        val whole = x / 10.0
        return kotlin.math.floor(whole + 0.5).toInt()
    }

    // ------------ Preferencias ------------
    companion object {
        private const val MODULE_PKG = "dev.joel.indriveautopilot"
        private const val PREF_FILE = "${MODULE_PKG}_preferences"

        fun fromPrefs(ctx: Activity): RuleEngine {
            val sp = tryXsp() ?: tryContextPrefs(ctx) ?: PreferenceManager.getDefaultSharedPreferences(ctx)
            return RuleEngine(
                RuleConfig(
                    minRating = sp.getString("minRating", "4.0")!!.toDouble(),
                    minReviews = sp.getString("minReviews", "15")!!.toInt(),
                    maxPickupKm = sp.getString("maxPickupKm", "1.5")!!.toDouble(),
                    dMaxKm = sp.getString("dMaxKm", "6.0")!!.toDouble(),

                    minFixedUnder3 = sp.getString("minFixedUnder3", "1.00")!!.toDouble(),
                    minFixed3to34  = sp.getString("minFixed3to34", "1.20")!!.toDouble(),
                    perKmFrom35    = sp.getString("perKmFrom35", "0.40")!!.toDouble(),
                    tolFrom35      = sp.getString("tolFrom35", "0.10")!!.toDouble(),
                    tolUnder3Enabled = sp.getBoolean("toleranceOnUnder3", false),
                    tolUnder3        = sp.getString("tolUnder3", "0.00")!!.toDouble(),
                    roundHalfUp    = sp.getBoolean("roundHalfUp", true),

                    allowUnknownReviewsCount = sp.getBoolean("allowUnknownReviewsCount", false),
                    allowUnknownStopsCount   = sp.getBoolean("allowUnknownStopsCount", false),

                    autoOpenFeedItems    = sp.getBoolean("autoOpenFeedItems", false),
                    showRejectToast      = sp.getBoolean("showRejectToast", false),
                    autoCloseOnReject    = sp.getBoolean("autoCloseOnReject", false),
                    useAccessibilityFallback = sp.getBoolean("useAccessibilityFallback", false),
                    nightPause           = sp.getBoolean("nightPause", false),
                    panicGesture         = sp.getBoolean("panicGesture", false),
                    batterySaver         = sp.getBoolean("batterySaver", false),

                    delayMinMs = sp.getString("delayMinMs", "700")!!.toLong(),
                    delayMaxMs = sp.getString("delayMaxMs", "1800")!!.toLong(),
                    jitterPct  = sp.getString("jitterPct", "15")!!.toInt(),
                    rateLimitSec = sp.getString("rateLimitSec", "20")!!.toLong(),

                    logEnabled = sp.getBoolean("logEnabled", true),
                    logTotals  = sp.getBoolean("logTotals", true)
                )
            )
        }

        private fun tryXsp(): SharedPreferences? {
            return try {
                val xsp = XSharedPreferences(MODULE_PKG, PREF_FILE)
                xsp.makeWorldReadable() // no-ops en LSPosed recientes
                xsp.reload()
                xsp
            } catch (_: Throwable) { null }
        }

        private fun tryContextPrefs(ctx: Context): SharedPreferences? {
            return try {
                val pc = ctx.createPackageContext(MODULE_PKG, Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE)
                PreferenceManager.getDefaultSharedPreferences(pc)
            } catch (_: Throwable) { null }
        }
    }

    // Exponer flags a quien los requiera
    val autoOpenFeedItems get() = cfg.autoOpenFeedItems
    val showRejectToast get() = cfg.showRejectToast
    val autoCloseOnReject get() = cfg.autoCloseOnReject
    val logEnabled get() = cfg.logEnabled
}