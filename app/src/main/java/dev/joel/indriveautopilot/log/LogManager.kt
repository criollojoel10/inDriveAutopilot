package dev.joel.indriveautopilot.log

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

object LogManager {

    private fun nowIso(): String {
        val tz = java.util.TimeZone.getDefault()
        val offset = tz.getOffset(System.currentTimeMillis())
        val hours = offset / 3600000
        val minutes = (offset % 3600000) / 60000
        val sign = if (offset >= 0) "+" else "-"
        val hh = "%02d".format(kotlin.math.abs(hours))
        val mm = "%02d".format(kotlin.math.abs(minutes))
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        return fmt.format(Date()) + sign + hh + ":" + mm
    }

    private fun todayName(): String {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return f.format(Date())
    }

    private fun monthKey(): String {
        val f = SimpleDateFormat("yyyy-MM", Locale.US)
        return f.format(Date())
    }

    private fun ensureFolder(context: Context): DocumentFile? {
        val p = PreferenceManager.getDefaultSharedPreferences(context)
        val uriStr = p.getString("logFolderUri", null) ?: return null
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(uriStr)) ?: return null
        return if (tree.canWrite()) tree else null
    }

    fun appendAccepted(
        context: Context,
        price: Double, rating: Double, reviews: Int, pickupKm: Double, dReal: Double,
        dRounded: Int, tier: String, minCalc: Double, tolerance: Double, source: String
    ): Boolean {
        val p = PreferenceManager.getDefaultSharedPreferences(context)
        if (!p.getBoolean("logEnabled", true)) return false

        val folder = ensureFolder(context) ?: return false
        val fileName = "accepted_${todayName()}.csv"
        val csvHeader = "timestamp,price,rating,reviews,pickup_km,d_real,d_rounded,tier,min_calc,tolerance,source\n"
        val line = "${nowIso()},${fmt(price)},${fmt(rating)},${reviews},${fmt(pickupKm)},${fmt(dReal)},${dRounded},${tier},${fmt(minCalc)},${fmt(tolerance)},${source}\n"

        val file = folder.findFile(fileName) ?: folder.createFile("text/csv", fileName) ?: return false

        // Intentar append con "wa"; si falla, leer+reescribir
        val cr = context.contentResolver
        val ok = tryAppend(cr, file.uri, csvHeader, line)
        return ok
    }

    fun updateTotals(context: Context, price: Double) {
        val p = PreferenceManager.getDefaultSharedPreferences(context)
        if (!p.getBoolean("logTotals", true)) return

        val folder = ensureFolder(context) ?: return
        val file = folder.findFile("totals.json") ?: folder.createFile("application/json", "totals.json") ?: return

        val cr = context.contentResolver
        val obj = readJson(cr, file.uri) ?: JSONObject().apply {
            put("total_all_time", 0.0)
            put("total_by_day", JSONObject())
            put("total_by_month", JSONObject())
        }

        val dayKey = todayName()
        val monKey = monthKey()

        obj.put("total_all_time", obj.optDouble("total_all_time", 0.0) + price)

        val byDay = obj.optJSONObject("total_by_day") ?: JSONObject().also { obj.put("total_by_day", it) }
        byDay.put(dayKey, byDay.optDouble(dayKey, 0.0) + price)

        val byMon = obj.optJSONObject("total_by_month") ?: JSONObject().also { obj.put("total_by_month", it) }
        byMon.put(monKey, byMon.optDouble(monKey, 0.0) + price)

        writeText(cr, file.uri, obj.toString())
    }

    private fun tryAppend(cr: ContentResolver, uri: Uri, header: String, line: String): Boolean {
        return try {
            // Si el archivo está vacío, escribir cabecera
            val needsHeader = isEmpty(cr, uri)
            cr.openOutputStream(uri, "wa")!!.use { os ->
                OutputStreamWriter(os).use { w ->
                    if (needsHeader) w.write(header)
                    w.write(line)
                    w.flush()
                }
            }
            true
        } catch (_: Throwable) {
            // Fallback: leer + añadir y escribir
            val existing = readText(cr, uri)
            val newText = buildString {
                if (existing.isNullOrEmpty()) append(header)
                else append(existing)
                append(line)
            }
            writeText(cr, uri, newText)
            true
        }
    }

    private fun readJson(cr: ContentResolver, uri: Uri): JSONObject? {
        return try {
            val txt = readText(cr, uri) ?: return null
            if (txt.isBlank()) null else JSONObject(txt)
        } catch (_: Throwable) { null }
    }

    private fun isEmpty(cr: ContentResolver, uri: Uri): Boolean {
        return try {
            cr.openInputStream(uri)?.use { it.available() <= 0 } ?: true
        } catch (_: Throwable) { true }
    }

    private fun readText(cr: ContentResolver, uri: Uri): String? {
        return try {
            cr.openInputStream(uri)?.use { ins ->
                BufferedReader(InputStreamReader(ins)).readText()
            }
        } catch (_: Throwable) { null }
    }

    private fun writeText(cr: ContentResolver, uri: Uri, text: String) {
        cr.openOutputStream(uri, "rwt")!!.use { os ->
            OutputStreamWriter(os).use { w ->
                w.write(text)
                w.flush()
            }
        }
    }

    private fun fmt(v: Double): String = if (v < 0) "" else String.format(Locale.US, "%.2f", v)
}