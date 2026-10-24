package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.R
import com.example.scriptbreak.logic.Account
import com.example.scriptbreak.ui.components.DemoPayLogoText
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppPurple
import com.example.scriptbreak.ui.theme.AppPurpleLight
import com.example.scriptbreak.ui.theme.FaintText
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText

/** Demo login: username + 4-digit PIN. The demo accounts are listed so judges can sign in. */
@Composable
fun LoginScreen(accounts: List<Account>, onLogin: (username: String, pin: String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(AppBackground).verticalScroll(rememberScrollState()).padding(24.dp),
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
            value = username, onValueChange = { username = it.trim().take(20) },
            label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = pin, onValueChange = { v -> pin = v.filter(Char::isDigit).take(4) },
            label = { Text("4-digit PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth(),
        )
        DemoPrimaryButton("Log in", { onLogin(username, pin) }, enabled = username.isNotBlank() && pin.length == 4)

        Surface(color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Demo accounts (tap to fill)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkDark)
                accounts.forEach { a ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { username = a.username; pin = a.demoLoginPin },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(a.displayName, fontSize = 16.sp, color = InkDark, fontWeight = FontWeight.SemiBold)
                            Text("${a.username} · PIN ${a.demoLoginPin}" + (a.demoUpiPin?.let { " · UPI PIN $it" } ?: ""),
                                fontSize = 14.sp, color = MutedText)
                        }
                        Surface(color = AppPurpleLight, shape = RoundedCornerShape(8.dp)) {
                            Text("Use", color = AppPurple, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
                Text("Each person has their own UPI PIN. Demo only: real UPI PINs are entered on the bank's secure pad and never seen by ScriptBreak.",
                    fontSize = 12.sp, color = FaintText)
            }
        }
    }
}
