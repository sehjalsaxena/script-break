package com.example.scriptbreak.logic

import com.example.scriptbreak.ml.ScamModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** "Pay from a QR image": same payee page, same AI check, same PIN as a typed UPI ID. */
class QrPaymentTest {
    private val model = ScamModel.fromJson(File("src/main/assets/scam_model.json").readText())
    private val scamQr = "upi://pay?pa=ramesh.k@demoupi&pn=Bank%20Refund%20Desk&am=20000&cu=INR&tn=Refund+verification"
    private val tvQr = "upi://pay?pa=guptaelectronics@demoupi&pn=Gupta%20Electronics&am=90000&cu=INR&tn=TV"
    private val rentQr = "upi://pay?pa=suresh.verma@demoupi&pn=Suresh%20Verma&am=18000&cu=INR&tn=Rent"

    private class Signals(val call: Boolean = false) : SignalSource {
        override val callNow get() = call
        override val screenShare = false
        override fun callSeenWithin30m(nowMs: Long) = false
        override fun isNight(nowMs: Long) = false
    }

    private fun sambhav(call: Boolean = false) = PaymentFlow(model, Signals(call), { }).apply { assertTrue(login("sambhav", "1111")) }

    /** A QR image as a camera/screenshot would give it: black and white pixels, with a white border. */
    private fun image(text: String, size: Int = 360): Triple<IntArray, Int, Int> {
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val px = IntArray(size * size) { i -> if (m.get(i % size, i / size)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        return Triple(px, size, size)
    }

    @Test
    fun readsEveryPartOfAUpiQr() {
        val q = UpiQr.parse(scamQr)!!
        assertEquals("ramesh.k@demoupi", q.upiId)
        assertEquals("Bank Refund Desk", q.name)
        assertEquals(20000.0, q.amount!!, 0.0)
        assertEquals("Refund verification", q.note)
        assertEquals("suresh.verma@demoupi", UpiQr.parse("UPI://PAY?PA=Suresh.Verma%40DemoUPI&AM=abc")!!.upiId)
        assertNull("bad amount is ignored", UpiQr.parse("upi://pay?pa=a.b@demoupi&am=abc")!!.amount)
        assertNull(UpiQr.parse("upi://pay?pa=a.b@demoupi&am=-5")!!.amount)
        assertNull("not a UPI code", UpiQr.parse("https://example.com/pay"))
        assertNull("no payee", UpiQr.parse("upi://pay?pn=Someone&am=100"))
        assertNull(UpiQr.parse(null))
    }

    @Test
    fun decodesTheQrInAnImage() {
        for (text in listOf(scamQr, tvQr, rentQr)) {
            val (px, w, h) = image(text)
            assertEquals(text, QrDecoder.decode(px, w, h))
        }
        val blank = IntArray(200 * 200) { 0xFFFFFFFF.toInt() }
        assertNull("no QR in a blank image", QrDecoder.decode(blank, 200, 200))
    }

    @Test
    fun scamQrOnACallGoesThroughTheSameCheck() {
        val f = sambhav(call = true)
        val (px, w, h) = image(scamQr)
        f.openFromQr(QrDecoder.decode(px, w, h))
        assertEquals(Screen.Payee, f.screen)
        assertEquals("Ramesh K.", f.payee!!.name)                       // never the name written in the QR
        assertTrue(f.qrNotice!!, f.qrNotice!!.contains("Bank Refund Desk"))
        f.toAmount()
        assertEquals("the QR's amount is filled in", 20_000L, f.amount)
        assertEquals("Refund verification", f.note)
        assertTrue("the Safety check repeats the QR name warning: ${f.aiView!!.facts}",
            f.aiView!!.facts.contains("⚠ The QR says \"Bank Refund Desk\", not the banking name"))
        f.pay()
        assertEquals(Screen.Pause, f.screen)
        assertTrue(f.pauseResult!!.reasons.contains(PaymentFlow.CALL_NOW))
    }

    @Test
    fun genuineQrPaymentsAreNotInterrupted() {
        val rent = sambhav()
        rent.openFromQr(rentQr)
        assertEquals("known", rent.statusOf(rent.payee!!))
        assertEquals("From a QR code. Check the name and the amount before you pay.", rent.qrNotice)
        rent.toAmount()
        assertFalse("a matching QR name is not a warning", rent.qrNameMismatch)
        assertTrue(rent.aiView!!.facts.none { it.contains("QR") })
        rent.pay()
        assertEquals("rent to the usual landlord: straight to the PIN", Screen.Pin, rent.screen)

        val tv = sambhav()
        tv.openFromQr(tvQr)
        tv.toAmount()
        assertNotNull("amber line only", tv.nudge)
        tv.pay()
        assertEquals("a shop QR with no sign of coaching: no popup (C1)", Screen.Pin, tv.screen)
    }

    @Test
    fun unusableQrCodesAreExplainedAndChangeNothing() {
        val f = sambhav()
        f.openFromQr(null)
        assertEquals("No QR code found in that image. Try a clearer picture.", f.message)
        f.openFromQr("https://example.com")
        assertEquals("This QR code is not a UPI payment code", f.message)
        f.openFromQr("upi://pay?pa=shop@okaxis&am=500")
        assertTrue(f.message!!, f.message!!.contains("only pays @demoupi"))
        assertEquals(Screen.Home, f.screen)
        assertNull(f.payee)

        f.openFromQr("upi://pay?pa=anil.mehta@demoupi&am=250000")
        assertEquals(PaymentFlow.LIMIT_MESSAGE, f.message)
        f.toAmount()
        assertEquals("over the limit: not filled in", 0L, f.amount)
    }

    @Test
    fun decodesACameraFrame() {
        val size = 480
        val m = QRCodeWriter().encode(scamQr, BarcodeFormat.QR_CODE, size, size)
        val y = ByteArray(size * size) { i -> if (m.get(i % size, i / size)) 0 else 255.toByte() }   // brightness plane
        assertEquals(scamQr, QrDecoder.decodeLuminance(y, size, size))
    }

    @Test
    fun cameraScannerOpensFromHomeAndBackGoesHome() {
        val f = sambhav(call = true)
        f.startQrScanner()
        assertEquals(Screen.QrScanner, f.screen)
        assertFalse("a non-UPI code keeps the scanner open", f.openFromQr("https://example.com"))
        assertEquals(Screen.QrScanner, f.screen)
        f.back()
        assertEquals(Screen.Home, f.screen)
        f.startQrScanner()
        assertTrue(f.openFromQr(scamQr))
        assertEquals(Screen.Payee, f.screen)
    }

    @Test
    fun judgeModeFillsItsInputsFromAQr() {
        val f = PaymentFlow(model, Signals(), { }).apply { assertTrue(login("judge", "0000")) }
        val judge = JudgeMode(model, emptyList())
        f.startQrScanner(forJudge = true)
        assertEquals(Screen.QrScanner, f.screen)
        assertNull(judge.fillFromQr(scamQr))
        f.closeQrScanner()
        assertEquals("back to Judge mode", Screen.Judge, f.screen)
        assertEquals(20000.0, judge.inputs.amount, 0.0)
        assertEquals("new", judge.inputs.payeeStatus)
        assertEquals("person", judge.inputs.payeeKind)
        assertTrue(judge.qrNote!!, judge.qrNote!!.contains("Bank Refund Desk") && judge.qrNote!!.contains("not verified"))

        assertNull(judge.fillFromQr(tvQr))
        assertEquals("merchant", judge.inputs.payeeKind)
        assertNull(judge.fillFromQr(rentQr))
        assertEquals("known", judge.inputs.payeeStatus)
        assertEquals(18000.0, judge.inputs.amount, 0.0)

        assertEquals("This QR code is not a UPI payment code", judge.fillFromQr("hello"))
        assertTrue(judge.fillFromQr("upi://pay?pa=shop@okaxis")!!.contains("real UPI ID"))
        assertEquals("unchanged after a bad QR", 18000.0, judge.inputs.amount, 0.0)
        f.startQrScanner(forJudge = true); f.back()
        assertEquals(Screen.Judge, f.screen)
    }

    @Test
    fun aQrAmountDoesNotLeakIntoTheNextPayment() {
        val f = sambhav()
        f.openFromQr(scamQr)
        f.back()                                                        // changed their mind
        f.openPerson("suresh"); f.toAmount()
        assertEquals(0L, f.amount)
        assertFalse(f.fromQr)
        assertNull(f.qrNotice)
    }
}
