package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.components.UserAvatar
import com.example.scriptbreak.ui.model.PayeeUi
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.SafeGreenTint
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

@Composable
fun PayeeScreen(
    state: PayeeUi,
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

        Spacer(modifier = Modifier.height(28.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val initials = state.name.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .ifEmpty { "RK" }

            UserAvatar(
                initials = initials,
                size = 78.dp,
                backgroundColor = Color(0xFF6B74D6)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = state.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = state.upiId,
                fontSize = 16.sp,
                color = MutedText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // History line pill badge
            Surface(
                color = SafeGreenTint,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Check,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.historyLine,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SafeGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Verified banking name card
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(SafeGreenTint)
                    ) {
                        Icon(
                            imageVector = AppIcons.VerifiedUser,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.verified_banking_name),
                            fontSize = 16.sp,
                            color = MutedText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.bankingName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )
                    }
                }
            }
        }

        DemoPrimaryButton(
            text = stringResource(R.string.pay),
            onClick = onPay,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PayeeScreenPreview() {
    ScriptBreakTheme {
        PayeeScreen(
            state = PayeeUi(
                name = "Ramesh K.",
                upiId = "ramesh.k@demoupi",
                bankingName = "RAMESH KUMAR",
                historyLine = "New payee – never paid"
            ),
            onPay = {},
            onBack = {}
        )
    }
}
