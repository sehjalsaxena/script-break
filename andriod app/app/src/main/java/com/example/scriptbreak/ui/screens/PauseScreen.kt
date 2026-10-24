package com.example.scriptbreak.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.model.PauseUi
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.FaintText
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.LineBorder
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.PauseOrange
import com.example.scriptbreak.ui.theme.PauseOrangeInk
import com.example.scriptbreak.ui.theme.PauseOrangeTint
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.SafeGreenTint
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

@Composable
fun PauseScreen(
    state: PauseUi,
    onQ1: (Boolean) -> Unit,
    onQ2: (Boolean) -> Unit,
    onAskFamily: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onWait: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
    ) {
        AppTopBar(
            title = stringResource(R.string.pause_title),
            onBack = onBack,
            showScriptBreakBadge = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        val infiniteTransition = rememberInfiniteTransition(label = "breathing")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Calming Breathing / Safety Header
            Surface(
                color = PauseOrangeTint,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                            .clip(CircleShape)
                            .background(PauseOrange)
                    ) {
                        Icon(
                            imageVector = AppIcons.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Take a breath · Scam protection check",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PauseOrangeInk
                        )
                        Text(
                            text = "Scammers create false urgency to bypass your normal judgment",
                            fontSize = 12.sp,
                            color = PauseOrangeInk.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Facts card (Orange border-left & tint)
            Surface(
                color = PauseOrangeTint,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = PauseOrange.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .background(PauseOrange)
                    )
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        val payeeFormatted = if (state.payeeName.endsWith(".")) state.payeeName else "${state.payeeName}."
                        val amountFormatted = if (state.amountText.startsWith("₹")) state.amountText else "₹${state.amountText}"

                        Text(
                            text = stringResource(R.string.pause_sends, amountFormatted, payeeFormatted),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PauseOrangeInk,
                            lineHeight = 24.sp
                        )

                        if (state.reasons.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            state.reasons.forEach { reason ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "• ",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PauseOrangeInk
                                    )
                                    Text(
                                        text = reason,
                                        fontSize = 16.sp,
                                        color = PauseOrangeInk,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Question 1
            QuestionCard(
                text = stringResource(R.string.pause_q1),
                answer = state.q1,
                onSelect = onQ1
            )

            // Question 2
            QuestionCard(
                text = stringResource(R.string.pause_q2),
                answer = state.q2,
                onSelect = onQ2
            )

            // Family Emergency Contact Section with live beacon dot
            Surface(
                color = if (state.askedFamily) SafeGreenTint else Color.White,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 0.5.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.askedFamily) SafeGreen.copy(alpha = 0.3f) else PauseOrange.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onAskFamily() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (state.askedFamily) SafeGreen else PauseOrange.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (state.askedFamily) AppIcons.Check else AppIcons.Phone,
                            contentDescription = null,
                            tint = if (state.askedFamily) Color.White else PauseOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.askedFamily) "Alert sent to ${state.contactName}" else "Unsure? Call ${state.contactName}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.askedFamily) SafeGreen else PauseOrangeInk
                        )
                        Text(
                            text = if (state.askedFamily) "Live protection signal sent via ScriptBreak" else "Trusted contact can verify if this payment is safe",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }

                    // Live active beacon dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (state.askedFamily) SafeGreen else PauseOrange)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (onWait != null) {
            DemoPrimaryButton(
                text = stringResource(R.string.critical_wait),
                onClick = onWait,
                containerColor = SafeGreen
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Next Button
        DemoPrimaryButton(
            text = stringResource(R.string.next),
            onClick = onNext,
            containerColor = PauseOrange
        )

        // No separate "Continue anyway" here: "Next" already continues to the PIN
        // (or to the Critical screen if an answer raised the level).

        Text(
            text = stringResource(R.string.checked_on_phone, state.modelVersion),
            fontSize = 14.sp,
            color = FaintText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QuestionCard(
    text: String,
    answer: Boolean?,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 0.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = InkDark,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OptionButton(
                    label = stringResource(R.string.yes),
                    selected = answer == true,
                    onClick = { onSelect(true) },
                    modifier = Modifier.weight(1f)
                )

                OptionButton(
                    label = stringResource(R.string.no),
                    selected = answer == false,
                    onClick = { onSelect(false) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) InkDark else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) InkDark else LineBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else InkDark
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PauseScreenPreview() {
    ScriptBreakTheme {
        PauseScreen(
            state = PauseUi(
                amountText = "20,000",
                payeeName = "Ramesh K.",
                reasons = listOf(
                    "Active phone call during payment",
                    "New payee – never paid before",
                    "Amount is 10× higher than usual"
                ),
                q1 = null,
                q2 = null,
                contactName = "Rohit",
                askedFamily = false
            ),
            onQ1 = {},
            onQ2 = {},
            onAskFamily = {},
            onNext = {},
            onBack = {}
        )
    }
}
