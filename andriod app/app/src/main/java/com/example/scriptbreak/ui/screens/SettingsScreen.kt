package com.example.scriptbreak.ui.screens

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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.components.DemoSecondaryButton
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppBlue
import com.example.scriptbreak.ui.theme.AppBlueLight
import com.example.scriptbreak.ui.theme.AppOrange
import com.example.scriptbreak.ui.theme.AppOrangeLight
import com.example.scriptbreak.ui.theme.CritRed
import com.example.scriptbreak.ui.theme.CritRedTint
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.LineBorder
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.SafeGreenTint

@Composable
fun SettingsScreen(
    trustedContactName: String,
    modelVersion: String,
    onTrustedContact: () -> Unit,
    onCheckBalance: () -> Unit,
    onAccount: () -> Unit,
    onResetDemo: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var callMonitoring by remember { mutableStateOf(true) }
    var screenRecordingShield by remember { mutableStateOf(true) }
    var adaptiveLearning by remember { mutableStateOf(true) }
    var hindiLanguage by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = "Settings",
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
            // Group 1: Security & Scam Safeguards
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ScriptBreak Scam Safeguards",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Trusted Contact Setting Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onTrustedContact() }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SafeGreenTint)
                        ) {
                            Icon(
                                imageVector = AppIcons.Security,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trusted Family Contact", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                            Text("$trustedContactName • 24h delay protected", fontSize = 12.sp, color = MutedText)
                        }
                        Icon(
                            imageVector = AppIcons.ChevronRight,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = LineBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Call monitoring toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AppBlueLight)
                        ) {
                            Icon(
                                imageVector = AppIcons.Phone,
                                contentDescription = null,
                                tint = AppBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Live Call Scam Interception", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                            Text("Alerts on active calls during transfer", fontSize = 12.sp, color = MutedText)
                        }
                        Switch(
                            checked = callMonitoring,
                            onCheckedChange = { callMonitoring = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = LineBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Screen share shield
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AppOrangeLight)
                        ) {
                            Icon(
                                imageVector = AppIcons.Lock,
                                contentDescription = null,
                                tint = AppOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Screen-Share Protection", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                            Text("Blocks payments during remote access", fontSize = 12.sp, color = MutedText)
                        }
                        Switch(
                            checked = screenRecordingShield,
                            onCheckedChange = { screenRecordingShield = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = LineBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Adaptive anomaly baseline
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SafeGreenTint)
                        ) {
                            Icon(
                                imageVector = AppIcons.Assessment,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Learned Payment Baseline", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                            Text("Nudges when amount is unusual for payee", fontSize = 12.sp, color = MutedText)
                        }
                        Switch(
                            checked = adaptiveLearning,
                            onCheckedChange = { adaptiveLearning = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppBlue
                            )
                        )
                    }
                }
            }

            // Group 2: Payment & Account Preferences
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Payments & Bank Preferences",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onAccount() }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AppBlueLight)
                        ) {
                            Icon(
                                imageVector = AppIcons.Person,
                                contentDescription = null,
                                tint = AppBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Profile & Accounts", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                            Text("Manage linked bank accounts and QR code", fontSize = 12.sp, color = MutedText)
                        }
                        Icon(
                            imageVector = AppIcons.ChevronRight,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = LineBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onCheckBalance() }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
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
                            Text("Check Bank Balance", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                            Text("Primary: Demo Bank •••• 4821", fontSize = 12.sp, color = MutedText)
                        }
                        Icon(
                            imageVector = AppIcons.ChevronRight,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Group 3: System & Demo Options
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "System & Diagnostics",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Language / भाषा", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                            Text(if (hindiLanguage) "हिन्दी (Hindi)" else "English (US)", fontSize = 12.sp, color = MutedText)
                        }
                        Switch(
                            checked = hindiLanguage,
                            onCheckedChange = {
                                hindiLanguage = it
                                Toast.makeText(context, if (it) "भाषा हिन्दी चुनी गई" else "Language set to English", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppOrange
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = LineBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ScriptBreak Engine", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                            Text("Model: $modelVersion", fontSize = 12.sp, color = MutedText)
                        }
                        Surface(
                            color = SafeGreenTint,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SafeGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DemoSecondaryButton(
                        text = "Reset Demo (Clear Session Data)",
                        onClick = {
                            onResetDemo()
                            Toast.makeText(context, "Demo payments reset to defaults", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Log Out Button
            DemoSecondaryButton(
                text = "Sign Out",
                onClick = onLogout
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
