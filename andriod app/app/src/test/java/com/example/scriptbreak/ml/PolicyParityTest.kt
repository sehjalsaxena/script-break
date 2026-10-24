package com.example.scriptbreak.ml

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The phone's DECISION equals the Python safety layer (policy.py) that every reported number comes from:
 * same level and same safety steps (C1, G1-G5, the analyst rule) on 4,960 decisions in ml/level_vectors.json
 * (made by ml/make_level_vectors.py). ScamModelParityTest checks the scores and reasons.
 */
class PolicyParityTest {
    private val model = ScamModel.fromJson(File("src/main/assets/scam_model.json").readText())
    private val data = JSONObject(File("../../ml/level_vectors.json").readText())

    @Test
    fun sameLevelAndSafetyStepsAsPython() {
        assertEquals("level_vectors.json must be made from the same model", data.getString("model_version"), model.modelVersion)
        val r = data.getJSONObject("rule")
        val rule = ApprovedRule(r.getString("id"),
            r.getJSONArray("all").let { a -> List(a.length()) { a.getString(it) } },
            r.getJSONArray("none").let { a -> List(a.length()) { a.getString(it) } })
        val vectors = data.getJSONArray("vectors")
        var checked = 0
        for (i in 0 until vectors.length()) {
            val v = vectors.getJSONObject(i)
            val p = Payment.fromJson(v.getJSONObject("features"))
            val results = v.getJSONArray("results")
            for (j in 0 until results.length()) {
                val e = results.getJSONObject(j)
                val q1 = if (e.isNull("q1")) null else e.getBoolean("q1")
                val q2 = if (e.isNull("q2")) null else e.getBoolean("q2")
                val got = model.assess(p, q1, q2, if (e.getBoolean("rule")) listOf(rule) else emptyList())
                val where = "payment $i, answers $q1/$q2, rule ${e.getBoolean("rule")}"
                assertEquals(where, e.getString("level"), got.level)
                val steps = e.getJSONArray("guardrails").let { a -> List(a.length()) { a.getString(it) } }
                assertEquals(where, steps, got.guardrails)
                checked++
            }
        }
        assertTrue("all decisions checked ($checked)", checked >= 4900)
    }
}
