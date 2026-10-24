package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
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
import com.example.scriptbreak.ui.components.DemoPayLogoText
import com.example.scriptbreak.ui.components.UserAvatar
import com.example.scriptbreak.ui.model.HomeUi
import com.example.scriptbreak.ui.model.PersonUi
import com.example.scriptbreak.ui.model.TxnUi
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppBlue
import com.example.scriptbreak.ui.theme.AppBlueLight
import com.example.scriptbreak.ui.theme.AppOrange
import com.example.scriptbreak.ui.theme.AppOrangeLight
import com.example.scriptbreak.ui.theme.CritRed
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.SafeGreenTint
import com.example.scriptbreak.ui.theme.AppSurface
import com.example.scriptbreak.ui.theme.AppBlueElectric
import com.example.scriptbreak.ui.theme.LocalDarkTheme
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

@Composable
fun HomeScreen(
    state: HomeUi,
    onSendContact: () -> Unit = {},
    onPayUpiId: () -> Unit,
    onPerson: (id: String) -> Unit,
    onViewAllPeople: () -> Unit = {},
    onHistory: () -> Unit,
    onBankSelf: () -> Unit = {},
    onBalance: () -> Unit,
    onNotifications: () -> Unit = {},
    onAccount: () -> Unit = {},
    // Settings (trusted contact). Optional so older callers still compile.
    onSettings: (() -> Unit)? = null,
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
                DemoPayLogoText(fontSize = 24.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.Security,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(14.dp)
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

            // Bell icon with brand orange notification badge dot
            Box {
                IconButton(onClick = onNotifications) {
                    Icon(
                        imageVector = AppIcons.Notifications,
                        contentDescription = "Notifications",
                        tint = InkDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AppOrange)
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            val userInitials = state.userName.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .ifEmpty { "MS" }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onAccount() }
            ) {
                UserAvatar(
                    initials = userInitials,
                    size = 40.dp,
                    backgroundColor = Color(0xFFE88A3C)
                )
            }
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
            // Screen-sharing banner: shown together with the call banner when both are happening
            if (state.screenShared) {
                item {
                    CallStatusBanner(
                        title = "Screen sharing detected",
                        subtitle = "Never share your screen while paying",
                        icon = AppIcons.ScreenShare,
                    )
                }
            }

            // Search bar with QR scan icon button
            item {
                Surface(
                    color = AppSurface,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onPayUpiId() }
                        ) {
                            Icon(
                                imageVector = AppIcons.Search,
                                contentDescription = "Search",
                                tint = MutedText,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.search_hint),
                                fontSize = 15.sp,
                                color = MutedText
                            )
                        }

                        // Prominent QR code scanner icon button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AppBlueLight)
                                .clickable { onPayUpiId() }
                        ) {
                            Icon(
                                imageVector = AppIcons.QrCode,
                                contentDescription = "Scan QR",
                                tint = AppBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Quick Actions 4x1 inside an elevated action dock
            item {
                Surface(
                    color = AppSurface,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickActionTile(
                            icon = AppIcons.Person,
                            label = stringResource(R.string.action_send),
                            tint = AppBlue,
                            background = AppBlueLight,
                            onClick = onSendContact
                        )
                        QuickActionTile(
                            icon = AppIcons.AlternateEmail,
                            label = stringResource(R.string.action_upi_id),
                            tint = AppOrange,
                            background = AppOrangeLight,
                            onClick = onPayUpiId
                        )
                        QuickActionTile(
                            icon = AppIcons.AccountBalance,
                            label = stringResource(R.string.action_bank),
                            tint = AppBlue,
                            background = AppBlueLight,
                            onClick = onBankSelf
                        )
                        QuickActionTile(
                            icon = AppIcons.Wallet,
                            label = stringResource(R.string.action_balance),
                            tint = SafeGreen,
                            background = SafeGreenTint,
                            onClick = onBalance
                        )
                    }
                }
            }

            // Recent people horizontal section
            item {
                Surface(
                    color = AppSurface,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppBlue,
                                modifier = Modifier.clickable { onViewAllPeople() }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(state.people) { person ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(72.dp)
                                        .clickable { onPerson(person.id) }
                                ) {
                                    val bg = try {
                                        Color(android.graphics.Color.parseColor(person.colorHex))
                                    } catch (_: Exception) {
                                        AppBlue
                                    }
                                    UserAvatar(
                                        initials = person.initials,
                                        size = 54.dp,
                                        backgroundColor = bg
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = person.name.split(" ").firstOrNull() ?: person.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = InkDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Trailing chevron circular button to View All People
                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(54.dp)
                                        .clickable { onViewAllPeople() }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(AppBlueLight)
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.ChevronRight,
                                            contentDescription = "View all",
                                            tint = AppBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "",
                                        fontSize = 14.sp
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
                    color = AppSurface,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.recent_payments),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        state.recent.forEach { txn ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onHistory() }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                val txnInitials = txn.name.split(" ")
                                    .mapNotNull { it.firstOrNull()?.toString() }
                                    .take(2)
                                    .joinToString("")
                                    .ifEmpty { "TX" }

                                Box {
                                    UserAvatar(
                                        initials = txnInitials,
                                        size = 44.dp,
                                        backgroundColor = AppBlueLight,
                                        textColor = AppBlue
                                    )
                                    // Paid outgoing indicator badge
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(AppOrangeLight)
                                            .align(Alignment.BottomEnd)
                                    ) {
                                        Text("↑", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppOrange)
                                    }
                                }

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
                                        text = "${txn.time} · Paid via UPI",
                                        fontSize = 13.sp,
                                        color = MutedText
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = txn.amount,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = InkDark
                                    )
                                    Text(
                                        text = "Completed",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SafeGreen
                                    )
                                }
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
            containerColor = AppSurface,
            tonalElevation = 4.dp
        ) {
            NavigationBarItem(
                selected = true,
                onClick = {},
                icon = { Icon(AppIcons.Home, contentDescription = "Home") },
                label = { Text(stringResource(R.string.nav_home), fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (LocalDarkTheme.current) AppBlueElectric else AppBlue,
                    selectedTextColor = if (LocalDarkTheme.current) AppBlueElectric else AppBlue,
                    indicatorColor = if (LocalDarkTheme.current) Color(0xFF1E2E46) else AppBlueLight,
                    unselectedIconColor = MutedText,
                    unselectedTextColor = MutedText
                )
            )
            NavigationBarItem(
                selected = false,
                onClick = onHistory,
                icon = { Icon(AppIcons.History, contentDescription = "History") },
                label = { Text(stringResource(R.string.nav_history), fontSize = 14.sp) }
            )
            NavigationBarItem(
                selected = false,
                onClick = onSettings ?: onHistory,
                icon = { Icon(AppIcons.Settings, contentDescription = "Settings") },
                label = { Text(stringResource(R.string.nav_settings), fontSize = 14.sp) }
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = AppBlue,
    background: Color = AppBlueLight,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(background)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkDark,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            maxLines = 2
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
            onSendContact = {},
            onPayUpiId = {},
            onPerson = {},
            onViewAllPeople = {},
            onHistory = {},
            onBankSelf = {},
            onBalance = {}
        )
    }
}
