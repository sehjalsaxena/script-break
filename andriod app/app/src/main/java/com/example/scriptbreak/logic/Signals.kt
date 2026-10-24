package com.example.scriptbreak.logic

import android.content.Context
import android.hardware.display.DisplayManager
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
    private val displays = context.getSystemService(DisplayManager::class.java)
    private var lastCallSeenAt = 0L
    private var mirrored = false

    /** A phone or internet call is active: the audio mode only, never who or what is on the call. */
    override var callNow by mutableStateOf(false)
        private set

    /**
     * Screen sharing, from two checks (never what is on the screen, never which app shares it):
     * 1. Android 15+: this app's window is being recorded or shared, e.g. screen share in a video call
     *    (WindowManager screen-recording callback, set by MainActivity).
     * 2. Any Android: the screen is mirrored or cast to another display (a presentation display exists).
     */
    override var screenShare by mutableStateOf(false)
        private set

    /** Set by MainActivity's Android 15+ screen-recording callback. */
    var screenRecorded = false
        set(value) { field = value; screenShare = value || mirrored }

    /** Set by MainActivity: true only while the app is on screen. Nothing is read in the background. */
    var appVisible = false

    override fun refresh(nowMs: Long) {
        if (!appVisible) return
        val mode = audio?.mode
        val inCall = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
        callNow = inCall
        if (inCall) lastCallSeenAt = nowMs
        mirrored = try { displays?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)?.isNotEmpty() == true } catch (e: Exception) { false }
        screenShare = screenRecorded || mirrored
    }

    /** "Hang up, then pay": a call was seen while this app was open in the last 30 minutes. */
    override fun callSeenWithin30m(nowMs: Long): Boolean =
        !callNow && lastCallSeenAt > 0 && nowMs - lastCallSeenAt <= 30 * 60 * 1000L

    override fun isNight(nowMs: Long): Boolean {
        val hour = Calendar.getInstance().apply { timeInMillis = nowMs }.get(Calendar.HOUR_OF_DAY)
        return hour >= 23 || hour < 6
    }
}
