package com.example.scriptbreak.logic

import com.example.scriptbreak.ml.ApprovedRule
import com.example.scriptbreak.ml.ScamModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Analyst mode: missed -> AI finds the pattern -> a person approves -> the phone catches it. */
class AnalystModeTest {
    private val assets = File("src/main/assets")   // unit tests run from "andriod app/app"
    private val ml = File("../../ml")
    private val before = ScamModel.fromJson(File(assets, "scam_model_unseen_sent_by_mistake.json").readText())
    private val discovery = Discovery.parse(File(assets, "discovery_unseen_sent_by_mistake.json").readText())
    private var pushed: List<ApprovedRule> = emptyList()
    private val analyst = AnalystMode(before, discovery) { pushed = it }

    @Test
    fun appCopiesMatchTheMlFiles() {
        for (f in listOf("scam_model_unseen_sent_by_mistake.json", "discovery_unseen_sent_by_mistake.json")) {
            assertEquals("$f in assets must equal ml/$f", File(ml, f).readText(), File(assets, f).readText())
        }
        assertTrue(before.modelVersion.endsWith("-without-sent_by_mistake"))
    }

    @Test
    fun discoveryFileIsReadCompletely() {
        assertEquals(1200, discovery.reports)
        assertEquals(273, discovery.missed)   // 270 before the C1 coaching check (25 Oct): 3 reports with no coaching sign are now Nudge only
        assertEquals(89, discovery.missedByType["sent_by_mistake"])
        assertEquals(5, discovery.clusters.size)
        val c1 = discovery.clusters.first()
        assertEquals("C1", c1.id)
        assertEquals(89, c1.size)
        assertEquals(mapOf("sent_by_mistake" to 89), c1.trueMix)           // one pure cluster
        assertEquals("approve", c1.suggestion)
        val r = c1.rule!!
        assertEquals(listOf("amount_matches_credit", "payee_new"), r.all)
        assertEquals(listOf("payee_is_sender"), r.none)
        assertEquals(1.0, r.catchesShare, 0.0)
        assertEquals(0.05, r.addedPer1000, 1e-9)
        assertTrue(discovery.clusters.drop(1).all { it.rule == null && it.suggestion == "reject" })
    }

    @Test
    fun missedThenApprovedThenCaught() {
        assertEquals("NONE", analyst.tryIt(analyst.scamPayment, withApprovedRules = true)!!.level)   // missed
        analyst.decide("C1", "approve")
        val r = analyst.tryIt(analyst.scamPayment, withApprovedRules = true)!!
        assertEquals("PAUSE", r.level)                                                             // caught
        assertTrue(r.guardrails.contains("L1"))
        assertTrue(r.reasons.any { it.startsWith("This sends back money you received from someone you have never paid") })
        assertEquals(listOf("L1"), pushed.map { it.id })                                            // sent to the app (G3)
        assertEquals("NONE", analyst.tryIt(analyst.scamPayment, withApprovedRules = false)!!.level) // the model alone still misses it
    }

    @Test
    fun genuineReturnToTheRealSenderIsNotInterrupted() {
        analyst.decide("C1", "approve")
        assertEquals("NONE", analyst.tryIt(analyst.genuinePayment, withApprovedRules = true)!!.level)
    }

    @Test
    fun shadowAndRejectDoNotReachPhones() {
        analyst.decide("C1", "shadow")
        assertTrue(pushed.isEmpty())
        assertEquals("NONE", analyst.tryIt(analyst.scamPayment, withApprovedRules = true)!!.level)
        analyst.decide("C1", "approve")
        assertEquals(1, pushed.size)
        analyst.decide("C1", "reject")
        assertTrue(pushed.isEmpty())
        analyst.decide("C1", "reject")                                  // tapping again clears the decision
        assertEquals(null, analyst.decisions["C1"])
    }

    private class NoSignals : SignalSource {
        override val callNow = false
        override val screenShare = false
        override fun callSeenWithin30m(nowMs: Long) = false
        override fun isNight(nowMs: Long) = false
    }

    @Test
    fun runInAppUsesTheBeforeModelAndTheApprovedRuleThenReturns() {
        val real = ScamModel.fromJson(File(assets, "scam_model.json").readText())
        val flow = PaymentFlow(real, NoSignals(), { })
        val a = AnalystMode(before, discovery) { flow.approvedRules = it }
        flow.pickProfile("analyst")
        assertEquals(Screen.Analyst, flow.screen)

        flow.runTest(a.scamPayment, null, null, testModel = before, back = Screen.Analyst, payeeName = "Vikram S.")
        flow.pay()
        assertEquals("missed before approval", Screen.Pin, flow.screen)
        flow.back(); flow.back()
        assertEquals(Screen.Analyst, flow.screen)

        a.decide("C1", "approve")
        flow.runTest(a.scamPayment, null, null, testModel = before, back = Screen.Analyst, payeeName = "Vikram S.")
        assertTrue(flow.modelVersion.endsWith("-without-sent_by_mistake"))
        flow.pay()
        assertEquals("caught after approval", Screen.Pause, flow.screen)
        assertNotNull(flow.pauseResult!!.reasons.firstOrNull { it.startsWith("This sends back money") })
        flow.wait30(); flow.done()
        assertEquals(Screen.Analyst, flow.screen)
        assertEquals("the normal app is back on the real model", real.modelVersion, flow.modelVersion)
    }
}
