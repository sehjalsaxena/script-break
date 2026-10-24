package com.example.scriptbreak.logic

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.scriptbreak.MainActivity
import com.example.scriptbreak.ui.model.FamilyAlertUi

/** Makes the trusted contact's phone buzz: a strong vibration and a high-priority notification. */
object FamilyBuzz {
    private const val CHANNEL = "family_alerts"

    fun buzz(context: Context, alert: FamilyAlertUi) {
        vibrate(context)
        notify(context, alert)
    }

    private fun vibrate(context: Context) {
        val pattern = longArrayOf(0, 600, 250, 600, 250, 900)
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
        v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun notify(context: Context, alert: FamilyAlertUi) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Family alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "When a family member asks you to call them about a payment"
        })
        val open = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("${alert.fromName} may be on a scam call")
            .setContentText("About to pay ${alert.amountText} to ${alert.payeeName}. Call now.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(1, n) } catch (e: SecurityException) { }
    }
}
