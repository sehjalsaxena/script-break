package com.example.scriptbreak.logic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.scriptbreak.ml.ApprovedRule
import com.example.scriptbreak.ml.Payment
import com.example.scriptbreak.ml.Result
import com.example.scriptbreak.ml.ScamModel
import com.example.scriptbreak.ui.model.FamilyAlertUi
import com.example.scriptbreak.ui.model.SuccessUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

enum class Screen { Login, UserPicker, Home, PayToUpi, Payee, Amount, Pause, Critical, Pin, Success, Waiting, FamilyAlert, FamilyWaiting, Judge, Analyst }

/** A payment made during this demo session (the app's own payment record). */
data class SessionPayment(
    val payeeId: String,
    val payeeName: String,
    val amount: Long,
    val atMs: Long,
    val payeeWasNew: Boolean,   // new or first paid < 7 days ago at the time
    val afterWarning: Boolean,  // a Pause or Critical screen was shown and the person continued
)

/** Sends "please call" to the trusted contact's phone. Firebase in the app; a list in tests. */
fun interface FamilyAlertSender {
    fun send(alert: FamilyAlertUi)
}

/**
 * The payment journey: Home -> payee -> amount -> (Pause) -> (Critical) -> PIN -> Success.
 *
 * ScriptBreak sits between the amount and the PIN. The AI model scores the payment and the safety
 * layer (inside ScamModel.assess) picks the level. Safety rules kept here:
 *  - fail-open: if the model is missing or scoring throws, the payment goes straight to the PIN
 *  - answers only raise the level (guaranteed by assess; Q1/Q2 are never model inputs)
 *  - "Continue anyway" is always reachable; on Critical it unlocks after a 10-second countdown
 */
class PaymentFlow(
    private val model: ScamModel?,
    private val signals: SignalSource,
    private val family: FamilyAlertSender,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    var screen by mutableStateOf(Screen.Login); private set
    /** Who is signed in (demo login: username + PIN). */
    var account by mutableStateOf<Account?>(null); private set
    var profile by mutableStateOf(DemoWorld.profiles.getValue("sharma")); private set
    var upiInput by mutableStateOf(""); private set
    var payee by mutableStateOf<PayeeRecord?>(null); private set
    var amountDigits by mutableStateOf(""); private set
    var note by mutableStateOf(""); private set
    var q1 by mutableStateOf<Boolean?>(null); private set
    var q2 by mutableStateOf<Boolean?>(null); private set
    var askedFamily by mutableStateOf(false); private set
    var hindi by mutableStateOf(false); private set
    var secondsLeft by mutableIntStateOf(0); private set
    /** Digits typed on the UPI PIN pad (kept only until the PIN is checked, never stored or sent). */
    private var pinEntry by mutableStateOf("")
    val pinDigits: Int get() = pinEntry.length
    private var pinAttempts = 0
    var pauseResult by mutableStateOf<Result?>(null); private set
    var criticalResult by mutableStateOf<Result?>(null); private set
    var lastAlert by mutableStateOf<FamilyAlertUi?>(null); private set
    var success by mutableStateOf<SuccessUi?>(null); private set
    /** A short notice for the person (shown as a toast), cleared by [consumeMessage]. */
    var message by mutableStateOf<String?>(null); private set
    val ledger = mutableStateListOf<SessionPayment>()
    /** Rules approved in the analyst console (guardrail G3). */
    var approvedRules: List<ApprovedRule> = emptyList()

    /** Judge mode switch: treat the phone as being on a call (for emulators and demos). Shown on screen. */
    var simulateCall by mutableStateOf(false)
    /** Judge mode "Run in app": these inputs replace the phone's signals for one payment. */
    var judgeInputs by mutableStateOf<Payment?>(null); private set
    var judgeAnswers: Pair<Boolean?, Boolean?> = null to null; private set
    /** Where a test payment returns when it ends (Judge or Analyst), or null for a normal payment. */
    private var returnTo: Screen? = null
    /** Analyst demo: score this one payment with a different model (the "before the new scam wave" one). */
    private var runModel: ScamModel? = null

    /** This phone is being used as the trusted contact's (Rohit's) phone. */
    var familyMode by mutableStateOf(false); private set

    private var pasted = false            // payee UPI ID pasted, not typed or picked
    private var leftApp = false           // left the app and came back during this payment
    private var warnedThisPayment = false // a Pause or Critical screen was shown for this payment

    val modelVersion: String get() = (runModel ?: model)?.modelVersion ?: "unavailable"
    val amount: Long get() = amountDigits.toLongOrNull() ?: 0L
    val callNow: Boolean get() = signals.callNow || simulateCall

    fun consumeMessage() { message = null }

    // ---- login ---------------------------------------------------------------------------------
    fun login(username: String, pin: String): Boolean {
        val a = DemoWorld.account(username)
        if (a == null || !a.checkLogin(pin)) { message = "Wrong username or PIN"; return false }
        account = a
        pickProfile(a.mode)
        return true
    }

    fun logout() {
        account = null; familyMode = false
        resetPayment()
        screen = Screen.Login
    }

    // ---- profiles and navigation ----------------------------------------------------------
    fun pickProfile(id: String) {
        when (id) {
            "sharma", "priya" -> {
                profile = DemoWorld.profiles.getValue(id)
                resetPayment()
                screen = Screen.Home
            }
            "rohit" -> {
                familyMode = true
                screen = if (lastAlert != null) Screen.FamilyAlert else Screen.FamilyWaiting
            }
            "judge" -> screen = Screen.Judge
            "analyst" -> screen = Screen.Analyst
            else -> message = "Unknown profile"
        }
    }

    /** System back button. Returns false when there is nowhere to go back to (leave the app). */
    fun back(): Boolean {
        when (screen) {
            Screen.Login -> return false
            Screen.UserPicker, Screen.Home, Screen.Judge, Screen.Analyst, Screen.FamilyAlert, Screen.FamilyWaiting -> logout()
            Screen.PayToUpi, Screen.Payee, Screen.Waiting -> finish()
            Screen.Amount -> if (returnTo != null) finish() else screen = Screen.Payee
            Screen.Pause, Screen.Critical, Screen.Pin -> backToAmount()
            Screen.Success -> done()
        }
        return true
    }

    fun onAppStopped() {
        if (screen in setOf(Screen.PayToUpi, Screen.Payee, Screen.Amount, Screen.Pause, Screen.Critical, Screen.Pin)) leftApp = true
    }

    // ---- Judge mode: run the judge's inputs through the real screens ----------------------------
    /** Opens the Amount screen with these exact inputs; Pay then shows the real Nudge/Pause/Critical. */
    fun runJudge(p: Payment, q1: Boolean?, q2: Boolean?) = runTest(p, q1, q2)

    /** Analyst "Run in app": same as Judge, but scored with [testModel] and returning to the Analyst tab. */
    fun runTest(p: Payment, q1: Boolean?, q2: Boolean?, testModel: ScamModel? = null,
                back: Screen = Screen.Judge, payeeName: String = "Test payee") {
        resetPayment()
        returnTo = back
        runModel = testModel
        judgeInputs = p
        judgeAnswers = q1 to q2
        payee = PayeeRecord("judge", payeeName, "test.payee@demoupi", payeeName.uppercase(), kind = p.payeeKind,
            status = if (p.payeeStatus == "known") "known" else "new", initials = "TP", colorHex = "#5A2CA0")
        amountDigits = p.amount.toLong().coerceAtLeast(0).toString()
        screen = Screen.Amount
    }

    // ---- choosing the payee ------------------------------------------------------------------
    fun startPayToUpi() { resetPayment(); screen = Screen.PayToUpi }

    /** Typing in the UPI ID field. A jump of 5+ characters at once is a paste from the keyboard. */
    fun upiChanged(text: String) {
        if (text.length - upiInput.length >= 5) pasted = true
        if (text.isEmpty()) pasted = false
        upiInput = text
    }

    /** Picked from the app's own list of recent UPI IDs: not a paste. */
    fun pickSuggestion(upiId: String) { upiInput = upiId; pasted = false }

    /** The person tapped "Paste". Only the text they chose to paste goes into the field. */
    fun pasteUpi(text: String?) {
        if (text.isNullOrBlank()) { message = "Nothing to paste"; return }
        upiInput = text.trim()
        pasted = true
    }

    fun continueWithUpi() {
        val p = DemoWorld.lookup(upiInput)
        if (p == null) { message = "Enter a valid UPI ID, like name@demoupi"; return }
        payee = p
        screen = Screen.Payee
    }

    fun openPerson(id: String) {
        val p = DemoWorld.byId(id) ?: return
        resetPayment()
        payee = p
        screen = Screen.Payee
    }

    fun toAmount() { amountDigits = ""; note = ""; screen = Screen.Amount }

    // ---- amount ------------------------------------------------------------------------------
    fun amountDigit(d: Int) {
        if (amountDigits.isEmpty() && d == 0) return
        val next = (amountDigits + d).toLongOrNull() ?: return
        if (next > DemoWorld.UPI_LIMIT) { message = LIMIT_MESSAGE; return }
        amountDigits += d.toString()
    }
    fun amountDelete() { amountDigits = amountDigits.dropLast(1) }
    fun noteChanged(text: String) { note = text.take(40) }

    /** The inline Nudge line on the Amount screen, or null when there is nothing to say. */
    val nudge: String?
        get() {
            val r = assessSafely(null, null, refreshSignals = false) ?: return null
            return if (r.level == "NUDGE") nudgeText(r) else null
        }

    fun pay() {
        if (amount <= 0) return
        if (amount > DemoWorld.UPI_LIMIT) { message = LIMIT_MESSAGE; return }
        val r = assessSafely(null, null)
        if (r == null) {                     // fail-open: never block a payment because of our own error
            message = "Safety check unavailable. You can still pay."
            goPin(); return
        }
        when (r.level) {
            "PAUSE" -> { warnedThisPayment = true; pauseResult = r; q1 = judgeAnswers.first; q2 = judgeAnswers.second; screen = Screen.Pause }
            "CRITICAL" -> showCritical(r)
            else -> goPin()                  // NONE and NUDGE: no extra tap
        }
    }

    // ---- Pause --------------------------------------------------------------------------------
    fun answerQ1(v: Boolean) { q1 = v }
    fun answerQ2(v: Boolean) { q2 = v }

    /** "Next" on Pause: re-check with the answers. They can only raise the level. */
    fun pauseNext() {
        val r = assessSafely(q1, q2)
        if (r != null && r.level == "CRITICAL") showCritical(r) else goPin()
    }

    // ---- Critical -----------------------------------------------------------------------------
    private fun showCritical(r: Result) {
        warnedThisPayment = true
        criticalResult = r
        secondsLeft = CONTINUE_DELAY_SECONDS
        screen = Screen.Critical
    }

    /** One second of the "Continue anyway" countdown (driven by the UI). */
    fun tick() { if (screen == Screen.Critical && secondsLeft > 0) secondsLeft-- }

    fun continueAnyway() { if (secondsLeft <= 0) goPin() }
    fun toggleHindi() { hindi = !hindi }

    /** "Wait 30 minutes": the payment is cancelled, nothing leaves the account. */
    fun wait30() { val r = returnTo; resetPayment(); returnTo = r; screen = Screen.Waiting }

    fun askFamily() {
        val p = payee ?: return
        if (askedFamily) return
        val reasons = (criticalResult ?: pauseResult)?.reasons?.take(3).orEmpty()
        val alert = FamilyAlertUi(profile.shownToFamilyAs, rupees(amount), p.name, reasons)
        lastAlert = alert
        askedFamily = true
        family.send(alert)
        message = "${profile.contactName} has been alerted"
    }

    // ---- the trusted contact's side (Rohit's phone) -----------------------------------------
    /** An alert arrived from the family link. Returns true if this phone should buzz. */
    fun onIncomingAlert(alert: FamilyAlertUi): Boolean {
        if (!familyMode) return false          // the sender's own alert echoing back
        lastAlert = alert
        screen = Screen.FamilyAlert
        return true
    }

    /** Rohit's answer reached the sender's phone. */
    fun onFamilyStatus(status: String) {
        if (familyMode) return
        when (status) {
            "calling" -> message = "${profile.contactName} is calling you now"
            "called" -> message = "${profile.contactName} says they have called you"
        }
    }

    /** Rohit handled the alert: back to waiting for the next one. */
    fun familyDone() { lastAlert = null; screen = Screen.FamilyWaiting }

    // ---- PIN and result ----------------------------------------------------------------------
    private fun goPin() {
        pinEntry = ""
        screen = Screen.Pin
        if (returnTo != null) message = "Test payment: any 4-digit PIN works"
    }
    fun pinDigit(d: Int) { if (pinEntry.length < PIN_LENGTH) pinEntry += d.toString() }
    fun pinDelete() { pinEntry = pinEntry.dropLast(1) }

    fun pinSubmit() {
        val p = payee ?: return
        if (pinEntry.length < PIN_LENGTH || amount <= 0) return
        // Test payments from Judge/Analyst accept any PIN; real ones need this person's own UPI PIN
        val pinOk = returnTo != null || account?.checkUpiPin(pinEntry) == true
        pinEntry = ""
        if (!pinOk) {
            pinAttempts++
            if (pinAttempts >= MAX_PIN_ATTEMPTS) {
                message = "Wrong UPI PIN $MAX_PIN_ATTEMPTS times. Payment cancelled; nothing left your account."
                finish()
            } else {
                val left = MAX_PIN_ATTEMPTS - pinAttempts
                message = "Incorrect UPI PIN. $left attempt${if (left == 1) "" else "s"} left"
            }
            return
        }
        val now = clock()
        val status = statusOf(p)
        ledger.add(SessionPayment(p.id, p.name, amount, now, status == "new" || status == "recent", warnedThisPayment))
        success = SuccessUi(
            amountText = rupees(amount),
            payeeName = p.name,
            upiRef = "4" + (now % 100_000_000_000L).toString().padStart(11, '0'),
            time = SimpleDateFormat("'Today,' h:mm a", Locale.ENGLISH).format(Date(now)),
            showReportLine = warnedThisPayment,
        )
        screen = Screen.Success
    }

    fun done() = finish()

    /** End of a payment: back to Home, or to Judge/Analyst if a test payment started there. */
    private fun finish() {
        val r = returnTo
        resetPayment()
        screen = r ?: Screen.Home
    }
    fun backToAmount() { q1 = null; q2 = null; secondsLeft = 0; screen = Screen.Amount }

    // ---- what the app knows about this payee and payment ----------------------------------------
    /** Payee status from the app's own records: known | new | recent | warned | flagged. */
    fun statusOf(p: PayeeRecord): String {
        val now = clock()
        if (p.id == "judge") judgeInputs?.let { return it.payeeStatus }
        val paid = ledger.filter { it.payeeId == p.id }
        return when {
            p.status == "flagged" -> "flagged"
            paid.any { it.afterWarning } -> "warned"
            p.status == "known" -> "known"
            paid.isEmpty() -> "new"
            now - paid.minOf { it.atMs } < SEVEN_DAYS -> "recent"
            else -> "known"
        }
    }

    fun historyLine(p: PayeeRecord): String {
        val paidHere = ledger.count { it.payeeId == p.id }
        return when (statusOf(p)) {
            "known" -> "Paid ${p.paidTimes + paidHere} times before"
            "recent" -> "First paid today"
            "warned" -> "Paid after an earlier warning"
            "flagged" -> "Reported in a fraud report"
            else -> "New payee – never paid"
        }
    }

    /** The 16 facts the model sees for the current payment, or null if there is no payment yet. */
    fun currentPayment(refreshSignals: Boolean = true): Payment? {
        val p = payee ?: return null
        val now = clock()
        if (refreshSignals) signals.refresh(now)   // not while drawing a screen (the UI also refreshes every second)
        judgeInputs?.let { return it.copy(amount = amount.toDouble()) }   // Judge "Run in app": exact inputs
        val status = statusOf(p)
        val isNew = status == "new" || status == "recent"
        return Payment(
            amount = amount.toDouble(),
            median90d = profile.median90d,
            payments90d = profile.payments90d,
            callNow = signals.callNow || simulateCall,
            callSeen30m = signals.callSeenWithin30m(now),
            screenShare = signals.screenShare,
            payeeStatus = status,
            payeeKind = p.kind,
            samePayee24h = if (isNew) ledger.count { it.payeeId == p.id && now - it.atMs < DAY } else 0,
            newPayees60m = ledger.filter { it.payeeWasNew && it.payeeId != p.id && now - it.atMs < HOUR }.map { it.payeeId }.distinct().size,
            sentNew24h = ledger.filter { it.payeeWasNew && now - it.atMs < DAY }.sumOf { it.amount }.toDouble(),
            creditUnknown48h = 0.0,
            payeeIsSender = false,
            payeePasted = pasted,
            appSwitch = leftApp,
            night = signals.isNight(now),
        )
    }

    /** The model + safety layer, wrapped so that any failure lets the payment through (null). */
    fun assessSafely(q1: Boolean?, q2: Boolean?, refreshSignals: Boolean = true): Result? {
        val m = runModel ?: model ?: return null
        val payment = currentPayment(refreshSignals) ?: return null
        if (payment.amount <= 0) return null
        return try {
            m.assess(payment, q1, q2, approvedRules)
        } catch (e: Exception) {
            null
        }
    }

    private fun nudgeText(r: Result): String {
        val p = payee ?: return ""
        val parts = mutableListOf<String>()
        if (statusOf(p) == "new") parts += "First payment to ${endSentence(p.name)}"
        val hasHistory = profile.payments90d >= 10 && profile.median90d > 0
        val ratio = amount / profile.median90d
        if (hasHistory && ratio >= 3) parts += "${rupees(amount)} is ${ratio.roundToLong()}× your usual payment."
        else r.reasons.firstOrNull { it != NEVER_PAID }?.let { parts += endSentence(it) }
        parts += "Check you know who this is."
        return parts.joinToString(" ")
    }

    private fun resetPayment() {
        upiInput = ""; payee = null; amountDigits = ""; note = ""
        q1 = null; q2 = null; askedFamily = false; secondsLeft = 0; pinEntry = ""; pinAttempts = 0
        pauseResult = null; criticalResult = null
        pasted = false; leftApp = false; warnedThisPayment = false
        judgeInputs = null; judgeAnswers = null to null; returnTo = null; runModel = null
    }

    companion object {
        const val CONTINUE_DELAY_SECONDS = 10
        const val PIN_LENGTH = 4
        const val MAX_PIN_ATTEMPTS = 3
        const val LIMIT_MESSAGE = "UPI allows up to ₹1,00,000 per payment"
        private const val HOUR = 60 * 60 * 1000L
        private const val DAY = 24 * HOUR
        private const val SEVEN_DAYS = 7 * DAY
        private const val NEVER_PAID = "You have never paid this payee before"

        /** ₹1,50,000 (Indian digit grouping). */
        fun rupees(amount: Long): String = "₹" + grouped(amount)

        /** 1,50,000: last three digits, then groups of two. Done by hand so every phone shows the same. */
        fun grouped(amount: Long): String {
            val digits = kotlin.math.abs(amount).toString()
            if (digits.length <= 3) return (if (amount < 0) "-" else "") + digits
            val head = digits.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
            return (if (amount < 0) "-" else "") + head + "," + digits.takeLast(3)
        }
        /** End a sentence without doubling the full stop ("Ramesh K." not "Ramesh K.."). */
        fun endSentence(s: String): String = if (s.endsWith(".")) s else "$s."
    }
}
