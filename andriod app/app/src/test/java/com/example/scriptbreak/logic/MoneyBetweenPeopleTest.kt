package com.example.scriptbreak.logic

import com.example.scriptbreak.ml.ScamModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Sambhav and Deepak can pay each other: the money arrives in the other's account, on their own phone. */
class MoneyBetweenPeopleTest {
    private val model = ScamModel.fromJson(File("src/main/assets/scam_model.json").readText())
    private var now = 1_800_000_000_000L

    private class Quiet : SignalSource {
        override val callNow = false
        override val screenShare = false
        override fun callSeenWithin30m(nowMs: Long) = false
        override fun isNight(nowMs: Long) = false
    }

    private fun phone(user: String, pin: String, sent: MutableList<DemoTransfer>) =
        PaymentFlow(model, Quiet(), { }, clock = { now }).apply { onSendMoney = { sent += it }; assertTrue(login(user, pin)) }

    private fun PaymentFlow.payTo(id: String, amount: Long, upiPin: String) {
        openPerson(id); toAmount(); amount.toString().forEach { amountDigit(it.digitToInt()) }; pay()
        upiPin.forEach { pinDigit(it.digitToInt()) }; pinSubmit()
    }

    @Test
    fun eachSeesTheOtherButNeverThemselves() {
        assertTrue(DemoWorld.peopleFor("sharma").any { it.name == "Deepak Rawat" })
        assertFalse(DemoWorld.peopleFor("sharma").any { it.name == "Sambhav Jain" })
        assertTrue(DemoWorld.contactsFor("priya").any { it.name == "Sambhav Jain" })
        assertFalse(DemoWorld.contactsFor("priya").any { it.name == "Deepak Rawat" })
        assertFalse(DemoWorld.allPeopleFor("sharma").any { it.upiId == "sambhav.jain@demoupi" })
        assertEquals("priya", DemoWorld.accountByUpi("Deepak.Rawat@demoupi")!!.mode)
    }

    @Test
    fun sambhavPaysDeepakAndItArrivesOnDeepaksPhone() {
        val sent = mutableListOf<DemoTransfer>()
        val sambhavPhone = phone("sambhav", "1111", sent)
        val deepakPhone = phone("deepak", "2222", mutableListOf())
        val before = sambhavPhone.balance
        val deepakBefore = deepakPhone.balance

        sambhavPhone.payTo("deepak", 500, "4821")
        assertEquals(Screen.Success, sambhavPhone.screen)
        assertEquals(before - 500, sambhavPhone.balance)
        val t = sent.single()
        assertEquals("priya", t.toProfile)
        assertEquals("Sambhav Jain", t.fromName)
        assertEquals("sambhav.jain@demoupi", t.fromUpi)

        deepakPhone.onMoneyReceived(t)                                    // through Firebase
        assertEquals("Received ₹500 from Sambhav Jain", deepakPhone.message)
        assertEquals(deepakBefore + 500, deepakPhone.balance)
        deepakPhone.onMoneyReceived(t)                                    // the same transfer twice counts once
        assertEquals(deepakBefore + 500, deepakPhone.balance)

        // and back: Deepak returns ₹200
        val back = mutableListOf<DemoTransfer>()
        deepakPhone.onSendMoney = { back += it }
        deepakPhone.payTo("sambhav", 200, "7316")
        sambhavPhone.onMoneyReceived(back.single())
        assertEquals(before - 500 + 200, sambhavPhone.balance)
        assertEquals("Received ₹200 from Deepak Rawat", sambhavPhone.message)
    }

    @Test
    fun onOnePhoneTheMoneyIsThereWhenTheOtherPersonLogsIn() {
        val f = phone("sambhav", "1111", mutableListOf())
        f.payTo("deepak", 1500, "4821")
        f.done(); f.logout(); f.login("deepak", "2222")
        assertEquals(1, f.received.size)
        assertEquals(1500L, f.received.single().amount)
        assertEquals(DemoWorld.profiles.getValue("priya").openingBalance + 1500, f.balance)
    }

    @Test
    fun deepakScansSambhavsOwnQrAndPaysHim() {
        val text = UpiQr.payText("sambhav.jain@demoupi", "Sambhav Jain")
        assertEquals("upi://pay?pa=sambhav.jain@demoupi&pn=Sambhav%20Jain&cu=INR", text)
        val size = 400
        val cells = UpiQr.matrix(text, size)
        val pixels = IntArray(size * size) { i -> if (cells[i / size][i % size]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        val scanned = QrDecoder.decode(pixels, size, size)
        assertEquals("the QR made on the phone scans back exactly", text, scanned)

        val sent = mutableListOf<DemoTransfer>()
        val deepak = phone("deepak", "2222", sent)
        assertTrue(deepak.openFromQr(scanned))
        assertEquals("Sambhav Jain", deepak.payee!!.name)
        assertFalse("the QR's name matches the banking name", deepak.qrNameMismatch)
        deepak.toAmount(); "250".forEach { deepak.amountDigit(it.digitToInt()) }; deepak.pay()
        "7316".forEach { deepak.pinDigit(it.digitToInt()) }; deepak.pinSubmit()
        assertEquals(Screen.Success, deepak.screen)
        assertEquals("sharma", sent.single().toProfile)

        val sambhav = phone("sambhav", "1111", mutableListOf())    // scanning your own QR: not allowed to pay yourself
        sambhav.openFromQr(scanned); sambhav.toAmount(); "100".forEach { sambhav.amountDigit(it.digitToInt()) }; sambhav.pay()
        assertEquals("That's your own UPI ID. Pick someone else to pay.", sambhav.message)
    }

    @Test
    fun sambhavPaysSehjalWithHerQr() {
        val text = "upi://pay?pa=sehjal.saxena@demoupi&pn=Sehjal%20Saxena&cu=INR"   // docs/demo_qr/6_pay_sehjal_qr.png
        val sent = mutableListOf<DemoTransfer>()
        val sambhav = phone("sambhav", "1111", sent)
        assertTrue(sambhav.openFromQr(text))
        assertEquals("Sehjal Saxena", sambhav.payee!!.name)
        assertEquals("known", sambhav.statusOf(sambhav.payee!!))
        assertFalse(sambhav.qrNameMismatch)
        sambhav.toAmount(); "300".forEach { sambhav.amountDigit(it.digitToInt()) }; sambhav.pay()
        assertEquals("paid before, normal amount: straight to the PIN", Screen.Pin, sambhav.screen)
        "4821".forEach { sambhav.pinDigit(it.digitToInt()) }; sambhav.pinSubmit()
        assertEquals(Screen.Success, sambhav.screen)
        assertEquals("rohit", sent.single().toProfile)

        val sehjal = phone("sehjal", "3333", mutableListOf())
        sehjal.onMoneyReceived(sent.single())
        assertEquals("Received ₹300 from Sambhav Jain", sehjal.message)
    }

    @Test
    fun youCannotPayYourselfOrMoreThanYouHave() {
        val sent = mutableListOf<DemoTransfer>()
        val f = phone("sambhav", "1111", sent)
        f.openPerson("sambhav"); f.toAmount(); "100".forEach { f.amountDigit(it.digitToInt()) }; f.pay()
        assertEquals("That's your own UPI ID. Pick someone else to pay.", f.message)
        assertEquals(Screen.Amount, f.screen)

        val g = phone("deepak", "2222", sent)                           // ₹1,86,400 in the demo account
        repeat(2) { now += 60_000; g.payTo("suresh", 90_000, "7316"); g.done() }
        g.payTo("suresh", 90_000, "7316")
        assertTrue(g.message!!, g.message!!.startsWith("Payment failed: not enough balance"))
        assertEquals(Screen.Home, g.screen)
        assertEquals(2, g.ledger.size)
        assertTrue("no money is sent to anyone for a declined payment", sent.isEmpty())
    }

    @Test
    fun judgeTestPaymentsMoveNoMoney() {
        val sent = mutableListOf<DemoTransfer>()
        val f = phone("judge", "0000", sent)
        f.runJudge(com.example.scriptbreak.ml.Payment(amount = 500.0, median90d = 650.0, payments90d = 42), null, null)
        f.pay(); "0000".forEach { f.pinDigit(it.digitToInt()) }; f.pinSubmit()
        assertTrue(sent.isEmpty())
        assertNull(f.message?.takeIf { it.startsWith("Received") })
    }

    @Test
    fun resetDemoClearsTheMoneyThisPersonReceived() {
        var cleared: String? = null
        val f = phone("deepak", "2222", mutableListOf()).apply { onResetMoney = { cleared = it } }
        f.onMoneyReceived(DemoTransfer("t1", "priya", "Sambhav Jain", "sambhav.jain@demoupi", 700, now, "4111"))
        assertEquals(1, f.received.size)
        f.resetDemo()
        assertTrue(f.received.isEmpty())
        assertEquals("priya", cleared)
        assertEquals(DemoWorld.profiles.getValue("priya").openingBalance, f.balance)
    }
}
