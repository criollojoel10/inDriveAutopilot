package dev.joel.indriveautopilot.hooks

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import dev.joel.indriveautopilot.logic.Humanizer // <-- ESTA LÍNEA ES LA SOLUCIÓN
import dev.joel.indriveautopilot.logic.Parsers
import dev.joel.indriveautopilot.logic.RuleEngine

object ActivityHooks {

    private const val SN_ACTIVITY = "sinet.startup.inDriver.city.driver.sn.refactor.ui.SnActivity"
    private const val DRIVER_ACTIVITY = "sinet.startup.inDriver.city.driver.ui.DriverActivity"

    fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookOnResume(lpparam.classLoader, SN_ACTIVITY)
        runCatching { hookOnResume(lpparam.classLoader, DRIVER_ACTIVITY) }
    }

    private fun hookOnResume(cl: ClassLoader, className: String) {
        val cls = XposedHelpers.findClassIfExists(className, cl) ?: return
        XposedHelpers.findAndHookMethod(cls, "onResume", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val act = param.thisObject as Activity
                Handler(Looper.getMainLooper()).postDelayed({
                    try { onActivityReady(act) }
                    catch (t: Throwable) {
                        XposedBridge.log("[Autopilot][onActivityReady][ERROR] ${t.stackTraceToString()}")
                    }
                }, 200)
            }
        })
        XposedBridge.log("[Autopilot] Hooked onResume: $className")
    }

    private fun onActivityReady(activity: Activity) {
        val rule = RuleEngine.fromPrefs(activity)

        // Opcional: auto-abrir candidatos del feed
        if (rule.autoOpenFeedItems) runCatching { tryAutoOpenFeedItem(activity, rule) }

        // Evaluar BottomSheet de oferta
        runCatching { tryAcceptFromBottomSheet(activity, rule) }
    }

    // ---------- Auto-abrir ítems del feed (prefiltro ligero) ----------
    private fun tryAutoOpenFeedItem(activity: Activity, rule: RuleEngine) {
        val root = activity.window?.decorView ?: return
        val recycler = Parsers.findViewByIdName(root, "orders_main_recyclerview_orders") ?: return
        val items = Parsers.findAllByIdName(root, "item_order_container")
        if (items.isEmpty()) return

        val candidate = items.firstOrNull { item ->
            // Prefiltro sin pickup ni paradas; eso se valida en BottomSheet
            val rating = Parsers.readTextByIdIn(item, "driver_common_textview_rating")?.let(Parsers::parseRating)
            val reviews = Parsers.readTextByIdIn(item, "driver_common_textview_rating_rides_done")?.let(Parsers::parseReviews)
            val price = Parsers.readTextByIdIn(item, "info_textview_stage_price_view")?.let(Parsers::parsePriceUsd)
            val dReal = Parsers.readTextByIdIn(item, "order_info_stage_textview_distance")?.let(Parsers::parseDistanceKm)
            rule.preFilterFeed(rating, reviews, price, dReal)
        }

        if (candidate != null) {
            Humanizer.clickView(candidate)
            XposedBridge.log("[Autopilot] Feed: abriendo candidato…")
        }
    }

    // ---------- Aceptar desde BottomSheet (decisión final) ----------
    private fun tryAcceptFromBottomSheet(activity: Activity, rule: RuleEngine) {
        val root = activity.window?.decorView as? ViewGroup ?: return

        // Datos
        val price = Parsers.parsePriceUsd(Parsers.findPriceText(root))
        val rating = Parsers.parseRating(Parsers.findRatingText(root))
        val reviews = Parsers.parseReviews(Parsers.findReviewsText(root))
        val pickupKm = Parsers.parseDistanceKm(Parsers.findPickupText(root))
        val dReal = Parsers.parseDistanceKm(Parsers.findStageDistanceText(root))

        // Paradas
        val stopsInfo = Parsers.detectStopsInfo(root)
        val hasMultipleStops = stopsInfo.multipleStops
        val stopsKnown = stopsInfo.known

        val verdict = rule.evaluate(rating, reviews, pickupKm, price, dReal, hasMultipleStops, stopsKnown)
        if (!verdict.accept) {
            if (rule.showRejectToast) {
                Toast.makeText(activity, verdict.reason ?: "No cumple reglas", Toast.LENGTH_SHORT).show()
            }
            if (rule.autoCloseOnReject) {
                // opcional apagado por defecto: intento de cierre suave (buscar botón 'Omitir' o similar)
                val skipBtn = Parsers.findButtonByText(root, listOf("Omitir", "Cerrar", "Skip", "Dismiss"))
                if (skipBtn != null) Humanizer.clickView(skipBtn)
            }
            return
        }

        val acceptBtn = findAcceptButton(root)
        if (acceptBtn != null) {
            Humanizer.humanClick(acceptBtn, activity)
            XposedBridge.log("[Autopilot] Oferta ACEPTADA")

            // Enviar LOG (Broadcast a nuestro proceso)
            if (rule.logEnabled) {
                val intent = Intent("dev.joel.indriveautopilot.LOG_ACCEPT").apply {
                    putExtra("price", price ?: -1.0)
                    putExtra("rating", rating ?: -1.0)
                    putExtra("reviews", reviews ?: -1)
                    putExtra("pickupKm", pickupKm ?: -1.0)
                    putExtra("dReal", dReal ?: -1.0)
                    putExtra("dRounded", verdict.dRounded)
                    putExtra("tier", verdict.tier) // "A","B","C"
                    putExtra("minCalc", verdict.minRequired)
                    putExtra("tolerance", verdict.toleranceApplied)
                    putExtra("source", verdict.source) // "SN"/"FEED" si más adelante lo clasificas por origen
                }
                activity.sendBroadcast(intent)
            }
        } else {
            XposedBridge.log("[Autopilot] Botón Aceptar no encontrado")
        }
    }

    private fun findAcceptButton(root: View): Button? {
        val targets = listOf("Aceptar", "Accept", "Tomar")
        Parsers.findButtonByText(root, targets)?.let { return it }
        val sheet = Parsers.findViewByIdName(root, "design_bottom_sheet") as? ViewGroup
        if (sheet != null) return Parsers.findLastButton(sheet)
        return Parsers.findLastButton(root)
    }
}
