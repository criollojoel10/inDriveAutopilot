package dev.joel.indriveautopilot.log

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.joel.indriveautopilot.log.LogManager.appendAccepted
import dev.joel.indriveautopilot.log.LogManager.updateTotals

class LogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "dev.joel.indriveautopilot.LOG_ACCEPT") return

        val price = intent.getDoubleExtra("price", -1.0)
        val rating = intent.getDoubleExtra("rating", -1.0)
        val reviews = intent.getIntExtra("reviews", -1)
        val pickupKm = intent.getDoubleExtra("pickupKm", -1.0)
        val dReal = intent.getDoubleExtra("dReal", -1.0)
        val dRounded = intent.getIntExtra("dRounded", -1)
        val tier = intent.getStringExtra("tier") ?: ""
        val minCalc = intent.getDoubleExtra("minCalc", -1.0)
        val tolerance = intent.getDoubleExtra("tolerance", 0.0)
        val source = intent.getStringExtra("source") ?: "SN"

        val ok = appendAccepted(context, price, rating, reviews, pickupKm, dReal, dRounded, tier, minCalc, tolerance, source)
        if (ok) updateTotals(context, price)
    }
}