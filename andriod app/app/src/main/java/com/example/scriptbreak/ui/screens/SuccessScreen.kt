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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.model.SuccessUi
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.CritRed
import com.example.scriptbreak.ui.theme.CritRedTint
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.ScriptBreakTheme
import com.example.scriptbreak.ui.theme.SuccessGreen

@Composable
fun SuccessScreen(
    state: SuccessUi,
    onDone: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Success Check Icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(SuccessGreen)
        ) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        val amountFormatted = if (state.amountText.startsWith("₹")) state.amountText else "₹${state.amountText}"
        Text(
            text = amountFormatted,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = InkDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.paid_successfully_to, state.payeeName),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Transaction Details Card
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow(label = "UPI Ref ID", value = state.upiRef)
                DetailRow(label = "Time", value = state.time)
                DetailRow(label = "From Account", value = "Demo Bank (•••• 4321)")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Optional Report line
        if (state.showReportLine) {
            Surface(
                color = CritRedTint,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.Warning,
                            contentDescription = null,
                            tint = CritRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.report_fraud_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CritRed
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.report_fraud_desc),
                        fontSize = 16.sp,
                        color = InkDark,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onReport,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.report_payment_btn),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CritRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        DemoPrimaryButton(
            text = stringResource(R.string.done),
            onClick = onDone
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = MutedText
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkDark
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SuccessScreenPreview() {
    ScriptBreakTheme {
        SuccessScreen(
            state = SuccessUi(
                amountText = "₹48,000",
                payeeName = "Ramesh K.",
                upiRef = "429810482910",
                time = "Today, 10:45 AM",
                showReportLine = true
            ),
            onDone = {},
            onReport = {}
        )
    }
}
