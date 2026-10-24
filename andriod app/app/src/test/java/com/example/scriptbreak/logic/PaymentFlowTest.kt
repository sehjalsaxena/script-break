package com.example.scriptbreak.logic

import com.example.scriptbreak.ml.ScamModel
import com.example.scriptbreak.ui.model.FamilyAlertUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The payment journey with the real AI model (scam_model.json) and fake phone signals. */
class PaymentFlowTest {
    private val model = ScamModel.fromJson(javaClass.classLoader!!.getResource("scam_model.json")!!.readText())

    private class FakeSignals(var call: Boolean = false, var share: Boolean = false, var night: Boolean = false) : SignalSource {
        override val callNow get() = call
        override val screenShare get() = share
        override fun callSeenWithin30m(nowMs: Long) = false
        override fun isNight(nowMs: Long) = night
    }

    private var now = 1_800_000_000_000L
    private val sent = mutableListOf<FamilyAlertUi>()

    private fun newFlow(signals: SignalSource, m: ScamModel? = model) =
        PaymentFlow(m, signals, { sent += it }, clock = { now }).apply { assertTrue(login("sambhav", "1111")) }

    private fun PaymentFlow.typeAmount(amount: Long) = amount.toString().forEach { amountDigit(it.digitToInt()) }

    /** Pay someone by typing their UPI ID, then press Pay. */
    private fun PaymentFlow.payTyped(upi: String, amount: Long) {
        startPayToUpi()
        upi.forEach { upiChanged(upiInput + it) }   // one character at a time = typed, not pasted
        continueWithUpi()
        toAmount()
        typeAmount(amount)
        pay()
    }

    /** Sambhav's own UPI PIN (demo). */
    private fun PaymentFlow.finishPin(pin: String = "4821") { pin.forEach { pinDigit(it.digitToInt()) }; pinSubmit() }

    @Test
    fun digitalArrestPausesThenYesGivesCriticalWithCountdown() {
        val f = newFlow(FakeSignals(call = true))
        f.payTyped("ramesh.k@demoupi", 20_000)
        assertEquals(Screen.Pause, f.screen)
        assertTrue(f.pauseResult!!.reasons.contains("You are on a call right now"))

        f.answerQ1(true)
        f.pauseNext()
        assertEquals(Screen.Critical, f.screen)
        assertEquals(10, f.secondsLeft)
        assertTrue("G4 fired", f.criticalResult!!.guardrails.contains("G4"))

        f.askFamily()
        assertEquals(1, sent.size)
        assertEquals("Sambhav", sent[0].fromName)
        assertEquals("₹20,000", sent[0].amountText)
        assertEquals("Ramesh K.", sent[0].payeeName)

        f.continueAnyway()                       // locked during the countdown
        assertEquals(Screen.Critical, f.screen)
        repeat(10) { f.tick() }
        f.continueAnyway()                       // always available after 10 s: the person decides
        assertEquals(Screen.Pin, f.screen)
        f.finishPin()
        assertEquals(Screen.Success, f.screen)
        assertTrue(f.success!!.showReportLine)
    }

    @Test
    fun coachedNoNoIsNotLowered() {
        val f = newFlow(FakeSignals(call = true))
        f.payTyped("ramesh.k@demoupi", 20_000)
        assertEquals(Screen.Pause, f.screen)
        f.answerQ1(false); f.answerQ2(false)
        assertEquals("PAUSE", f.assessSafely(false, false)!!.level)   // still Pause, never lower
        f.pauseNext()
        assertEquals(Screen.Pin, f.screen)                             // then the person decides
    }

    @Test
    fun rentToTheUsualLandlordShowsNothing() {
        val f = newFlow(FakeSignals())
        f.openPerson("suresh")
        assertEquals("Paid 14 times before", f.historyLine(f.payee!!))
        f.toAmount(); f.typeAmount(18_000)
        assertNull(f.nudge)
        f.pay()
        assertEquals(Screen.Pin, f.screen)
        f.finishPin()
        assertFalse(f.success!!.showReportLine)
    }

    @Test
    fun newPersonGetsTheInlineNudgeLine() {
        val f = newFlow(FakeSignals())
        f.startPayToUpi(); "anil.mehta@demoupi".forEach { f.upiChanged(f.upiInput + it) }; f.continueWithUpi()
        f.toAmount(); f.typeAmount(15_000)
        assertEquals("First payment to Anil Mehta. ₹15,000 is 23× your usual payment. Check you know who this is.", f.nudge)
        f.pay()
        assertEquals("a Nudge needs no extra tap", Screen.Pin, f.screen)
    }

    @Test
    fun aVeryLargeFirstPaymentToANewPersonGetsAPause() {
        // Decision of 25 Oct (guardrail G5): ₹40,000 to someone never paid, 62× the usual amount, is at least a Pause
        // even with no call. Before G5 this was only the amber line. The cost: a genuine new-landlord deposit is paused too.
        val f = newFlow(FakeSignals())
        f.startPayToUpi(); "anil.mehta@demoupi".forEach { f.upiChanged(f.upiInput + it) }; f.continueWithUpi()
        f.toAmount(); f.typeAmount(40_000)
        assertEquals("First payment to Anil Mehta. ₹40,000 is 62× your usual payment. Check you know who this is.", f.nudge)
        f.pay()
        assertEquals(Screen.Pause, f.screen)
        assertTrue(f.pauseResult!!.guardrails.contains("G5"))
        f.pauseNext()                                               // answers "No" (or none): the person can continue
        assertEquals(Screen.Pin, f.screen)
    }

    @Test
    fun pastedIdAndLeavingTheAppGoStraightToCritical() {
        val f = newFlow(FakeSignals(call = true))
        f.startPayToUpi()
        f.pasteUpi("ramesh.k@demoupi")
        f.continueWithUpi(); f.toAmount()
        f.onAppStopped()                         // left the app mid-payment (e.g. to copy something)
        f.typeAmount(48_000)
        assertTrue(f.currentPayment()!!.payeePasted)
        assertTrue(f.currentPayment()!!.appSwitch)
        f.pay()
        assertEquals(Screen.Critical, f.screen)
    }

    @Test
    fun keyboardPasteIsDetectedAndPickingFromRecentIsNot() {
        val f = newFlow(FakeSignals())
        f.startPayToUpi(); f.upiChanged("ramesh.k@demoupi"); f.continueWithUpi(); f.toAmount(); f.typeAmount(100)
        assertTrue(f.currentPayment()!!.payeePasted)
        f.startPayToUpi(); f.pickSuggestion("ramesh.k@demoupi"); f.continueWithUpi(); f.toAmount(); f.typeAmount(100)
        assertFalse(f.currentPayment()!!.payeePasted)
    }

    @Test
    fun genuineFalseAlarmUserGetsPastInTwoTaps() {
        val f = newFlow(FakeSignals(call = true))
        f.payTyped("manoj.elec@demoupi", 6_000)
        assertEquals(Screen.Pause, f.screen)    // accepted false alarm: a new electrician guiding the payment
        f.pauseNext()                            // no answers needed
        assertEquals(Screen.Pin, f.screen)
    }

    @Test
    fun failOpenWhenTheModelIsMissing() {
        val f = newFlow(FakeSignals(call = true), m = null)
        f.payTyped("ramesh.k@demoupi", 48_000)
        assertEquals(Screen.Pin, f.screen)
        assertNotNull(f.message)
    }

    @Test
    fun aPayeePaidAfterAWarningIsWarnedNextTime() {
        val f = newFlow(FakeSignals(call = true))
        f.payTyped("ramesh.k@demoupi", 20_000)
        f.pauseNext(); f.finishPin(); f.done()
        assertEquals("warned", f.statusOf(DemoWorld.byId("ramesh")!!))
        now += 60_000
        f.payTyped("ramesh.k@demoupi", 2_000)    // small repeat on a call: G2 makes it Critical
        assertEquals(Screen.Critical, f.screen)
        assertTrue(f.criticalResult!!.guardrails.contains("G2"))
    }

    @Test
    fun repeatsAndTodaysTotalsFeedTheModel() {
        val f = newFlow(FakeSignals())
        f.payTyped("anil.mehta@demoupi", 3_000); f.finishPin(); f.done()
        now += 5 * 60_000
        f.payTyped("anil.mehta@demoupi", 3_000)
        val p = f.currentPayment()!!
        assertEquals("recent", p.payeeStatus)
        assertEquals(1, p.samePayee24h)
        assertEquals(3_000.0, p.sentNew24h, 0.0)
    }

    @Test
    fun answersNeverLowerTheLevelForAnyDemoPayee() {
        val rank = mapOf("NONE" to 0, "NUDGE" to 1, "PAUSE" to 2, "CRITICAL" to 3)
        for (call in listOf(false, true)) for (id in listOf("ramesh", "anil", "manoj", "gupta", "suresh", "kirana")) {
            for (amount in listOf(500L, 3_000L, 20_000L, 48_000L)) {
                val f = newFlow(FakeSignals(call = call))
                f.openPerson(id); f.toAmount(); f.typeAmount(amount)
                val base = rank.getValue(f.assessSafely(null, null)!!.level)
                for (a in listOf(null, false, true)) for (b in listOf(null, false, true)) {
                    assertTrue("$id $amount call=$call q1=$a q2=$b", rank.getValue(f.assessSafely(a, b)!!.level) >= base)
                }
            }
        }
    }

    @Test
    fun loginOpensTheRightModeAndRejectsWrongPins() {
        val f = PaymentFlow(model, FakeSignals(), { })
        assertEquals(Screen.Login, f.screen)
        assertFalse(f.login("sambhav", "9999"))
        assertFalse(f.login("nobody", "1111"))
        assertEquals(Screen.Login, f.screen)
        assertTrue(f.login("  Sambhav ", "1111"))                   // username is not case-sensitive
        assertEquals(Screen.Home, f.screen)
        assertEquals("Sambhav Jain", f.profile.displayName)
        assertTrue(f.back())                                        // Back on Home logs out
        assertEquals(Screen.Login, f.screen)
        assertNull(f.account)
        assertTrue(f.login("judge", "0000")); assertEquals(Screen.Judge, f.screen); f.logout()
        assertTrue(f.login("analyst", "9999")); assertEquals(Screen.Analyst, f.screen); f.logout()
        assertTrue(f.login("sehjal", "3333")); assertEquals(Screen.FamilyWaiting, f.screen)
        assertTrue(f.back())                                        // Back on Sehjal's screen logs out
        assertEquals(Screen.Login, f.screen)
        assertFalse(f.back())                                       // on Login there is nothing to go back to
    }

    @Test
    fun everyPersonHasTheirOwnUpiPinAndThreeWrongTriesCancel() {
        val f = newFlow(FakeSignals())
        f.openPerson("suresh"); f.toAmount(); f.typeAmount(500); f.pay()
        assertEquals(Screen.Pin, f.screen)
        f.finishPin("7316")                                         // Deepak's UPI PIN: wrong for Sambhav
        assertEquals(Screen.Pin, f.screen)
        assertEquals("Incorrect UPI PIN. 2 attempts left", f.message)
        f.finishPin("0000")
        assertEquals("Incorrect UPI PIN. 1 attempt left", f.message)
        f.finishPin("1234")
        assertEquals(Screen.Home, f.screen)                         // cancelled, nothing paid
        assertTrue(f.ledger.isEmpty())

        val priya = PaymentFlow(model, FakeSignals(), { }).apply { login("deepak", "2222") }
        priya.openPerson("suresh"); priya.toAmount(); priya.typeAmount(500); priya.pay()
        priya.finishPin("7316")
        assertEquals(Screen.Success, priya.screen)
    }

    @Test
    fun oneLakhIsTheMostForOnePayment() {
        val f = newFlow(FakeSignals())
        f.openPerson("suresh"); f.toAmount()
        f.typeAmount(100_000)
        assertEquals(100_000L, f.amount)
        f.amountDigit(0)                                            // ₹10,00,000: refused
        assertEquals(100_000L, f.amount)
        assertEquals(PaymentFlow.LIMIT_MESSAGE, f.message)
        f.amountDelete(); f.amountDigit(5)                          // ₹1,00,005: also refused
        assertEquals(10_000L, f.amount)
        f.amountDelete(); f.amountDigit(5)                          // ₹10,005 is fine
        assertEquals(10_005L, f.amount)

        // a Judge test above the limit is refused at Pay
        f.logout(); f.login("judge", "0000")
        f.runJudge(com.example.scriptbreak.ml.Payment(amount = 150_000.0, median90d = 650.0, payments90d = 42), null, null)
        f.pay()
        assertEquals(Screen.Amount, f.screen)
        assertEquals(PaymentFlow.LIMIT_MESSAGE, f.message)
    }

    @Test
    fun familyAlertReachesRohitsPhoneAndHisAnswerComesBack() {
        val rohitPhone = PaymentFlow(model, FakeSignals(), { }).apply { login("sehjal", "3333") }
        assertEquals(Screen.FamilyWaiting, rohitPhone.screen)
        val sharmaPhone = newFlow(FakeSignals(call = true))
        sharmaPhone.payTyped("ramesh.k@demoupi", 20_000)
        sharmaPhone.askFamily()
        assertTrue("Sehjal's phone buzzes", rohitPhone.onIncomingAlert(sent.single()))
        assertEquals(Screen.FamilyAlert, rohitPhone.screen)
        assertEquals("Sambhav", rohitPhone.lastAlert!!.fromName)
        assertFalse("the sender's own echo doesn't buzz", sharmaPhone.onIncomingAlert(sent.single()))
        sharmaPhone.consumeMessage()
        sharmaPhone.onFamilyStatus("calling")
        assertEquals("Sehjal is calling you now", sharmaPhone.message)
        rohitPhone.familyDone()
        assertEquals(Screen.FamilyWaiting, rohitPhone.screen)
    }

    @Test
    fun rupeesUseIndianGrouping() {
        assertEquals("₹1,50,000", PaymentFlow.rupees(150_000))
        assertEquals("₹48,000", PaymentFlow.rupees(48_000))
    }
}
