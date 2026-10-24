package com.example.scriptbreak.logic

import com.example.scriptbreak.ml.ScamModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Demo scenario 3: the model misses a scam, the AI finds the pattern, a human approves, the app now pauses it. */
class AnalystDemoTest {
    private val assets = File("src/main/assets")
    private val model = ScamModel.fromJson(File(assets, "scam_model.json").readText())
    private val before = ScamModel.fromJson(File(assets, "scam_model_unseen_sent_by_mistake.json").readText())
    private val discovery = Discovery.parse(File(assets, "discovery_unseen_sent_by_mistake.json").readText())

    private class Quiet : SignalSource {
        override val callNow = false
        override val screenShare = false
        override fun callSeenWithin30m(nowMs: Long) = false
        override fun isNight(nowMs: Long) = false
    }

    @Test
    fun missedBeforeApprovalPausedAfter() {
        val flow = PaymentFlow(model, Quiet(), { }).apply { login("analyst", "9999") }
        val analyst = AnalystMode(before, discovery) { rules -> flow.approvedRules = rules }
        fun runInApp() = flow.runTest(analyst.scamPayment, null, null, testModel = before, back = Screen.Analyst, payeeName = "Vikram S.")

        runInApp(); flow.pay()
        assertEquals("missed: straight to the PIN", Screen.Pin, flow.screen)
        flow.back(); flow.back()

        val cluster = discovery.clusters.first { it.rule != null }
        assertEquals(89, cluster.size)
        analyst.decide(cluster.id, "approve")                             // the human decision
        runInApp(); flow.pay()
        assertEquals("caught after approval", Screen.Pause, flow.screen)
        assertTrue(flow.pauseResult!!.guardrails.contains(cluster.rule!!.id))
        // returning the money to the person who really sent it is still not warned
        assertEquals("NONE", analyst.tryIt(analyst.genuinePayment, withApprovedRules = true)!!.level)
    }
}
