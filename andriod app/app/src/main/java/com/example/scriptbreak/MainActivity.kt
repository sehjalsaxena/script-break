package com.example.scriptbreak

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import java.util.Locale
import java.util.function.Consumer

class LocalizedActivityContext(
    val activity: MainActivity,
    private val localizedContext: Context
) : ContextWrapper(activity), ActivityResultRegistryOwner {
    override fun getResources(): Resources = localizedContext.resources
    override val activityResultRegistry: ActivityResultRegistry
        get() = activity.activityResultRegistry
}

class MainActivity : ComponentActivity() {
    private lateinit var signals: AndroidSignals
    private lateinit var flow: PaymentFlow
    private lateinit var judge: JudgeMode
    private lateinit var analyst: AnalystMode
    private lateinit var family: FamilyLink

    // Android 15+: is this app's own window being recorded or shared right now?
    private val screenRecording = Consumer<Int> { state ->
        signals.screenRecorded = state == WindowManager.SCREEN_RECORDING_STATE_VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        signals = AndroidSignals(applicationContext)
        // If the model file can't be loaded, the app still works: every payment goes straight
        // to the PIN (fail-open), as promised. It never blocks a payment because of our own error.
        val model = try { ScamModel.fromAssets(this) } catch (e: Exception) { null }
        // "Ask family" goes to the trusted contact's phone through Firebase (and is kept on this phone too)
        family = FamilyLink(applicationContext)
        flow = PaymentFlow(model, signals, family)
        family.listen(
            onAlert = { _, alert -> if (flow.onIncomingAlert(alert)) FamilyBuzz.buzz(applicationContext, alert) },
            onStatus = { _, status ->
                flow.onFamilyStatus(status)
                // Sehjal has called: the alert has done its job, so the sender's phone deletes it from Firebase
                if (status == "called") family.clear()
            },
            onCleared = { flow.onAlertCleared() },
        )
        flow.onFamilyLogin = { family.fetchLatest() }
        // money between the demo people (e.g. Sambhav pays Deepak) arrives on the receiver's phone
        flow.onSendMoney = { family.sendPayment(it) }
        flow.onResetMoney = { family.clearPaymentsTo(it) }
        family.listenPayments { flow.onMoneyReceived(it) }
        // remember who uses this phone, so only Sehjal's phone buzzes while it is on the login screen
        val prefs = getSharedPreferences("scriptbreak", MODE_PRIVATE)
        flow.phoneOwner = prefs.getString("last_user", null)
        flow.onLoggedIn = { user -> prefs.edit().putString("last_user", user).apply() }
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
        openAlertIfAsked(intent)   // the app was started by tapping the alert notification
        setContent {
            val isDark = flow.isDarkMode
            val isHindi = flow.hindi

            val currentLocale = remember(isHindi) {
                if (isHindi) Locale.forLanguageTag("hi-IN") else Locale.forLanguageTag("en-IN")
            }
            val currentConfig = LocalConfiguration.current
            val localizedConfig = remember(currentConfig, currentLocale) {
                Configuration(currentConfig).apply {
                    setLocale(currentLocale)
                    setLayoutDirection(currentLocale)
                }
            }
            val localizedContext = remember(currentLocale) {
                createConfigurationContext(localizedConfig)
            }
            val localizedWrapper = remember(localizedContext) {
                LocalizedActivityContext(this@MainActivity, localizedContext)
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfig,
                LocalContext provides localizedWrapper,
                LocalActivityResultRegistryOwner provides this@MainActivity,
                LocalOnBackPressedDispatcherOwner provides this@MainActivity
            ) {
                ScriptBreakTheme(darkTheme = isDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = com.example.scriptbreak.ui.theme.AppBackground
                    ) {
                        AppRoot(flow, signals, judge, analyst, family, results)
                    }
                }
            }
        }
    }

    /** Sehjal tapped the phone notification while the app was already open in the background. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openAlertIfAsked(intent)
    }

    private fun openAlertIfAsked(intent: Intent?) {
        if (intent?.getBooleanExtra(FamilyBuzz.EXTRA_OPEN_ALERT, false) != true) return
        intent.removeExtra(FamilyBuzz.EXTRA_OPEN_ALERT)   // only once, not again when the screen turns
        flow.openAlertFromNotification()
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
