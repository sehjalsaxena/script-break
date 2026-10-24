package com.example.scriptbreak

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.scriptbreak.logic.AndroidSignals
import com.example.scriptbreak.logic.FamilyAlertSender
import com.example.scriptbreak.logic.JudgeMode
import com.example.scriptbreak.logic.PaymentFlow
import com.example.scriptbreak.ml.ScamModel
import com.example.scriptbreak.ui.AppRoot
import com.example.scriptbreak.ui.theme.ScriptBreakTheme
import java.util.function.Consumer

class MainActivity : ComponentActivity() {
    private lateinit var signals: AndroidSignals
    private lateinit var flow: PaymentFlow
    private lateinit var judge: JudgeMode

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
        // Until the Firebase relay is added, the alert is kept on this phone (Rohit's profile shows it).
        flow = PaymentFlow(model, signals, FamilyAlertSender { })
        // The 28 Round 1 stories (a copy of tests/cases.json) as Judge mode presets
        val stories = try { JudgeMode.parseStories(assets.open("cases.json").bufferedReader().use { it.readText() }) } catch (e: Exception) { emptyList() }
        judge = JudgeMode(model, stories)
        setContent {
            ScriptBreakTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(flow, signals, judge)
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

    override fun onStop() {
        flow.onAppStopped()   // leaving the app mid-payment is one of the model's signals
        signals.appVisible = false
        if (Build.VERSION.SDK_INT >= 35) windowManager.removeScreenRecordingCallback(screenRecording)
        super.onStop()
    }
}
