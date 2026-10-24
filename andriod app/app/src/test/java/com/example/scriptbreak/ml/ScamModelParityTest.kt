package com.example.scriptbreak.ml

// Parity test: the Kotlin scorer must give the same score and reasons as Python for the
// 500 payments in test_vectors.json. Put this in app/src/test/java/com/example/scriptbreak/ml/
// and copy scam_model.json + test_vectors.json into app/src/test/resources/.
// Run: ./gradlew test   (JVM unit test; add testImplementation("org.json:json:20240303")
// to app/build.gradle.kts, because Android's built-in org.json is a stub in JVM tests)

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ScamModelParityTest {
    private fun resource(name: String) = javaClass.classLoader!!.getResource(name)!!.readText()

    @Test
    fun matchesPython() {
        val model = ScamModel.fromJson(resource("scam_model.json"))
        val tv = JSONObject(resource("test_vectors.json"))
        val vectors = tv.getJSONArray("vectors")
        val tol = tv.getDouble("tolerance")
        var worst = 0.0
        for (i in 0 until vectors.length()) {
            val v = vectors.getJSONObject(i)
            val p = Payment.fromJson(v.getJSONObject("features"))
            val x = model.featurize(p)
            val (s, contrib) = model.score(x)
            worst = maxOf(worst, abs(s - v.getDouble("expected_score")))
            val expected = v.getJSONArray("expected_reasons").let { a -> List(a.length()) { a.getString(it) } }
            assertEquals("reasons for vector $i", expected, model.reasons(x, contrib))
        }
        assertTrue("max score difference $worst > $tol", worst <= tol)
    }

    @Test
    fun answersOnlyRaise() {
        val model = ScamModel.fromJson(resource("scam_model.json"))
        val p = Payment(amount = 48000.0, median90d = 650.0, payments90d = 42, callNow = true, payeeStatus = "new")
        val none = ScamModel.rank(model.assess(p).level)
        val no = ScamModel.rank(model.assess(p, q1 = false, q2 = false).level)
        val yes = ScamModel.rank(model.assess(p, q1 = true).level)
        assertTrue(no >= none && yes >= no)
    }
}
