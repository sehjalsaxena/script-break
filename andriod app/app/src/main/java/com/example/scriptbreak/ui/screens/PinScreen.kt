package com.example.scriptbreak.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.components.NumericKeypad
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppBlue
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.LineBorder
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.scriptbreak.ui.components.isShortScreen

@Composable
fun PinScreen(
    amountText: String,
    payeeName: String,
    pinDigits: Int,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Short phones get smaller keys; the text above the keypad scrolls, so Submit is never pushed off screen
        val short = isShortScreen(maxHeight)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = if (short) 8.dp else 16.dp)
                .navigationBarsPadding()
        ) {
            AppTopBar(
                onBack = onBack,
                showScriptBreakBadge = true
            )

            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (short) 4.dp else 16.dp)
                    ) {
                        val amountFormatted = if (amountText.startsWith("₹")) amountText else "₹$amountText"
                        Text(
                            text = stringResource(R.string.sending_amount_to, amountFormatted, payeeName),
                            fontSize = 16.sp,
                            color = MutedText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(if (short) 8.dp else 12.dp))

                        Text(
                            text = stringResource(R.string.enter_upi_pin),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )

                        Spacer(modifier = Modifier.height(if (short) 14.dp else 20.dp))

                        // 4 Pin dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(22.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(4) { index ->
                                val isFilled = index < pinDigits
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isFilled) AppBlue else Color.Transparent)
                                        .border(
                                            width = 2.dp,
                                            color = if (isFilled) AppBlue else LineBorder,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.pin_security_note),
                        fontSize = if (short) 14.sp else 16.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center,
                        lineHeight = if (short) 19.sp else 22.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (short) 6.dp else 8.dp))

            NumericKeypad(
                onDigit = onDigit,
                onDelete = onDelete,
                keyHeight = if (short) 44.dp else 56.dp,
                spacing = if (short) 6.dp else 8.dp
            )

            Spacer(modifier = Modifier.height(if (short) 8.dp else 12.dp))

            DemoPrimaryButton(
                text = "Submit",
                onClick = onSubmit,
                enabled = pinDigits >= 4
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PinScreenPreview() {
    ScriptBreakTheme {
        PinScreen(
            amountText = "₹48,000",
            payeeName = "Ramesh K.",
            pinDigits = 3,
            onDigit = {},
            onDelete = {},
            onSubmit = {},
            onBack = {}
        )
    }
}
