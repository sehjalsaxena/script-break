package com.example.scriptbreak

import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.scriptbreak.logic.AnalystMode
import com.example.scriptbreak.logic.AndroidSignals
import com.example.scriptbreak.logic.Discovery
import com.example.scriptbreak.logic.FamilyBuzz
import com.example.scriptbreak.logic.FamilyLink
import com.example.scriptbreak.logic.JudgeMode
import com.example.scriptbreak.logic.PaymentFlow
import com.example.scriptbreak.logic.ResultsData
import com.example.scriptbreak.ml.ScamModel
import com.example.scriptbreak.ui.AppRoot
import com.example.scriptbreak.ui.theme.ScriptBreakTheme
import java.util.function.Consumer

class MainActivity : ComponentActivity() {
    private lateinit var signals: AndroidSignals
    private lateinit var flow: PaymentFlow
    private lateinit var judge: JudgeMode
    private lateinit var analyst: AnalystMode
    private lateinit var family: FamilyLink

    // Android 15+: is this app's own window being recorded or shared right now?
    private val screenRecording = Consumer<Int> { state ->
        signals.screenShare = state == WindowManager.SCREEN_RECORDING_STATE_VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        signals = AndroidSignals(applicationContext)
        // If the model file can't be loaded, the app still works: every payment goes straight
        // to the PIN (fail-open), as promised. It never blocks a payment because of our own error.
        val model = try { ScamModel.fromAssets(this) } catch (e: Exception) { null }
        // The score cut-offs, from the model files, for the "What the AI sees" card
        fun cutoffs(file: String) = try {
            org.json.JSONObject(assets.open(file).bufferedReader().use { it.readText() }).let { o ->
                val t = o.getJSONObject("thresholds")
                o.getString("model_version") to listOf(t.getDouble("nudge"), t.getDouble("pause"), t.getDouble("critical"))
            }
        } catch (e: Exception) { null }
        // "Ask family" goes to the trusted contact's phone through Firebase (and is kept on this phone too)
        family = FamilyLink(applicationContext)
        flow = PaymentFlow(model, signals, family)
        flow.scoreCutoffs = listOfNotNull(cutoffs("scam_model.json"), cutoffs("scam_model_unseen_sent_by_mistake.json")).toMap()
        family.listen(
            onAlert = { _, alert -> if (flow.onIncomingAlert(alert)) FamilyBuzz.buzz(applicationContext, alert) },
            onStatus = { _, status ->
                flow.onFamilyStatus(status)
                // Rohit has called: the alert has done its job, so the sender's phone deletes it from Firebase
                if (status == "called" && !flow.familyMode) family.clear()
            },
        )
        // The 28 Round 1 stories (a copy of tests/cases.json) as Judge mode presets
        val stories = try { JudgeMode.parseStories(assets.open("cases.json").bufferedReader().use { it.readText() }) } catch (e: Exception) { emptyList() }
        judge = JudgeMode(model, stories)
        // Analyst demo: the model from BEFORE the "sent by mistake" wave, and what the AI found in the missed reports
        val beforeModel = try { ScamModel.fromAssets(this, "scam_model_unseen_sent_by_mistake.json") } catch (e: Exception) { null }
        val discovery = try { Discovery.parse(assets.open("discovery_unseen_sent_by_mistake.json").bufferedReader().use { it.readText() }) } catch (e: Exception) { null }
        // Approved rules become guardrail G3 everywhere in the app
        analyst = AnalystMode(beforeModel, discovery) { rules -> flow.approvedRules = rules }
        // Full test results for the scoreboard (ml/results, made by ml/run_all_tests.py)
        val results = try {
            ResultsData.parse(
                assets.open("report.json").bufferedReader().use { it.readText() },
                try { assets.open("test_summary.json").bufferedReader().use { it.readText() } } catch (e: Exception) { null },
            )
        } catch (e: Exception) { null }
        setContent {
            ScriptBreakTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.example.scriptbreak.ui.theme.AppBackground
                ) {
                    AppRoot(flow, signals, judge, analyst, family, results)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        signals.appVisible = true   // phone signals are read only while the app is on screen
        if (Build.VERSION.SDK_INT >= 35) {
            screenRecording.accept(windowManager.addScreenRecordingCallback(mainExecutor, screenRecording))
        }
    }

    override fun onDestroy() {
        family.stop()
        super.onDestroy()
    }

    override fun onStop() {
        // leaving the app mid-payment is one of the model's signals; the screen turning off is not leaving
        val screenOn = getSystemService(PowerManager::class.java)?.isInteractive ?: true
        if (screenOn) flow.onAppStopped()
        signals.appVisible = false
        if (Build.VERSION.SDK_INT >= 35) windowManager.removeScreenRecordingCallback(screenRecording)
        super.onStop()
    }
}
