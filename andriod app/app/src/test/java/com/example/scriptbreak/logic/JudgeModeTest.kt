package com.example.scriptbreak.logic

import com.example.scriptbreak.ml.Payment
import com.example.scriptbreak.ml.ScamModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Judge mode: inputs, the 28 stories, the test-file loader, and "Run in app". */
class JudgeModeTest {
    private val model = ScamModel.fromJson(javaClass.classLoader!!.getResource("scam_model.json")!!.readText())
    // Unit tests run from the app module folder ("andriod app/app")
    private val assets = File("src/main/assets")
    private val repo = File("../..")
    private val stories = JudgeMode.parseStories(File(assets, "cases.json").readText())
    private val judge = JudgeMode(model, stories)

    @Test
    fun appCopiesMatchTheMasterFiles() {
        assertEquals("assets/cases.json must equal tests/cases.json", File(repo, "tests/cases.json").readText(), File(assets, "cases.json").readText())
        assertEquals("assets/sample_kit.jsonl must equal ml/sample_kit.jsonl", File(repo, "ml/sample_kit.jsonl").readText(), File(assets, "sample_kit.jsonl").readText())
    }

    @Test
    fun allTwentyEightStoriesLoadWithTheirAnswers() {
        assertEquals(28, stories.size)
        val s01 = stories.first { it.id == "S01" }
        assertEquals(true, s01.q1)
        assertNull(s01.q2)
        assertEquals(48000.0, s01.payment.amount, 0.0)
        judge.loadStory(s01)
        assertEquals("CRITICAL", judge.result!!.level)
        assertEquals("S01", judge.storyId)
    }

    @Test
    fun storyLevelsMatchTheModelReport() {
        // The same totals as ml/results/REPORT.md: 14 of 16 scams flagged, 2 of 10 genuine interrupted
        val flagged = { s: Story -> judge.levelOf(s) in setOf("PAUSE", "CRITICAL") }
        assertEquals(14, stories.filter { it.kind == "scam" }.count(flagged))
        assertEquals(2, stories.filter { it.kind == "legit" }.count(flagged))
        assertEquals("NONE", judge.levelOf(stories.first { it.id == "S06" }))   // named failure: split payments
        assertEquals("PAUSE", judge.levelOf(stories.first { it.id == "L04" }))  // named failure: new landlord
    }

    @Test
    fun editingInputsChangesTheResultLive() {
        judge.reset()
        assertEquals("PAUSE", judge.result!!.level)            // default: on a call, new payee, ₹20,000
        judge.edit { it.copy(callNow = false, payeeStatus = "known") }
        assertEquals("NONE", judge.result!!.level)
        judge.edit { it.copy(payeeStatus = "flagged") }
        assertEquals("CRITICAL", judge.result!!.level)          // G1
        assertTrue(judge.result!!.guardrails.contains("G1"))
    }

    @Test
    fun readsJsonArrayRecordsJsonLinesAndCsv() {
        val array = """[{"features":{"amount":48000,"median_90d":650,"payments_90d":42,"call_now":true,"payee_status":"new"},"label":1,"workflow":"digital_arrest"},
                        {"amount":350,"median_90d":650,"payee_status":"known","payee_kind":"merchant","label":0}]"""
        val a = judge.scoreKit(array, "a.json")
        assertEquals(2, a.rows.size)
        assertEquals(1, a.scamsFlagged.k)
        assertEquals(0, a.genuineInterrupted.k)

        val wrapped = """{"records": [{"amount": 5000, "label": "genuine"}]}"""
        assertEquals(0, judge.scoreKit(wrapped, "w.json").rows.single().label)

        val jsonl = "{\"amount\": 20000, \"median_90d\": 650, \"call_now\": true, \"payee_status\": \"new\", \"label\": 1}\n{\"amount\": 100, \"label\": 0}\n"
        assertEquals(2, judge.scoreKit(jsonl, "k.jsonl").rows.size)

        val csv = "amount,median_90d,payments_90d,call_now,payee_status,payee_kind,label,q1\n" +
                  "20000,650,42,true,new,person,1,\n" +
                  "20000,650,42,1,new,person,1,yes\n" +
                  "18000,900,50,false,known,person,0,\n"
        val c = judge.scoreKit(csv, "c.csv")
        assertEquals(3, c.rows.size)
        assertEquals("PAUSE", c.rows[0].level)
        assertEquals("CRITICAL", c.rows[1].level)                // q1 = yes at Pause -> G4
        assertEquals("NONE", c.rows[2].level)
    }

    @Test
    fun badRowsAreReportedNotScored() {
        val bad = """[{"amount": 0}, {"amount": 500, "payee_status": "stranger"}, {"amount": 500, "payee_kind": "bank"}, {"amount": 500}]"""
        val r = judge.scoreKit(bad, "bad.json")
        assertEquals(1, r.rows.size)
        assertEquals(3, r.problems.size)
        assertTrue(r.problems[0].contains("amount"))
    }

    @Test
    fun ourSampleFileGivesRatesInLineWithTheReport() {
        val r = judge.scoreKit(File(assets, "sample_kit.jsonl").readText(), "sample")
        assertEquals(400, r.rows.size)
        assertEquals(200, r.scamsFlagged.n)
        assertEquals(200, r.genuineInterrupted.n)
        assertTrue("scams flagged ${r.scamsFlagged.rate}", r.scamsFlagged.rate in 0.6..0.95)
        assertTrue("genuine interrupted ${r.genuineInterrupted.rate}", r.genuineInterrupted.rate <= 0.03)
        assertEquals(8, r.byWorkflow.size)
    }

    @Test
    fun wilsonIntervalIsSane() {
        val r = Rate(80, 100)
        assertEquals(0.8, r.rate, 1e-9)
        assertTrue(r.low in 0.70..0.80 && r.high in 0.80..0.90)
        assertEquals(0.0, Rate(0, 0).rate, 0.0)
    }

    // ---- "Run in app" and simulate call, through the real payment flow ----
    private class NoSignals : SignalSource {
        override val callNow = false
        override val screenShare = false
        override fun callSeenWithin30m(nowMs: Long) = false
        override fun isNight(nowMs: Long) = false
    }

    @Test
    fun runInAppUsesTheJudgesExactInputsAndReturnsToJudge() {
        val flow = PaymentFlow(model, NoSignals(), { }).apply { pickProfile("judge") }
        assertEquals(Screen.Judge, flow.screen)
        val p = Payment(amount = 20000.0, median90d = 650.0, payments90d = 42, callNow = true, payeeStatus = "new")
        flow.runJudge(p, q1 = true, q2 = null)
        assertEquals(Screen.Amount, flow.screen)
        assertEquals(20000L, flow.amount)
        flow.pay()
        assertEquals(Screen.Pause, flow.screen)
        assertEquals(true, flow.q1)                              // the judge's answer is pre-filled
        flow.pauseNext()
        assertEquals(Screen.Critical, flow.screen)
        flow.wait30()
        flow.done()
        assertEquals(Screen.Judge, flow.screen)                  // back to Judge mode, not Home
        assertNull(flow.judgeInputs)
    }

    @Test
    fun simulateCallMakesTheWholeAppSeeACall() {
        val flow = PaymentFlow(model, NoSignals(), { }).apply { pickProfile("sharma") }
        flow.simulateCall = true
        assertTrue(flow.callNow)
        flow.openPerson("manoj"); flow.toAmount(); "6000".forEach { flow.amountDigit(it.digitToInt()) }
        assertTrue(flow.currentPayment()!!.callNow)
        flow.pay()
        assertEquals(Screen.Pause, flow.screen)
    }
}
