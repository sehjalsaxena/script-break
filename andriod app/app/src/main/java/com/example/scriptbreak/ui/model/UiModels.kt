package com.example.scriptbreak.ui.model

/**
 * UI State models for all DemoPay and ScriptBreak screens.
 * Uses only simple types (String, Long, Int, Boolean, List<String>, nullable values).
 */

data class PersonUi(
    val id: String,
    val name: String,
    val initials: String,
    val upiId: String = "",
    val colorHex: String = "#5A2CA0"
)

data class TxnUi(
    val id: String,
    val name: String,
    val time: String,
    val amount: String,
    val isCredit: Boolean = false
)

data class HomeUi(
    val userName: String,
    val onCall: Boolean,
    val people: List<PersonUi>,
    val recent: List<TxnUi>
)

data class PayeeUi(
    val name: String,
    val upiId: String,
    val bankingName: String,
    val historyLine: String // "Paid 14 times" or "New payee"
)

data class AmountUi(
    val payeeName: String,
    val amountText: String,
    val note: String,
    val nudge: String?, // show amber inline line only when not null
    val payEnabled: Boolean,
    val aiLevel: String? = null,          // demo view: what the AI decided for this amount
    val aiScore: Double? = null,
    val aiFacts: List<String> = emptyList(),
)

data class PauseUi(
    val amountText: String,
    val payeeName: String,
    val reasons: List<String>,
    val q1: Boolean?,
    val q2: Boolean?,
    val contactName: String,
    val askedFamily: Boolean,
    val modelVersion: String = "" // shown in the footer: "Checked on this phone · AI model …"
)

data class CriticalUi(
    val amountText: String,
    val payeeName: String,
    val isNewPayee: Boolean,
    val reasons: List<String>,
    val contactName: String,
    val askedFamily: Boolean,
    val continueSecondsLeft: Int, // button disabled while > 0
    val hindi: Boolean
)

data class SuccessUi(
    val amountText: String,
    val payeeName: String,
    val upiRef: String,
    val time: String,
    val showReportLine: Boolean
)

data class FamilyAlertUi(
    val fromName: String,
    val amountText: String,
    val payeeName: String,
    val reasons: List<String>
)
