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
        assertEquals("Sehjal's alerts list, not a jump", Screen.FamilyWaiting, rohitPhone.screen)
        val item = rohitPhone.familyInbox.single()
        assertEquals("Sambhav", item.alert.fromName)
        assertFalse("unread", item.read)
        rohitPhone.openAlert(item.key)                                   // tap it in the list
        assertEquals(Screen.FamilyAlert, rohitPhone.screen)
        assertEquals("Sambhav", rohitPhone.lastAlert!!.fromName)
        rohitPhone.back()
        assertEquals("Back returns to the list, it does not log out", Screen.FamilyWaiting, rohitPhone.screen)
        assertTrue(rohitPhone.familyInbox.single().read)
    }

    @Test
    fun tappingThePhoneNotificationOpensTheAlert() {
        val p = phone().apply { phoneOwner = "rohit"; login("sehjal", "3333") }
        assertTrue("buzz + phone notification", p.onIncomingAlert(alert.copy(sentAtMs = now)))
        assertEquals("nothing jumps open by itself", Screen.FamilyWaiting, p.screen)
        p.openAlertFromNotification()
        assertEquals(Screen.FamilyAlert, p.screen)

        // the notification tapped while the phone is on the login page: opens right after Sehjal logs in
        val q = phone().apply { phoneOwner = "rohit" }
        q.onIncomingAlert(alert.copy(sentAtMs = now))
        q.openAlertFromNotification()
        assertEquals(Screen.Login, q.screen)
        q.login("sehjal", "3333")
        assertEquals(Screen.FamilyAlert, q.screen)

        // ...also when the alert only arrives from Firebase after the login
        val r = phone().apply { phoneOwner = "rohit" }
        r.openAlertFromNotification()
        r.login("sehjal", "3333")
        assertEquals(Screen.FamilyWaiting, r.screen)
        r.onIncomingAlert(alert.copy(sentAtMs = now))
        assertEquals(Screen.FamilyAlert, r.screen)

        // someone else is using the phone: it says who the alert is for
        val s = phone().apply { login("sambhav", "1111") }
        s.openAlertFromNotification()
        assertEquals("This alert is for Sehjal. Log in as sehjal to open it.", s.message)
        assertEquals(Screen.Home, s.screen)
    }

    @Test
    fun otherPhonesOnTheLoginScreenDoNotBuzz() {
        val judgePhone = phone().apply { phoneOwner = "judge" }
        assertFalse("not Sehjal's phone: no buzz", judgePhone.onIncomingAlert(alert))
        val newPhone = phone()                                           // nobody has logged in here yet
        assertFalse(newPhone.onIncomingAlert(alert))
        judgePhone.login("sehjal", "3333")                                // but Sehjal can still read it there
        assertEquals(Screen.FamilyWaiting, judgePhone.screen)
        assertEquals(1, judgePhone.familyInbox.size)
        assertEquals("rohit", judgePhone.phoneOwner)
    }

    @Test
    fun alertIsKeptQuietlyWhileSomeoneElseUsesThePhone() {
        val p = phone().apply { login("judge", "0000") }
        assertFalse("no buzz in the middle of Judge mode", p.onIncomingAlert(alert))
        assertEquals(Screen.Judge, p.screen)
        p.logout(); p.login("sehjal", "3333")
        assertEquals(Screen.FamilyWaiting, p.screen)
        assertEquals("Sambhav", p.familyInbox.single().alert.fromName)
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
        assertEquals(Screen.FamilyWaiting, p.screen)
        assertEquals("the list shows when it was sent", sent.sentAtMs, p.familyInbox.single().key)
        assertEquals("Sehjal's login also asks Firebase", 1, fetched)
        // Firebase answers with the same alert: listed once, no second buzz
        assertFalse(p.onIncomingAlert(sent))
        assertEquals(1, p.familyInbox.size)
        p.openAlert(sent.sentAtMs!!)
        assertEquals(Screen.FamilyAlert, p.screen)
        // once Sehjal has answered, it is marked done and never comes back
        p.familyDone()
        assertEquals(Screen.FamilyWaiting, p.screen)
        assertTrue(p.familyInbox.single().done)
        assertFalse(p.onIncomingAlert(sent))
    }

    @Test
    fun alertFetchedOnlyAfterLoginOpensTheAlert() {
        val p = phone()
        p.login("sehjal", "3333")                                         // app was closed when it was sent
        assertEquals(Screen.FamilyWaiting, p.screen)
        assertTrue(p.onIncomingAlert(alert.copy(sentAtMs = now - 20 * 60_000)))
        assertEquals("listed (and the phone notification shown), no jump", Screen.FamilyWaiting, p.screen)
        assertEquals(1, p.familyInbox.size)
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
        assertTrue("listed as answered, not open", q.familyInbox.single().done)
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
