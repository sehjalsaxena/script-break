package com.example.scriptbreak.logic

import org.json.JSONObject

/** One row of the AI vs rules vs bank-rule comparison. */
data class TypeResult(val key: String, val label: String, val ai: Double, val rules: Double, val neverSeen: Double?)

/**
 * The full test results (ml/results/report.json and test_summary.json, made by ml/run_all_tests.py on
 * 33,600 synthetic payments the model never trained on). Shown on the Results scoreboard.
 */
data class ResultsData(
    val modelVersion: String,
    val testGenuine: Int,
    val testScams: Int,
    val aiInScope: Double, val rulesInScope: Double, val bankInScope: Double,
    val aiAll8: Double, val rulesAll8: Double, val bankAll8: Double,
    val aiGenuinePer1000: Double, val rulesGenuinePer1000: Double, val bankGenuinePer1000: Double,
    val nudgedPer1000: Double,
    val rocAuc: Double,
    val byType: List<TypeResult>,
    val missedByType: Map<String, Int>,
    val falseAlarmPatterns: List<String>,
    val fairness: Map<String, Rate>,   // genuine payments interrupted by mistake, by group
) {
    companion object {
        fun parse(reportJson: String, summaryJson: String?): ResultsData {
            val r = JSONObject(reportJson)
            val c = r.getJSONObject("comparison")
            val ai = c.getJSONObject("ai_model"); val rules = c.getJSONObject("round1_rules_v0.3"); val bank = c.getJSONObject("bank_amount_rule_10k")
            val model = r.getJSONObject("model")
            val hidden = r.optJSONObject("hidden_scam_type")
            val byWf = model.getJSONObject("by_workflow")
            val types = byWf.keys().asSequence().map { k ->
                TypeResult(k, byWf.getJSONObject(k).getString("label"),
                    ai.getJSONObject("by_workflow").getDouble(k), rules.getJSONObject("by_workflow").getDouble(k),
                    hidden?.optJSONObject(k)?.getDouble("flagged_when_never_seen"))
            }.toList()
            val f = r.getJSONObject("failures")
            val missed = f.getJSONObject("missed_scams_by_type").let { o -> o.keys().asSequence().associateWith { o.getInt(it) } }
            val patterns = f.getJSONArray("false_alarm_patterns").let { a ->
                List(a.length()) { i -> a.getJSONObject(i).let { "${it.getString("payee")} ${it.getString("kind")}, ${it.getString("call")}: ${it.getInt("count")}" } }
            }
            val fairness = summaryJson?.let { JSONObject(it).optJSONObject("fairness") }?.let { o ->
                o.keys().asSequence().associateWith { g -> o.getJSONObject(g).let { Rate(it.getInt("k"), it.getInt("n")) } }
            }.orEmpty()
            val ts = r.getJSONObject("test_set")
            return ResultsData(
                modelVersion = r.getString("model_version"),
                testGenuine = ts.getInt("genuine"), testScams = ts.getInt("scams"),
                aiInScope = ai.getDouble("scams_flagged_in_scope6"), rulesInScope = rules.getDouble("scams_flagged_in_scope6"), bankInScope = bank.getDouble("scams_flagged_in_scope6"),
                aiAll8 = ai.getDouble("scams_flagged_all8"), rulesAll8 = rules.getDouble("scams_flagged_all8"), bankAll8 = bank.getDouble("scams_flagged_all8"),
                aiGenuinePer1000 = ai.getDouble("genuine_interrupted_per_1000"), rulesGenuinePer1000 = rules.getDouble("genuine_interrupted_per_1000"),
                bankGenuinePer1000 = bank.getDouble("genuine_interrupted_per_1000"),
                nudgedPer1000 = model.getDouble("genuine_nudged_per_1000"), rocAuc = model.getDouble("roc_auc"),
                byType = types, missedByType = missed, falseAlarmPatterns = patterns, fairness = fairness,
            )
        }
    }
}
