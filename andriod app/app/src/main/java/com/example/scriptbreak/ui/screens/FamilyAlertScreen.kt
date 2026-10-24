package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.R
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.model.FamilyAlertUi
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppSurface
import com.example.scriptbreak.ui.theme.CritRed
import com.example.scriptbreak.ui.theme.CritRedTint
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.LineBorder
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.PauseOrange
import com.example.scriptbreak.ui.theme.PauseOrangeTint
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

@Composable
fun FamilyAlertScreen(
    state: FamilyAlertUi,
    onCallNow: () -> Unit,
    onCalled: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Top App Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = "Back",
                        tint = InkDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
                    fontSize = 13.sp,
                    color = MutedText
                )
            }

            // Red notification indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CritRedTint)
                    .border(1.dp, CritRed.copy(alpha = 0.3f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(CritRed)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Alert Card
            Surface(
                color = CritRedTint,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CritRed.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CritRed)
                        ) {
                            Icon(
                                imageVector = AppIcons.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.family_alert_title, state.fromName),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CritRed
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val amountFormatted = if (state.amountText.startsWith("₹")) state.amountText else "₹${state.amountText}"
                    Text(
                        text = stringResource(R.string.family_alert_sub, state.fromName, amountFormatted, state.payeeName),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = InkDark,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // the real time the alert was sent; refreshed every 30 seconds
                    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
                    state.sentAtMs?.let { sent ->
                        val mins = ((now - sent) / 60_000).coerceAtLeast(0)
                        val ago = when {
                            mins < 1 -> "just now"
                            mins < 60 -> "$mins min ago"
                            else -> "${mins / 60} h ${mins % 60} min ago"
                        }
                        Text(
                            text = (if (mins >= 10) "Missed alert · " else "") + "Sent at " +
                                SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(sent)) + " · " + ago,
                            fontSize = 14.sp,
                            fontWeight = if (mins >= 10) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (mins >= 10) CritRed else MutedText
                        )
                    }
                }
            }

            // Signals Card
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE8ECF2)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    state.reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isCall = reason.contains("call", ignoreCase = true)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isCall) CritRedTint else PauseOrangeTint)
                            ) {
                                Icon(
                                    imageVector = if (isCall) AppIcons.Phone else AppIcons.Warning,
                                    contentDescription = null,
                                    tint = if (isCall) CritRed else PauseOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = reason,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = InkDark,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // "Call Sambhav now" button (Orange, >= 48dp, with phone icon)
        Button(
            onClick = onCallNow,
            colors = ButtonDefaults.buttonColors(
                containerColor = PauseOrange,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Phone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.family_alert_call_now, state.fromName),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // I've called button (Outlined / Secondary, >= 48dp)
        OutlinedButton(
            onClick = onCalled,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, LineBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = InkDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.family_alert_called),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Disclaimer footnote: "Sehjal can't see the account or stop the payment."
        Text(
            text = stringResource(R.string.family_alert_disclaimer),
            fontSize = 14.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FamilyAlertScreenPreview() {
    ScriptBreakTheme {
        FamilyAlertScreen(
            state = FamilyAlertUi(
                fromName = "Sambhav",
                amountText = "₹48,000",
                payeeName = "Ramesh K.",
                reasons = listOf(
                    "Call detected during this payment",
                    "New payee – never paid before",
                    "Amount is 4x+ higher than usual"
                )
            ),
            onCallNow = {},
            onCalled = {}
        )
    }
}
