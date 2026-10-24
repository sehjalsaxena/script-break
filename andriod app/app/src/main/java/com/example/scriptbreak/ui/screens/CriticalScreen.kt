package com.example.scriptbreak.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.model.CriticalUi
import com.example.scriptbreak.ui.theme.CritRed
import com.example.scriptbreak.ui.theme.CritRedTint
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

@Composable
fun CriticalScreen(
    state: CriticalUi,
    onWait: () -> Unit,
    onAskFamily: () -> Unit,
    onContinue: () -> Unit,
    onToggleLanguage: () -> Unit,
    onReadAloud: () -> Unit,
    modifier: Modifier = Modifier
) {
    val h = state.hindi

    // Exact copy strings from SPEC.md Section 12
    val titleText = if (h) "रुकिए। यह एक आम धोखाधड़ी जैसा है।" else "Stop. This matches a common scam."
    val calmText = if (h) "आप किसी मुसीबत में नहीं हैं। इस भुगतान का फ़ैसला आपके हाथ में है।" else "You are not in trouble. You are in control of this payment."
    val neverText = if (h) "पुलिस, CBI, RBI और बैंक कभी भी पैसे की जाँच, सुरक्षा या उसे छुड़ाने के लिए पैसे भेजने को नहीं कहते।" else "Police, CBI, RBI and banks never ask you to move money to verify, protect or release it."
    val toPayeeText = if (h) {
        if (state.isNewPayee) "${state.amountText}, ${state.payeeName} को (नया प्राप्तकर्ता)" else "${state.amountText}, ${state.payeeName} को"
    } else {
        if (state.isNewPayee) "${state.amountText} to ${state.payeeName}, a new payee" else "${state.amountText} to ${state.payeeName}"
    }
    val lastLineText = if (h) "अगर कोई आपसे इस स्क्रीन को अनदेखा करने को कह रहा है, तो वही धोखाधड़ी है।" else "If someone is telling you to ignore this screen, that is the scam."
    val waitBtnText = if (h) "30 मिनट रुकें" else "Wait 30 minutes"
    val askFamilyBtnText = if (state.askedFamily) {
        if (h) "${state.contactName} से कॉल करने को कहा गया है" else "${state.contactName} has been asked to call you"
    } else {
        if (h) "${state.contactName} से कहें कि मुझे कॉल करें" else "Ask ${state.contactName} to call me"
    }
    val continueBtnText = if (state.continueSecondsLeft > 0) {
        if (h) "फिर भी जारी रखें (${state.continueSecondsLeft} सेकंड)" else "Continue anyway (${state.continueSecondsLeft} s)"
    } else {
        if (h) "फिर भी जारी रखें" else "Continue anyway"
    }
    val footerReportText = if (h) "भुगतान कर चुके हैं? अभी रिपोर्ट करें: 1930 पर कॉल करें" else "Already paid? Report now: call 1930"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Full width Red Header (CritRed)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CritRed)
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = titleText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 28.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Language toggle pill
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { onToggleLanguage() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "EN",
                                fontSize = 12.sp,
                                fontWeight = if (!h) FontWeight.Bold else FontWeight.Normal,
                                color = if (!h) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = " | ",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "हिंदी",
                                fontSize = 12.sp,
                                fontWeight = if (h) FontWeight.Bold else FontWeight.Normal,
                                color = if (h) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Read aloud speaker button
                    IconButton(
                        onClick = onReadAloud,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.VolumeUp,
                            contentDescription = "Read aloud",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = calmText,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    lineHeight = 22.sp
                )
            }
        }

        // Body content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Police / CBI reassurance line
            Text(
                text = neverText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = InkDark,
                lineHeight = 22.sp
            )

            // Amount & reasons box
            Surface(
                color = CritRedTint,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CritRed.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = toPayeeText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )

                    if (state.reasons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        state.reasons.forEach { reason ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InkDark
                                )
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
            }

            // LAST LINE of the message (anti-coaching callout)
            Surface(
                color = InkDark,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = lastLineText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }

            // Buttons
            // 1. Wait 30 minutes
            DemoPrimaryButton(
                text = waitBtnText,
                onClick = onWait,
                containerColor = SafeGreen
            )

            // 2. Ask Family
            OutlinedButton(
                onClick = onAskFamily,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = if (state.askedFamily) "✓ $askFamilyBtnText" else askFamilyBtnText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.askedFamily) SafeGreen else InkDark
                )
            }

            // 3. Continue anyway (countdown delay)
            Button(
                onClick = onContinue,
                enabled = state.continueSecondsLeft <= 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MutedText,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MutedText.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = continueBtnText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Footer: Already paid? Report now: call 1930
        Text(
            text = footerReportText,
            fontSize = 14.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CriticalScreenPreview() {
    ScriptBreakTheme {
        CriticalScreen(
            state = CriticalUi(
                amountText = "₹20,000",
                payeeName = "Ramesh K.",
                isNewPayee = true,
                reasons = listOf(
                    "Active phone call during this payment",
                    "New payee – never paid before",
                    "Amount is 10× higher than usual"
                ),
                contactName = "Rohit",
                askedFamily = false,
                continueSecondsLeft = 10,
                hindi = false
            ),
            onWait = {},
            onAskFamily = {},
            onContinue = {},
            onToggleLanguage = {},
            onReadAloud = {}
        )
    }
}
