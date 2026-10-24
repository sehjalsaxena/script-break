package com.example.scriptbreak.ui

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.scriptbreak.R
import com.example.scriptbreak.logic.AnalystMode
import com.example.scriptbreak.logic.FamilyLink
import com.example.scriptbreak.logic.JudgeMode
import com.example.scriptbreak.logic.KitResult
import com.example.scriptbreak.logic.PaymentFlow
import com.example.scriptbreak.logic.ResultsData
import com.example.scriptbreak.logic.Story
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
import com.example.scriptbreak.ui.screens.AccountScreen
import com.example.scriptbreak.ui.screens.AllPeopleScreen
import com.example.scriptbreak.ui.screens.AmountScreen
import com.example.scriptbreak.ui.screens.AnalystScreen
import com.example.scriptbreak.ui.screens.BankSelfScreen
import com.example.scriptbreak.ui.screens.CheckBalanceScreen
import com.example.scriptbreak.ui.screens.ContactsScreen
import com.example.scriptbreak.ui.screens.CriticalScreen
import com.example.scriptbreak.ui.screens.FamilyAlertScreen
import com.example.scriptbreak.ui.screens.FamilyWaitingScreen
import com.example.scriptbreak.ui.screens.HistoryScreen
import com.example.scriptbreak.ui.screens.HomeScreen
import com.example.scriptbreak.ui.screens.JudgeScreen
import com.example.scriptbreak.ui.screens.LiveCheck
import com.example.scriptbreak.ui.screens.LoginScreen
import com.example.scriptbreak.ui.screens.NotificationsScreen
import com.example.scriptbreak.ui.screens.ReportPacketScreen
import com.example.scriptbreak.ui.screens.ResultsTab
import com.example.scriptbreak.ui.screens.SettingsScreen
import com.example.scriptbreak.ui.screens.TrustedContactScreen
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
import kotlin.math.roundToLong

/** The real app: Sehjal's screens driven by PaymentFlow (AI model + safety layer + phone signals). */
@Composable
fun AppRoot(flow: PaymentFlow, signals: SignalSource, judge: JudgeMode, analyst: AnalystMode, family: FamilyLink, results: ResultsData?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Live scoreboard: the 28 stories and 400 test payments, scored on this phone and timed
    var live by remember { mutableStateOf<LiveCheck?>(null) }
    var liveRunning by remember { mutableStateOf(false) }
    fun runLive() {
        if (liveRunning) return
        liveRunning = true
        scope.launch {
            live = withContext(Dispatchers.Default) {
                try {
                    val text = context.assets.open("sample_kit.jsonl").bufferedReader().use { it.readText() }
                    val t0 = System.nanoTime()
                    val kit = judge.scoreKit(text, "sample_kit.jsonl")
                    val ms = (System.nanoTime() - t0) / 1e6
                    val flagged = { s: Story -> judge.levelOf(s) in setOf("PAUSE", "CRITICAL") }
                    val scams = judge.stories.filter { it.kind == "scam" }
                    val genuine = judge.stories.filter { it.kind == "legit" }
                    LiveCheck(scams.count(flagged), scams.size, genuine.count(flagged), genuine.size, kit, ms)
                } catch (e: Exception) { null }
            }
            liveRunning = false
        }
    }

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
    // Rohit's phone needs permission to show the alert as a notification (Android 13+)
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(flow.familyMode) {
        if (flow.familyMode && Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
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
    BackHandler(enabled = flow.screen != Screen.Login) { flow.back() }

    val tts = rememberTextToSpeech(context)

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        AnimatedContent(
            targetState = flow.screen,
            label = "water_drop_screens",
            transitionSpec = {
                val waveDecelerate = CubicBezierEasing(0.08f, 0.82f, 0.12f, 1.0f)
                (scaleIn(
                    initialScale = 0.68f,
                    transformOrigin = TransformOrigin(0.5f, 0.42f),
                    animationSpec = tween(durationMillis = 520, easing = waveDecelerate)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 380, easing = LinearOutSlowInEasing)
                )).togetherWith(
                    scaleOut(
                        targetScale = 0.94f,
                        transformOrigin = TransformOrigin(0.5f, 0.42f),
                        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    )
                )
            }
        ) { screen ->
            when (screen) {
                Screen.Login -> LoginScreen(accounts = DemoWorld.accounts, onLogin = { u, p -> flow.login(u, p) })

                Screen.UserPicker -> UserPickerScreen(onPick = flow::pickProfile)

                Screen.Home -> HomeScreen(
                    state = HomeUi(
                        userName = flow.profile.displayName,
                        onCall = flow.callNow,
                        screenShared = flow.screenShareNow,
                        people = DemoWorld.homePeople.map { PersonUi(it.id, it.name, it.initials, it.upiId, it.colorHex) },
                        recent = flow.ledger.mapIndexed { i, it ->
                            TxnUi("p$i-${it.atMs}", it.payeeName, "Today", rupees(it.amount))
                        }.reversed() + DemoWorld.seededRecent(flow.profile.id),
                    ),
                    onSendContact = flow::startContacts,
                    onPayUpiId = flow::startPayToUpi,
                    onPerson = flow::openPerson,
                    onViewAllPeople = flow::startAllPeople,
                    onHistory = flow::startHistory,
                    onBankSelf = flow::startBankSelf,
                    onBalance = flow::startCheckBalance,
                    onNotifications = flow::startNotifications,
                    onAccount = flow::startAccount,
                    onSettings = flow::startSettings,
                )

                Screen.Contacts -> ContactsScreen(
                    contacts = DemoWorld.contacts,
                    onSelectContact = flow::openContact,
                    onBack = { flow.back() }
                )

                Screen.AllPeople -> AllPeopleScreen(
                    people = DemoWorld.homePeople + DemoWorld.payees.filter { it.status != "known" },
                    onPerson = flow::openPerson,
                    onBack = { flow.back() }
                )

                Screen.History -> HistoryScreen(
                    transactions = flow.ledger.mapIndexed { i, it ->
                        TxnUi("p$i-${it.atMs}", it.payeeName, "Today", rupees(it.amount))
                    }.reversed() + DemoWorld.seededRecent(flow.profile.id),
                    onBack = { flow.back() }
                )

                Screen.BankSelf -> BankSelfScreen(
                    onBack = { flow.back() },
                    onBankAccount = flow::openBankAccount,
                    onSelfTransfer = flow::openSelfTransfer,
                )

                Screen.CheckBalance -> CheckBalanceScreen(
                    userProfileName = flow.profile.displayName,
                    onBack = { flow.back() }
                )

                Screen.PayToUpi -> {
                    val recentUpiList = remember(flow.ledger) {
                        val fromLedger = flow.ledger.reversed().mapNotNull { txn ->
                            DemoWorld.payees.firstOrNull { it.name == txn.payeeName }?.let { it.upiId to it.name }
                        }
                        val knownPeople = DemoWorld.homePeople.map { it.upiId to it.name }
                        (fromLedger + knownPeople).distinctBy { it.first }.take(3)
                    }
                    PayToUpiScreen(
                        upiId = flow.upiInput,
                        onUpiIdChange = flow::upiChanged,
                        onPaste = { flow.pasteUpi(readClipboardOnTap(context)) },
                        onContinue = flow::continueWithUpi,
                        onBack = { flow.back() },
                        onPickSuggestion = flow::pickSuggestion,
                        recentPayees = recentUpiList,
                    )
                }

                Screen.Payee -> flow.payee?.let { p ->
                    PayeeScreen(
                        state = PayeeUi(p.name, p.upiId, p.bankingName, flow.historyLine(p)),
                        onPay = flow::toAmount,
                        onBack = { flow.back() },
                    )
                }

                Screen.Amount -> flow.payee?.let { p ->
                    val ai = flow.aiView
                    AmountScreen(
                        state = AmountUi(
                            payeeName = p.name,
                            amountText = if (flow.amount > 0) grouped(flow.amount) else "",
                            note = flow.note,
                            nudge = flow.nudge,
                            payEnabled = flow.amount in 1..DemoWorld.UPI_LIMIT,
                            aiLevel = ai?.level,
                            aiScore = ai?.score,
                            aiFacts = ai?.facts.orEmpty(),
                            payeeUpiId = p.upiId,
                            aiCutoffs = flow.currentCutoffs,
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
                            contactName = flow.contact.name,
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
                        contactName = flow.contact.name,
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
                        onDigit = flow::pinDigit,
                        onDelete = flow::pinDelete,
                        onSubmit = flow::pinSubmit,
                        onBack = { flow.back() },
                    )
                }

                Screen.Success -> flow.success?.let { s ->
                    SuccessScreen(state = s, onDone = flow::done, onReport = flow::openReport)
                }

                Screen.Waiting -> WaitingScreen(onHome = flow::done)

                Screen.Report -> flow.reportPacket?.let { p ->
                    ReportPacketScreen(
                        packet = p,
                        onCall1930 = { dial(context, "1930") },
                        onReportOnline = { openWeb(context, "https://cybercrime.gov.in") },
                        onShare = { share(context, p.asText()) },
                        onBack = { flow.back() },
                    )
                }

                Screen.TrustedContact -> TrustedContactScreen(
                    current = flow.contact,
                    pending = flow.pendingContact,
                    otpFor = flow.otpFor,
                    demoOtp = flow.demoOtp,
                    onSendCode = flow::requestContactChange,
                    onVerify = flow::verifyContactOtp,
                    onCancelPending = flow::cancelPendingContact,
                    onBack = { flow.back() },
                    usualPaymentText = "${rupees(flow.usualPayment.roundToLong())} (median of ${flow.paymentsLast90d().size} payments in 90 days)",
                    onResetDemo = flow::resetDemo,
                )

                Screen.Notifications -> NotificationsScreen(
                    onBack = { flow.back() }
                )

                Screen.Account -> AccountScreen(
                    userName = flow.profile.displayName,
                    upiId = "${flow.account?.username ?: flow.profile.id}@demoupi",
                    phoneNumber = "+91 98765 43210",
                    trustedContactName = flow.contact.name,
                    onCheckBalance = flow::startCheckBalance,
                    onTrustedContact = flow::openTrustedContact,
                    onSwitchProfile = flow::startUserPicker,
                    onLogout = flow::logout,
                    onBack = { flow.back() }
                )

                Screen.Settings -> SettingsScreen(
                    trustedContactName = flow.contact.name,
                    modelVersion = flow.modelVersion,
                    isDarkMode = flow.isDarkMode,
                    onToggleDarkMode = { flow.isDarkMode = it },
                    isHindi = flow.hindi,
                    onToggleLanguage = { flow.hindi = it },
                    onTrustedContact = flow::openTrustedContact,
                    onCheckBalance = flow::startCheckBalance,
                    onAccount = flow::startAccount,
                    onResetDemo = flow::resetDemo,
                    onLogout = flow::logout,
                    onBack = { flow.back() }
                )

                Screen.Judge -> JudgeScreen(
                    judge = judge,
                    simulateCall = flow.simulateCall,
                    onSimulateCall = { flow.simulateCall = it },
                    simulateScreenShare = flow.simulateScreenShare,
                    onSimulateScreenShare = { flow.simulateScreenShare = it },
                    onRunInApp = { flow.runJudge(judge.inputs, judge.q1, judge.q2) },
                    onPickFile = { pickFile.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream", "*/*")) },
                    onLoadSample = { scoreKit({ context.assets.open("sample_kit.jsonl").bufferedReader().use { it.readText() } }, "sample_kit.jsonl (400 synthetic test payments)") },
                    onBack = { flow.back() },
                    resultsContent = {
                        LaunchedEffect(Unit) { if (live == null) runLive() }
                        ResultsTab(results, live, liveRunning, flow.ledger.size, flow.ledger.count { it.afterWarning }, ::runLive)
                    },
                )

                Screen.Analyst -> AnalystScreen(
                    analyst = analyst,
                    onRunInApp = {
                        flow.runTest(analyst.scamPayment, null, null, testModel = analyst.beforeModel,
                            back = Screen.Analyst, payeeName = "Vikram S.")
                    },
                    onBack = { flow.back() },
                )

                Screen.FamilyWaiting -> FamilyWaitingScreen(online = family.online, onBack = { flow.back() })

                Screen.FamilyAlert -> flow.lastAlert?.let { a ->
                    FamilyAlertScreen(
                        state = a,
                        onCallNow = { family.setStatus("calling"); dial(context, null) },
                        onCalled = { family.setStatus("called"); flow.familyDone() },
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

private fun openWeb(context: Context, url: String) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { toast(context, url) }
}

/** The person chooses where the report packet goes (WhatsApp, email, notes and so on). */
private fun share(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    try { context.startActivity(Intent.createChooser(send, "Share report packet")) } catch (e: Exception) { toast(context, "No app to share with") }
}

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
