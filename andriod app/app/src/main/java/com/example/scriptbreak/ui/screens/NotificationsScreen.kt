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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppBlue
import com.example.scriptbreak.ui.theme.AppBlueLight
import com.example.scriptbreak.ui.theme.AppOrange
import com.example.scriptbreak.ui.theme.AppOrangeLight
import com.example.scriptbreak.ui.theme.AppSurface
import com.example.scriptbreak.ui.theme.CritRed
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.NudgeAmber
import com.example.scriptbreak.ui.theme.NudgeAmberTint
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.SafeGreenTint

data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val time: String,
    val category: String, // "Security" or "Transactions"
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    var unread: Boolean = false
)

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blue = AppBlue
    val orange = AppOrange
    val green = SafeGreen
    val blueLight = AppBlueLight
    val orangeLight = AppOrangeLight
    val greenTint = SafeGreenTint

    val notifications = remember(blue, orange, green, blueLight, orangeLight, greenTint) {
        mutableStateListOf(
            NotificationItem(
                id = "1",
                title = "ScriptBreak Shield Active",
                description = "Real-time AI fraud detection is actively monitoring for suspicious calls and screen-sharing apps.",
                time = "Just now",
                category = "Security",
                icon = AppIcons.Security,
                iconTint = green,
                iconBg = greenTint,
                unread = true
            ),
            NotificationItem(
                id = "2",
                title = "Payment Verified Safe",
                description = "Your recent transfer to Sunita Iyer passed all safety checks with 0 risk indicators.",
                time = "Today 10:42 AM",
                category = "Transactions",
                icon = AppIcons.Check,
                iconTint = green,
                iconBg = greenTint,
                unread = true
            ),
            NotificationItem(
                id = "3",
                title = "Trusted Contact Protection",
                description = "Sehjal Saxena is active as your trusted contact. High-risk payments trigger an alert.",
                time = "2 hours ago",
                category = "Security",
                icon = AppIcons.Person,
                iconTint = blue,
                iconBg = blueLight,
                unread = false
            ),
            NotificationItem(
                id = "4",
                title = "UPI PIN Security Advice",
                description = "DemoPay and your bank will never ask for your UPI PIN over call or message to receive funds.",
                time = "Yesterday",
                category = "Security",
                icon = AppIcons.Lock,
                iconTint = orange,
                iconBg = orangeLight,
                unread = false
            ),
            NotificationItem(
                id = "5",
                title = "Primary Account Ready",
                description = "Demo Bank savings account (•••• 4821) is verified and linked for instant payments.",
                time = "2 days ago",
                category = "Transactions",
                icon = AppIcons.AccountBalance,
                iconTint = blue,
                iconBg = blueLight,
                unread = false
            )
        )
    }

    var selectedFilter by remember { mutableStateOf("All") }
    val filteredList = remember(selectedFilter, notifications.toList()) {
        if (selectedFilter == "All") notifications
        else notifications.filter { it.category == selectedFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = "Notifications",
            onBack = onBack,
            showScriptBreakBadge = true,
            trailingContent = {
                if (notifications.isNotEmpty()) {
                    Text(
                        text = "Clear all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppOrange,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { notifications.clear() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("All", "Security", "Transactions").forEach { filter ->
                val selected = selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = filter,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppBlue,
                        selectedLabelColor = Color.White,
                        containerColor = AppSurface,
                        labelColor = InkDark
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (selected) AppBlue else Color(0xFFDDE2EA),
                        selectedBorderColor = AppBlue,
                        enabled = true,
                        selected = selected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(AppBlueLight)
                    ) {
                        Icon(
                            imageVector = AppIcons.Notifications,
                            contentDescription = null,
                            tint = AppBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "You're all caught up!",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No new notifications right now",
                        fontSize = 14.sp,
                        color = MutedText
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    Surface(
                        color = AppSurface,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { item.unread = false }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(item.iconBg)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = item.iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = item.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (item.unread) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(AppOrange)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = item.description,
                                    fontSize = 13.sp,
                                    color = MutedText,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = item.time,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
