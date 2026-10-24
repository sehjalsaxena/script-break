package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.R
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.CallStatusBanner
import com.example.scriptbreak.ui.components.UserAvatar
import com.example.scriptbreak.ui.model.HomeUi
import com.example.scriptbreak.ui.model.PersonUi
import com.example.scriptbreak.ui.model.TxnUi
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppPurple
import com.example.scriptbreak.ui.theme.AppPurpleLight
import com.example.scriptbreak.ui.theme.BodyDark
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

@Composable
fun HomeScreen(
    state: HomeUi,
    onPayUpiId: () -> Unit,
    onPerson: (id: String) -> Unit,
    onHistory: () -> Unit,
    onBalance: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.Security,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        fontSize = 13.sp,
                        color = SafeGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(onClick = onHistory) {
                Icon(
                    imageVector = AppIcons.Notifications,
                    contentDescription = "Notifications",
                    tint = InkDark
                )
            }

            val userInitials = state.userName.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .ifEmpty { "MS" }

            UserAvatar(
                initials = userInitials,
                size = 38.dp,
                backgroundColor = Color(0xFFE88A3C)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Call Status Banner if onCall
            if (state.onCall) {
                item {
                    CallStatusBanner()
                }
            }

            // Search bar
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPayUpiId() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Search,
                            contentDescription = "Search",
                            tint = MutedText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.search_hint),
                            fontSize = 14.sp,
                            color = MutedText
                        )
                    }
                }
            }

            // Quick Actions 4x1
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionTile(
                        icon = AppIcons.Person,
                        label = stringResource(R.string.action_send),
                        onClick = onPayUpiId
                    )
                    QuickActionTile(
                        icon = AppIcons.AlternateEmail,
                        label = stringResource(R.string.action_upi_id),
                        onClick = onPayUpiId
                    )
                    QuickActionTile(
                        icon = AppIcons.AccountBalance,
                        label = stringResource(R.string.action_bank),
                        onClick = onBalance
                    )
                    QuickActionTile(
                        icon = AppIcons.Wallet,
                        label = stringResource(R.string.action_balance),
                        onClick = onBalance
                    )
                }
            }

            // Recent people horizontal section
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.recent_people),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkDark,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.view_all),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppPurple,
                                modifier = Modifier.clickable { onPayUpiId() }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(state.people) { person ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(68.dp)
                                        .clickable { onPerson(person.id) }
                                ) {
                                    val bg = try {
                                        Color(android.graphics.Color.parseColor(person.colorHex))
                                    } catch (_: Exception) {
                                        AppPurple
                                    }
                                    UserAvatar(
                                        initials = person.initials,
                                        size = 52.dp,
                                        backgroundColor = bg
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = person.name.split(" ").firstOrNull() ?: person.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = InkDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Payments Section
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.recent_payments),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        state.recent.forEach { txn ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { onHistory() }
                            ) {
                                val txnInitials = txn.name.split(" ")
                                    .mapNotNull { it.firstOrNull()?.toString() }
                                    .take(2)
                                    .joinToString("")
                                    .ifEmpty { "TX" }

                                UserAvatar(
                                    initials = txnInitials,
                                    size = 42.dp,
                                    backgroundColor = AppPurpleLight,
                                    textColor = AppPurple
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = txn.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkDark
                                    )
                                    Text(
                                        text = txn.time,
                                        fontSize = 13.sp,
                                        color = MutedText
                                    )
                                }

                                Text(
                                    text = txn.amount,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InkDark
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Bottom Navigation Bar
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 4.dp
        ) {
            NavigationBarItem(
                selected = true,
                onClick = {},
                icon = { Icon(AppIcons.Home, contentDescription = "Home") },
                label = { Text(stringResource(R.string.nav_home), fontSize = 12.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppPurple,
                    selectedTextColor = AppPurple,
                    indicatorColor = AppPurpleLight
                )
            )
            NavigationBarItem(
                selected = false,
                onClick = onHistory,
                icon = { Icon(AppIcons.History, contentDescription = "History") },
                label = { Text(stringResource(R.string.nav_history), fontSize = 12.sp) }
            )
            NavigationBarItem(
                selected = false,
                onClick = onHistory,
                icon = { Icon(AppIcons.Settings, contentDescription = "Settings") },
                label = { Text(stringResource(R.string.nav_settings), fontSize = 12.sp) }
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(76.dp)
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(AppPurpleLight)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AppPurple,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = InkDark,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ScriptBreakTheme {
        HomeScreen(
            state = HomeUi(
                userName = "Mr. Sharma",
                onCall = true,
                people = listOf(
                    PersonUi("ramesh", "Ramesh K.", "RK", "ramesh.k@demoupi", "#6B74D6"),
                    PersonUi("anil", "Anil Mehta", "AM", "anil.m@demoupi", "#B07A3A"),
                    PersonUi("sunita", "Sunita Iyer", "SI", "sunita.i@demoupi", "#D0679A")
                ),
                recent = listOf(
                    TxnUi("1", "Sunita Iyer", "Today 10:42 AM", "₹450"),
                    TxnUi("2", "Amazon", "Yesterday, 6:12 AM", "₹1,299"),
                    TxnUi("3", "Aarav Sharma", "16 Oct 2:00 PM", "₹800")
                )
            ),
            onPayUpiId = {},
            onPerson = {},
            onHistory = {},
            onBalance = {}
        )
    }
}
