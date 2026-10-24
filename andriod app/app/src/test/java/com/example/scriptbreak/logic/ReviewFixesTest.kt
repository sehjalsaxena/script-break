package com.example.scriptbreak.logic

import com.example.scriptbreak.ml.Payment
import com.example.scriptbreak.ml.ScamModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Bugs found in the full code review (24 Oct), each with a test so they stay fixed. */
class ReviewFixesTest {
    private val model = ScamModel.fromJson(File("src/main/assets/scam_model.json").readText())
    private var now = 1_800_000_000_000L

    private class NoSignals(val call: Boolean = false) : SignalSource {
        override val callNow get() = call
        override val screenShare = false
        override fun callSeenWithin30m(nowMs: Long) = false
        override fun isNight(nowMs: Long) = false
    }

    private fun flow(user: String, pin: String, call: Boolean = false) =
        PaymentFlow(model, NoSignals(call), { }, clock = { now }).apply { assertTrue(login(user, pin)) }

    private fun PaymentFlow.finishPin(pin: String) { pin.forEach { pinDigit(it.digitToInt()) }; pinSubmit() }

    @Test
    fun judgeTestPaymentsDoNotChangeAnyonesHistory() {
        val f = flow("sambhav", "1111")
        f.runJudge(Payment(amount = 500.0, median90d = 650.0, payments90d = 42, payeeStatus = "known"), null, null)
        f.pay()
        assertEquals(Screen.Pin, f.screen)
        f.finishPin("0000")                                          // test runs accept any PIN
        assertEquals(Screen.Success, f.screen)
        assertTrue("a test payment is not a real payment", f.ledger.isEmpty())
    }

    @Test
    fun eachPersonHasTheirOwnPaymentRecord() {
        val f = flow("sambhav", "1111")
        f.openPerson("suresh"); f.toAmount(); "500".forEach { f.amountDigit(it.digitToInt()) }; f.pay()
        f.finishPin("4821")
        assertEquals(1, f.ledger.size)
        f.logout()
        assertTrue(f.login("deepak", "2222"))
        assertTrue("Deepak does not see Sambhav's payment", f.ledger.isEmpty())
        f.logout(); f.login("sambhav", "1111")
        assertEquals(1, f.ledger.size)
    }

    @Test
    fun nudgeLineUsesTheJudgesUsualPaymentNotTheProfiles() {
        val f = flow("sambhav", "1111")
        // usual payment ₹10,000 in the judge's inputs: ₹2,000 is NOT "3× your usual payment"
        val p = Payment(amount = 2000.0, median90d = 10_000.0, payments90d = 40, payeeStatus = "new")
        f.runJudge(p, null, null)
        val text = f.nudge ?: return                                   // only checked if the model nudges
        assertFalse(text, text.contains("× your usual payment"))
    }

    @Test
    fun readingTheContactChangesNothingAndAFinishedChangeIsKept() {
        val f = flow("sambhav", "1111")
        f.openTrustedContact()
        f.requestContactChange("Anita", "daughter", "9811122233")
        f.verifyContactOtp(f.demoOtp!!)
        now += PaymentFlow.TWENTY_FOUR_HOURS
        assertEquals("Anita", f.contact.name)
        f.cancelPendingContact()                                     // nothing is waiting any more
        assertEquals("Anita stays: her 24 hours have passed", "Anita", f.contact.name)
        f.requestContactChange("Vijay", "", "9822233344")
        f.verifyContactOtp(f.demoOtp!!)
        assertEquals("Anita is in charge until Vijay's 24 hours pass", "Anita", f.contact.name)
    }

    @Test
    fun bankAccountTransfersGetTheSameCheckAndLimit() {
        val f = flow("sambhav", "1111", call = true)
        f.startBankSelf()
        f.openBankAccount("Ramesh Kumar", "10023456789", "demo0001234")
        assertEquals(Screen.Payee, f.screen)
        assertEquals("new", f.statusOf(f.payee!!))
        f.toAmount(); "20000".forEach { f.amountDigit(it.digitToInt()) }; f.pay()
        assertEquals("a new account on a call is checked like a UPI payment", Screen.Pause, f.screen)

        f.done()
        f.openSelfTransfer()
        assertEquals("known", f.statusOf(f.payee!!))
        f.toAmount(); "100000".forEach { f.amountDigit(it.digitToInt()) }; f.amountDigit(0)
        assertEquals(PaymentFlow.LIMIT_MESSAGE, f.message)
        assertNotEquals(1_000_000L, f.amount)
    }

    @Test
    fun testFilesMayUseOneAndZeroForYesAndNo() {
        val (rows, problems) = KitParser.parse("""[{"amount": 20000, "median_90d": 650, "payments_90d": 42, "call_now": 1, "payee_status": "NEW", "label": 1}]""")
        assertTrue(problems.toString(), problems.isEmpty())
        assertTrue(rows[0].payment.callNow)
        assertEquals("new", rows[0].payment.payeeStatus)
    }
}
