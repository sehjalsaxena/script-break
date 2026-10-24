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
import androidx.compose.runtime.getValue
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
import com.example.scriptbreak.ui.theme.InkDark
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
    ) {
        AppTopBar(
            onBack = onBack,
            showScriptBreakBadge = true
        )

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
                    text = "ramesh.k@demoupi",
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
                    .background(AppPurple)
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
                    modifier = Modifier.padding(14.dp)
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = NudgeAmberInk,
                        lineHeight = 22.sp
                    )
                }
            }
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
                focusedBorderColor = AppPurple,
                unfocusedBorderColor = Color(0xFFDDE2EA),
                cursorColor = AppPurple,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

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
