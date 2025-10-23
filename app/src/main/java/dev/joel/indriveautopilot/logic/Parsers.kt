package dev.joel.indriveautopilot.logic

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

data class StopsInfo(val known: Boolean, val multipleStops: Boolean)

object Parsers {

    // ----- Búsquedas genéricas -----
    fun findViewByIdName(root: View, idName: String): View? =
        findAll(root).firstOrNull { v ->
            val entry = runCatching { v.resources.getResourceEntryName(v.id) }.getOrNull()
            entry == idName
        }

    fun findAllByIdName(root: View, idName: String): List<View> =
        findAll(root).filter { v ->
            val entry = runCatching { v.resources.getResourceEntryName(v.id) }.getOrNull()
            entry == idName
        }

    fun readTextByIdIn(node: View, idName: String): String? {
        val v = if (node is ViewGroup) findViewByIdName(node, idName) else null
        return (v as? TextView)?.text?.toString()?.trim()
    }

    private fun findAll(root: View): List<View> {
        val out = mutableListOf<View>()
        fun dfs(v: View) {
            out.add(v)
            if (v is ViewGroup) for (i in 0 until v.childCount) dfs(v.getChildAt(i))
        }
        dfs(root); return out
    }

    fun findButtonByText(root: View, texts: List<String>): Button? {
        val lowers = texts.map { it.lowercase() }
        for (v in findAll(root)) if (v is Button) {
            val t = v.text?.toString()?.trim() ?: continue
            if (lowers.contains(t.lowercase())) return v
        }
        return null
    }

    fun findLastButton(root: View): Button? =
        findAll(root).filterIsInstance<Button>().lastOrNull { it.isShown && it.isEnabled && it.isClickable }

    // ----- Extracción por ID/heurística -----
    fun findPriceText(root: View): String? {
        readTextByIdIn(root, "info_textview_stage_price_view")?.let { if (it.isNotBlank()) return it }
        val tx = findAll(root).filterIsInstance<TextView>().map { it.text?.toString() ?: "" }
        return tx.firstOrNull { PRICE_REGEX.containsMatchIn(it) }
    }

    fun findRatingText(root: View): String? {
        readTextByIdIn(root, "driver_common_textview_rating")?.let { if (it.isNotBlank()) return it }
        val tx = findAll(root).filterIsInstance<TextView>().map { it.text?.toString() ?: "" }
        return tx.firstOrNull { RATING_REGEX.containsMatchIn(it) }
    }

    fun findReviewsText(root: View): String? {
        readTextByIdIn(root, "driver_common_textview_rating_rides_done")?.let { if (it.isNotBlank()) return it }
        val tx = findAll(root).filterIsInstance<TextView>().map { it.text?.toString() ?: "" }
        return tx.firstOrNull { REVIEWS_REGEX.containsMatchIn(it) }
    }

    fun findPickupText(root: View): String? {
        val tx = findAll(root).filterIsInstance<TextView>().map { it.text?.toString() ?: "" }
        return tx.firstOrNull { DIST_REGEX.containsMatchIn(it.lowercase()) }
    }

    fun findStageDistanceText(root: View): String? {
        readTextByIdIn(root, "order_info_stage_textview_distance")?.let { if (it.isNotBlank()) return it }
        val tx = findAll(root).filterIsInstance<TextView>().map { it.text?.toString() ?: "" }
        return tx.firstOrNull { DIST_REGEX.containsMatchIn(it.lowercase()) }
    }

    // ----- Paradas (1 destino vs múltiples) -----
    fun detectStopsInfo(root: View): StopsInfo {
        // 1) Si existe un contenedor conocido (no tenemos id estable), usamos heurística por texto
        val toText = findAll(root).filterIsInstance<TextView>().mapNotNull { it.text?.toString()?.trim() }.firstOrNull {
            val entry = runCatching { it }.getOrNull()
            // No podemos usar resourceEntryName aquí; usamos contenido
            // Señales textuales de múltiples destinos:
            MULTI_SEP.any { sep -> it.contains(sep) } || it.count { ch -> ch == '\n' } >= 1
        }

        if (toText != null) {
            // Heurística textual: separadores fuertes
            val hasMulti = MULTI_SEP.any { toText.contains(it) } || toText.lines().size >= 2
            return StopsInfo(known = true, multipleStops = hasMulti)
        }

        // 2) Fallback: inspección de jerarquía. Si vemos varios bloques tipo dirección.
        val possibleBlocks = findAll(root).filterIsInstance<TextView>().filter { tv ->
            val s = tv.text?.toString()?.trim().orEmpty()
            s.isNotEmpty() && (ADDRESS_HINT.any { h -> s.contains(h, ignoreCase = true) } || s.length in 10..80)
        }
        if (possibleBlocks.size >= 2) return StopsInfo(known = true, multipleStops = true)

        return StopsInfo(known = false, multipleStops = false)
    }

    // ----- Parsers numéricos -----
    fun parsePriceUsd(text: String?): Double? {
        if (text.isNullOrBlank()) return null
        val m = PRICE_REGEX.find(text.replace('\u00A0', ' '))
        return m?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()
    }

    fun parseRating(text: String?): Double? {
        if (text.isNullOrBlank()) return null
        return text.replace('\u00A0', ' ').trim().replace(',', '.').toDoubleOrNull()
    }

    fun parseReviews(text: String?): Int? {
        if (text.isNullOrBlank()) return null
        val m = REVIEWS_REGEX.find(text.replace('\u00A0', ' '))
        return m?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    fun parseDistanceKm(text: String?): Double? {
        if (text.isNullOrBlank()) return null
        val norm = text.lowercase().replace('\u00A0', ' ').replace("~", "").trim()
        DIST_KM_REGEX.find(norm)?.let {
            return it.groupValues[1].replace(',', '.').toDoubleOrNull()
        }
        DIST_M_REGEX.find(norm)?.let {
            val m = it.groupValues[1].toDoubleOrNull() ?: return null
            return m / 1000.0
        }
        return null
    }

    private val PRICE_REGEX = Regex("""\$?\s*(\d+(?:[.,]\d{1,2})?)""")
    private val RATING_REGEX = Regex("""\b(\d+(?:[.,]\d+)?)\b""")
    private val REVIEWS_REGEX = Regex("""\((\d{1,4})\)""")
    private val DIST_REGEX = Regex("""\b(\d+(?:[.,]\d+)?)\s*(km|kil[oó]metro[s]?|m|metro[s]?)\b""")
    private val DIST_KM_REGEX = Regex("""(\d+(?:[.,]\d+)?)\s*(km|kil[oó]metro[s]?)""")
    private val DIST_M_REGEX = Regex("""(\d+)\s*(m|metro[s]?)""")

    private val MULTI_SEP = listOf("•", "·", ";", " → ", "->")
    private val ADDRESS_HINT = listOf("calle", "av.", "avenida", "&", " y ")
}