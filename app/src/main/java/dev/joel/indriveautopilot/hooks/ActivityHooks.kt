package dev.joel.indriveautopilot.hooks

import android.app.Activity
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object ActivityHooks {

    // Ajusta estos nombres si tu app muestra otro nombre de clase en dumpsys/logcat
    private const val SN_ACTIVITY =
        "sinet.startup.inDriver.city.driver.sn.refactor.ui.SnActivity"
    private const val DRIVER_ACTIVITY =
        "sinet.startup.inDriver.city.driver.ui.DriverActivity"

    fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 1) Enganchar el onResume de la superclase android.app.Activity
        val actCls = XposedHelpers.findClassIfExists("android.app.Activity", lpparam.classLoader)
            ?: run {
                XposedBridge.log("[Autopilot][ERROR] android.app.Activity no encontrado")
                return
            }

        // Hook común para onResume y onPostResume
        val hook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val act = param.thisObject as Activity
                val name = act.javaClass.name
                if (name == SN_ACTIVITY || name == DRIVER_ACTIVITY) {
                    // Log de confirmación del enganche
                    XposedBridge.log("[Autopilot] onResume/onPostResume hit: $name")
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            onActivityReady(act) // ← tu lógica actual (no la cambies)
                        } catch (t: Throwable) {
                            XposedBridge.log("[Autopilot][onActivityReady][ERROR] ${t.stackTraceToString()}")
                        }
                    }, 200)
                }
            }
        }

        // 2) Hook en onResume y respaldo en onPostResume
        XposedHelpers.findAndHookMethod(actCls, "onResume", hook)
        XposedHelpers.findAndHookMethod(actCls, "onPostResume", hook)

        XposedBridge.log("[Autopilot] Hooks instalados (android.app.Activity.onResume/onPostResume)")
    }

    // Mantén tu implementación existente
    private fun onActivityReady(activity: Activity) {
        // ... tu código actual: extraer UI, evaluar reglas, humanizer, aceptar, logs, etc.
    }
}
