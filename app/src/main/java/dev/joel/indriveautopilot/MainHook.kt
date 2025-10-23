package dev.joel.indriveautopilot

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import dev.joel.indriveautopilot.hooks.ActivityHooks
import de.robv.android.xposed.XposedBridge

class MainHook : IXposedHookLoadPackage {
    private val targetPkg = "sinet.startup.inDriver"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != targetPkg) return
        try {
            ActivityHooks.install(lpparam)
            XposedBridge.log("[Autopilot] Hooks instalados en ${lpparam.packageName}")
        } catch (t: Throwable) {
            XposedBridge.log("[Autopilot][ERROR] ${t.stackTraceToString()}")
        }
    }
}
