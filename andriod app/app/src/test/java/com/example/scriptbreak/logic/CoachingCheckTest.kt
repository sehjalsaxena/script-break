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
    fun g5BigFirstPaymentToANewPersonIsAlwaysAtLeastAPause() {
        // ₹35,000 to someone never paid, 54× the usual ₹650, typed, no call, at night: the model alone says 0.48 (Nudge)
        val stranger = Payment(amount = 35_000.0, median90d = 650.0, payments90d = 42, payeeStatus = "new", night = true)
        val r = model.assess(stranger)
        assertTrue("model alone: below the Pause line (${r.score})", r.score <= 0.4865)
        assertEquals("PAUSE", r.level)
        assertTrue(r.guardrails.contains("G5"))
        assertEquals("a Yes answer still gives Critical", "CRITICAL", model.assess(stranger, q1 = true).level)
        // not G5: a shop (the TV), under 30× the usual amount, under ₹20,000, or someone paid before
        assertEquals("NUDGE", model.assess(tv).level)
        assertTrue(!model.assess(stranger.copy(amount = 19_000.0)).guardrails.contains("G5"))
        assertTrue(!model.assess(stranger.copy(median90d = 1_500.0)).guardrails.contains("G5"))
        assertTrue(!model.assess(stranger.copy(payeeStatus = "known")).guardrails.contains("G5"))
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

        // but ₹35,000 to a stranger's UPI ID (typed, no call) gets the Pause screen
        val g = PaymentFlow(model, signals, { }).apply { login("sambhav", "1111") }
        g.startPayToUpi(); "new.person@demoupi".forEach { g.upiChanged(g.upiInput + it) }; g.continueWithUpi(); g.toAmount()
        "35000".forEach { g.amountDigit(it.digitToInt()) }
        g.pay()
        assertEquals(Screen.Pause, g.screen)
    }
}
