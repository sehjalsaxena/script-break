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

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/** Sehjal's phone, waiting for an "Ask family" alert. */
@Composable
fun FamilyWaitingScreen(online: Boolean, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AppBackground).padding(16.dp).navigationBarsPadding()) {
        AppTopBar(title = "Sehjal's phone", onBack = onBack, showScriptBreakBadge = true)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(AppPurpleLight), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Notifications, contentDescription = null, tint = AppPurple, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Waiting for alerts from Sambhav", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InkDark, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text("If Sambhav taps \"Ask Sehjal to call me\" during a risky payment, this phone buzzes and shows who is being paid and why it looks risky.",
                fontSize = 16.sp, color = MutedText, textAlign = TextAlign.Center, lineHeight = 22.sp)
            Spacer(Modifier.height(16.dp))
            Text(if (online) "● Connected: alerts arrive from Sambhav's phone" else "● Not connected: alerts only show on this same phone",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (online) SafeGreen else NudgeAmber, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text("Sehjal can't see the account or stop the payment.", fontSize = 14.sp, color = MutedText, textAlign = TextAlign.Center)
        }
    }
}
