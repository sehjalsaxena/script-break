package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.logic.PendingContact
import com.example.scriptbreak.logic.ReportPacket
import com.example.scriptbreak.logic.TrustedContact
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.components.DemoSecondaryButton
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.CritRed
import com.example.scriptbreak.ui.theme.FaintText
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.LineBorder
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.NudgeAmber
import com.example.scriptbreak.ui.theme.NudgeAmberTint
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.SafeGreenTint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun WhiteCard(content: @Composable () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column {
        Text(label, fontSize = 13.sp, color = FaintText)
        Text(value, fontSize = 17.sp, color = InkDark, fontWeight = FontWeight.SemiBold)
    }
}

/** Report packet: everything the 1930 helpline asks for, ready to read out or share. */
@Composable
fun ReportPacketScreen(
    packet: ReportPacket,
    onCall1930: () -> Unit,
    onReportOnline: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AppBackground).padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTopBar(title = "Report this payment", onBack = onBack, showScriptBreakBadge = true)
        Surface(color = Color(0xFFFBEAEA), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Text("If this was a scam, call 1930 now. Banks can freeze money faster the sooner you report. Keep this screen open while you call.",
                fontSize = 16.sp, color = CritRed, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(14.dp))
        }
        WhiteCard {
            Field("Amount", packet.amountText)
            Field("Paid to", packet.payeeName)
            Field("Payee UPI ID", packet.payeeUpiId)
            Field("UPI reference number", packet.upiRef)
            Field("Date and time", packet.whenText)
            if (packet.levelShown != "NONE") {
                HorizontalDivider(color = LineBorder)
                Text("Warning shown before paying: ${packet.levelShown}", fontSize = 15.sp, color = InkDark, fontWeight = FontWeight.SemiBold)
                packet.reasons.forEach { Text("• $it", fontSize = 15.sp, color = InkDark) }
            }
        }
        DemoPrimaryButton("Call 1930 (cyber-crime helpline)", onCall1930, containerColor = CritRed)
        DemoSecondaryButton("Report online: cybercrime.gov.in", onReportOnline)
        DemoSecondaryButton("Share this packet", onShare)
        Text("This packet stays on your phone. It leaves only if you share it.", fontSize = 13.sp, color = FaintText)
        Spacer(Modifier.height(16.dp))
    }
}

/** Trusted contact: who "Ask family" reaches. Changing it needs a code and waits 24 hours. */
@Composable
fun TrustedContactScreen(
    current: TrustedContact,
    pending: PendingContact?,
    otpFor: TrustedContact?,
    demoOtp: String?,
    onSendCode: (name: String, relation: String, phone: String) -> Unit,
    onVerify: (code: String) -> Unit,
    onCancelPending: () -> Unit,
    onBack: () -> Unit,
    usualPaymentText: String? = null,
    onResetDemo: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(AppBackground).padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTopBar(title = "Trusted contact", onBack = onBack, showScriptBreakBadge = true)
        WhiteCard {
            Text("Your trusted contact", fontSize = 14.sp, color = MutedText)
            Text(current.label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = InkDark)
            if (current.phone.isNotBlank()) Text(current.maskedPhone, fontSize = 15.sp, color = MutedText)
            Surface(color = SafeGreenTint, shape = RoundedCornerShape(8.dp)) {
                Text("● Active: gets \"Ask family\" alerts", color = SafeGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Text("They can't see your account or stop a payment. They only get a short alert and call you.", fontSize = 14.sp, color = MutedText)
        }
        if (pending != null) {
            WhiteCard {
                Text("Change requested", fontSize = 14.sp, color = MutedText)
                Text(pending.contact.label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkDark)
                Surface(color = NudgeAmberTint, shape = RoundedCornerShape(8.dp)) {
                    Text("Active in 24 hours, from " + SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH).format(Date(pending.activeAtMs)),
                        color = NudgeAmber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Text("Why wait? A scammer on a call may tell you to change your contact to their number. The 24-hour wait means " +
                    "${current.name} still gets the alert during that call.", fontSize = 14.sp, color = MutedText)
                DemoSecondaryButton("Cancel this change", onCancelPending)
            }
        } else if (otpFor == null) {
            WhiteCard {
                Text("Change trusted contact", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkDark)
                OutlinedTextField(name, { name = it.take(30) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(relation, { relation = it.take(20) }, label = { Text("Relation (e.g. daughter)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, label = { Text("Mobile number") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                DemoPrimaryButton("Send code to this number", { onSendCode(name, relation, phone) }, enabled = name.isNotBlank() && phone.length == 10)
            }
        } else {
            WhiteCard {
                Text("Enter the code sent to ${otpFor.maskedPhone}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkDark)
                Text("Ask ${otpFor.name} to read it out. This proves the number is really theirs.", fontSize = 14.sp, color = MutedText)
                if (demoOtp != null) {
                    Surface(color = NudgeAmberTint, shape = RoundedCornerShape(8.dp)) {
                        Text("Demo: no SMS is sent. The code is $demoOtp", color = NudgeAmber, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, label = { Text("6-digit code") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
                Row {
                    DemoPrimaryButton("Verify", { onVerify(code) }, Modifier.weight(1f), enabled = code.length == 6)
                    Spacer(Modifier.width(10.dp))
                    DemoSecondaryButton("Cancel", onCancelPending, Modifier.weight(1f))
                }
            }
        }
        if (usualPaymentText != null || onResetDemo != null) {
            WhiteCard {
                Text("Demo", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkDark)
                if (usualPaymentText != null) {
                    Text("Usual payment the AI sees: $usualPaymentText", fontSize = 15.sp, color = InkDark)
                    Text("Learned from your payments: it changes as you pay.", fontSize = 14.sp, color = MutedText)
                }
                if (onResetDemo != null) DemoSecondaryButton("Reset demo (clear this session's payments)", onResetDemo)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
