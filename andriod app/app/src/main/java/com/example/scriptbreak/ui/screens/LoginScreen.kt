package com.example.scriptbreak.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.R
import com.example.scriptbreak.logic.Account
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.DemoPayLogoText
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppBlue
import com.example.scriptbreak.ui.theme.AppBlueLight
import com.example.scriptbreak.ui.theme.AppOrange
import com.example.scriptbreak.ui.theme.AppOrangeLight
import com.example.scriptbreak.ui.theme.AppSurface
import com.example.scriptbreak.ui.theme.FaintText
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.LineBorder
import com.example.scriptbreak.ui.theme.MutedText

/** Demo login: username + 4-digit PIN. Tap top-right circular button to expand demo profiles. */
@Composable
fun LoginScreen(accounts: List<Account>, onLogin: (username: String, pin: String) -> Unit) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showProfiles by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Main Login Form
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "DemoPay Logo",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            DemoPayLogoText(fontSize = 32.sp)
            Text("protected by ScriptBreak", fontSize = 16.sp, color = MutedText)
            Text("Demo app – no real money or bank accounts are used", fontSize = 14.sp, color = FaintText)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.trim().take(20) },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { v -> pin = v.filter(Char::isDigit).take(4) },
                label = { Text("4-digit PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            DemoPrimaryButton(
                "Log in",
                { onLogin(username, pin) },
                enabled = username.isNotBlank() && pin.length == 4
            )
            Spacer(Modifier.height(32.dp))
        }

        // Dimmed backdrop scrim when square profile dialog/card is open
        if (showProfiles) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showProfiles = false }
            )
            BackHandler { showProfiles = false }
        }

        // Small circular button on top-right corner when collapsed
        if (!showProfiles) {
            Surface(
                shape = CircleShape,
                color = AppSurface,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, LineBorder),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable { showProfiles = true }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.Person,
                        contentDescription = "Select demo profile",
                        tint = AppBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    // Small orange dot indicator badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 9.dp, end = 9.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppOrange)
                    )
                }
            }
        }

        // Morphing square container that emerges from top right when clicked
        AnimatedVisibility(
            visible = showProfiles,
            enter = scaleIn(
                initialScale = 0.15f,
                transformOrigin = TransformOrigin(1f, 0f),
                animationSpec = tween(300, easing = CubicBezierEasing(0.08f, 0.82f, 0.12f, 1.0f))
            ) + fadeIn(animationSpec = tween(220)),
            exit = scaleOut(
                targetScale = 0.15f,
                transformOrigin = TransformOrigin(1f, 0f),
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(160)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, LineBorder),
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header row with Person avatar, Title, and Close (X) button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AppBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.Person,
                                contentDescription = null,
                                tint = AppBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Demo accounts",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkDark
                            )
                            Text(
                                "Tap to fill credentials",
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }
                        IconButton(
                            onClick = { showProfiles = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Close,
                                contentDescription = "Close",
                                tint = MutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(LineBorder)
                    )

                    // Account rows
                    accounts.forEach { a ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    username = a.username
                                    pin = a.demoLoginPin
                                    showProfiles = false
                                    Toast.makeText(context, "Filled ${a.displayName}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    a.displayName,
                                    fontSize = 15.sp,
                                    color = InkDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${a.username} · PIN ${a.demoLoginPin}" + (a.demoUpiPin?.let { " · UPI PIN $it" } ?: ""),
                                    fontSize = 13.sp,
                                    color = MutedText
                                )
                            }
                            Surface(
                                color = AppOrangeLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    username = a.username
                                    pin = a.demoLoginPin
                                    showProfiles = false
                                    Toast.makeText(context, "Filled ${a.displayName}", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    "Use",
                                    color = AppOrange,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Each person has their own UPI PIN. Demo only: real UPI PINs are entered on the bank's secure pad and never seen by ScriptBreak.",
                        fontSize = 11.sp,
                        color = FaintText,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
