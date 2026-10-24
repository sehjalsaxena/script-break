package com.example.scriptbreak.logic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.scriptbreak.ml.ApprovedRule
import com.example.scriptbreak.ml.Payment
import com.example.scriptbreak.ml.Result
import com.example.scriptbreak.ml.ScamModel
import org.json.JSONObject

/** A rule the AI proposes for one cluster of missed scam reports. */
data class ProposedRule(
    val id: String,
    val name: String,              // plain words, e.g. "same amount as that money + new payee, not payee is that sender"
    val all: List<String>,
    val none: List<String>,
    val explain: String,           // the reason the phone shows when this rule fires
    val catchesShare: Double,      // share of the cluster it catches
    val addedPer1000: Double,      // extra genuine payments interrupted per 1,000 (backtest)
)

data class Cluster(
    val id: String,
    val size: Int,
    val pattern: List<String>,     // the events most reports in this cluster share
    val trueMix: Map<String, Int>, // the real scam types (the AI never saw these; shown to judges on request)
    val suggestion: String,        // approve | shadow mode | reject (the written approval policy)
    val rule: ProposedRule?,
    val cheapestAddedPer1000: Double?, // no rule within budget: cost of the cheapest rule catching 70%
)

/** What the AI found in a month of scam reports the phone's model missed (analyst_discovery.py). */
data class Discovery(
    val reports: Int,
    val missed: Int,
    val missedByType: Map<String, Int>,
    val method: String,
    val silhouette: Double,
    val budgetPer1000: Double,
    val minShareCaught: Double,
    val clusters: List<Cluster>,
) {
    companion object {
        fun parse(json: String): Discovery {
            val o = JSONObject(json)
            val pol = o.getJSONObject("policy")
            val cl = o.getJSONObject("clustering")
            val arr = o.getJSONArray("clusters")
            val clusters = List(arr.length()) { i ->
                val c = arr.getJSONObject(i)
                val r = c.optJSONObject("proposed_rule")
                Cluster(
                    id = c.getString("cluster_id"), size = c.getInt("size"),
                    pattern = c.getJSONArray("pattern").let { a -> List(a.length()) { a.getString(it) } },
                    trueMix = c.getJSONObject("ground_truth_mix_for_judges").let { m -> m.keys().asSequence().associateWith { m.getInt(it) } },
                    suggestion = c.getString("suggestion"),
                    rule = r?.let {
                        ProposedRule(it.getString("id"), it.getString("name"),
                            it.getJSONArray("all").let { a -> List(a.length()) { a.getString(it) } },
                            it.getJSONArray("none").let { a -> List(a.length()) { a.getString(it) } },
                            it.getString("explain"), it.getDouble("catches_share_of_cluster"), it.getDouble("added_false_alarms_per_1000"))
                    },
                    cheapestAddedPer1000 = c.optJSONObject("cheapest_rule_reaching_70pct")?.getDouble("added_false_alarms_per_1000"),
                )
            }
            val byType = o.getJSONObject("missed_by_type")
            return Discovery(
                reports = o.getInt("reports"), missed = o.getInt("missed_by_model"),
                missedByType = byType.keys().asSequence().associateWith { byType.getInt(it) },
                method = cl.getString("method"), silhouette = cl.getDouble("silhouette"),
                budgetPer1000 = pol.getDouble("new_rule_budget_per_1000"), minShareCaught = pol.getDouble("min_share_caught"),
                clusters = clusters,
            )
        }
    }
}

/**
 * Analyst mode: the AI clusters scam reports the phone's model missed and proposes one rule per
 * cluster; a human analyst approves, rejects or runs it in shadow mode. Approved rules become
 * guardrail G3 on the phone (at least Pause). The AI never changes the phone by itself.
 *
 * The demo uses the model trained WITHOUT "sent by mistake" (scam_model_unseen_sent_by_mistake.json),
 * i.e. the day a new scam wave starts: it misses those payments until the analyst approves the rule.
 */
class AnalystMode(
    val beforeModel: ScamModel?,               // the model that has never seen the new scam wave
    val discovery: Discovery?,
    private val onApprovedRulesChanged: (List<ApprovedRule>) -> Unit,
) {
    /** clusterId -> "approve" | "shadow" | "reject" (the analyst's decision). */
    val decisions = mutableStateMapOf<String, String>()
    var showTrueTypes by mutableStateOf(false)

    val approvedRules: List<ApprovedRule>
        get() = discovery?.clusters.orEmpty().filter { decisions[it.id] == "approve" && it.rule != null }
            .map { c -> c.rule!!.let { ApprovedRule(it.id, it.all, it.none, it.explain) } }

    fun decide(clusterId: String, decision: String) {
        if (decisions[clusterId] == decision) decisions.remove(clusterId) else decisions[clusterId] = decision
        onApprovedRulesChanged(approvedRules)
    }

    /** "Try it": a stranger sent ₹5,000 "by mistake" and asks for it back, to a DIFFERENT new account. */
    val scamPayment = Payment(amount = 5000.0, median90d = 2500.0, payments90d = 50, callNow = true,
        payeeStatus = "new", creditUnknown48h = 5000.0)
    /** The genuine case: returning the money to the person who actually sent it. Must stay unwarned. */
    val genuinePayment = scamPayment.copy(callNow = false, payeeIsSender = true)

    fun tryIt(p: Payment, withApprovedRules: Boolean): Result? =
        try { beforeModel?.assess(p, approved = if (withApprovedRules) approvedRules else emptyList()) } catch (e: Exception) { null }

    companion object {
        /** Plain names for the scam types (shown only when "show true scam types" is on). */
        val TYPE_NAMES = mapOf(
            "digital_arrest" to "Digital arrest", "customer_care" to "Fake customer care", "kyc_official" to "Fake KYC / bank official",
            "courier" to "Courier / customs", "voice_clone" to "AI voice clone", "hang_up_and_pay" to "Hang up, then pay",
            "sent_by_mistake" to "\"Sent by mistake\"", "chat_task" to "Task scam over chat",
        )
    }
}
