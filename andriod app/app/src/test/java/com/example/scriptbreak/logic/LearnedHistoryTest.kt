package com.example.scriptbreak.logic

import com.example.scriptbreak.ml.ScamModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** No fixed amount limits: the usual payment is learned from the person's payments, and the screens show what the AI saw. */
class LearnedHistoryTest {
    private val model = ScamModel.fromJson(File("src/main/assets/scam_model.json").readText())
    private var now = 1_800_000_000_000L

    private class Signals(var call: Boolean = false) : SignalSource {
        override val callNow get() = call
        override val screenShare = false
        override fun callSeenWithin30m(nowMs: Long) = false
        override fun isNight(nowMs: Long) = false
    }

    private fun sharma(s: SignalSource = Signals()) =
        PaymentFlow(model, s, { }, clock = { now }).apply { assertTrue(login("sharma", "1111")) }

    private fun PaymentFlow.payKnown(id: String, amount: Long) {
        openPerson(id); toAmount(); amount.toString().forEach { amountDigit(it.digitToInt()) }; pay()
        if (screen == Screen.Pause) pauseNext()
        if (screen == Screen.Critical) { repeat(10) { tick() }; continueAnyway() }
        "4821".forEach { pinDigit(it.digitToInt()) }; pinSubmit()
        assertEquals(Screen.Success, screen); done()
    }

    @Test
    fun startingHistoryMatchesTheProfiles() {
        for ((id, p) in DemoWorld.profiles) {
            val h = DemoWorld.history90d(id)
            assertEquals(id, p.payments90d, h.size)
            assertEquals(id, p.median90d, PaymentFlow.median(h), 0.0)
        }
    }

    @Test
    fun theUsualPaymentMovesAsThePersonPays() {
        val f = sharma()
        assertEquals(650.0, f.usualPayment, 0.0)
        repeat(30) { now += 60_000; f.payKnown("suresh", 20_000) }
        assertEquals(72, f.paymentsLast90d().size)
        assertTrue("usual payment learned from the 30 big payments: ${f.usualPayment}", f.usualPayment > 2000)
        // and the model is given exactly that number
        f.startPayToUpi(); "ramesh.k@demoupi".forEach { f.upiChanged(f.upiInput + it) }; f.continueWithUpi(); f.toAmount()
        "20000".forEach { f.amountDigit(it.digitToInt()) }
        assertEquals(f.usualPayment, f.currentPayment()!!.median90d, 0.0)
        assertEquals(72, f.currentPayment()!!.payments90d)
        // payments older than 90 days drop out
        now += 91L * 24 * 60 * 60 * 1000
        assertEquals(650.0, f.usualPayment, 0.0)
    }

    @Test
    fun theAmountLineStaysWhenTheAmountGetsBigger() {
        val f = sharma(Signals(call = true))
        f.startPayToUpi(); "ramesh.k@demoupi".forEach { f.upiChanged(f.upiInput + it) }; f.continueWithUpi(); f.toAmount()
        "20000".forEach { f.amountDigit(it.digitToInt()) }
        val ai = f.aiView!!
        assertEquals("PAUSE", ai.level)
        assertNotNull("the line does not vanish at Pause level", f.nudge)
        assertTrue(ai.facts.toString(), ai.facts.contains("On a call now"))
        assertTrue(ai.facts.toString(), ai.facts.contains("New payee: never paid"))
        assertTrue(ai.facts.toString(), ai.facts.contains("Usual payment ₹650 (median of 42 payments in 90 days)"))
    }

    @Test
    fun keyboardSuggestionsAreTypingAndAWholeUpiIdIsAPaste() {
        val f = sharma()
        f.startPayToUpi()
        f.upiChanged("ram"); f.upiChanged("ramesh"); f.upiChanged("ramesh.k@"); f.upiChanged("ramesh.k@demoupi")
        f.continueWithUpi(); f.toAmount(); f.amountDigit(5)
        assertFalse("autocomplete is not a paste", f.currentPayment()!!.payeePasted)

        f.done(); f.startPayToUpi()
        f.upiChanged("ramesh.k@demoupi")                         // the whole ID at once
        f.continueWithUpi(); f.toAmount(); f.amountDigit(5)
        assertTrue(f.currentPayment()!!.payeePasted)
    }

    @Test
    fun resetDemoGoesBackToTheStart() {
        val f = sharma()
        f.payKnown("suresh", 20_000)
        f.simulateCall = true
        f.resetDemo()
        assertTrue(f.ledger.isEmpty())
        assertFalse(f.simulateCall)
        assertEquals(650.0, f.usualPayment, 0.0)
        assertEquals("Rohit", f.contact.name)
        assertEquals("9876543210", f.contact.phone)
    }
}
