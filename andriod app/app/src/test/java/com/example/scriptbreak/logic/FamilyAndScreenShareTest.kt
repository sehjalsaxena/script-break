package com.example.scriptbreak.logic

import com.example.scriptbreak.ml.ScamModel
import com.example.scriptbreak.ui.model.FamilyAlertUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Three-phone demo: the family alert reaches Sehjal whenever he logs in; call and screen share are both shown. */
class FamilyAndScreenShareTest {
    private val model = ScamModel.fromJson(File("src/main/assets/scam_model.json").readText())
    private var now = 1_800_000_000_000L
    private val alert = FamilyAlertUi("Sambhav", "₹48,000", "Ramesh K.", listOf("You are on a call right now"))

    private class Signals(val call: Boolean = false, val share: Boolean = false) : SignalSource {
        override val callNow get() = call
        override val screenShare get() = share
        override fun callSeenWithin30m(nowMs: Long) = false
        override fun isNight(nowMs: Long) = false
    }

    private fun phone(s: SignalSource = Signals(), sent: MutableList<FamilyAlertUi> = mutableListOf()) =
        PaymentFlow(model, s, { sent += it }, clock = { now })

    private fun PaymentFlow.payRamesh(amount: String) {
        startPayToUpi(); "ramesh.k@demoupi".forEach { upiChanged(upiInput + it) }; continueWithUpi(); toAmount()
        amount.forEach { amountDigit(it.digitToInt()) }; pay()
    }

    @Test
    fun alertArrivingBeforeRohitLogsInIsShownWhenHeDoes() {
        val rohitPhone = phone().apply { phoneOwner = "rohit" }         // Sehjal's phone, on the login screen
        assertEquals(Screen.Login, rohitPhone.screen)
        assertTrue("Sehjal's phone buzzes even before login", rohitPhone.onIncomingAlert(alert))
        assertEquals(Screen.Login, rohitPhone.screen)
        now += 2 * 60_000
        assertTrue(rohitPhone.login("sehjal", "3333"))
        assertEquals(Screen.FamilyAlert, rohitPhone.screen)
        assertEquals("Sambhav", rohitPhone.lastAlert!!.fromName)
    }

    @Test
    fun otherPhonesOnTheLoginScreenDoNotBuzz() {
        val judgePhone = phone().apply { phoneOwner = "judge" }
        assertFalse("not Sehjal's phone: no buzz", judgePhone.onIncomingAlert(alert))
        val newPhone = phone()                                           // nobody has logged in here yet
        assertFalse(newPhone.onIncomingAlert(alert))
        judgePhone.login("sehjal", "3333")                                // but Sehjal can still read it there
        assertEquals(Screen.FamilyAlert, judgePhone.screen)
        assertEquals("rohit", judgePhone.phoneOwner)
    }

    @Test
    fun alertIsKeptQuietlyWhileSomeoneElseUsesThePhone() {
        val p = phone().apply { login("judge", "0000") }
        assertFalse("no buzz in the middle of Judge mode", p.onIncomingAlert(alert))
        assertEquals(Screen.Judge, p.screen)
        p.logout(); p.login("sehjal", "3333")
        assertEquals(Screen.FamilyAlert, p.screen)
    }

    @Test
    fun aMissedAlertIsStillShownWithItsTimeWhenRohitLogsIn() {
        val p = phone()
        var fetched = 0
        p.onFamilyLogin = { fetched++ }
        val sent = alert.copy(sentAtMs = now)
        p.onIncomingAlert(sent)
        now += 45 * 60_000                                               // Sehjal logs in 45 minutes later
        p.login("sehjal", "3333")
        assertEquals(Screen.FamilyAlert, p.screen)
        assertEquals("the screen shows when it was sent", sent.sentAtMs, p.lastAlert!!.sentAtMs)
        assertEquals("Sehjal's login also asks Firebase", 1, fetched)
        // Firebase answers with the same alert: shown, but no second buzz
        assertFalse(p.onIncomingAlert(sent))
        assertEquals(Screen.FamilyAlert, p.screen)
        // once Sehjal has answered, the same alert never comes back
        p.familyDone()
        assertFalse(p.onIncomingAlert(sent))
        assertEquals(Screen.FamilyWaiting, p.screen)
    }

    @Test
    fun alertFetchedOnlyAfterLoginOpensTheAlert() {
        val p = phone()
        p.login("sehjal", "3333")                                         // app was closed when it was sent
        assertEquals(Screen.FamilyWaiting, p.screen)
        assertTrue(p.onIncomingAlert(alert.copy(sentAtMs = now - 20 * 60_000)))
        assertEquals(Screen.FamilyAlert, p.screen)
    }

    @Test
    fun oldOrHandledAlertsAreNotShown() {
        val p = phone()
        p.onIncomingAlert(alert)
        now += 25L * 60 * 60_000                                         // more than 24 hours later
        p.login("sehjal", "3333")
        assertEquals(Screen.FamilyWaiting, p.screen)

        val q = phone()
        q.onIncomingAlert(alert)
        q.onAlertCleared()                                               // Sehjal already handled it on another phone
        q.login("sehjal", "3333")
        assertEquals(Screen.FamilyWaiting, q.screen)
        assertNull(q.lastAlert)
    }

    @Test
    fun callAndScreenShareAreBothShownWhenBothHappen() {
        val sent = mutableListOf<FamilyAlertUi>()
        val f = phone(Signals(call = true, share = true), sent).apply { login("sambhav", "1111") }
        f.payRamesh("20000")
        val shown = (f.criticalResult ?: f.pauseResult)!!
        assertEquals("CRITICAL", shown.level)
        assertTrue(shown.reasons.toString(), PaymentFlow.CALL_NOW in shown.reasons && PaymentFlow.SCREEN_SHARE in shown.reasons)
        f.askFamily()
        val r = sent.single().reasons
        assertTrue("Sehjal is told about both: $r", PaymentFlow.CALL_NOW in r && PaymentFlow.SCREEN_SHARE in r)
    }

    @Test
    fun screenShareAloneOrCallAloneIsShown() {
        val share = phone(Signals(share = true)).apply { login("sambhav", "1111") }
        share.payRamesh("20000")
        assertEquals(Screen.Pause, share.screen)
        assertTrue(PaymentFlow.SCREEN_SHARE in share.pauseResult!!.reasons)
        assertFalse(PaymentFlow.CALL_NOW in share.pauseResult!!.reasons)

        val call = phone(Signals(call = true)).apply { login("sambhav", "1111") }
        call.payRamesh("20000")
        assertTrue(PaymentFlow.CALL_NOW in call.pauseResult!!.reasons)
        assertFalse(PaymentFlow.SCREEN_SHARE in call.pauseResult!!.reasons)
    }

    @Test
    fun judgeCanSimulateScreenSharing() {
        val f = phone().apply { login("sambhav", "1111") }
        f.simulateScreenShare = true
        assertTrue(f.screenShareNow)
        f.payRamesh("20000")
        assertEquals(Screen.Pause, f.screen)
        assertTrue(PaymentFlow.SCREEN_SHARE in f.pauseResult!!.reasons)
        f.resetDemo()
        assertFalse(f.screenShareNow)
    }
}
