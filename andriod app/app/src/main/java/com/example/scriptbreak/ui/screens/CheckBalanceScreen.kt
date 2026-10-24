package com.example.scriptbreak.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppPurple
import com.example.scriptbreak.ui.theme.AppPurpleLight
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.SafeGreenTint
import com.example.scriptbreak.ui.theme.ScriptBreakTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CheckBalanceScreen(
    userProfileName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var sbiLoading by remember { mutableStateOf(false) }
    var sbiBalance by remember { mutableStateOf<String?>(null) }

    var hdfcLoading by remember { mutableStateOf(false) }
    var hdfcBalance by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = stringResource(R.string.action_balance),
            onBack = onBack,
            showScriptBreakBadge = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Security Info Banner
        Surface(
            color = SafeGreenTint,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Security,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Bank balance is retrieved securely through official NPCI UPI network",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SafeGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Linked Bank Accounts",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MutedText
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Account 1: State Bank of India
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 0.5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(AppPurpleLight)
                    ) {
                        Icon(
                            imageVector = AppIcons.AccountBalance,
                            contentDescription = null,
                            tint = AppPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "State Bank of India",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )
                        Text(
                            text = "Savings A/c •••• 4821 (Primary)",
                            fontSize = 13.sp,
                            color = MutedText
                        )
                    }

                    if (sbiBalance == null) {
                        if (sbiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AppPurple,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Surface(
                                color = AppPurpleLight,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable {
                                    sbiLoading = true
                                    scope.launch {
                                        delay(800)
                                        sbiBalance = "₹ 48,250.00"
                                        sbiLoading = false
                                    }
                                }
                            ) {
                                Text(
                                    text = "Check",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppPurple,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = sbiBalance != null,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF0F0F0))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Available Balance",
                                    fontSize = 13.sp,
                                    color = MutedText
                                )
                                Text(
                                    text = sbiBalance.orEmpty(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InkDark
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = AppIcons.Check,
                                    contentDescription = null,
                                    tint = SafeGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Updated just now",
                                    fontSize = 12.sp,
                                    color = SafeGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Account 2: HDFC Bank
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 0.5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(AppPurpleLight)
                    ) {
                        Icon(
                            imageVector = AppIcons.AccountBalance,
                            contentDescription = null,
                            tint = AppPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HDFC Bank",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )
                        Text(
                            text = "Savings A/c •••• 7319 (Secondary)",
                            fontSize = 13.sp,
                            color = MutedText
                        )
                    }

                    if (hdfcBalance == null) {
                        if (hdfcLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AppPurple,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Surface(
                                color = AppPurpleLight,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable {
                                    hdfcLoading = true
                                    scope.launch {
                                        delay(800)
                                        hdfcBalance = "₹ 12,400.00"
                                        hdfcLoading = false
                                    }
                                }
                            ) {
                                Text(
                                    text = "Check",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppPurple,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = hdfcBalance != null,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF0F0F0))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Available Balance",
                                    fontSize = 13.sp,
                                    color = MutedText
                                )
                                Text(
                                    text = hdfcBalance.orEmpty(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InkDark
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = AppIcons.Check,
                                    contentDescription = null,
                                    tint = SafeGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Updated just now",
                                    fontSize = 12.sp,
                                    color = SafeGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        DemoPrimaryButton(
            text = "Done",
            onClick = onBack
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CheckBalanceScreenPreview() {
    ScriptBreakTheme {
        CheckBalanceScreen(userProfileName = "Mr. Sharma", onBack = {})
    }
}
