package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.R
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.components.DemoSecondaryButton
import com.example.scriptbreak.ui.model.FamilyAlertUi
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.CritRed
import com.example.scriptbreak.ui.theme.CritRedTint
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.PauseOrange
import com.example.scriptbreak.ui.theme.PauseOrangeInk
import com.example.scriptbreak.ui.theme.PauseOrangeTint
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

@Composable
fun FamilyAlertScreen(
    state: FamilyAlertUi,
    onCallNow: () -> Unit,
    onCalled: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
    ) {
        // Top App Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark
                )
                Text(
                    text = "Family alert (Rohit's phone)",
                    fontSize = 13.sp,
                    color = MutedText
                )
            }

            Surface(
                color = CritRedTint,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CritRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Alert",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CritRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Alert Card
            Surface(
                color = PauseOrangeTint,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PauseOrange.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PauseOrange)
                        ) {
                            Icon(
                                imageVector = AppIcons.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.family_alert_title, state.fromName),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PauseOrangeInk
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val amountFormatted = if (state.amountText.startsWith("₹")) state.amountText else "₹${state.amountText}"
                    Text(
                        text = stringResource(R.string.family_alert_sub, state.fromName, amountFormatted, state.payeeName),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = PauseOrangeInk,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "2 mins ago",
                        fontSize = 13.sp,
                        color = MutedText
                    )
                }
            }

            // Reasons Card
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Signals detected on phone",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )

                    state.reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = AppIcons.Warning,
                                contentDescription = null,
                                tint = PauseOrange,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reason,
                                fontSize = 16.sp,
                                color = InkDark,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            // Disclaimer card - MUST SHOW: "Rohit can't see the account or stop the payment."
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDE2EA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Info,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.family_alert_disclaimer),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MutedText,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Call button
        DemoPrimaryButton(
            text = stringResource(R.string.family_alert_call_now, state.fromName),
            onClick = onCallNow,
            containerColor = PauseOrange
        )

        Spacer(modifier = Modifier.height(8.dp))

        // I've called button
        DemoSecondaryButton(
            text = stringResource(R.string.family_alert_called),
            onClick = onCalled
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FamilyAlertScreenPreview() {
    ScriptBreakTheme {
        FamilyAlertScreen(
            state = FamilyAlertUi(
                fromName = "Papa",
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
