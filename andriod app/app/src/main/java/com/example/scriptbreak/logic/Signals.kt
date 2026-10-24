package com.example.scriptbreak.logic

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

/**
 * Phone signals the model uses, read only while the app is open.
 * Never read: call audio, caller number, call log, messages, contacts, location, clipboard contents.
 */
interface SignalSource {
    val callNow: Boolean
    val screenShare: Boolean
    fun callSeenWithin30m(nowMs: Long): Boolean
    fun isNight(nowMs: Long): Boolean
    fun refresh(nowMs: Long) {}
}

class AndroidSignals(context: Context) : SignalSource {
    private val audio = context.getSystemService(AudioManager::class.java)
    private var lastCallSeenAt = 0L

    /** A phone or internet call is active: the audio mode only, never who or what is on the call. */
    override var callNow by mutableStateOf(false)
        private set

    /** This app's screen is being recorded or shared (Android 15+ callback, set by MainActivity). */
    override var screenShare by mutableStateOf(false)

    override fun refresh(nowMs: Long) {
        val mode = audio?.mode
        val inCall = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
        callNow = inCall
        if (inCall) lastCallSeenAt = nowMs
    }

    /** "Hang up, then pay": a call was seen while this app was open in the last 30 minutes. */
    override fun callSeenWithin30m(nowMs: Long): Boolean =
        !callNow && lastCallSeenAt > 0 && nowMs - lastCallSeenAt <= 30 * 60 * 1000L

    override fun isNight(nowMs: Long): Boolean {
        val hour = Calendar.getInstance().apply { timeInMillis = nowMs }.get(Calendar.HOUR_OF_DAY)
        return hour >= 23 || hour < 6
    }
}
