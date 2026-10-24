package com.example.scriptbreak.logic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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

enum class Screen {
    Login, UserPicker, Home, PayToUpi, Contacts, AllPeople, History, BankSelf, CheckBalance,
    Payee, Amount, Pause, Critical, Pin, Success, Waiting, FamilyAlert, FamilyWaiting, Judge, Analyst,
    Report, TrustedContact, Notifications, Account, Settings
}

/** A payment made during this demo session (the app's own payment record). */
/** What the AI noticed for the payment being typed, in plain words (the "Safety check" card). */
data class AiView(val level: String, val score: Double?, val facts: List<String>, val reminderOnly: Boolean = false)

data class SessionPayment(
    val payeeId: String,
    val payeeName: String,
    val amount: Long,
    val atMs: Long,
    val payeeWasNew: Boolean,   // new or first paid < 7 days ago at the time
    val afterWarning: Boolean,  // a Pause or Critical screen was shown and the person continued
)

/** Everything the 1930 cyber-crime helpline asks for, for one payment (stays on the phone). */
data class ReportPacket(
    val amountText: String,
    val payeeName: String,
    val payeeUpiId: String,
    val upiRef: String,
    val whenText: String,
    val levelShown: String,          // the warning shown before paying (NONE if none)
    val reasons: List<String>,
) {
    /** Plain text for "Share" (the person chooses where it goes). */
    fun asText(): String = buildString {
        appendLine("Report packet (DemoPay, protected by ScriptBreak)")
        appendLine("Amount: $amountText")
        appendLine("Paid to: $payeeName ($payeeUpiId)")
        appendLine("UPI reference: $upiRef")
        appendLine("Date and time: $whenText")
        if (levelShown != "NONE") appendLine("Warning shown before paying: $levelShown")
        reasons.forEach { appendLine("- $it") }
        append("Call 1930 or report at cybercrime.gov.in")
    }
}

/** The person "Ask family" reaches. */
data class TrustedContact(val name: String, val relation: String, val phone: String) {
    val label get() = if (relation.isBlank()) name else "$name ($relation)"
    val maskedPhone get() = "+91 " + phone.take(2) + "••••" + phone.takeLast(4)
}

/** A new contact waiting out its 24-hour delay. */
data class PendingContact(val contact: TrustedContact, val activeAtMs: Long)

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
    var hindi by mutableStateOf(false)
    var isDarkMode by mutableStateOf(false)
    var secondsLeft by mutableIntStateOf(0); private set
    /** Digits typed on the UPI PIN pad (kept only until the PIN is checked, never stored or sent). */
    private var pinEntry by mutableStateOf("")
    val pinDigits: Int get() = pinEntry.length
    private var pinAttempts = 0
    var pauseResult by mutableStateOf<Result?>(null); private set
    var criticalResult by mutableStateOf<Result?>(null); private set
    var lastAlert by mutableStateOf<FamilyAlertUi?>(null); private set
    private var lastAlertAt = 0L   // when it was sent (or got here, if unknown), so a very old alert is not shown
    private val handledAlerts = mutableSetOf<Long>()   // alerts Sehjal already answered on this phone
    /** Called when Sehjal logs in: MainActivity reads the latest alert from Firebase right away. */
    var onFamilyLogin: () -> Unit = {}
    /** Who last logged in on THIS phone (saved by MainActivity), so only Sehjal's phone buzzes on the login screen. */
    var phoneOwner: String? = null
    var onLoggedIn: (String) -> Unit = {}
    var success by mutableStateOf<SuccessUi?>(null); private set
    var reportPacket by mutableStateOf<ReportPacket?>(null); private set
    /** A short notice for the person (shown as a toast), cleared by [consumeMessage]. */
    var message by mutableStateOf<String?>(null); private set
    /** Each person's own payment record: Deepak never sees, or is scored on, Sambhav's payments. */
    private val ledgers = mutableMapOf<String, androidx.compose.runtime.snapshots.SnapshotStateList<SessionPayment>>()
    val ledger: androidx.compose.runtime.snapshots.SnapshotStateList<SessionPayment>
        get() = ledgers.getOrPut(profile.id) { mutableStateListOf() }
    /** Rules approved in the analyst console (guardrail G3). */
    var approvedRules: List<ApprovedRule> = emptyList()

    /** Judge mode switch: treat the phone as being on a call (for emulators and demos). Shown on screen. */
    var simulateCall by mutableStateOf(false)
    /** Judge mode switch: act as if the screen is being shared (for phones older than Android 15). */
    var simulateScreenShare by mutableStateOf(false)
    /** Judge mode "Run in app": these inputs replace the phone's signals for one payment. */
    var judgeInputs by mutableStateOf<Payment?>(null); private set
    var judgeAnswers: Pair<Boolean?, Boolean?> = null to null; private set
    /** Where a test payment returns when it ends (Judge or Analyst), or null for a normal payment. */
    private var returnTo: Screen? = null
    /** Analyst demo: score this one payment with a different model (the "before the new scam wave" one). */
    private var runModel: ScamModel? = null

    // ---- trusted contact (per person) ----
    private val contacts = mutableStateMapOf(*DEFAULT_CONTACTS.toList().toTypedArray())
    private val pendingContacts = mutableStateMapOf<String, PendingContact>()
    /** Demo OTP: no SMS is sent, the code is shown on screen. Never a real OTP. */
    var otpFor by mutableStateOf<TrustedContact?>(null); private set
    var demoOtp by mutableStateOf<String?>(null); private set

    /** This phone is being used as the trusted contact's (Sehjal's) phone. */
    var familyMode by mutableStateOf(false); private set

    private var pasted = false            // payee UPI ID pasted, not typed or picked
    private var prefillAmount = ""        // amount written in a scanned QR code (the person can change it)
    private var prefillNote = ""
    /** This payment started from a QR image; the name written in the QR (not verified). */
    var fromQr by mutableStateOf(false); private set
    var qrName by mutableStateOf<String?>(null); private set
    private var leftApp = false           // left the app and came back during this payment
    private var warnedThisPayment = false // a Pause or Critical screen was shown for this payment

    val modelVersion: String get() = (runModel ?: model)?.modelVersion ?: "unavailable"
    val amount: Long get() = amountDigits.toLongOrNull() ?: 0L
    val callNow: Boolean get() = signals.callNow || simulateCall
    val screenShareNow: Boolean get() = signals.screenShare || simulateScreenShare

    fun consumeMessage() { message = null }

    /** The trusted contact in charge now (a pending one takes over only after its 24 hours). */
    val contact: TrustedContact
        get() {
            val id = profile.id
            pendingContacts[id]?.let { if (clock() >= it.activeAtMs) return it.contact }   // its 24 hours have passed
            return contacts[id] ?: TrustedContact(profile.contactName, "", "")
        }
    val pendingContact: PendingContact? get() = pendingContacts[profile.id]?.takeIf { clock() < it.activeAtMs }

    fun openTrustedContact() { otpFor = null; demoOtp = null; screen = Screen.TrustedContact }

    /** Step 1: the person enters the new contact; a one-time code goes to that number (demo: shown). */
    fun requestContactChange(name: String, relation: String, phone: String) {
        val digits = phone.filter(Char::isDigit).takeLast(10)
        when {
            name.isBlank() -> message = "Enter the contact's name"
            !Regex("^[6-9][0-9]{9}$").matches(digits) -> message = "Enter a 10-digit Indian mobile number"
            else -> {
                otpFor = TrustedContact(name.trim(), relation.trim(), digits)
                demoOtp = (100000 + (clock() % 900000)).toString()
                message = "Code sent to +91 ••••••${digits.takeLast(4)}"
            }
        }
    }

    /** Step 2: the code from the new contact's phone. The new contact becomes active in 24 hours. */
    fun verifyContactOtp(code: String) {
        val c = otpFor ?: return
        if (code.trim() != demoOtp) { message = "Wrong code"; return }
        settleContact()   // an earlier change whose 24 hours have passed is kept, not overwritten
        pendingContacts[profile.id] = PendingContact(c, clock() + TWENTY_FOUR_HOURS)
        otpFor = null; demoOtp = null
        message = "${c.name} will be your trusted contact in 24 hours. ${contact.name} stays until then."
    }

    /** Cancels a change that is still waiting; one whose 24 hours have passed is already in charge and stays. */
    fun cancelPendingContact() {
        settleContact()
        pendingContacts.remove(profile.id); otpFor = null; demoOtp = null
    }

    private fun settleContact() {
        val id = profile.id
        pendingContacts[id]?.let { if (clock() >= it.activeAtMs) { contacts[id] = it.contact; pendingContacts.remove(id) } }
    }

    // ---- login ---------------------------------------------------------------------------------
    fun login(username: String, pin: String): Boolean {
        val a = DemoWorld.account(username)
        if (a == null || !a.checkLogin(pin)) { message = "Wrong username or PIN"; return false }
        account = a
        phoneOwner = a.mode; onLoggedIn(a.mode)
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
                if (freshAlert == null) lastAlert = null
                screen = if (lastAlert != null) Screen.FamilyAlert else Screen.FamilyWaiting
                onFamilyLogin()   // also ask Firebase, in case the alert came while this app was closed
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
            Screen.PayToUpi, Screen.Contacts, Screen.AllPeople, Screen.History, Screen.BankSelf, Screen.CheckBalance,
            Screen.Notifications, Screen.Account, Screen.Settings, Screen.Payee, Screen.Waiting -> finish()
            Screen.Amount -> if (returnTo != null) finish() else screen = Screen.Payee
            Screen.Pause, Screen.Critical, Screen.Pin -> backToAmount()
            Screen.Success -> done()
            Screen.Report -> screen = Screen.Success
            Screen.TrustedContact -> screen = Screen.Settings
        }
        return true
    }

    fun onAppStopped() {
        if (screen in setOf(Screen.PayToUpi, Screen.Contacts, Screen.AllPeople, Screen.Payee, Screen.Amount, Screen.Pause, Screen.Critical, Screen.Pin)) leftApp = true
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

    // ---- choosing the payee and navigation ---------------------------------------------------
    fun startPayToUpi() { resetPayment(); screen = Screen.PayToUpi }
    fun startContacts() { resetPayment(); screen = Screen.Contacts }
    fun startAllPeople() { resetPayment(); screen = Screen.AllPeople }
    fun startHistory() { screen = Screen.History }
    fun startBankSelf() { screen = Screen.BankSelf }
    fun startCheckBalance() { screen = Screen.CheckBalance }
    fun startNotifications() { screen = Screen.Notifications }
    fun startAccount() { screen = Screen.Account }
    fun startSettings() { screen = Screen.Settings }
    fun startUserPicker() { screen = Screen.UserPicker }

    /** Typing in the UPI ID field. A jump of 5+ characters at once is a paste from the keyboard. */
    fun upiChanged(text: String) {
        // 5+ characters at once that bring in the "@handle" = a whole UPI ID pasted. A keyboard suggestion
        // completing a word ("ramesh") or the handle after "@" was typed is still typing.
        if (text.length - upiInput.length >= 5 && '@' in text && '@' !in upiInput) pasted = true
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
        val trimmed = upiInput.trim().lowercase()
        if (!trimmed.endsWith("@demoupi")) {
            message = "Only @demoupi UPI IDs are supported (e.g. name@demoupi)"
            return
        }
        val p = DemoWorld.lookup(trimmed)
        if (p == null) {
            message = "Enter a valid UPI ID, like name@demoupi"
            return
        }
        payee = p
        screen = Screen.Payee
    }

    fun openPerson(id: String) {
        val p = DemoWorld.byId(id) ?: return
        resetPayment()
        payee = p
        screen = Screen.Payee
    }

    fun openContact(c: ContactItem) {
        val p = (if (c.payeeId != null) DemoWorld.byId(c.payeeId) else null)
            ?: DemoWorld.lookup(c.upiId)
            ?: PayeeRecord(c.id, c.name, c.upiId, c.name.uppercase(), initials = c.initials, colorHex = c.colorHex)
        resetPayment()
        payee = p
        screen = Screen.Payee
    }

    /** "To Bank A/c": a transfer to an account number gets the same ScriptBreak check as a UPI payment. */
    fun openBankAccount(holderName: String, accountNumber: String, ifsc: String) {
        val acct = accountNumber.filter(Char::isDigit)
        val name = holderName.trim().ifBlank { "Account holder" }
        resetPayment()
        payee = DemoWorld.payees.firstOrNull { it.id == "acct:$acct" } ?: PayeeRecord(
            id = "acct:$acct", name = name, upiId = "A/c ••••${acct.takeLast(4)} · ${ifsc.trim().uppercase()}",
            bankingName = name.uppercase(), initials = name.split(' ').filter { it.isNotBlank() }.take(2)
                .joinToString("") { it.first().uppercase() }.ifBlank { "AC" }, colorHex = "#6B74D6")
        screen = Screen.Payee
    }

    /** "To Self A/c": moving money to the person's own second demo account (a payee they have paid before). */
    fun openSelfTransfer() {
        resetPayment()
        payee = SELF_ACCOUNT
        screen = Screen.Payee
    }

    fun toAmount() { amountDigits = prefillAmount; note = prefillNote; screen = Screen.Amount }

    /**
     * "Pay from a QR image". The QR is only another way to enter the payee: the same payee page,
     * the same AI check and the same UPI PIN follow. Only @demoupi IDs are paid (demo, no real money).
     */
    fun openFromQr(text: String?) {
        if (text == null) { message = "No QR code found in that image. Try a clearer picture."; return }
        val qr = UpiQr.parse(text) ?: run { message = "This QR code is not a UPI payment code"; return }
        if (!qr.upiId.endsWith("@demoupi")) {
            message = "This QR is for a real UPI ID (${qr.upiId}). DemoPay is a demo: it only pays @demoupi IDs, no real money."
            return
        }
        // the payee comes from the app's own records / the UPI ID, never from the name written in the QR
        val p = DemoWorld.payees.firstOrNull { it.upiId == qr.upiId } ?: DemoWorld.lookup(qr.upiId)
            ?: run { message = "This QR has an invalid UPI ID"; return }
        resetPayment()
        payee = p
        fromQr = true
        qrName = qr.name
        val rupeesInQr = qr.amount?.let { Math.round(it) }   // whole rupees in this demo
        prefillAmount = when {
            rupeesInQr == null || rupeesInQr <= 0 -> ""
            rupeesInQr > DemoWorld.UPI_LIMIT -> { message = LIMIT_MESSAGE; "" }
            else -> rupeesInQr.toString()
        }
        prefillNote = qr.note.orEmpty()
        screen = Screen.Payee
    }

    /** Shown on the payee page for a QR payment. The name inside a QR can say anything, so it is checked here. */
    val qrNotice: String?
        get() {
            val p = payee ?: return null
            if (!fromQr) return null
            val n = qrName
            fun norm(s: String) = s.lowercase().filter(Char::isLetterOrDigit)
            val matches = n == null || norm(n).let { q -> q.isNotEmpty() && (norm(p.name).contains(q) || q.contains(norm(p.name)) ||
                norm(p.bankingName).contains(q) || q.contains(norm(p.bankingName))) }
            return if (matches) "From a QR code. Check the name and the amount before you pay."
            else "From a QR code. The QR says \"$n\", but the banking name below is different. Check who you are paying."
        }

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
            // shown for Nudge AND above: it must not vanish when a bigger amount makes it a Pause
            return if (r.level != "NONE") nudgeText(r) else null
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
    fun toggleDarkMode() { isDarkMode = !isDarkMode }

    /** "Wait 30 minutes": the payment is cancelled, nothing leaves the account. */
    fun wait30() { val r = returnTo; resetPayment(); returnTo = r; screen = Screen.Waiting }

    fun askFamily() {
        val p = payee ?: return
        if (askedFamily) return
        val all = (criticalResult ?: pauseResult)?.reasons.orEmpty()
        val live = all.filter { it == CALL_NOW || it == SCREEN_SHARE }
        val reasons = (live + (all - live.toSet())).take(3)
        val alert = FamilyAlertUi(profile.shownToFamilyAs, rupees(amount), p.name, reasons, sentAtMs = clock())
        lastAlert = alert; lastAlertAt = clock()   // also kept here, for a one-phone demo (log in as Sehjal)
        askedFamily = true
        family.send(alert)
        message = "${contact.name} has been alerted"
    }

    // ---- the trusted contact's side (Sehjal's phone) -----------------------------------------
    /**
     * An alert arrived from another phone. It is ALWAYS kept, even if nobody is logged in, and opens as
     * soon as Sehjal logs in. Returns true if this phone should buzz.
     */
    fun onIncomingAlert(alert: FamilyAlertUi): Boolean {
        if (alert.sentAtMs != null && alert.sentAtMs in handledAlerts) return false   // Sehjal already answered it
        val same = lastAlert != null && lastAlert?.sentAtMs == alert.sentAtMs && alert.sentAtMs != null
        lastAlert = alert; lastAlertAt = alert.sentAtMs ?: clock()
        return when {
            familyMode -> {                                      // Sehjal is logged in: show it now
                if (screen == Screen.FamilyWaiting || !same) screen = Screen.FamilyAlert
                !same                                            // buzz once per alert, not again on login
            }
            account == null -> !same && phoneOwner == "rohit"    // login screen of ROHIT's phone: buzz; other phones keep it quietly
            else -> false                                        // someone else is using this phone: keep it quietly
        }
    }

    /** The alert was handled, deleted or became too old: stop keeping it (unless it is on screen now). */
    fun onAlertCleared() { if (screen != Screen.FamilyAlert) lastAlert = null }

    /** A kept alert, if it is under 24 hours old (the same as Firebase keeps it). */
    private val freshAlert: FamilyAlertUi? get() = lastAlert?.takeIf { clock() - lastAlertAt < KEEP_ALERT }

    /** Sehjal's answer reached the sender's phone. */
    fun onFamilyStatus(status: String) {
        if (familyMode) return
        when (status) {
            "calling" -> message = "${contact.name} is calling you now"
            "called" -> message = "${contact.name} says they have called you"
        }
    }

    /** Sehjal handled the alert: back to waiting for the next one. */
    fun familyDone() {
        lastAlert?.sentAtMs?.let { handledAlerts += it }
        lastAlert = null; screen = Screen.FamilyWaiting
    }

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
        // Judge/Analyst test payments are not real: they must not change anyone's history or the model's inputs
        if (returnTo == null) ledger.add(SessionPayment(p.id, p.name, amount, now, status == "new" || status == "recent", warnedThisPayment))
        val shown = criticalResult ?: pauseResult
        val upiRef = "4" + (now % 100_000_000_000L).toString().padStart(11, '0')
        reportPacket = ReportPacket(
            amountText = rupees(amount), payeeName = p.name, payeeUpiId = p.upiId, upiRef = upiRef,
            whenText = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH).format(Date(now)),
            levelShown = shown?.level ?: "NONE", reasons = shown?.reasons.orEmpty(),
        )
        success = SuccessUi(
            amountText = rupees(amount),
            payeeName = p.name,
            upiRef = upiRef,
            time = SimpleDateFormat("'Today,' h:mm a", Locale.ENGLISH).format(Date(now)),
            showReportLine = warnedThisPayment,
        )
        screen = Screen.Success
    }

    fun done() = finish()

    /** "Report payment" on the Success screen. */
    fun openReport() { if (reportPacket != null) screen = Screen.Report }

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

    /** Every payment in the last 90 days: the synthetic starting history plus each real payment made in this app. */
    fun paymentsLast90d(now: Long = clock()): List<Long> =
        DemoWorld.history90d(profile.id) + ledger.filter { now - it.atMs < NINETY_DAYS }.map { it.amount }

    /** The usual payment the AI compares against: the median of the last 90 days, so it moves as the person pays. */
    val usualPayment: Double get() = median(paymentsLast90d())

    /** The 16 facts the model sees for the current payment, or null if there is no payment yet. */
    fun currentPayment(refreshSignals: Boolean = true): Payment? {
        val p = payee ?: return null
        val now = clock()
        if (refreshSignals) signals.refresh(now)   // not while drawing a screen (the UI also refreshes every second)
        judgeInputs?.let { return it.copy(amount = amount.toDouble()) }   // Judge "Run in app": exact inputs
        val status = statusOf(p)
        val isNew = status == "new" || status == "recent"
        val history = paymentsLast90d(now)
        return Payment(
            amount = amount.toDouble(),
            median90d = median(history),
            payments90d = history.size,
            callNow = signals.callNow || simulateCall,
            callSeen30m = signals.callSeenWithin30m(now),
            screenShare = screenShareNow,
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

    /**
     * The "Safety check" card on the Amount screen: what the AI noticed, in plain words a customer understands.
     * Each line starts with "✓ " (reassuring) or "⚠ " (a warning sign). Warning signs come first.
     */
    val aiView: AiView?
        get() {
            val p = currentPayment(refreshSignals = false) ?: return null
            if (p.amount <= 0) return null
            val r = assessSafely(null, null, refreshSignals = false)
            val warn = mutableListOf<String>(); val ok = mutableListOf<String>()
            val simulated = judgeInputs == null
            if (p.callNow) warn += "⚠ You're on a call" + if (simulated && simulateCall && !signals.callNow) " (simulated)" else ""
            else if (p.callSeen30m) warn += "⚠ You were on a call a few minutes ago"
            else ok += "✓ Not on a call"
            if (p.screenShare) warn += "⚠ Your screen is being shared" + if (simulated && simulateScreenShare && !signals.screenShare) " (simulated)" else ""
            when (p.payeeStatus) {
                "new" -> warn += "⚠ First payment to this person"
                "recent" -> warn += "⚠ You only started paying them this week"
                "warned" -> warn += "⚠ You were warned about them before"
                "flagged" -> warn += "⚠ Reported for fraud"
                else -> ok += "✓ You've paid them before"
            }
            val hasHistory = p.payments90d >= 10 && p.median90d > 0
            val ratio = if (hasHistory) p.amount / p.median90d else 0.0
            val usual = rupees(p.median90d.roundToLong())
            when {
                !hasHistory -> warn += "⚠ Not much payment history yet"
                ratio >= 3 -> warn += "⚠ ${ratio.roundToLong()}× your usual $usual"
                else -> ok += "✓ A normal amount for you"
            }
            if (p.payeePasted) warn += "⚠ The UPI ID was pasted"
            if (p.appSwitch) warn += "⚠ You left the app while paying"
            if (p.newPayees60m > 0 || p.sentNew24h > 0) warn += "⚠ You already paid new people today"
            if (p.samePayee24h > 0) warn += "⚠ Second payment to them today"
            if (p.night) warn += "⚠ Late at night"
            return AiView(r?.level ?: "UNAVAILABLE", r?.score, warn + ok,
                reminderOnly = r?.guardrails?.contains("C1") == true)
        }

    /** Clears everything this session did (payments, contact changes, alerts), back to the starting demo. */
    fun resetDemo() {
        ledgers.clear(); contacts.clear(); contacts.putAll(DEFAULT_CONTACTS); pendingContacts.clear()
        otpFor = null; demoOtp = null; lastAlert = null; simulateCall = false; simulateScreenShare = false
        resetPayment()
        message = "Demo reset: payment history is back to the start"
    }

    /**
     * The model lists its top 3 reasons, so with a call AND screen sharing one could drop off the list.
     * Both are facts the person must see: add whichever is missing (the level is not changed).
     */
    private fun withLiveSignals(r: Result, p: Payment): Result {
        val missing = buildList {
            if (p.callNow && CALL_NOW !in r.reasons) add(CALL_NOW)
            if (p.screenShare && SCREEN_SHARE !in r.reasons) add(SCREEN_SHARE)
        }
        return if (missing.isEmpty()) r else r.copy(reasons = r.reasons + missing)
    }

    /** The model + safety layer, wrapped so that any failure lets the payment through (null). */
    fun assessSafely(q1: Boolean?, q2: Boolean?, refreshSignals: Boolean = true): Result? {
        val m = runModel ?: model ?: return null
        val payment = currentPayment(refreshSignals) ?: return null
        if (payment.amount <= 0) return null
        return try {
            withLiveSignals(m.assess(payment, q1, q2, approvedRules), payment)
        } catch (e: Exception) {
            null
        }
    }

    private fun nudgeText(r: Result): String {
        val p = payee ?: return ""
        val parts = mutableListOf<String>()
        if (statusOf(p) == "new") parts += "First payment to ${endSentence(p.name)}"
        val seen = currentPayment(refreshSignals = false)
        val median = seen?.median90d ?: profile.median90d
        val hasHistory = (seen?.payments90d ?: profile.payments90d) >= 10 && median > 0
        val ratio = if (median > 0) amount / median else 0.0
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
        prefillAmount = ""; prefillNote = ""; fromQr = false; qrName = null
        judgeInputs = null; judgeAnswers = null to null; returnTo = null; runModel = null
    }

    companion object {
        const val CONTINUE_DELAY_SECONDS = 10
        const val PIN_LENGTH = 4
        const val MAX_PIN_ATTEMPTS = 3
        /** The person's own second account in the demo (fictional bank). */
        val SELF_ACCOUNT = PayeeRecord("self", "Your Sample Bank A/c", "A/c ••••7319 · SMPL0007319", "YOUR OWN ACCOUNT",
            status = "known", paidTimes = 4, initials = "SB", colorHex = "#6B74D6")
        const val TWENTY_FOUR_HOURS = 24 * 60 * 60 * 1000L
        const val LIMIT_MESSAGE = "UPI allows up to ₹1,00,000 per payment"
        private const val HOUR = 60 * 60 * 1000L
        private const val DAY = 24 * HOUR
        private const val SEVEN_DAYS = 7 * DAY
        private const val NINETY_DAYS = 90 * DAY
        private const val KEEP_ALERT = 24 * 60 * 60 * 1000L
        const val CALL_NOW = "You are on a call right now"
        const val SCREEN_SHARE = "Your screen is being shared or a remote-access app is active"

        private val DEFAULT_CONTACTS = mapOf(
            "sharma" to TrustedContact("Sehjal", "friend", "9876543210"),
            "priya" to TrustedContact("Sehjal", "friend", "9876543210"),
        )

        /** Median, the same way Python's statistics.median does it (mean of the middle two for an even count). */
        fun median(xs: List<Long>): Double {
            if (xs.isEmpty()) return 0.0
            val s = xs.sorted(); val n = s.size
            return if (n % 2 == 1) s[n / 2].toDouble() else (s[n / 2 - 1] + s[n / 2]) / 2.0
        }
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
