package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppPurple
import com.example.scriptbreak.ui.theme.AppPurpleLight
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.NudgeAmber
import com.example.scriptbreak.ui.theme.SafeGreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import com.example.scriptbreak.logic.FamilyInboxItem
import com.example.scriptbreak.ui.theme.AppSurface
import com.example.scriptbreak.ui.theme.CritRed
import com.example.scriptbreak.ui.theme.CritRedTint
import com.example.scriptbreak.ui.theme.LineBorder
import com.example.scriptbreak.ui.theme.SafeGreenTint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/**
 * Sehjal's phone: the alerts list. A new alert buzzes the phone and shows a phone notification; tapping the
 * notification, or an alert here, opens "Call now / I've called". Nothing jumps open by itself.
 */
@Composable
fun FamilyWaitingScreen(
    online: Boolean,
    alerts: List<FamilyInboxItem> = emptyList(),
    onOpen: (Long) -> Unit = {},
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AppBackground).padding(16.dp).navigationBarsPadding()) {
        AppTopBar(title = "Sehjal's alerts", onBack = onBack, showScriptBreakBadge = true)
        Text(if (online) "● Connected: alerts arrive from Sambhav's phone" else "● Not connected: alerts only show on this same phone",
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (online) SafeGreen else NudgeAmber,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
        if (alerts.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(96.dp).clip(CircleShape).background(AppPurpleLight), contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Notifications, contentDescription = null, tint = AppPurple, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text("No alerts yet", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InkDark, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text("If Sambhav taps \"Ask Sehjal to call me\" during a risky payment, this phone buzzes and shows a notification. Tap it to see who is being paid and why it looks risky.",
                    fontSize = 16.sp, color = MutedText, textAlign = TextAlign.Center, lineHeight = 22.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(alerts, key = { it.key }) { item -> AlertRow(item) { onOpen(item.key) } }
            }
        }
        Text("Sehjal can't see the account or stop the payment.", fontSize = 13.sp, color = MutedText,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun AlertRow(item: FamilyInboxItem, onClick: () -> Unit) {
    val a = item.alert
    val sent = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(item.key))
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (item.done) LineBorder else CritRed.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(if (item.done) SafeGreenTint else CritRedTint), contentAlignment = Alignment.Center) {
                Icon(if (item.done) AppIcons.Check else AppIcons.Warning, contentDescription = null,
                    tint = if (item.done) SafeGreen else CritRed, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${a.fromName} may be on a scam call", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkDark)
                Text("${a.amountText} to ${a.payeeName}", fontSize = 14.sp, color = InkDark)
                Text(if (item.done) "Sent at $sent · Answered" else "Sent at $sent · Tap to open",
                    fontSize = 13.sp, color = if (item.done) SafeGreen else MutedText)
            }
            if (!item.read) Box(Modifier.size(10.dp).clip(CircleShape).background(CritRed))
        }
    }
}
