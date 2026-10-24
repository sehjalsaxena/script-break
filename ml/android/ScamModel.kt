package com.example.scriptbreak.ml

// ScriptBreak scam-score model for Android. Drop this file into the app module, put
// scam_model.json in app/src/main/assets/, and call:
//
//     val model = ScamModel.fromAssets(context)          // once, at app start
//     val result = model.assess(payment, q1 = null, q2 = null)
//     // result.level = NONE | NUDGE | PAUSE | CRITICAL, result.score, result.reasons
//
// It runs fully on the phone: no network, no ML library (org.json is built into Android).
// This is the same logic as ml/scorer.py + ml/policy.py, checked by ScamModelParityTest
// against test_vectors.json. If you change anything here, change the Python too.

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.expm1
import kotlin.math.ln1p
import kotlin.math.min

/** The facts the app collects about one payment. Field names match schema.json. */
data class Payment(
    val amount: Double,
    val median90d: Double = 0.0,
    val payments90d: Int = 50,
    val callNow: Boolean = false,
    val callSeen30m: Boolean = false,
    val screenShare: Boolean = false,
    val payeeStatus: String = "known",   // known | new | recent | warned | flagged
    val payeeKind: String = "person",    // person | merchant | biller
    val samePayee24h: Int = 0,
    val newPayees60m: Int = 0,
    val sentNew24h: Double = 0.0,
    val creditUnknown48h: Double = 0.0,
    val payeeIsSender: Boolean = false,
    val payeePasted: Boolean = false,
    val appSwitch: Boolean = false,
    val night: Boolean = false,
) {
    /** C1: any sign that someone may be steering this payment (same list as policy.py coaching_signs). */
    fun hasCoachingSign(): Boolean = callNow || callSeen30m || screenShare || payeePasted || appSwitch ||
        newPayees60m > 0 || sentNew24h > 0 || creditUnknown48h > 0 || samePayee24h > 0

    /** G5: a first payment to a new person, at least 30x the usual amount and at least Rs 20,000 (same as policy.py). */
    fun isBigFirstPaymentToPerson(): Boolean {
        val hasHistory = payments90d >= 10 && median90d > 0
        return (payeeStatus == "new" || payeeStatus == "recent") && payeeKind == "person" && hasHistory &&
            amount >= 30 * median90d && amount >= 20000
    }

    /** Event tokens, used to match rules approved in the analyst console. */
    fun tokens(): Set<String> = buildSet {
        if (callNow) add("call_now")
        if (callSeen30m) add("call_seen_30m")
        if (screenShare) add("screen_share")
        if (payeeStatus == "new" || payeeStatus == "recent") add("payee_new")
        if (payeeStatus == "warned") add("payee_warned")
        if (payeeStatus == "flagged") add("payee_flagged")
        if (payeeKind == "merchant") add("payee_merchant")
        if (payeeKind == "biller") add("payee_biller")
        // amount tokens: same definitions as round1/engine.py tokens()
        val hist = payments90d >= 10 && median90d > 0
        fun big(a: Double) = if (hist) a >= 2000 && a >= 5 * median90d else a >= 5000
        val isNew = payeeStatus == "new" || payeeStatus == "recent"
        if (big(amount)) add("amount_big")
        if (isNew && big(amount + sentNew24h) && !big(amount)) add("amount_cumulative")
        if (isNew && samePayee24h >= 1) add("repeat_same_payee")
        if (!hist) add("low_history")
        if (newPayees60m >= 2) add("many_new_payees")
        if (creditUnknown48h > 0) add("credit_unknown_48h")
        if (creditUnknown48h > 0 && abs(amount - creditUnknown48h) <= 0.02 * creditUnknown48h) add("amount_matches_credit")
        if (payeeIsSender) add("payee_is_sender")
        if (payeePasted) add("payee_pasted")
        if (appSwitch) add("app_switch")
        if (night) add("night")
    }

    companion object {
        /** Build from the JSON shape used in the datasets and the judge kit. */
        fun fromJson(o: JSONObject) = Payment(
            amount = o.optDouble("amount", 0.0), median90d = o.optDouble("median_90d", 0.0),
            payments90d = o.optInt("payments_90d", 50), callNow = o.optBoolean("call_now"),
            callSeen30m = o.optBoolean("call_seen_30m"), screenShare = o.optBoolean("screen_share"),
            payeeStatus = o.optString("payee_status", "known"), payeeKind = o.optString("payee_kind", "person"),
            samePayee24h = o.optInt("same_payee_24h"), newPayees60m = o.optInt("new_payees_60m"),
            sentNew24h = o.optDouble("sent_new_24h", 0.0), creditUnknown48h = o.optDouble("credit_unknown_48h", 0.0),
            payeeIsSender = o.optBoolean("payee_is_sender"), payeePasted = o.optBoolean("payee_pasted"),
            appSwitch = o.optBoolean("app_switch"), night = o.optBoolean("night"),
        )
    }
}

/** A rule approved in the analyst console: all tokens present, none of `none` present. */
data class ApprovedRule(val id: String, val all: List<String>, val none: List<String> = emptyList(), val explain: String = "")

data class Result(
    val level: String,           // NONE | NUDGE | PAUSE | CRITICAL
    val score: Double,           // 0..1, the model's scam probability
    val reasons: List<String>,   // up to 3 plain-language reasons
    val guardrails: List<String>,// safety rules that changed the level (G1, G2, G4, approved rule ids)
    val modelVersion: String,
)

class ScamModel private constructor(private val m: JSONObject) {

    private class Tree(o: JSONObject) {
        val feature = o.getJSONArray("feature").toIntArray()
        val threshold = o.getJSONArray("threshold").toDoubleArray()
        val left = o.getJSONArray("left").toIntArray()
        val right = o.getJSONArray("right").toIntArray()
        val value = o.getJSONArray("value").toDoubleArray()

        /** Walk to the leaf; add each split's (child - parent) value to the split feature. */
        fun walk(x: DoubleArray, contrib: DoubleArray, scale: Double): Double {
            var node = 0
            while (left[node] != -1) {
                val f = feature[node]
                val child = if (x[f] <= threshold[node]) left[node] else right[node]
                contrib[f] += scale * (value[child] - value[node])
                node = child
            }
            return value[node]
        }
    }

    val modelVersion: String = m.getString("model_version")
    private val type = m.getString("model_type")
    private val features: List<String> = m.getJSONArray("features").toStringList()
    private val th = m.getJSONObject("thresholds")
    private val tNudge = th.getDouble("nudge")
    private val tPause = th.getDouble("pause")
    private val tCritical = th.getDouble("critical")
    private val nudgeMinAmount = m.optDouble("nudge_min_amount", 1000.0)
    private val reasonText = m.getJSONObject("reasons")
    private val pre = m.getJSONObject("preprocessing")
    private val minHistory = pre.getInt("min_history")
    private val creditMatch = pre.getDouble("credit_match")
    private val ratioCap = pre.getDouble("ratio_cap")
    private val countCap = pre.getInt("count_cap")
    private val statuses = pre.getJSONArray("payee_statuses").toStringList()
    private val kinds = pre.getJSONArray("payee_kinds").toStringList()

    private val trees: List<Tree> = when (type) {
        "gradient_boosting" -> m.getJSONObject("gb").getJSONArray("trees").objects().map { Tree(it) }
        "random_forest" -> m.getJSONObject("rf").getJSONArray("trees").objects().map { Tree(it) }
        else -> emptyList()
    }

    /** Same numbers, same order as features.py featurize(). */
    fun featurize(p: Payment): DoubleArray {
        val hist = p.payments90d >= minHistory && p.median90d > 0
        val ratio = if (hist) min(p.amount / p.median90d, ratioCap) else 0.0
        val ratio24 = if (hist) min((p.amount + p.sentNew24h) / p.median90d, ratioCap) else 0.0
        val credit = p.creditUnknown48h
        val f = HashMap<String, Double>()
        f["log_amount"] = ln1p(p.amount)
        f["has_history"] = b(hist)
        f["log_ratio"] = ln1p(ratio)
        f["log_ratio_24h"] = ln1p(ratio24)
        f["log_sent_new_24h"] = ln1p(p.sentNew24h)
        f["call_now"] = b(p.callNow)
        f["call_seen_30m"] = b(p.callSeen30m)
        f["screen_share"] = b(p.screenShare)
        f["same_payee_24h"] = min(p.samePayee24h, countCap).toDouble()
        f["new_payees_60m"] = min(p.newPayees60m, countCap).toDouble()
        f["credit_unknown"] = b(credit > 0)
        f["amount_matches_credit"] = b(credit > 0 && abs(p.amount - credit) <= creditMatch * credit)
        f["payee_is_sender"] = b(p.payeeIsSender)
        f["payee_pasted"] = b(p.payeePasted)
        f["app_switch"] = b(p.appSwitch)
        f["night"] = b(p.night)
        for (s in statuses) f["status_$s"] = b(p.payeeStatus == s)
        for (k in kinds) f["kind_$k"] = b(p.payeeKind == k)
        return DoubleArray(features.size) { f.getValue(features[it]) }
    }

    /** Scam probability plus each feature's contribution to it. */
    fun score(x: DoubleArray): Pair<Double, DoubleArray> {
        val contrib = DoubleArray(features.size)
        return when (type) {
            "gradient_boosting" -> {
                val gb = m.getJSONObject("gb")
                val lr = gb.getDouble("learning_rate")
                var raw = gb.getDouble("init_raw")
                for (t in trees) raw += lr * t.walk(x, contrib, lr)
                sigmoid(raw) to contrib
            }
            "random_forest" -> {
                val n = trees.size.toDouble()
                var s = 0.0
                for (t in trees) s += t.walk(x, contrib, 1.0 / n)
                (s / n) to contrib
            }
            "logistic_regression" -> {
                val lr = m.getJSONObject("lr")
                val mean = lr.getJSONArray("mean").toDoubleArray()
                val scale = lr.getJSONArray("scale").toDoubleArray()
                val coef = lr.getJSONArray("coef").toDoubleArray()
                var z = lr.getDouble("intercept")
                for (i in x.indices) { contrib[i] = coef[i] * (x[i] - mean[i]) / scale[i]; z += contrib[i] }
                sigmoid(z) to contrib
            }
            else -> error("unknown model type $type")
        }
    }

    // ---- reasons: features grouped the way a person thinks (same as scorer.py GROUPS) ----
    private val groups: List<Pair<String, List<String>>> = listOf(
        "amount" to listOf("log_amount", "has_history", "log_ratio", "log_ratio_24h", "log_sent_new_24h"),
        "payee" to listOf("status_known", "status_new", "status_recent", "status_warned", "status_flagged",
            "kind_person", "kind_merchant", "kind_biller"),
        "call" to listOf("call_now", "call_seen_30m"),
        "screen_share" to listOf("screen_share"),
        "repeat" to listOf("same_payee_24h"),
        "fan_out" to listOf("new_payees_60m"),
        "credit" to listOf("credit_unknown", "amount_matches_credit", "payee_is_sender"),
        "pasted" to listOf("payee_pasted"),
        "app_switch" to listOf("app_switch"),
        "night" to listOf("night"),
    )

    private fun text(name: String): String? = if (reasonText.isNull(name)) null else reasonText.getString(name)

    private fun groupText(g: String, f: Map<String, Double>): String? = when (g) {
        "amount" -> when {
            f.getValue("has_history") > 0 && f.getValue("log_ratio") >= ln1p(3.0) ->
                "The amount is ${BigDecimal(expm1(f.getValue("log_ratio"))).setScale(0, RoundingMode.HALF_EVEN)} times your usual payment"
            f.getValue("has_history") > 0 && f.getValue("log_ratio_24h") >= ln1p(3.0) -> text("log_ratio_24h")
            f.getValue("has_history") == 0.0 && f.getValue("log_amount") >= ln1p(5000.0) ->
                "This is a large payment and you have little payment history"
            else -> null
        }
        "payee" -> listOf("flagged", "warned", "new", "recent").firstOrNull { f.getValue("status_$it") > 0 }?.let { text("status_$it") }
        "call" -> when {
            f.getValue("call_now") > 0 -> text("call_now")
            f.getValue("call_seen_30m") > 0 -> text("call_seen_30m")
            else -> null
        }
        "credit" -> when {
            f.getValue("payee_is_sender") > 0 -> null
            f.getValue("amount_matches_credit") > 0 -> text("amount_matches_credit")
            f.getValue("credit_unknown") > 0 -> text("credit_unknown")
            else -> null
        }
        else -> groups.first { it.first == g }.second[0].let { n -> if (f.getValue(n) > 0) text(n) else null }
    }

    fun reasons(x: DoubleArray, contrib: DoubleArray, top: Int = 3): List<String> {
        val f = features.indices.associate { features[it] to x[it] }
        val c = features.indices.associate { features[it] to contrib[it] }
        val pushes = groups.map { (g, names) -> g to names.sumOf { c.getValue(it) } }
        val out = ArrayList<String>()
        // stable sort by push, highest first (same order as Python's sorted())
        for ((g, push) in pushes.sortedByDescending { it.second }) {
            if (push <= 0 || out.size >= top) break
            groupText(g, f)?.let { out.add(it) }
        }
        return out
    }

    /**
     * Score -> level, then the safety layer (policy.py): C1 coaching check, then G1, G2, G3, G5, G4.
     * Answers can only RAISE the level.
     * If anything throws, the caller must let the payment go through (fail-open).
     */
    fun assess(p: Payment, q1: Boolean? = null, q2: Boolean? = null, approved: List<ApprovedRule> = emptyList()): Result {
        val x = featurize(p)
        val (s, contrib) = score(x)
        var level = when {
            s > tCritical -> "CRITICAL"
            s > tPause -> "PAUSE"
            s > tNudge && p.amount >= nudgeMinAmount -> "NUDGE"
            else -> "NONE"
        }
        val fired = ArrayList<String>()
        val reasons = ArrayList(reasons(x, contrib))
        // C1 coaching check: a full-screen Pause/Critical needs at least one sign that someone may be steering
        // the payment. A big amount alone (e.g. a TV in a shop) gets the amber Nudge line. Same as policy.py.
        val big = p.isBigFirstPaymentToPerson()
        if (rank(level) > rank("NUDGE") && !p.hasCoachingSign() && !big) {
            level = if (p.amount >= nudgeMinAmount) "NUDGE" else "NONE"; fired.add("C1")
        }
        val live = p.callNow || p.callSeen30m || p.screenShare
        if (p.payeeStatus == "flagged") { level = "CRITICAL"; fired.add("G1") }
        if (p.payeeStatus == "warned") { level = atLeast(level, if (live) "CRITICAL" else "NUDGE"); fired.add("G2") }
        val toks = p.tokens()
        for (r in approved) {
            if (r.all.all { it in toks } && r.none.none { it in toks }) {
                level = atLeast(level, "PAUSE"); fired.add(r.id)
                if (r.explain.isNotEmpty()) reasons.add(r.explain)
            }
        }
        // G5: a first payment to a new person of at least 30x the usual amount and Rs 20,000 -> at least Pause
        if (big && rank(level) < rank("PAUSE")) { level = "PAUSE"; fired.add("G5") }
        if (rank(level) >= rank("PAUSE") && (q1 == true || q2 == true)) {
            level = "CRITICAL"; fired.add("G4")
            if (q1 == true) reasons.add("You said you were told this is needed to verify, protect or release money")
            if (q2 == true) reasons.add("You said someone asked you to keep this payment secret")
        }
        return Result(level, s, reasons, fired, modelVersion)
    }

    companion object {
        fun fromJson(text: String) = ScamModel(JSONObject(text))
        fun fromAssets(context: Context, file: String = "scam_model.json") =
            fromJson(context.assets.open(file).bufferedReader().use { it.readText() })

        private val RANK = mapOf("NONE" to 0, "NUDGE" to 1, "PAUSE" to 2, "CRITICAL" to 3)
        fun rank(level: String) = RANK.getValue(level)
        private fun atLeast(level: String, floor: String) = if (rank(floor) > rank(level)) floor else level
        private fun b(v: Boolean) = if (v) 1.0 else 0.0
        private fun sigmoid(z: Double) = if (z >= 0) 1.0 / (1.0 + exp(-z)) else exp(z) / (1.0 + exp(z))
        private fun JSONArray.toIntArray() = IntArray(length()) { getInt(it) }
        private fun JSONArray.toDoubleArray() = DoubleArray(length()) { getDouble(it) }
        private fun JSONArray.toStringList() = List(length()) { getString(it) }
        private fun JSONArray.objects() = List(length()) { getJSONObject(it) }
    }
}
