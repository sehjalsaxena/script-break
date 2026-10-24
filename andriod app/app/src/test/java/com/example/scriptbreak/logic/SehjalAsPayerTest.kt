package com.example.scriptbreak.logic

import com.example.scriptbreak.ml.ScamModel
import com.example.scriptbreak.ui.model.FamilyAlertUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Sehjal pays like Sambhav and Deepak; alerts meant for Sehjal arrive in Sehjal's Notifications. */
class SehjalAsPayerTest {
    private val model = ScamModel.fromJson(File("src/main/assets/scam_model.json").readText())
    private var now = 1_800_000_000_000L

    private class Signals(val call: Boolean = false) : SignalSource {
        override val callNow get() = call
        override val screenShare = false
        override fun callSeenWithin30m(nowMs: Long) = false
        override fun isNight(nowMs: Long) = false
    }

    private fun phone(call: Boolean = false, sent: MutableList<FamilyAlertUi> = mutableListOf()) =
        PaymentFlow(model, Signals(call), { sent += it }, clock = { now })

    @Test
    fun sehjalPaysWithOwnUpiPinAndBalance() {
        val f = phone().apply { assertTrue(login("sehjal", "3333")) }
        assertEquals(Screen.Home, f.screen)
        assertEquals("Sehjal Saxena", f.profile.displayName)
        assertEquals(152_750L, f.balance)
        assertFalse("never sees themselves", DemoWorld.peopleFor("rohit").any { it.name == "Sehjal Saxena" })
        f.openPerson("sambhav"); f.toAmount(); "500".forEach { f.amountDigit(it.digitToInt()) }; f.pay()
        assertEquals(Screen.Pin, f.screen)
        "4821".forEach { f.pinDigit(it.digitToInt()) }; f.pinSubmit()          // Sambhav's PIN: wrong for Sehjal
        assertEquals("Incorrect UPI PIN. 2 attempts left", f.message)
        "2580".forEach { f.pinDigit(it.digitToInt()) }; f.pinSubmit()
        assertEquals(Screen.Success, f.screen)
        assertEquals(152_250L, f.balance)
    }

    @Test
    fun sambhavsAlertLandsInSehjalsNotifications() {
        val sent = mutableListOf<FamilyAlertUi>()
        val sambhav = phone(call = true, sent = sent).apply { login("sambhav", "1111") }
        sambhav.startPayToUpi(); "ramesh.k@demoupi".forEach { sambhav.upiChanged(sambhav.upiInput + it) }; sambhav.continueWithUpi()
        sambhav.toAmount(); "20000".forEach { sambhav.amountDigit(it.digitToInt()) }; sambhav.pay()
        sambhav.askFamily()
        assertEquals("rohit", sent.single().toProfile)

        val sehjal = phone().apply { phoneOwner = "rohit"; login("sehjal", "3333") }
        assertTrue("buzz + phone notification", sehjal.onIncomingAlert(sent.single()))
        assertEquals("no jump: Sehjal stays on Home", Screen.Home, sehjal.screen)
        assertEquals("in Sehjal's Notifications", 1, sehjal.myAlerts.size)
        sehjal.startNotifications()
        sehjal.openAlert(sehjal.myAlerts.single().key)                    // tap it in Notifications
        assertEquals(Screen.FamilyAlert, sehjal.screen)
        sehjal.familyDone()                                               // I've called
        assertEquals(Screen.Notifications, sehjal.screen)
        assertTrue(sehjal.myAlerts.single().done)

        val deepak = phone().apply { login("deepak", "2222") }           // not for Deepak: not in his list, no buzz
        assertFalse(deepak.onIncomingAlert(sent.single()))
        assertTrue(deepak.myAlerts.isEmpty())
        deepak.openAlertFromNotification()
        assertEquals("This alert is for Sehjal Saxena. Log in as sehjal to open it.", deepak.message)
    }

    @Test
    fun sehjalsOwnAskFamilyGoesToSambhav() {
        val sent = mutableListOf<FamilyAlertUi>()
        val sehjal = phone(call = true, sent = sent).apply { login("sehjal", "3333") }
        assertEquals("Sambhav", sehjal.contact.name)
        sehjal.startPayToUpi(); "ramesh.k@demoupi".forEach { sehjal.upiChanged(sehjal.upiInput + it) }; sehjal.continueWithUpi()
        sehjal.toAmount(); "20000".forEach { sehjal.amountDigit(it.digitToInt()) }; sehjal.pay()
        assertEquals(Screen.Pause, sehjal.screen)
        sehjal.askFamily()
        assertEquals("Sambhav has been alerted", sehjal.message)
        val alert = sent.single()
        assertEquals("Sehjal", alert.fromName)
        assertEquals("sharma", alert.toProfile)

        val sambhav = phone().apply { phoneOwner = "sharma"; login("sambhav", "1111") }
        assertTrue(sambhav.onIncomingAlert(alert))
        sambhav.openAlertFromNotification()                                // tap the phone notification
        assertEquals(Screen.FamilyAlert, sambhav.screen)
        assertEquals("Sehjal", sambhav.lastAlert!!.fromName)
        sehjal.consumeMessage(); sehjal.onFamilyStatus("calling")
        assertEquals("Sambhav is calling you now", sehjal.message)
    }
}
