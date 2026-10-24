package com.example.scriptbreak.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.scriptbreak.ui.components.UserAvatar
import com.example.scriptbreak.ui.model.TxnUi
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppPurple
import com.example.scriptbreak.ui.theme.AppPurpleLight
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.SafeGreenTint
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

@Composable
fun HistoryScreen(
    transactions: List<TxnUi>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTxn by remember { mutableStateOf<TxnUi?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = stringResource(R.string.recent_payments),
            onBack = onBack,
            showScriptBreakBadge = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Protection summary card
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
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Real-time Scam Protection Active",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeGreen
                    )
                    Text(
                        text = "All outgoing payments are evaluated on-device before PIN",
                        fontSize = 13.sp,
                        color = SafeGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Transactions (${transactions.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MutedText
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recent transactions found",
                    fontSize = 15.sp,
                    color = MutedText
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transactions, key = { it.id }) { txn ->
                    val txnInitials = txn.name.split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .ifEmpty { "TX" }

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedTxn = txn }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            UserAvatar(
                                initials = txnInitials,
                                size = 44.dp,
                                backgroundColor = AppPurpleLight,
                                textColor = AppPurple
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = txn.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = InkDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = txn.time,
                                    fontSize = 13.sp,
                                    color = MutedText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = AppIcons.Check,
                                        contentDescription = null,
                                        tint = SafeGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Paid successfully",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SafeGreen
                                    )
                                }
                            }

                            Text(
                                text = txn.amount,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    ScriptBreakTheme {
        HistoryScreen(
            transactions = listOf(
                TxnUi("1", "Sunita Iyer", "Today 10:42 AM", "₹450"),
                TxnUi("2", "Sharma Kirana Store", "Yesterday, 6:12 PM", "₹1,299"),
                TxnUi("3", "Rohit (son)", "14 Oct, 2:00 PM", "₹3,000")
            ),
            onBack = {}
        )
    }
}
