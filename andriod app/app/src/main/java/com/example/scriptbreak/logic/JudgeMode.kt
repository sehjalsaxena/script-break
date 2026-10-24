package com.example.scriptbreak.logic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.scriptbreak.ml.Payment
import com.example.scriptbreak.ml.Result
import com.example.scriptbreak.ml.ScamModel
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.sqrt

/** One of the 28 Round 1 behaviour stories (tests/cases.json). Never used to train the model. */
data class Story(
    val id: String,
    val kind: String,          // scam | boundary | legit
    val title: String,
    val payment: Payment,
    val q1: Boolean?,
    val q2: Boolean?,
    val round1Level: String,   // the level the Round 1 rules were designed to give
)

/** One scored row of a judge's test file. */
data class KitRow(
    val index: Int,
    val label: Int?,           // 1 scam, 0 genuine, null if the file has no labels
    val workflow: String?,
    val level: String,
    val score: Double,
    val amount: Double,
    val reasons: List<String>,
) {
    val flagged get() = level == "PAUSE" || level == "CRITICAL"
}

/** A share with a 95% Wilson interval, for honest error bars. */
data class Rate(val k: Int, val n: Int) {
    val rate get() = if (n == 0) 0.0 else k.toDouble() / n
    val low get() = wilson(-1)
    val high get() = wilson(1)
    private fun wilson(sign: Int): Double {
        if (n == 0) return 0.0
        val z = 1.96; val p = rate
        val d = 1 + z * z / n
        val c = (p + z * z / (2 * n)) / d
        val h = z * sqrt(p * (1 - p) / n + z * z / (4.0 * n * n)) / d
        return (c + sign * h).coerceIn(0.0, 1.0)
    }
}

data class KitResult(
    val fileName: String,
    val rows: List<KitRow>,
    val problems: List<String>, // rows that could not be read, with the reason
) {
    val hasLabels get() = rows.any { it.label != null }
    val levelCounts get() = listOf("NONE", "NUDGE", "PAUSE", "CRITICAL").associateWith { l -> rows.count { it.level == l } }
    val scamsFlagged get() = rows.filter { it.label == 1 }.let { s -> Rate(s.count { it.flagged }, s.size) }
    val genuineInterrupted get() = rows.filter { it.label == 0 }.let { g -> Rate(g.count { it.flagged }, g.size) }
    val genuineNudged get() = rows.filter { it.label == 0 }.let { g -> Rate(g.count { it.level == "NUDGE" }, g.size) }
    val missedScams get() = rows.filter { it.label == 1 && !it.flagged }
    val falseAlarms get() = rows.filter { it.label == 0 && it.flagged }
    val byWorkflow: Map<String, Rate>
        get() = rows.filter { it.workflow != null && it.label == 1 }.groupBy { it.workflow!! }
            .mapValues { (_, r) -> Rate(r.count { it.flagged }, r.size) }.toSortedMap()
}

/**
 * Judge mode: judges set any of the 16 inputs (or pick one of the 28 stories) and see what the
 * AI model + safety layer decide, live; or load their own test file and get error rates.
 * Uses exactly the same ScamModel.assess as the payment flow.
 */
class JudgeMode(private val model: ScamModel?, val stories: List<Story>) {
    var inputs by mutableStateOf(DEFAULT); private set
    var q1 by mutableStateOf<Boolean?>(null); private set
    var q2 by mutableStateOf<Boolean?>(null); private set
    var storyId by mutableStateOf<String?>(null); private set
    var tab by mutableIntStateOf(0)
    var kit by mutableStateOf<KitResult?>(null)
    var kitLoading by mutableStateOf(false)

    val modelVersion: String get() = model?.modelVersion ?: "unavailable"
    val result: Result? get() = assess(inputs, q1, q2)

    fun assess(p: Payment, q1: Boolean?, q2: Boolean?): Result? =
        try { model?.assess(p, q1, q2) } catch (e: Exception) { null }

    fun edit(change: (Payment) -> Payment) { inputs = change(inputs); storyId = null }
    fun setAnswers(q1: Boolean?, q2: Boolean?) { this.q1 = q1; this.q2 = q2 }
    fun reset() { inputs = DEFAULT; q1 = null; q2 = null; storyId = null }

    fun loadStory(s: Story) {
        inputs = s.payment; q1 = s.q1; q2 = s.q2; storyId = s.id; tab = 0
    }

    fun levelOf(s: Story): String = assess(s.payment, s.q1, s.q2)?.level ?: "-"

    /** Read and score a test file. Pure function of the text: safe to run off the main thread. */
    fun scoreKit(text: String, fileName: String): KitResult {
        val (records, problems) = KitParser.parse(text)
        val rows = records.mapIndexedNotNull { i, r ->
            val res = assess(r.payment, r.q1, r.q2) ?: return@mapIndexedNotNull null
            KitRow(i + 1, r.label, r.workflow, res.level, res.score, r.payment.amount, res.reasons)
        }
        return KitResult(fileName, rows, problems)
    }

    companion object {
        /** Sambhav on a call, paying ₹20,000 to a new payee: the demo starting point. */
        val DEFAULT = Payment(amount = 20000.0, median90d = 650.0, payments90d = 42, callNow = true, payeeStatus = "new")

        fun parseStories(json: String): List<Story> {
            val arr = JSONArray(json)
            return List(arr.length()) { i ->
                val c = arr.getJSONObject(i)
                val s = c.getJSONObject("session")
                Story(
                    id = c.getString("id"), kind = c.getString("kind"), title = c.getString("title"),
                    payment = Payment.fromJson(s),
                    q1 = if (s.has("q1") && !s.isNull("q1")) s.getBoolean("q1") else null,
                    q2 = if (s.has("q2") && !s.isNull("q2")) s.getBoolean("q2") else null,
                    round1Level = c.getString("expected"),
                )
            }
        }
    }
}

/** Reads judge test files: JSON array, {"records": [...]}, JSON Lines, or CSV with a header row. */
object KitParser {
    data class Parsed(val payment: Payment, val q1: Boolean?, val q2: Boolean?, val label: Int?, val workflow: String?)

    private val STATUSES = setOf("known", "new", "recent", "warned", "flagged")
    private val KINDS = setOf("person", "merchant", "biller")
    private val BOOLS = setOf("call_now", "call_seen_30m", "screen_share", "payee_is_sender", "payee_pasted", "app_switch", "night", "q1", "q2")
    private val INTS = setOf("payments_90d", "same_payee_24h", "new_payees_60m")
    private val NUMBERS = setOf("amount", "median_90d", "sent_new_24h", "credit_unknown_48h")

    fun parse(text: String): Pair<List<Parsed>, List<String>> {
        val t = text.trim().removePrefix("﻿")
        val objects: List<JSONObject> = when {
            t.startsWith("[") -> JSONArray(t).let { a -> List(a.length()) { a.getJSONObject(it) } }
            t.startsWith("{") && t.lines().count { it.isNotBlank() } > 1 && t.lines().filter { it.isNotBlank() }.all { it.trimStart().startsWith("{") && it.trimEnd().endsWith("}") } ->
                t.lines().filter { it.isNotBlank() }.map { JSONObject(it) }
            t.startsWith("{") -> JSONObject(t).let { o ->
                val key = listOf("records", "rows", "vectors", "payments").firstOrNull { o.has(it) }
                if (key != null) o.getJSONArray(key).let { a -> List(a.length()) { a.getJSONObject(it) } } else listOf(o)
            }
            else -> csvToJson(t)
        }
        val out = ArrayList<Parsed>(); val problems = ArrayList<String>()
        objects.forEachIndexed { i, rec ->
            val f = normalise(rec.optJSONObject("features") ?: rec)
            val status = f.optString("payee_status", "known"); val kind = f.optString("payee_kind", "person")
            when {
                !f.has("amount") || f.optDouble("amount", 0.0) <= 0 -> problems += "Row ${i + 1}: amount missing or not above 0"
                status !in STATUSES -> problems += "Row ${i + 1}: unknown payee_status \"$status\""
                kind !in KINDS -> problems += "Row ${i + 1}: unknown payee_kind \"$kind\""
                else -> out += Parsed(Payment.fromJson(f), triState(rec, f, "q1"), triState(rec, f, "q2"), label(rec), rec.optString("workflow").ifBlank { null })
            }
        }
        return out to problems
    }

    /** Accepts 1/0, "yes"/"no" and "TRUE" for yes/no inputs, and any case for payee status and kind. */
    private fun normalise(f: JSONObject): JSONObject {
        val o = JSONObject(f.toString())
        for (k in BOOLS) if (o.has(k) && !o.isNull(k) && o.get(k) !is Boolean) {
            if (k == "q1" || k == "q2") continue   // answers keep "not answered" (handled by triState)
            o.put(k, o.get(k).toString().trim().lowercase() in setOf("true", "1", "1.0", "yes"))
        }
        for (k in listOf("payee_status", "payee_kind")) if (o.has(k) && !o.isNull(k)) o.put(k, o.getString(k).trim().lowercase())
        return o
    }

    private fun triState(rec: JSONObject, f: JSONObject, key: String): Boolean? {
        val o = if (rec.has(key)) rec else if (f.has(key)) f else return null
        if (o.isNull(key)) return null
        return when (val v = o.get(key)) { is Boolean -> v; else -> v.toString().trim().lowercase() in setOf("true", "1", "yes") }
    }

    private fun label(rec: JSONObject): Int? {
        if (!rec.has("label") || rec.isNull("label")) return null
        return when (rec.get("label").toString().trim().lowercase()) {
            "1", "true", "scam", "fraud" -> 1
            "0", "false", "genuine", "legit" -> 0
            else -> null
        }
    }

    /** Simple CSV (comma-separated, header row, no commas inside values). */
    private fun csvToJson(t: String): List<JSONObject> {
        val lines = t.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val header = lines.first().split(',').map { it.trim().trim('"').lowercase() }
        return lines.drop(1).map { line ->
            val cells = line.split(',').map { it.trim().trim('"') }
            JSONObject().apply {
                header.forEachIndexed { j, h ->
                    val v = cells.getOrNull(j) ?: return@forEachIndexed
                    if (v.isEmpty()) return@forEachIndexed
                    when (h) {
                        in BOOLS -> put(h, v.lowercase() in setOf("true", "1", "yes"))
                        in INTS -> put(h, v.toDoubleOrNull()?.toInt() ?: 0)
                        in NUMBERS -> put(h, v.toDoubleOrNull() ?: 0.0)
                        else -> put(h, v)
                    }
                }
            }
        }
    }
}
