package com.example.scriptbreak.logic

import com.example.scriptbreak.ui.model.TxnUi

/**
 * The demo world: who uses the phone, and who can be paid. All names, UPI IDs and numbers are
 * fictional. A real UPI app would read these from its own payment records on the phone.
 */
data class Profile(
    val id: String,
    val displayName: String,
    val median90d: Double,      // usual (median) payment over 90 days, in rupees
    val payments90d: Int,       // number of payments in 90 days
    val contactName: String,    // trusted contact for "Ask family"
    val shownToFamilyAs: String // how the trusted contact's phone names this person
)

data class PayeeRecord(
    val id: String,
    val name: String,
    val upiId: String,
    val bankingName: String,    // the verified name a UPI app shows before paying
    val kind: String = "person",// person | merchant | biller
    val status: String = "new", // status before this session: known | new | flagged
    val paidTimes: Int = 0,
    val initials: String,
    val colorHex: String,
)

object DemoWorld {
    val profiles: Map<String, Profile> = listOf(
        Profile("sharma", "Mr. Sharma", median90d = 650.0, payments90d = 42, contactName = "Rohit", shownToFamilyAs = "Papa"),
        Profile("priya", "Priya", median90d = 1200.0, payments90d = 60, contactName = "Rohit", shownToFamilyAs = "Priya"),
    ).associateBy { it.id }

    val payees: List<PayeeRecord> = listOf(
        // paid before (the people row on Home)
        PayeeRecord("rohit", "Rohit (son)", "rohit.sharma@demoupi", "ROHIT SHARMA", status = "known", paidTimes = 31, initials = "RS", colorHex = "#3D8FD1"),
        PayeeRecord("suresh", "Suresh Verma", "suresh.verma@demoupi", "SURESH KUMAR VERMA", status = "known", paidTimes = 14, initials = "SV", colorHex = "#4F7FB0"),
        PayeeRecord("sunita", "Sunita Iyer", "sunita.iyer@demoupi", "SUNITA IYER", status = "known", paidTimes = 9, initials = "SI", colorHex = "#D0679A"),
        PayeeRecord("kirana", "Sharma Kirana Store", "sharmakirana@demoupi", "SHARMA GENERAL STORE", kind = "merchant", status = "known", paidTimes = 23, initials = "SK", colorHex = "#4F9B7A"),
        // never paid: reached by typing or pasting their UPI ID
        PayeeRecord("ramesh", "Ramesh K.", "ramesh.k@demoupi", "RAMESH KUMAR", initials = "RK", colorHex = "#6B74D6"),
        PayeeRecord("anil", "Anil Mehta", "anil.mehta@demoupi", "ANIL MEHTA", initials = "AM", colorHex = "#B07A3A"),
        PayeeRecord("manoj", "Manoj Electricals", "manoj.elec@demoupi", "MANOJ KUMAR", initials = "ME", colorHex = "#7A8A3A"),
        PayeeRecord("gupta", "Gupta Electronics", "guptaelectronics@demoupi", "GUPTA ELECTRONICS", kind = "merchant", initials = "GE", colorHex = "#8A5BB0"),
    )

    val homePeople: List<PayeeRecord> get() = payees.filter { it.status == "known" }

    fun byId(id: String): PayeeRecord? = payees.firstOrNull { it.id == id }

    private val upiPattern = Regex("^[a-z0-9._-]{2,}@[a-z]{2,}$")

    /** Find a payee by UPI ID. Unknown but valid IDs become a new person named from the ID. */
    fun lookup(upiId: String): PayeeRecord? {
        val id = upiId.trim().lowercase()
        if (!upiPattern.matches(id)) return null
        payees.firstOrNull { it.upiId == id }?.let { return it }
        val local = id.substringBefore('@')
        val words = local.split('.', '_', '-').filter { w -> w.isNotBlank() && w.none { it.isDigit() } }
        val name = words.joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }.ifBlank { local }
        val initials = words.take(2).joinToString("") { it.first().uppercase() }.ifBlank { local.take(2).uppercase() }
        return PayeeRecord("upi:$id", name, id, name.uppercase(), initials = initials, colorHex = "#6B74D6")
    }

    /** Earlier payments shown on Home (synthetic). */
    fun seededRecent(profileId: String): List<TxnUi> = when (profileId) {
        "priya" -> listOf(
            TxnUi("s1", "Sunita Iyer", "Yesterday, 7:40 PM", "₹1,200"),
            TxnUi("s2", "Sharma Kirana Store", "20 Oct, 6:15 PM", "₹860"),
        )
        else -> listOf(
            TxnUi("s1", "Sharma Kirana Store", "Yesterday, 6:12 PM", "₹420"),
            TxnUi("s2", "Rohit (son)", "14 Oct, 2:00 PM", "₹3,000"),
            TxnUi("s3", "Suresh Verma", "1 Oct, 9:30 AM", "₹18,000"),
        )
    }
}
