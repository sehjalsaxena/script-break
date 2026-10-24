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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.R
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.components.DemoSecondaryButton
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppBlue
import com.example.scriptbreak.ui.theme.AppBlueElectric
import com.example.scriptbreak.ui.theme.AppBlueLight
import com.example.scriptbreak.ui.theme.AppOrange
import com.example.scriptbreak.ui.theme.AppOrangeLight
import com.example.scriptbreak.ui.theme.AppSurface
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.LineBorder
import com.example.scriptbreak.ui.theme.LocalDarkTheme
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.SafeGreenTint

@Composable
fun SettingsScreen(
    trustedContactName: String,
    modelVersion: String,
    isDarkMode: Boolean = false,
    onToggleDarkMode: (Boolean) -> Unit = {},
    isHindi: Boolean = false,
    onToggleLanguage: (Boolean) -> Unit = {},
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = stringResource(R.string.settings_title),
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
            // Group 1: Display & Appearance (Dark Mode & Language)
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_display_group),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dark Mode Setting Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDarkMode) Color(0xFF282A2F) else AppBlueLight)
                        ) {
                            Icon(
                                imageVector = AppIcons.DarkMode,
                                contentDescription = null,
                                tint = if (isDarkMode) Color(0xFFFDD663) else AppBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_dark_mode),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkDark
                            )
                            Text(
                                text = stringResource(R.string.settings_dark_mode_sub),
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = {
                                onToggleDarkMode(it)
                                Toast.makeText(
                                    context,
                                    if (it) "डार्क मोड सक्रिय / Dark mode enabled" else "लाइट मोड सक्रिय / Light mode enabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppOrange,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = LineBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = LineBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Language Setting Row (Segmented EN / हिन्दी selector)
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
                                imageVector = AppIcons.Language,
                                contentDescription = null,
                                tint = AppOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_language),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkDark
                            )
                            Text(
                                text = if (isHindi) stringResource(R.string.settings_language_hi) else stringResource(R.string.settings_language_en),
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }

                        // Segmented Interactive Pill
                        Surface(
                            color = if (isDarkMode) Color(0xFF282A2F) else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(modifier = Modifier.padding(3.dp)) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (!isHindi) (if (isDarkMode) AppBlueElectric else AppBlue) else Color.Transparent)
                                        .clickable {
                                            if (isHindi) {
                                                onToggleLanguage(false)
                                                Toast.makeText(context, "Language switched to English", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "EN",
                                        fontSize = 12.sp,
                                        fontWeight = if (!isHindi) FontWeight.Bold else FontWeight.Medium,
                                        color = if (!isHindi) Color.White else MutedText
                                    )
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isHindi) AppOrange else Color.Transparent)
                                        .clickable {
                                            if (!isHindi) {
                                                onToggleLanguage(true)
                                                Toast.makeText(context, "भाषा हिन्दी में बदली गई", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "हिन्दी",
                                        fontSize = 12.sp,
                                        fontWeight = if (isHindi) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isHindi) Color.White else MutedText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Group 2: Security & Scam Safeguards
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_security_group),
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
                            Text(
                                text = stringResource(R.string.settings_trusted_contact),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkDark
                            )
                            Text(
                                text = stringResource(R.string.settings_trusted_contact_sub, trustedContactName),
                                fontSize = 12.sp,
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
                                tint = if (isDarkMode) AppBlueElectric else AppBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_call_monitoring),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkDark
                            )
                            Text(
                                text = stringResource(R.string.settings_call_monitoring_sub),
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }
                        Switch(
                            checked = callMonitoring,
                            onCheckedChange = { callMonitoring = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (isDarkMode) AppBlueElectric else AppBlue
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
                            Text(
                                text = stringResource(R.string.settings_screen_shield),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkDark
                            )
                            Text(
                                text = stringResource(R.string.settings_screen_shield_sub),
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }
                        Switch(
                            checked = screenRecordingShield,
                            onCheckedChange = { screenRecordingShield = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (isDarkMode) AppBlueElectric else AppBlue
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
                            Text(
                                text = stringResource(R.string.settings_adaptive_baseline),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkDark
                            )
                            Text(
                                text = stringResource(R.string.settings_adaptive_baseline_sub),
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }
                        Switch(
                            checked = adaptiveLearning,
                            onCheckedChange = { adaptiveLearning = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (isDarkMode) AppBlueElectric else AppBlue
                            )
                        )
                    }
                }
            }

            // Group 3: Payment & Account Preferences
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_payments_group),
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
                                tint = if (isDarkMode) AppBlueElectric else AppBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_profile_accounts),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkDark
                            )
                            Text(
                                text = stringResource(R.string.settings_profile_accounts_sub),
                                fontSize = 12.sp,
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
                                tint = if (isDarkMode) AppBlueElectric else AppBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_check_balance),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkDark
                            )
                            Text(
                                text = stringResource(R.string.settings_check_balance_sub),
                                fontSize = 12.sp,
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
            }

            // Group 4: System & Diagnostics
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_system_group),
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
                            Text(
                                text = stringResource(R.string.settings_engine),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkDark
                            )
                            Text(
                                text = stringResource(R.string.settings_engine_sub, modelVersion),
                                fontSize = 12.sp,
                                color = MutedText
                            )
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

                    Spacer(modifier = Modifier.height(14.dp))

                    DemoSecondaryButton(
                        text = stringResource(R.string.settings_reset_demo),
                        onClick = {
                            onResetDemo()
                            Toast.makeText(context, "Demo payments reset to defaults", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Log Out Button
            DemoSecondaryButton(
                text = stringResource(R.string.settings_sign_out),
                onClick = onLogout
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
