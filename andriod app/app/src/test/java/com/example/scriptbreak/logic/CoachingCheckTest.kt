package com.example.scriptbreak.logic

import com.example.scriptbreak.ml.Payment
import com.example.scriptbreak.ml.ScamModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** C1 coaching check: a big purchase with no sign of a scammer gets the amber line, not a popup. */
class CoachingCheckTest {
    private val model = ScamModel.fromJson(File("src/main/assets/scam_model.json").readText())
    private val tv = Payment(amount = 90_000.0, median90d = 650.0, payments90d = 42, payeeStatus = "new", payeeKind = "merchant")

    @Test
    fun aTvInAShopWithNoCallIsOnlyANudge() {
        val r = model.assess(tv)
        assertTrue("the model alone would Pause (score ${r.score})", r.score > 0.4865)
        assertEquals("NUDGE", r.level)
        assertTrue(r.guardrails.contains("C1"))
        assertTrue("the reasons are still shown on the amber line", r.reasons.isNotEmpty())
    }

    @Test
    fun anySignOfCoachingKeepsThePopup() {
        val signs = listOf(
            tv.copy(callNow = true), tv.copy(callSeen30m = true), tv.copy(screenShare = true), tv.copy(payeePasted = true),
            tv.copy(appSwitch = true), tv.copy(newPayees60m = 1), tv.copy(sentNew24h = 5000.0),
            tv.copy(creditUnknown48h = 90_000.0), tv.copy(samePayee24h = 1),
        )
        for (p in signs) {
            val r = model.assess(p)
            assertTrue("$p -> ${r.level}", ScamModel.rank(r.level) >= ScamModel.rank("PAUSE") || r.score <= 0.4865)
            assertTrue(!r.guardrails.contains("C1"))
        }
    }

    @Test
    fun flaggedOrWarnedPayeesAndApprovedRulesStillRaiseTheLevel() {
        assertEquals("CRITICAL", model.assess(tv.copy(payeeStatus = "flagged")).level)               // G1
        val rule = com.example.scriptbreak.ml.ApprovedRule("R1", listOf("amount_big", "payee_new"))
        assertEquals("PAUSE", model.assess(tv, approved = listOf(rule)).level)                       // G3
    }

    @Test
    fun inTheAppTheTvPurchaseShowsTheAmberLineAndGoesStraightToThePin() {
        val signals = object : SignalSource {
            override val callNow = false
            override val screenShare = false
            override fun callSeenWithin30m(nowMs: Long) = false
            override fun isNight(nowMs: Long) = false
        }
        val f = PaymentFlow(model, signals, { }).apply { login("sambhav", "1111") }
        f.openPerson("gupta"); f.toAmount(); "90000".forEach { f.amountDigit(it.digitToInt()) }
        assertNotNull("amber line while typing", f.nudge)
        f.pay()
        assertEquals("no popup: straight to the UPI PIN", Screen.Pin, f.screen)
    }
}
