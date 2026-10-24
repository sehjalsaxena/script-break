package com.example.scriptbreak.logic

import com.example.scriptbreak.ui.model.TxnUi
import java.security.MessageDigest

/**
 * The demo world: who uses the phone, and who can be paid. All names, UPI IDs and numbers are
 * fictional. A real UPI app would read these from its own payment records on the phone.
 */
data class Profile(
    val id: String,
    val displayName: String,
    val median90d: Double,      // usual (median) payment in the synthetic 90-day history the demo starts with
    val payments90d: Int,       // number of payments in that history
    val contactName: String,    // trusted contact for "Ask family"
    val shownToFamilyAs: String,// how the trusted contact's phone names this person
    val openingBalance: Long = 0, // demo balance before this session's payments (synthetic)
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

/**
 * A demo login. PINs are kept only as salted SHA-256 hashes; the plain demo PINs are listed on the
 * login screen on purpose, so judges can sign in. A real bank app uses its own secure login and NPCI's
 * UPI PIN pad; ScriptBreak never sees or stores a real UPI PIN.
 */
data class Account(
    val username: String,
    val displayName: String,
    val mode: String,              // profile / mode opened after login: sharma | priya | rohit | judge | analyst
    val loginPinHash: String,
    val upiPinHash: String?,       // only people who pay have a UPI PIN
    val demoLoginPin: String,      // shown on the login screen (demo only)
    val demoUpiPin: String?,
    val upiId: String? = null,     // this person's own demo UPI ID (others can pay it)
) {
    fun checkLogin(pin: String) = DemoWorld.hash("$username:$pin") == loginPinHash
    fun checkUpiPin(pin: String) = upiPinHash != null && DemoWorld.hash("$username-upi:$pin") == upiPinHash
}

data class ContactItem(
    val id: String,
    val name: String,
    val phone: String,
    val upiId: String,
    val initials: String,
    val colorHex: String,
    val payeeId: String? = null
)

object DemoWorld {
    /** Maximum for one UPI payment (the NPCI limit for most person-to-person payments). */
    const val UPI_LIMIT = 100_000L

    val accounts: List<Account> = listOf(
        Account("sambhav", "Sambhav Jain", mode = "sharma", loginPinHash = "ba9daeba79544e90605d91663f69a50ee8655080c1505c869f2bebcb7a2de7eb", upiPinHash = "3833b52b6eea3d8af29f5963cfba0d4d13c01165ce25fae17e75f968c491e9d8", demoLoginPin = "1111", demoUpiPin = "4821", upiId = "sambhav.jain@demoupi"),
        Account("deepak", "Deepak Rawat", mode = "priya", loginPinHash = "2fda858c3af4887cfb817e4937a32673b0746e4a9bd22dc123dc5c51efd00fc9", upiPinHash = "486eb1896cba6cdd1dd5beae845bca5a27fc1c810bafe162bc40b11beffa42d3", demoLoginPin = "2222", demoUpiPin = "7316", upiId = "deepak.rawat@demoupi"),
        Account("sehjal", "Sehjal Saxena", mode = "rohit", loginPinHash = "241d05bd50b7bbb45b47bc989f746535fb7caa56109678ca7a82fe9167321cfa", upiPinHash = "d9ffb2cad0ce93c67843b59297e8329c549836fa5142a2b89aa1162fa346ee26", demoLoginPin = "3333", demoUpiPin = "2580", upiId = "sehjal.saxena@demoupi"),
        Account("judge", "Judge", mode = "judge", loginPinHash = "72cdfeadb538864524a9e9a8641acf4a305c9b21d757a2c144de8c6744bb08c6", upiPinHash = null, demoLoginPin = "0000", demoUpiPin = null),
        Account("analyst", "Analyst", mode = "analyst", loginPinHash = "f1806240e1e119d841fdb68070019777d55c9e57d56d02f1bc2dbe28da141c9e", upiPinHash = null, demoLoginPin = "9999", demoUpiPin = null),
    )

    fun account(username: String): Account? = accounts.firstOrNull { it.username == username.trim().lowercase() }

    fun hash(s: String): String =
        MessageDigest.getInstance("SHA-256").digest("scriptbreak:$s".toByteArray()).joinToString("") { "%02x".format(it) }

    val profiles: Map<String, Profile> = listOf(
        Profile("sharma", "Sambhav Jain", median90d = 650.0, payments90d = 42, contactName = "Sehjal", shownToFamilyAs = "Sambhav", openingBalance = 248_250),
        Profile("rohit", "Sehjal Saxena", median90d = 800.0, payments90d = 48, contactName = "Sambhav", shownToFamilyAs = "Sehjal", openingBalance = 152_750),
        Profile("priya", "Deepak Rawat", median90d = 1200.0, payments90d = 60, contactName = "Sehjal", shownToFamilyAs = "Deepak", openingBalance = 186_400),
    ).associateBy { it.id }

    val payees: List<PayeeRecord> = listOf(
        // paid before (the people row on Home)
        PayeeRecord("rohit", "Sehjal Saxena", "sehjal.saxena@demoupi", "SEHJAL SAXENA", status = "known", paidTimes = 31, initials = "SS", colorHex = "#3D8FD1"),
        PayeeRecord("suresh", "Suresh Verma", "suresh.verma@demoupi", "SURESH KUMAR VERMA", status = "known", paidTimes = 14, initials = "SV", colorHex = "#4F7FB0"),
        PayeeRecord("sunita", "Sunita Iyer", "sunita.iyer@demoupi", "SUNITA IYER", status = "known", paidTimes = 9, initials = "SI", colorHex = "#D0679A"),
        PayeeRecord("kirana", "Sharma Kirana Store", "sharmakirana@demoupi", "SHARMA GENERAL STORE", kind = "merchant", status = "known", paidTimes = 23, initials = "SK", colorHex = "#4F9B7A"),
        // the two payers in the demo: each can pay the other (money arrives on the other's phone)
        PayeeRecord("sambhav", "Sambhav Jain", "sambhav.jain@demoupi", "SAMBHAV JAIN", status = "known", paidTimes = 5, initials = "SJ", colorHex = "#C2185B"),
        PayeeRecord("deepak", "Deepak Rawat", "deepak.rawat@demoupi", "DEEPAK RAWAT", status = "known", paidTimes = 5, initials = "DR", colorHex = "#00897B"),
        // never paid: reached by typing or pasting their UPI ID
        PayeeRecord("ramesh", "Ramesh K.", "ramesh.k@demoupi", "RAMESH KUMAR", initials = "RK", colorHex = "#6B74D6"),
        PayeeRecord("anil", "Anil Mehta", "anil.mehta@demoupi", "ANIL MEHTA", initials = "AM", colorHex = "#B07A3A"),
        PayeeRecord("manoj", "Manoj Electricals", "manoj.elec@demoupi", "MANOJ KUMAR", initials = "ME", colorHex = "#7A8A3A"),
        PayeeRecord("gupta", "Gupta Electronics", "guptaelectronics@demoupi", "GUPTA ELECTRONICS", kind = "merchant", initials = "GE", colorHex = "#8A5BB0"),
    )

    val homePeople: List<PayeeRecord> get() = payees.filter { it.status == "known" }

    /** Whose phone gets this person's "Ask family" alert: Sehjal for Sambhav and Deepak, Sambhav for Sehjal. */
    fun alertRecipient(profileId: String): String = if (profileId == "rohit") "sharma" else "rohit"

    /** This person's own demo UPI ID (null for Judge / Analyst). */
    fun ownUpi(profileId: String): String? = accounts.firstOrNull { it.mode == profileId }?.upiId

    /** The demo person who owns this UPI ID, if any (their phone receives the money). */
    fun accountByUpi(upiId: String): Account? = accounts.firstOrNull { it.upiId != null && it.upiId == upiId.trim().lowercase() }

    /** People and contacts as one person sees them: never themselves. */
    fun peopleFor(profileId: String): List<PayeeRecord> = homePeople.filter { it.upiId != ownUpi(profileId) }
    fun contactsFor(profileId: String): List<ContactItem> = contacts.filter { it.upiId != ownUpi(profileId) }
    fun allPeopleFor(profileId: String): List<PayeeRecord> =
        peopleFor(profileId) + payees.filter { it.status != "known" && it.upiId != ownUpi(profileId) }

    val contacts: List<ContactItem> = listOf(
        ContactItem("c1", "Sehjal Saxena", "+91 98765 43210", "sehjal.saxena@demoupi", "SS", "#3D8FD1", "rohit"),
        ContactItem("c2", "Suresh Verma", "+91 98111 22334", "suresh.verma@demoupi", "SV", "#4F7FB0", "suresh"),
        ContactItem("c3", "Sunita Iyer", "+91 98222 33445", "sunita.iyer@demoupi", "SI", "#D0679A", "sunita"),
        ContactItem("c4", "Sharma Kirana Store", "+91 98333 44556", "sharmakirana@demoupi", "SK", "#4F9B7A", "kirana"),
        ContactItem("c5", "Anil Mehta", "+91 98444 55667", "anil.mehta@demoupi", "AM", "#B07A3A", "anil"),
        // Ramesh K. (the scammer's account in the demo) is deliberately NOT a saved contact: the caller dictates his UPI ID
        ContactItem("c7", "Manoj Electricals", "+91 98666 11223", "manoj.elec@demoupi", "ME", "#7A8A3A", "manoj"),
        ContactItem("c8", "Gupta Electronics", "+91 98777 22334", "guptaelectronics@demoupi", "GE", "#8A5BB0", "gupta"),
        ContactItem("c9", "Deepak Rawat", "+91 98888 33445", "deepak.rawat@demoupi", "DR", "#00897B", "deepak"),
        ContactItem("c10", "Sambhav Jain", "+91 98999 44556", "sambhav.jain@demoupi", "SJ", "#C2185B", "sambhav"),
    )

    fun byId(id: String): PayeeRecord? = payees.firstOrNull { it.id == id }

    // Enforce that UPI ID is strictly @demoupi
    private val upiPattern = Regex("^[a-z0-9._-]{2,}@demoupi$")

    /** Find a payee by UPI ID. Only @demoupi IDs are supported. Unknown but valid IDs become a new person named from the ID. */
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

    /**
     * The synthetic 90-day payment history each person starts with (amounts only, all within 90 days).
     * Built so its median is exactly the profile's usual payment. Every real payment made in the app is
     * added to it (PaymentFlow.paymentsLast90d), so the usual payment the AI sees changes as the person pays.
     */
    fun history90d(profileId: String): List<Long> {
        val p = profiles[profileId] ?: return emptyList()
        val m = p.median90d
        // Sambhav's regular payments: rent to Suresh and money to Sehjal, every month
        val recurring = if (profileId == "sharma") listOf(18_000L, 18_000L, 18_000L, 3_000L, 3_000L, 3_000L) else emptyList()
        val below = p.payments90d / 2 - 1
        val above = p.payments90d - below - 2 - recurring.size
        fun r10(x: Double) = Math.round(x / 10) * 10
        val low = List(below) { i -> r10(m * (0.2 + 0.75 * i / maxOf(1, below - 1))) }    // 20% to 95% of usual
        val high = List(above) { i -> r10(m * (1.1 + 2.9 * i / maxOf(1, above - 1))) }    // 110% to 400% of usual
        return low + listOf(m.toLong(), m.toLong()) + high + recurring
    }

    /** Earlier payments shown on Home (synthetic). */
    fun seededRecent(profileId: String): List<TxnUi> = when (profileId) {
        "rohit" -> listOf(
            TxnUi("s1", "Sunita Iyer", "Yesterday, 8:10 PM", "₹600"),
            TxnUi("s2", "Sharma Kirana Store", "21 Oct, 5:40 PM", "₹740"),
        )
        "priya" -> listOf(
            TxnUi("s1", "Sunita Iyer", "Yesterday, 7:40 PM", "₹1,200"),
            TxnUi("s2", "Sharma Kirana Store", "20 Oct, 6:15 PM", "₹860"),
        )
        else -> listOf(
            TxnUi("s1", "Sharma Kirana Store", "Yesterday, 6:12 PM", "₹420"),
            TxnUi("s2", "Sehjal Saxena", "14 Oct, 2:00 PM", "₹3,000"),
            TxnUi("s3", "Suresh Verma", "1 Oct, 9:30 AM", "₹18,000"),
        )
    }
}
