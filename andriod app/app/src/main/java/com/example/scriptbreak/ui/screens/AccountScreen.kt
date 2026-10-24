package com.example.scriptbreak.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.components.DemoSecondaryButton
import com.example.scriptbreak.ui.components.UserAvatar
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppBlue
import com.example.scriptbreak.ui.theme.AppBlueLight
import com.example.scriptbreak.ui.theme.AppOrange
import com.example.scriptbreak.ui.theme.AppOrangeLight
import com.example.scriptbreak.ui.theme.AppSurface
import com.example.scriptbreak.ui.theme.CritRed
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.LineBorder
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.SafeGreenTint

@Composable
fun AccountScreen(
    userName: String,
    upiId: String,
    phoneNumber: String = "+91 98765 43210",
    trustedContactName: String = "Sehjal Saxena",
    onCheckBalance: () -> Unit,
    onTrustedContact: () -> Unit,
    onSwitchProfile: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initials = userName.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .ifEmpty { "MS" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = "My Account",
            onBack = onBack,
            showScriptBreakBadge = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Card
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            initials = initials,
                            size = 54.dp,
                            backgroundColor = AppOrange
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = phoneNumber,
                                fontSize = 14.sp,
                                color = MutedText
                            )
                        }

                        Surface(
                            color = SafeGreenTint,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Verified",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SafeGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = LineBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppBlueLight)
                            .clickable {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("UPI ID", upiId))
                                Toast.makeText(context, "UPI ID copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.AlternateEmail,
                            contentDescription = null,
                            tint = AppBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = upiId,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppBlue,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = AppIcons.ContentPaste,
                            contentDescription = "Copy",
                            tint = AppBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Quick QR Code Card: tap to show this person's own UPI QR (others scan it to pay them)
            var showQr by remember { mutableStateOf(false) }
            if (showQr) MyUpiQrDialog(userName, upiId) { showQr = false }
            Surface(
                color = AppSurface,   // follows light / dark mode
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth().clickable { showQr = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppBlueLight)
                    ) {
                        Icon(
                            imageVector = AppIcons.QrCode,
                            contentDescription = null,
                            tint = AppBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My UPI QR Code",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )
                        Text(
                            text = "Receive payments instantly to $upiId",
                            fontSize = 13.sp,
                            color = MutedText
                        )
                    }

                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Linked Bank Accounts Card
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Bank Accounts",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Check Balance",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppOrange,
                            modifier = Modifier.clickable { onCheckBalance() }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppBlueLight)
                        ) {
                            Icon(
                                imageVector = AppIcons.AccountBalance,
                                contentDescription = null,
                                tint = AppBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Demo Bank", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                            Text("Savings A/c •••• 4821 • Primary", fontSize = 12.sp, color = MutedText)
                        }
                        Text("UPI Active", fontSize = 12.sp, color = SafeGreen, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = LineBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppBlueLight)
                        ) {
                            Icon(
                                imageVector = AppIcons.AccountBalance,
                                contentDescription = null,
                                tint = AppBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sample Bank", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                            Text("Savings A/c •••• 7319 • Secondary", fontSize = 12.sp, color = MutedText)
                        }
                        Text("Linked", fontSize = 12.sp, color = MutedText, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Security & Protection
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Protection & Safety",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrustedContact() }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SafeGreenTint)
                        ) {
                            Icon(
                                imageVector = AppIcons.Security,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trusted Family Contact", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                            Text(trustedContactName, fontSize = 12.sp, color = MutedText)
                        }
                        Icon(
                            imageVector = AppIcons.ChevronRight,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = LineBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppBlueLight)
                        ) {
                            Icon(
                                imageVector = AppIcons.Lock,
                                contentDescription = null,
                                tint = AppBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("UPI Daily Limit", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                            Text("₹1,00,000 per payment (demo limit)", fontSize = 12.sp, color = MutedText)
                        }
                    }
                }
            }

            // Actions: Switch Profile & Logout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DemoSecondaryButton(
                    text = "Switch Profile",
                    onClick = onSwitchProfile,
                    modifier = Modifier.weight(1f)
                )
                DemoSecondaryButton(
                    text = "Log Out",
                    onClick = onLogout,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** This person's own UPI QR code, made on the phone (ZXing, no network). Black on white even in dark mode, so it scans. */
@Composable
private fun MyUpiQrDialog(userName: String, upiId: String, onClose: () -> Unit) {
    val qr = remember(upiId, userName) {
        val size = 600
        val cells = com.example.scriptbreak.logic.UpiQr.matrix(com.example.scriptbreak.logic.UpiQr.payText(upiId, userName), size)
        val pixels = IntArray(size * size) { i -> if (cells[i / size][i % size]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
        android.graphics.Bitmap.createBitmap(pixels, size, size, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { androidx.compose.material3.TextButton(onClick = onClose) { Text("Close", color = AppBlue) } },
        title = { Text("Pay $userName", fontWeight = FontWeight.Bold, color = InkDark) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(10.dp)) {
                    androidx.compose.foundation.Image(bitmap = qr, contentDescription = "UPI QR code for $upiId",
                        modifier = Modifier.size(220.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(upiId, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                Text("Scan in DemoPay to pay $userName. Demo only: no real money.", fontSize = 13.sp, color = MutedText)
            }
        },
        containerColor = AppSurface,
    )
}
