package com.example.scriptbreak.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.scriptbreak.R
import com.example.scriptbreak.logic.AnalystMode
import com.example.scriptbreak.logic.JudgeMode
import com.example.scriptbreak.logic.KitResult
import com.example.scriptbreak.logic.PaymentFlow
import com.example.scriptbreak.logic.PaymentFlow.Companion.grouped
import com.example.scriptbreak.logic.PaymentFlow.Companion.rupees
import com.example.scriptbreak.logic.Screen
import com.example.scriptbreak.logic.SignalSource
import com.example.scriptbreak.ui.model.AmountUi
import com.example.scriptbreak.ui.model.CriticalUi
import com.example.scriptbreak.ui.model.HomeUi
import com.example.scriptbreak.ui.model.PauseUi
import com.example.scriptbreak.ui.model.PayeeUi
import com.example.scriptbreak.ui.model.PersonUi
import com.example.scriptbreak.ui.model.TxnUi
import com.example.scriptbreak.ui.screens.AmountScreen
import com.example.scriptbreak.ui.screens.AnalystScreen
import com.example.scriptbreak.ui.screens.CriticalScreen
import com.example.scriptbreak.ui.screens.FamilyAlertScreen
import com.example.scriptbreak.ui.screens.HomeScreen
import com.example.scriptbreak.ui.screens.JudgeScreen
import com.example.scriptbreak.ui.screens.PauseScreen
import com.example.scriptbreak.ui.screens.PayToUpiScreen
import com.example.scriptbreak.ui.screens.PayeeScreen
import com.example.scriptbreak.ui.screens.PinScreen
import com.example.scriptbreak.ui.screens.SuccessScreen
import com.example.scriptbreak.ui.screens.UserPickerScreen
import com.example.scriptbreak.ui.screens.WaitingScreen
import com.example.scriptbreak.logic.DemoWorld
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** The real app: Sehjal's screens driven by PaymentFlow (AI model + safety layer + phone signals). */
@Composable
fun AppRoot(flow: PaymentFlow, signals: SignalSource, judge: JudgeMode, analyst: AnalystMode) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Judge mode: score a test file off the main thread (a big file must not freeze the screen)
    fun scoreKit(read: () -> String?, name: String) {
        judge.kitLoading = true
        scope.launch {
            judge.kit = withContext(Dispatchers.Default) {
                try {
                    val text = read() ?: error("the file is empty")
                    judge.scoreKit(text, name)
                } catch (e: Exception) {
                    KitResult(name, emptyList(), listOf("Could not read the file: ${e.message}"))
                }
            }
            judge.kitLoading = false
        }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scoreKit({ context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }, displayName(context, uri))
    }

    // Phone signals: refresh the call state every second while the app is open (nothing runs in the background)
    LaunchedEffect(Unit) {
        while (true) {
            signals.refresh(System.currentTimeMillis())
            delay(1000)
        }
    }
    // The 10-second "Continue anyway" countdown on the Critical screen
    LaunchedEffect(flow.screen, flow.secondsLeft) {
        if (flow.screen == Screen.Critical && flow.secondsLeft > 0) {
            delay(1000)
            flow.tick()
        }
    }
    // One-off notices ("Rohit has been alerted", fail-open note)
    LaunchedEffect(flow.message) {
        flow.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            flow.consumeMessage()
        }
    }
    BackHandler(enabled = flow.screen != Screen.UserPicker) { flow.back() }

    val tts = rememberTextToSpeech(context)

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = flow.screen, label = "screens") { screen ->
            when (screen) {
                Screen.UserPicker -> UserPickerScreen(onPick = flow::pickProfile)

                Screen.Home -> HomeScreen(
                    state = HomeUi(
                        userName = flow.profile.displayName,
                        onCall = flow.callNow,
                        people = DemoWorld.homePeople.map { PersonUi(it.id, it.name, it.initials, it.upiId, it.colorHex) },
                        recent = flow.ledger.reversed().map {
                            TxnUi("p${it.atMs}", it.payeeName, "Today", rupees(it.amount))
                        } + DemoWorld.seededRecent(flow.profile.id),
                    ),
                    onPayUpiId = flow::startPayToUpi,
                    onPerson = flow::openPerson,
                    onHistory = { toast(context, "Full history is not part of this demo") },
                    onBalance = { toast(context, "Demo app: no real bank balance") },
                )

                Screen.PayToUpi -> PayToUpiScreen(
                    upiId = flow.upiInput,
                    onUpiIdChange = flow::upiChanged,
                    onPaste = { flow.pasteUpi(readClipboardOnTap(context)) },
                    onContinue = flow::continueWithUpi,
                    onBack = { flow.back() },
                    onPickSuggestion = flow::pickSuggestion,
                )

                Screen.Payee -> flow.payee?.let { p ->
                    PayeeScreen(
                        state = PayeeUi(p.name, p.upiId, p.bankingName, flow.historyLine(p)),
                        onPay = flow::toAmount,
                        onBack = { flow.back() },
                    )
                }

                Screen.Amount -> flow.payee?.let { p ->
                    AmountScreen(
                        state = AmountUi(
                            payeeName = p.name,
                            amountText = if (flow.amount > 0) grouped(flow.amount) else "",
                            note = flow.note,
                            nudge = flow.nudge,
                            payEnabled = flow.amount > 0,
                        ),
                        onDigit = flow::amountDigit,
                        onDelete = flow::amountDelete,
                        onNoteChange = flow::noteChanged,
                        onPay = flow::pay,
                        onBack = { flow.back() },
                    )
                }

                Screen.Pause -> flow.payee?.let { p ->
                    PauseScreen(
                        state = PauseUi(
                            amountText = rupees(flow.amount),
                            payeeName = p.name,
                            reasons = flow.pauseResult?.reasons.orEmpty(),
                            q1 = flow.q1,
                            q2 = flow.q2,
                            contactName = flow.profile.contactName,
                            askedFamily = flow.askedFamily,
                            modelVersion = flow.modelVersion,
                        ),
                        onQ1 = flow::answerQ1,
                        onQ2 = flow::answerQ2,
                        onAskFamily = flow::askFamily,
                        onNext = flow::pauseNext,
                        onBack = { flow.back() },
                        onWait = flow::wait30,
                    )
                }

                Screen.Critical -> flow.payee?.let { p ->
                    val status = flow.statusOf(p)
                    val state = CriticalUi(
                        amountText = rupees(flow.amount),
                        payeeName = p.name,
                        isNewPayee = status == "new" || status == "recent",
                        reasons = flow.criticalResult?.reasons.orEmpty(),
                        contactName = flow.profile.contactName,
                        askedFamily = flow.askedFamily,
                        continueSecondsLeft = flow.secondsLeft,
                        hindi = flow.hindi,
                    )
                    CriticalScreen(
                        state = state,
                        onWait = flow::wait30,
                        onAskFamily = flow::askFamily,
                        onContinue = flow::continueAnyway,
                        onToggleLanguage = flow::toggleHindi,
                        onReadAloud = { speakCritical(context, tts[0], state) },
                        onBack = { flow.back() },
                    )
                }

                Screen.Pin -> flow.payee?.let { p ->
                    PinScreen(
                        amountText = rupees(flow.amount),
                        payeeName = p.name,
                        pinDigits = flow.pinDigits,
                        onDigit = { flow.pinDigit() },
                        onDelete = flow::pinDelete,
                        onSubmit = flow::pinSubmit,
                        onBack = { flow.back() },
                    )
                }

                Screen.Success -> flow.success?.let { s ->
                    SuccessScreen(state = s, onDone = flow::done, onReport = { dial(context, "1930") })
                }

                Screen.Waiting -> WaitingScreen(onHome = flow::done)

                Screen.Judge -> JudgeScreen(
                    judge = judge,
                    simulateCall = flow.simulateCall,
                    onSimulateCall = { flow.simulateCall = it },
                    onRunInApp = { flow.runJudge(judge.inputs, judge.q1, judge.q2) },
                    onPickFile = { pickFile.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream", "*/*")) },
                    onLoadSample = { scoreKit({ context.assets.open("sample_kit.jsonl").bufferedReader().use { it.readText() } }, "sample_kit.jsonl (400 synthetic test payments)") },
                    onBack = { flow.back() },
                )

                Screen.Analyst -> AnalystScreen(
                    analyst = analyst,
                    onRunInApp = {
                        flow.runTest(analyst.scamPayment, null, null, testModel = analyst.beforeModel,
                            back = Screen.Analyst, payeeName = "Vikram S.")
                    },
                    onBack = { flow.back() },
                )

                Screen.FamilyAlert -> flow.lastAlert?.let { a ->
                    FamilyAlertScreen(
                        state = a,
                        onCallNow = { dial(context, null) },
                        onCalled = { flow.back() },
                        onBack = { flow.back() },
                    )
                }
            }
        }
    }
}

private fun displayName(context: Context, uri: Uri): String =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    } ?: "test file"

private fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

/** Read the clipboard only when the person taps "Paste", and only to put that text in the field. */
private fun readClipboardOnTap(context: Context): String? {
    val cm = context.getSystemService(ClipboardManager::class.java) ?: return null
    return cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
}

/** Opens the phone's dialer (the person presses call themselves; no call permission needed). */
private fun dial(context: Context, number: String?) {
    val intent = Intent(Intent.ACTION_DIAL).apply { if (number != null) data = Uri.parse("tel:$number") }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        toast(context, if (number != null) "Call $number" else "Open your phone app to call")
    }
}

/** Android's built-in text-to-speech, started once; read at tap time (it starts up asynchronously). */
@Composable
private fun rememberTextToSpeech(context: Context): Array<TextToSpeech?> {
    val holder = remember { arrayOfNulls<TextToSpeech>(1) }
    DisposableEffect(Unit) {
        holder[0] = TextToSpeech(context.applicationContext) { }
        onDispose { holder[0]?.shutdown() }
    }
    return holder
}

/** Read the Critical message aloud, in English or Hindi (the same strings as the screen). */
private fun speakCritical(context: Context, tts: TextToSpeech?, s: CriticalUi) {
    if (tts == null) { toast(context, "Read-aloud is not available on this phone"); return }
    val locale = if (s.hindi) Locale.forLanguageTag("hi-IN") else Locale.forLanguageTag("en-IN")
    val ctx = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(locale) })
    val toPayee = if (s.isNewPayee) ctx.getString(R.string.critical_to_payee_new, s.amountText, s.payeeName)
                  else ctx.getString(R.string.critical_to_payee, s.amountText, s.payeeName)
    val text = listOf(
        ctx.getString(R.string.critical_title), ctx.getString(R.string.critical_calm),
        ctx.getString(R.string.critical_never), toPayee,
    ).plus(s.reasons).plus(ctx.getString(R.string.critical_last)).joinToString(" ")
    if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) tts.language = locale
    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "critical")
}
