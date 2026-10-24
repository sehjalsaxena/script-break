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
        Account("sharma", "Mr. Sharma", mode = "sharma", loginPinHash = "6500a6be6e406c1d4eccd746abb05e909b07debe4f5e1470ec5775c15bae45dd", upiPinHash = "4a0a54fdcb49a5571952304eac16d4968b34b150c733c528a1c8247a9c1f4fc3", demoLoginPin = "1111", demoUpiPin = "4821"),
        Account("priya", "Priya", mode = "priya", loginPinHash = "082b8fa8512c9854e21e7a009cc24648bcca3402c1a4ed66e0aaf2f69a058651", upiPinHash = "eefc5d580978d29279dc9756eb56750e65dc468be5657d70db72c3ee44159507", demoLoginPin = "2222", demoUpiPin = "7316"),
        Account("rohit", "Rohit (son)", mode = "rohit", loginPinHash = "57e9ba44af7e1c3711f7c66ed8fdf9a569534fd0192b06f87abb13640b350ab3", upiPinHash = null, demoLoginPin = "3333", demoUpiPin = null),
        Account("judge", "Judge", mode = "judge", loginPinHash = "72cdfeadb538864524a9e9a8641acf4a305c9b21d757a2c144de8c6744bb08c6", upiPinHash = null, demoLoginPin = "0000", demoUpiPin = null),
        Account("analyst", "Analyst", mode = "analyst", loginPinHash = "f1806240e1e119d841fdb68070019777d55c9e57d56d02f1bc2dbe28da141c9e", upiPinHash = null, demoLoginPin = "9999", demoUpiPin = null),
    )

    fun account(username: String): Account? = accounts.firstOrNull { it.username == username.trim().lowercase() }

    fun hash(s: String): String =
        MessageDigest.getInstance("SHA-256").digest("scriptbreak:$s".toByteArray()).joinToString("") { "%02x".format(it) }

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

    val contacts: List<ContactItem> = listOf(
        ContactItem("c1", "Rohit (son)", "+91 98765 43210", "rohit.sharma@demoupi", "RS", "#3D8FD1", "rohit"),
        ContactItem("c2", "Suresh Verma", "+91 98111 22334", "suresh.verma@demoupi", "SV", "#4F7FB0", "suresh"),
        ContactItem("c3", "Sunita Iyer", "+91 98222 33445", "sunita.iyer@demoupi", "SI", "#D0679A", "sunita"),
        ContactItem("c4", "Sharma Kirana Store", "+91 98333 44556", "sharmakirana@demoupi", "SK", "#4F9B7A", "kirana"),
        ContactItem("c5", "Anil Mehta", "+91 98444 55667", "anil.mehta@demoupi", "AM", "#B07A3A", "anil"),
        ContactItem("c6", "Ramesh K.", "+91 98555 66778", "ramesh.k@demoupi", "RK", "#6B74D6", "ramesh"),
        ContactItem("c7", "Manoj Electricals", "+91 98666 11223", "manoj.elec@demoupi", "ME", "#7A8A3A", "manoj"),
        ContactItem("c8", "Gupta Electronics", "+91 98777 22334", "guptaelectronics@demoupi", "GE", "#8A5BB0", "gupta"),
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
        // Mr. Sharma's regular payments: rent to Suresh and money to Rohit, every month
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
