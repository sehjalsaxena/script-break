package com.example.scriptbreak.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.scriptbreak.ui.theme.CritRed
import com.example.scriptbreak.ui.theme.PauseOrange
import com.example.scriptbreak.ui.theme.SafeGreen
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
import com.example.scriptbreak.ui.components.NumericKeypad
import com.example.scriptbreak.ui.components.UserAvatar
import com.example.scriptbreak.ui.model.AmountUi
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppPurple
import com.example.scriptbreak.ui.theme.AppBlue
import com.example.scriptbreak.ui.theme.AppOrange
import com.example.scriptbreak.ui.theme.AppSurface
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.LineBorder
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.NudgeAmber
import com.example.scriptbreak.ui.theme.NudgeAmberBar
import com.example.scriptbreak.ui.theme.NudgeAmberInk
import com.example.scriptbreak.ui.theme.NudgeAmberTint
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

@Composable
fun AmountScreen(
    state: AmountUi,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onNoteChange: (String) -> Unit,
    onPay: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            onBack = onBack,
            showScriptBreakBadge = true
        )

        // Everything above the keypad scrolls, so the keypad and Continue never get pushed off screen
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
        // Payee header preview (Avatar + Name + UPI ID)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            val initials = state.payeeName.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .ifEmpty { "RK" }

            UserAvatar(
                initials = initials,
                size = 46.dp,
                backgroundColor = Color(0xFF6B74D6)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "To " + state.payeeName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark
                )
                Text(
                    text = state.payeeUpiId,
                    fontSize = 14.sp,
                    color = MutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Amount display with vertical blinking cursor
        val infiniteTransition = rememberInfiniteTransition(label = "amountCursor")
        val cursorAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    1f at 0
                    1f at 499
                    0f at 500
                    0f at 999
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "cursorAlpha"
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "₹",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark
            )
            Spacer(modifier = Modifier.width(6.dp))
            if (state.amountText.isNotEmpty()) {
                Text(
                    text = state.amountText,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            // Vertical blinking cursor line like other UPI apps
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .graphicsLayer { alpha = cursorAlpha }
                    .background(AppOrange)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nudge line (amber inline, no extra tap)
        if (!state.nudge.isNullOrBlank()) {
            Surface(
                color = NudgeAmberTint,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NudgeAmberBar),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Warning,
                        contentDescription = null,
                        tint = NudgeAmber,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = state.nudge,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = NudgeAmberInk,
                        lineHeight = 21.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.aiLevel != null) {
            SafetyCheckCard(state.aiLevel, state.aiScore, state.aiFacts, state.aiReminderOnly)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add note field
        OutlinedTextField(
            value = state.note,
            onValueChange = onNoteChange,
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(R.string.add_note_optional),
                    fontSize = 16.sp,
                    color = MutedText
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = InkDark,
                unfocusedTextColor = InkDark,
                focusedBorderColor = AppBlue,
                unfocusedBorderColor = LineBorder,
                cursorColor = AppBlue,
                focusedContainerColor = AppSurface,
                unfocusedContainerColor = AppSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Keypad
        NumericKeypad(
            onDigit = onDigit,
            onDelete = onDelete
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Pay Button
        DemoPrimaryButton(
            text = stringResource(R.string.continue_btn),
            onClick = onPay,
            enabled = state.payEnabled,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}

/** "Safety check": what the AI noticed, in plain words, updated on every key press. Tap to see why. */
@Composable
private fun SafetyCheckCard(level: String, score: Double?, facts: List<String>, reminderOnly: Boolean) {
    var open by remember { mutableStateOf(false) }
    val (color, label) = when (level) {
        "CRITICAL" -> CritRed to "Looks like a scam"
        "PAUSE" -> PauseOrange to "Needs a quick check"
        "NUDGE" -> NudgeAmber to "Check before you pay"
        "NONE" -> SafeGreen to "Looks normal"
        else -> MutedText to "Check not available"
    }
    Surface(
        color = AppSurface,   // follows light / dark mode
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
        modifier = Modifier.fillMaxWidth().clickable { open = !open }
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Safety check", fontSize = 13.sp, color = MutedText)
                    Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = color)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (open) "Hide" else "Why?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppPurple)
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = if (open) AppIcons.ArrowDropUp else AppIcons.ArrowDropDown,
                        contentDescription = null,
                        tint = AppPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (open) {
                Spacer(modifier = Modifier.height(8.dp))
                facts.forEach { line ->
                    val isCheck = line.startsWith("✓")
                    val isWarn = line.startsWith("⚠")
                    val cleanText = line.removePrefix("✓").removePrefix("⚠").trim()
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (isCheck) AppIcons.Check else if (isWarn) AppIcons.Warning else AppIcons.Info,
                            contentDescription = null,
                            tint = if (isCheck) SafeGreen else if (isWarn) NudgeAmber else MutedText,
                            modifier = Modifier
                                .size(15.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = cleanText,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = if (isCheck) SafeGreen else InkDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                score?.let {
                    Text("AI risk score: ${Math.round(it * 100)}%", fontSize = 13.sp, color = MutedText,
                        modifier = Modifier.padding(top = 6.dp))
                }
                if (reminderOnly) Text("No sign that anyone is guiding you, so this is only a reminder.",
                    fontSize = 13.sp, color = MutedText, lineHeight = 18.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AmountScreenPreview() {
    ScriptBreakTheme {
        AmountScreen(
            state = AmountUi(
                payeeName = "Ramesh K.",
                amountText = "48000",
                note = "Verification deposit",
                nudge = "First payment to Ramesh K. ₹48,000 is 10× your usual payment. Check you know who this is.",
                payEnabled = true
            ),
            onDigit = {},
            onDelete = {},
            onNoteChange = {},
            onPay = {},
            onBack = {}
        )
    }
}
