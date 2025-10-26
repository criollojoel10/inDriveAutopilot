package dev.joel.indriveautopilot.logic

import android.content.SharedPreferences
import kotlin.math.floor

data class Verdict(
    val accept: Boolean,
    val reason: String? = null,
    val dRounded: Int = -1,
    val tier: String = "",            // "A","B","C"
    val minRequired: Double = -1.0,
    val toleranceApplied: Double = 0.0,
    val source: String = "SN"         // opcional: "SN" / "FEED"
)

data class RuleConfig(
    // --- Criterios generales ---
    val minRating: Double,
    val minReviews: Int,
    val maxPickupKm: Double,
    val dMaxKm: Double,

    // --- Política de precio (Joel) ---
    // Tramo A: D_real < 3.0 -> $1.00 (estricto, salvo tolUnder3Enabled)
    val minFixedUnder3: Double,
    // Tramo B: 3.0..3.4 -> $1.20 (estricto, salvo tolUnder3Enabled)
    val minFixed3to34: Double,
    // Tramo C: >=3.5 (usa D_redondeada .5↑) -> perKmFrom35 * Dred
    val perKmFrom35: Double,
    // Tramo C tolerancia (resta permitida)
    val tolFrom35: Double,
    // Tolerancia en A/B habilitada + valor
    val tolUnder3Enabled: Boolean,
    val tolUnder3: Double,
    // Redondeo .5 hacia arriba para D_redondeada
    val roundHalfUp: Boolean,

    // --- Lecturas flexibles ---
    val allowUnknownReviewsCount: Boolean,
    val allowUnknownStopsCount: Boolean,

    // --- Comportamiento (opcionales) ---
    val autoOpenFeedItems: Boolean,
    val showRejectToast: Boolean,
    val autoCloseOnReject: Boolean,
    val useAccessibilityFallback: Boolean,
    val nightPause: Boolean,
    val panicGesture: Boolean,
    val batterySaver: Boolean,

    // --- Humanizer ---
    val delayMinMs: Long,
    val delayMaxMs: Long,
    val jitterPct: Int,
    val rateLimitSec: Long,

    // --- Logs ---
    val logEnabled: Boolean,
    val logTotals: Boolean
)

class RuleEngine(private val cfg: RuleConfig) {

    // -------- Prefiltro en feed (sin pickup ni paradas) --------
    fun preFilterFeed(
        rating: Double?, reviews: Int?, price: Double?, dReal: Double?
    ): Boolean {
        if (rating == null || price == null || dReal == null) return false
        if (dReal > cfg.dMaxKm) return false
        if (rating < cfg.minRating) return false
        if (reviews == null && !cfg.allowUnknownReviewsCount) return false
        if (reviews != null && reviews < cfg.minReviews) return false

        val (minReq, tol, _, _) = minPriceForD(dReal)
        return price + tol >= minReq
    }

    // -------- Evaluación final (con pickup y paradas) --------
    fun evaluate(
        rating: Double?, reviews: Int?, pickupKm: Double?, price: Double?, dReal: Double?,
        hasMultipleStops: Boolean, stopsKnown: Boolean
    ): Verdict {
        // Datos críticos
        if (price == null || dReal == null || pickupKm == null) {
            return Verdict(accept = false, reason = "Datos incompletos")
        }

        // Límite de distancia REAL (no redondeada)
        if (dReal > cfg.dMaxKm) {
            return Verdict(accept = false, reason = "D > ${cfg.dMaxKm}")
        }

        // Paradas (un solo destino)
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
                accept = true,
                dRounded = dRounded,
                tier = tier,
                minRequired = minReq,
                toleranceApplied = tol
            )
        } else {
            Verdict(
                accept = false,
                reason = "Precio insuficiente",
                dRounded = dRounded,
                tier = tier,
                minRequired = minReq,
                toleranceApplied = tol
            )
        }
    }

    // -------- Cálculo de precio mínimo por D --------
    private fun minPriceForD(dReal: Double): TierCalc {
        // Tramo A: D_real < 3.0 km -> $1.00 (estricto salvo tolUnder3Enabled)
        if (dReal < 3.0) {
            val min = cfg.minFixedUnder3
            val tol = if (cfg.tolUnder3Enabled) -cfg.tolUnder3 else 0.0
            val dR = dReal.roundHalfUpInt(cfg.roundHalfUp)
            return TierCalc(min, tol, dR, "A")
        }
        // Tramo B: 3.0..3.4 km -> $1.20 (estricto salvo tolUnder3Enabled)
        if (dReal <= 3.4) {
            val min = cfg.minFixed3to34
            val tol = if (cfg.tolUnder3Enabled) -cfg.tolUnder3 else 0.0
            val dR = dReal.roundHalfUpInt(cfg.roundHalfUp)
            return TierCalc(min, tol, dR, "B")
        }
        // Tramo C: >= 3.5 km -> perKmFrom35 * D_redondeada (.5 hacia arriba)
        val dR = dReal.roundHalfUpInt(cfg.roundHalfUp)
        val min = cfg.perKmFrom35 * dR
        val tol = -cfg.tolFrom35
        return TierCalc(min, tol, dR, "C")
    }

    private data class TierCalc(
        val min: Double,
        val tol: Double,
        val dRounded: Int,
        val tier: String
    )

    // Redondeo .5 hacia arriba a entero (3.5->4, 3.4->3)
    private fun Double.roundHalfUpInt(enabled: Boolean): Int {
        if (!enabled) return floor(this).toInt()
        return floor(this + 0.5).toInt()
    }

    // -------- Exponer flags a otros componentes --------
    val autoOpenFeedItems get() = cfg.autoOpenFeedItems
    val showRejectToast get() = cfg.showRejectToast
    val autoCloseOnReject get() = cfg.autoCloseOnReject
    val logEnabled get() = cfg.logEnabled
    val logTotals get() = cfg.logTotals

    // ===================== Preferencias (Modern API / SharedPreferences) =====================
    companion object {
        /**
         * Construye un RuleEngine leyendo desde SharedPreferences.
         * En Modern API, obtén preferencias remotas del módulo con:
         *   val prefs = module.getRemotePreferences("dev.joel.indriveautopilot")
         * y pásalas a este método.
         */
        fun fromPrefs(prefs: SharedPreferences): RuleEngine {
            // --- Criterios generales ---
            val minRating   = prefs.getString("minRating", "4.0")!!.toDouble()
            val minReviews  = prefs.getString("minReviews", "15")!!.toInt()
            val maxPickupKm = prefs.getString("maxPickupKm", "1.5")!!.toDouble()
            val dMaxKm      = prefs.getString("dMaxKm", "6.0")!!.toDouble()

            // --- Política de precio ---
            val minFixedUnder3 = prefs.getString("minFixedUnder3", "1.00")!!.toDouble()
            val minFixed3to34  = prefs.getString("minFixed3to34", "1.20")!!.toDouble()
            val perKmFrom35    = prefs.getString("perKmFrom35", "0.40")!!.toDouble()
            val tolFrom35      = prefs.getString("tolFrom35", "0.10")!!.toDouble()
            val tolUnder3Enabled = prefs.getBoolean("toleranceOnUnder3", false)
            val tolUnder3        = prefs.getString("tolUnder3", "0.00")!!.toDouble()
            val roundHalfUp      = prefs.getBoolean("roundHalfUp", true)

            // --- Lecturas flexibles ---
            val allowUnknownReviewsCount = prefs.getBoolean("allowUnknownReviewsCount", false)
            val allowUnknownStopsCount   = prefs.getBoolean("allowUnknownStopsCount", false)

            // --- Comportamiento ---
            val autoOpenFeedItems    = prefs.getBoolean("autoOpenFeedItems", false)
            val showRejectToast      = prefs.getBoolean("showRejectToast", false)
            val autoCloseOnReject    = prefs.getBoolean("autoCloseOnReject", false)
            val useAccessibilityFallback = prefs.getBoolean("useAccessibilityFallback", false)
            val nightPause           = prefs.getBoolean("nightPause", false)
            val panicGesture         = prefs.getBoolean("panicGesture", false)
            val batterySaver         = prefs.getBoolean("batterySaver", false)

            // --- Humanizer ---
            val delayMinMs   = prefs.getString("delayMinMs", "700")!!.toLong()
            val delayMaxMs   = prefs.getString("delayMaxMs", "1800")!!.toLong()
            val jitterPct    = prefs.getString("jitterPct", "15")!!.toInt()
            val rateLimitSec = prefs.getString("rateLimitSec", "20")!!.toLong()

            // --- Logs ---
            val logEnabled = prefs.getBoolean("logEnabled", true)
            val logTotals  = prefs.getBoolean("logTotals", true)

            return RuleEngine(
                RuleConfig(
                    // generales
                    minRating, minReviews, maxPickupKm, dMaxKm,
                    // precio
                    minFixedUnder3, minFixed3to34, perKmFrom35, tolFrom35,
                    tolUnder3Enabled, tolUnder3, roundHalfUp,
                    // flex
                    allowUnknownReviewsCount, allowUnknownStopsCount,
                    // comportamiento
                    autoOpenFeedItems, showRejectToast, autoCloseOnReject,
                    useAccessibilityFallback, nightPause, panicGesture, batterySaver,
                    // humanizer
                    delayMinMs, delayMaxMs, jitterPct, rateLimitSec,
                    // logs
                    logEnabled, logTotals
                )
            )
        }
    }
}
