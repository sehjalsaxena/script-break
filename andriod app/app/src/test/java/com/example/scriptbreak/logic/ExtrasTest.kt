package com.example.scriptbreak.logic

import com.example.scriptbreak.ml.ScamModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Report packet, trusted contact (OTP + 24 hours), and the results scoreboard data. */
class ExtrasTest {
    private val assets = File("src/main/assets")
    private val ml = File("../../ml")
    private val model = ScamModel.fromJson(File(assets, "scam_model.json").readText())
    private var now = 1_800_000_000_000L

    private class OnCall : SignalSource {
        override val callNow = true
        override val screenShare = false
        override fun callSeenWithin30m(nowMs: Long) = false
        override fun isNight(nowMs: Long) = false
    }

    private fun sharma() = PaymentFlow(model, OnCall(), { }, clock = { now }).apply { login("sharma", "1111") }

    @Test
    fun reportPacketHasEverythingFor1930() {
        val f = sharma()
        f.startPayToUpi(); "ramesh.k@demoupi".forEach { f.upiChanged(f.upiInput + it) }; f.continueWithUpi()
        f.toAmount(); "20000".forEach { f.amountDigit(it.digitToInt()) }
        f.pay(); f.pauseNext()                                   // Pause shown, person continues
        "4821".forEach { f.pinDigit(it.digitToInt()) }; f.pinSubmit()
        f.openReport()
        assertEquals(Screen.Report, f.screen)
        val p = f.reportPacket!!
        assertEquals("₹20,000", p.amountText)
        assertEquals("Ramesh K.", p.payeeName)
        assertEquals("ramesh.k@demoupi", p.payeeUpiId)
        assertEquals(12, p.upiRef.length)
        assertEquals("PAUSE", p.levelShown)
        assertTrue(p.reasons.contains("You are on a call right now"))
        val text = p.asText()
        listOf("₹20,000", "ramesh.k@demoupi", p.upiRef, "1930").forEach { assertTrue(it, text.contains(it)) }
        f.back()
        assertEquals(Screen.Success, f.screen)
    }

    @Test
    fun newTrustedContactNeedsTheCodeAndWaits24Hours() {
        val f = sharma()
        assertEquals("Rohit", f.contact.name)
        f.openTrustedContact()
        f.requestContactChange("Anita", "daughter", "12345")           // not a valid mobile number
        assertNull(f.otpFor)
        f.requestContactChange("Anita", "daughter", "9811122233")
        val code = f.demoOtp!!
        f.verifyContactOtp("000000")
        assertEquals("Wrong code", f.message)
        f.verifyContactOtp(code)
        assertNotNull(f.pendingContact)
        assertEquals("Rohit stays until the 24 hours pass", "Rohit", f.contact.name)
        now += PaymentFlow.TWENTY_FOUR_HOURS - 1
        assertEquals("Rohit", f.contact.name)
        now += 1
        assertEquals("Anita", f.contact.name)
        assertNull(f.pendingContact)
    }

    @Test
    fun aPendingChangeCanBeCancelled() {
        val f = sharma()
        f.openTrustedContact()
        f.requestContactChange("Scammer", "", "9000000000")
        f.verifyContactOtp(f.demoOtp!!)
        f.cancelPendingContact()
        now += PaymentFlow.TWENTY_FOUR_HOURS
        assertEquals("Rohit", f.contact.name)
    }

    @Test
    fun scoreboardDataMatchesTheTestKit() {
        for (name in listOf("report.json", "test_summary.json")) {
            assertEquals("assets/$name must equal ml/results/$name", File(ml, "results/$name").readText(), File(assets, name).readText())
        }
        val d = ResultsData.parse(File(assets, "report.json").readText(), File(assets, "test_summary.json").readText())
        assertEquals(30000, d.testGenuine)
        assertEquals(3600, d.testScams)
        assertEquals(8, d.byType.size)
        assertTrue(d.aiInScope > d.rulesInScope && d.rulesInScope > d.bankInScope)
        assertTrue(d.aiGenuinePer1000 <= 8.0)
        assertTrue(d.byType.all { it.neverSeen != null })
        assertTrue("fairness table present", d.fairness.keys.any { it.startsWith("First-time users") })
    }
}
