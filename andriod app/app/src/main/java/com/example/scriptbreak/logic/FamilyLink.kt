package com.example.scriptbreak.logic

import android.content.Context
import java.util.UUID
import com.example.scriptbreak.R
import com.example.scriptbreak.ui.model.FamilyAlertUi
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ChildEventListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * "Ask family" between two phones, through Firebase Realtime Database (free plan).
 * Sambhav's phone writes one short alert; Sehjal's phone (Sehjal profile) listens and shows it.
 * Sehjal's answer ("calling", "called") goes back the same way.
 *
 * Only what the person chose to send leaves the phone: who, amount, payee name and up to 3 reasons.
 * No account number, no UPI PIN, nothing about the call. If Firebase is not set up or there is no
 * internet, the alert still shows on this phone (Sehjal profile) and Firebase sends it once online.
 */
class FamilyLink(context: Context) : FamilyAlertSender {
    private val node: DatabaseReference? = try {
        val url = context.getString(R.string.firebase_database_url)
        if (url.isBlank() || FirebaseApp.getApps(context).isEmpty() && FirebaseApp.initializeApp(context) == null) null
        else FirebaseDatabase.getInstance(url).reference.child("alerts").child(FAMILY_ID)
    } catch (e: Exception) {
        null
    }
    private val auth: FirebaseAuth? = if (node != null) try { FirebaseAuth.getInstance() } catch (e: Exception) { null } else null

    /**
     * A random id for this install (no phone number, IMEI or account). It tells "my own alert echoing
     * back" apart from "an alert from another phone", whoever is logged in on either phone.
     */
    private val deviceId: String = context.getSharedPreferences("scriptbreak", Context.MODE_PRIVATE).let { prefs ->
        prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("device_id", it).apply() }
    }

    /** Demo payments between the team's phones (e.g. Sambhav pays Deepak). */
    private val payments: DatabaseReference? = node?.parent?.child("payments")

    /** True when alerts can reach another phone (Firebase configured). */
    val online: Boolean get() = node != null

    private val retry = android.os.Handler(android.os.Looper.getMainLooper())

    /** Signs in (anonymously) first; with no internet yet, tries again every 5 seconds. */
    private fun signedIn(then: () -> Unit) {
        val a = auth ?: return
        if (a.currentUser != null) then()
        else a.signInAnonymously().addOnSuccessListener { then() }
            .addOnFailureListener { retry.postDelayed({ signedIn(then) }, 5_000) }
    }

    override fun send(alert: FamilyAlertUi) {
        val n = node ?: return
        val now = alert.sentAtMs ?: System.currentTimeMillis()   // the time "Ask family" was tapped, even if sign-in is slow
        signedIn {
            n.setValue(mapOf(
                "id" to now,
                "fromName" to alert.fromName,
                "amountText" to alert.amountText,
                "payeeName" to alert.payeeName,
                "reasons" to alert.reasons.take(3),
                "status" to "sent",
                "sentAt" to now,
                "fromDevice" to deviceId,
                "to" to alert.toProfile,    // the trusted contact's demo account
            ))
        }
    }

    /** Sehjal's answer: "calling" or "called". */
    fun setStatus(status: String) {
        val n = node ?: return
        signedIn { n.child("status").setValue(status) }
    }

    /** Delete the alert from Firebase: nothing is kept once it has been handled (or after 24 hours). */
    fun clear() {
        val n = node ?: return
        signedIn { n.removeValue() }
    }

    private var listener: ValueEventListener? = null
    private var onAlert: (Long, FamilyAlertUi) -> Unit = { _, _ -> }
    private var onStatus: (Long, String) -> Unit = { _, _ -> }
    private var onCleared: () -> Unit = {}

    /**
     * Listen for alerts and status changes, from app start, whoever is logged in.
     * [onAlert]: an alert from ANOTHER phone that Sehjal has not handled yet (kept up to 24 hours).
     * [onStatus]: Sehjal's answer to an alert THIS phone sent.
     * [onCleared]: the alert is gone (handled, deleted or too old). Call [stop] to remove the listener.
     */
    fun listen(onAlert: (Long, FamilyAlertUi) -> Unit, onStatus: (Long, String) -> Unit, onCleared: () -> Unit = {}) {
        this.onAlert = onAlert; this.onStatus = onStatus; this.onCleared = onCleared
        val n = node ?: return
        stop()
        signedIn {
            val l = object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) = handle(s, onLogin = false)
                override fun onCancelled(e: DatabaseError) {}
            }
            listener = l
            n.addValueEventListener(l)
        }
    }

    /**
     * Sehjal just logged in: read the alert from Firebase NOW, so one sent while he was logged out, or
     * while this app was closed, is shown straight away (with the time it was sent).
     */
    fun fetchLatest() {
        val n = node ?: return
        signedIn { n.get().addOnSuccessListener { handle(it, onLogin = true) } }
    }

    private fun handle(s: DataSnapshot, onLogin: Boolean) {
        val id = (s.child("id").value as? Number)?.toLong()
        if (id == null) { onCleared(); return }                              // no alert: deleted or never sent
        val sentAt = (s.child("sentAt").value as? Number)?.toLong() ?: id
        val status = s.child("status").value as? String ?: "sent"
        if (System.currentTimeMillis() - sentAt >= KEEP_FOR) {               // over 24 hours: delete, keep nothing
            s.ref.removeValue(); onCleared(); return
        }
        val mine = s.child("fromDevice").value == deviceId
        // still open until Sehjal taps "I've called"; a live change to "calling" is Sehjal's own tap, not a new alert
        val open = if (onLogin) status != "called" else status == "sent"
        when {
            open && !mine -> onAlert(id, FamilyAlertUi(
                fromName = s.child("fromName").value as? String ?: "",
                amountText = s.child("amountText").value as? String ?: "",
                payeeName = s.child("payeeName").value as? String ?: "",
                reasons = s.child("reasons").children.mapNotNull { it.value as? String },
                sentAtMs = sentAt,
                toProfile = s.child("to").value as? String ?: "rohit",
            ))
            mine && status != "sent" && !onLogin -> onStatus(id, status)       // Sehjal answered OUR alert
            !mine && status == "called" -> onCleared()                        // handled on Sehjal's phone
        }
    }

    /** Sends a demo payment to the receiver's phone: who, how much, the UPI reference and note. */
    fun sendPayment(t: DemoTransfer) {
        val ref = payments ?: return
        signedIn {
            ref.child(t.id).setValue(mapOf(
                "to" to t.toProfile, "fromName" to t.fromName, "fromUpi" to t.fromUpi, "amount" to t.amount,
                "atMs" to t.atMs, "upiRef" to t.upiRef, "note" to t.note,
            ))
        }
    }

    /** Every demo payment, including ones sent while this app was closed. Older than 24 hours: deleted. */
    fun listenPayments(onReceived: (DemoTransfer) -> Unit) {
        val ref = payments ?: return
        signedIn {
            ref.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(s: DataSnapshot, previous: String?) {
                    val atMs = (s.child("atMs").value as? Number)?.toLong() ?: return
                    if (System.currentTimeMillis() - atMs >= KEEP_FOR) { s.ref.removeValue(); return }
                    onReceived(DemoTransfer(
                        id = s.key ?: return,
                        toProfile = s.child("to").value as? String ?: return,
                        fromName = s.child("fromName").value as? String ?: "",
                        fromUpi = s.child("fromUpi").value as? String ?: "",
                        amount = (s.child("amount").value as? Number)?.toLong() ?: return,
                        atMs = atMs,
                        upiRef = s.child("upiRef").value as? String ?: "",
                        note = s.child("note").value as? String ?: "",
                    ))
                }
                override fun onChildChanged(s: DataSnapshot, previous: String?) {}
                override fun onChildRemoved(s: DataSnapshot) {}
                override fun onChildMoved(s: DataSnapshot, previous: String?) {}
                override fun onCancelled(e: DatabaseError) {}
            })
        }
    }

    /** Reset demo: delete the money this person received. */
    fun clearPaymentsTo(profileId: String) {
        val ref = payments ?: return
        signedIn {
            ref.get().addOnSuccessListener { snap ->
                snap.children.filter { it.child("to").value == profileId }.forEach { it.ref.removeValue() }
            }
        }
    }

    fun stop() {
        listener?.let { node?.removeEventListener(it) }
        listener = null
    }

    companion object {
        /** Demo pairing: Sambhav's family. A real app pairs phones with the trusted-contact OTP. */
        const val FAMILY_ID = "sharma-family"
        /** An alert Sehjal has not handled is kept this long, then deleted. */
        private const val KEEP_FOR = 24 * 60 * 60 * 1000L
    }
}
