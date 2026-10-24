package com.example.scriptbreak.logic

import android.content.Context
import java.util.UUID
import com.example.scriptbreak.R
import com.example.scriptbreak.ui.model.FamilyAlertUi
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * "Ask family" between two phones, through Firebase Realtime Database (free plan).
 * Mr. Sharma's phone writes one short alert; Rohit's phone (Rohit profile) listens and shows it.
 * Rohit's answer ("calling", "called") goes back the same way.
 *
 * Only what the person chose to send leaves the phone: who, amount, payee name and up to 3 reasons.
 * No account number, no UPI PIN, nothing about the call. If Firebase is not set up or there is no
 * internet, the alert still shows on this phone (Rohit profile) and Firebase sends it once online.
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
        val now = System.currentTimeMillis()   // the time "Ask family" was tapped, even if sign-in is slow
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
                "to" to "rohit",            // the trusted contact's demo account
            ))
        }
    }

    /** Rohit's answer: "calling" or "called". */
    fun setStatus(status: String) {
        val n = node ?: return
        signedIn { n.child("status").setValue(status) }
    }

    /** Delete the alert from Firebase: nothing is kept once it has been handled. */
    fun clear() {
        val n = node ?: return
        signedIn { n.removeValue() }
    }

    private var listener: ValueEventListener? = null

    /**
     * Listen for alerts and status changes, from app start, whoever is logged in.
     * [onAlert]: a new alert from ANOTHER phone (sent in the last 10 minutes).
     * [onStatus]: Rohit's answer to an alert THIS phone sent.
     * [onCleared]: the alert is gone (handled, deleted or too old). Call [stop] to remove the listener.
     */
    fun listen(onAlert: (Long, FamilyAlertUi) -> Unit, onStatus: (Long, String) -> Unit, onCleared: () -> Unit = {}) {
        val n = node ?: return
        stop()
        signedIn {
            val l = object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    val id = (s.child("id").value as? Number)?.toLong()
                    if (id == null) { onCleared(); return }                     // no alert: deleted or never sent
                    val sentAt = (s.child("sentAt").value as? Number)?.toLong() ?: 0L
                    val status = s.child("status").value as? String ?: "sent"
                    if (System.currentTimeMillis() - sentAt >= TEN_MINUTES) {   // stale: delete, keep nothing
                        s.ref.removeValue(); onCleared(); return
                    }
                    val mine = s.child("fromDevice").value == deviceId
                    if (status == "sent") {
                        if (mine) return                                         // our own alert echoing back
                        onAlert(id, FamilyAlertUi(
                            fromName = s.child("fromName").value as? String ?: "",
                            amountText = s.child("amountText").value as? String ?: "",
                            payeeName = s.child("payeeName").value as? String ?: "",
                            reasons = s.child("reasons").children.mapNotNull { it.value as? String },
                        ))
                    } else if (mine) {
                        onStatus(id, status)                                     // Rohit answered OUR alert
                    } else if (status == "called") {
                        onCleared()                                              // handled on Rohit's phone
                    }
                }
                override fun onCancelled(e: DatabaseError) {}
            }
            listener = l
            n.addValueEventListener(l)
        }
    }

    fun stop() {
        listener?.let { node?.removeEventListener(it) }
        listener = null
    }

    companion object {
        /** Demo pairing: Mr. Sharma's family. A real app pairs phones with the trusted-contact OTP. */
        const val FAMILY_ID = "sharma-family"
        private const val TEN_MINUTES = 10 * 60 * 1000L
    }
}
