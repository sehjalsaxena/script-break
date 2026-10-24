package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.logic.JudgeMode
import com.example.scriptbreak.logic.KitResult
import com.example.scriptbreak.logic.KitRow
import com.example.scriptbreak.logic.Rate
import com.example.scriptbreak.logic.Story
import com.example.scriptbreak.ml.Payment
import com.example.scriptbreak.ml.Result
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.components.DemoSecondaryButton
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppBlue
import com.example.scriptbreak.ui.theme.AppPurple
import com.example.scriptbreak.ui.theme.AppPurpleLight
import com.example.scriptbreak.ui.theme.AppSurface
import com.example.scriptbreak.ui.theme.CritRed
import com.example.scriptbreak.ui.theme.CritRedTint
import com.example.scriptbreak.ui.theme.FaintText
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.LineBorder
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.NudgeAmber
import com.example.scriptbreak.ui.theme.NudgeAmberTint
import com.example.scriptbreak.ui.theme.PanelBg
import com.example.scriptbreak.ui.theme.PauseOrange
import com.example.scriptbreak.ui.theme.PauseOrangeTint
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.SafeGreenTint
import java.util.Locale

/**
 * 1-Tap preset scenarios specifically designed for hackathon judges to evaluate
 * the on-device AI fraud detection model instantly with vector icons and zero emojis.
 */
private data class QuickScenario(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val expectedBadge: String,
    val badgeColorType: String,
    val payment: Payment,
    val q1: Boolean? = null,
    val q2: Boolean? = null,
)

private val PRESET_SCENARIOS = listOf(
    QuickScenario(
        id = "digital_arrest",
        icon = AppIcons.Warning,
        title = "Digital Arrest",
        subtitle = "Fake police call demanding secret fine (₹50k)",
        expectedBadge = "CRITICAL",
        badgeColorType = "CRITICAL",
        payment = Payment(
            amount = 50000.0,
            median90d = 500.0,
            payments90d = 35,
            callNow = true,
            payeeStatus = "new",
            payeePasted = true
        ),
        q1 = true,
        q2 = true,
    ),
    QuickScenario(
        id = "screen_share",
        icon = AppIcons.ScreenShare,
        title = "Remote Takeover",
        subtitle = "Remote-access app sharing the screen (₹28k)",
        expectedBadge = "CRITICAL",
        badgeColorType = "CRITICAL",
        payment = Payment(
            amount = 28000.0,
            median90d = 700.0,
            payments90d = 20,
            screenShare = true,
            appSwitch = true,
            payeeStatus = "new",
            payeePasted = true
        ),
    ),
    QuickScenario(
        id = "reverse_refund",
        icon = AppIcons.History,
        title = "Wrong Credit",
        subtitle = "Stranger's 'mistaken' ₹10k, to be sent on to another account",
        expectedBadge = "CRITICAL",
        badgeColorType = "CRITICAL",
        payment = Payment(
            amount = 10000.0,
            median90d = 1200.0,
            payments90d = 40,
            creditUnknown48h = 10000.0,
            payeeIsSender = false,   // returning it to the real sender is genuine (no warning); another account is the scam
            payeeStatus = "new"
        ),
    ),
    QuickScenario(
        id = "late_night",
        icon = AppIcons.DarkMode,
        title = "Late-Night Transfer",
        subtitle = "Unusual 2:00 AM transfer to new payee (₹8.5k)",
        expectedBadge = "NUDGE",
        badgeColorType = "NUDGE",
        payment = Payment(
            amount = 8500.0,
            median90d = 400.0,
            payments90d = 25,
            night = true,
            payeeStatus = "new"
        ),
    ),
    QuickScenario(
        id = "normal_grocery",
        icon = AppIcons.VerifiedUser,
        title = "Daily Grocery",
        subtitle = "Routine shopping to known merchant (₹350)",
        expectedBadge = "SAFE",
        badgeColorType = "SAFE",
        payment = Payment(
            amount = 350.0,
            median90d = 450.0,
            payments90d = 60,
            payeeStatus = "known",
            payeeKind = "merchant"
        ),
    ),
    QuickScenario(
        id = "sharma_demo",
        icon = AppIcons.Refresh,
        title = "Sambhav Demo",
        subtitle = "Benchmark ₹20k on phone call to new payee",
        expectedBadge = "BENCHMARK",
        badgeColorType = "PAUSE",
        payment = JudgeMode.DEFAULT,
    ),
)

/**
 * Judge Mode: Interactive AI Defense Sandbox & Showcase for Hackathon Judges.
 * Fully responsive across all Android device screen sizes with generous breathing room.
 */
@Composable
fun JudgeScreen(
    judge: JudgeMode,
    simulateCall: Boolean,
    onSimulateCall: (Boolean) -> Unit,
    simulateScreenShare: Boolean = false,
    onSimulateScreenShare: (Boolean) -> Unit = {},
    onRunInApp: () -> Unit,
    onPickFile: () -> Unit,
    onLoadSample: () -> Unit,
    onBack: () -> Unit,
    resultsContent: @Composable () -> Unit = {},
    onScanQr: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = "Judge Mode",
            onBack = onBack,
            showScriptBreakBadge = true,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(4.dp))

        // Horizontally Scrollable Segmented Pill Tabs with Generous Touch Targets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                Triple("Live Sandbox", AppIcons.Tune, 0),
                Triple("Scam Stories", AppIcons.MenuBook, 1),
                Triple("Batch Test", AppIcons.Folder, 2),
                Triple("Scoreboard", AppIcons.Assessment, 3)
            )
            tabs.forEach { (title, icon, index) ->
                val isSelected = judge.tab == index
                Surface(
                    color = if (isSelected) AppPurple else PanelBg,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isSelected) AppPurple else LineBorder),
                    shadowElevation = if (isSelected) 1.5.dp else 0.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { judge.tab = index }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else MutedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else InkDark
                        )
                    }
                }
            }
        }

        when (judge.tab) {
            0 -> InputsTab(
                judge = judge,
                simulateCall = simulateCall,
                onSimulateCall = onSimulateCall,
                onRunInApp = onRunInApp,
                simulateScreenShare = simulateScreenShare,
                onSimulateScreenShare = onSimulateScreenShare,
                onScanQr = onScanQr
            )
            1 -> StoriesTab(judge = judge)
            2 -> FileTab(
                judge = judge,
                onPickFile = onPickFile,
                onLoadSample = onLoadSample
            )
            else -> resultsContent()
        }
    }
}

// ---------------------------------------------------------------------------------- Sandbox Tab
@Composable
private fun InputsTab(
    judge: JudgeMode,
    simulateCall: Boolean,
    onSimulateCall: (Boolean) -> Unit,
    onRunInApp: () -> Unit,
    simulateScreenShare: Boolean,
    onSimulateScreenShare: (Boolean) -> Unit,
    onScanQr: (() -> Unit)? = null
) {
    val p = judge.inputs

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // 1-Tap Quick Presets for Hackathon Judges
        item {
            QuickPresetsRow(
                currentInputs = p,
                onSelect = { payment, q1, q2 ->
                    judge.edit { payment }
                    judge.setAnswers(q1, q2)
                }
            )
        }

        // A QR code fills the payee and amount (camera or image), then the verdict below updates live
        if (onScanQr != null) {
            item {
                DemoSecondaryButton("Fill from a QR code (camera or image)", onScanQr)
                judge.qrNote?.let { Text(it, fontSize = 13.sp, color = MutedText, modifier = Modifier.padding(top = 6.dp)) }
            }
        }

        // Live AI Decision Verdict Banner
        item {
            ResultCard(
                r = judge.result,
                modelVersion = judge.modelVersion,
                story = judge.stories.firstOrNull { it.id == judge.storyId }?.let {
                    "${it.id}: ${it.title} (Target: ${it.round1Level})"
                },
                onRunInApp = onRunInApp,
                onReset = { judge.reset() },
                canRun = p.amount > 0
            )
        }

        // Card 1: Payment & Baseline Spending Profile
        item {
            SectionCard(
                title = "Payment & Spending Baseline",
                icon = AppIcons.CreditCard,
                subtitle = "How this transfer compares to the user's historical account behavior"
            ) {
                JudgeNumberField(
                    label = "Payment Amount (₹)",
                    helper = "Amount the user is attempting to transfer",
                    value = p.amount.toLong()
                ) { v -> judge.edit { it.copy(amount = v.toDouble()) } }

                JudgeNumberField(
                    label = "Typical 90-Day Normal Payment (₹)",
                    helper = "User's median transaction baseline (0 = new account without history)",
                    value = p.median90d.toLong()
                ) { v -> judge.edit { it.copy(median90d = v.toDouble()) } }

                JudgeNumberField(
                    label = "Past 90-Day Transaction Count",
                    helper = "Number of completed transfers in the past 3 months",
                    value = p.payments90d.toLong()
                ) { v -> judge.edit { it.copy(payments90d = v.toInt()) } }
            }
        }

        // Card 2: Live Device & Sensor Telemetry
        item {
            SectionCard(
                title = "Real-Time Device Telemetry",
                icon = AppIcons.Phone,
                subtitle = "Live sensors detecting social engineering, coercion, or remote takeover"
            ) {
                JudgeSwitchRow(
                    label = "Active phone call right now",
                    helper = "User is on a live voice or video call while paying",
                    checked = p.callNow
                ) { v -> judge.edit { it.copy(callNow = v) } }

                JudgeSwitchRow(
                    label = "Phone call detected in last 30 minutes",
                    helper = "Call concluded immediately before launching DemoPay",
                    checked = p.callSeen30m
                ) { v -> judge.edit { it.copy(callSeen30m = v) } }

                JudgeSwitchRow(
                    label = "Screen sharing or remote access active",
                    helper = "AnyDesk, TeamViewer, RustDesk, or screen recording running",
                    checked = p.screenShare
                ) { v -> judge.edit { it.copy(screenShare = v) } }

                JudgeSwitchRow(
                    label = "Payee UPI ID was pasted from clipboard",
                    helper = "Address copied from an external chat (WhatsApp, Telegram, SMS)",
                    checked = p.payeePasted
                ) { v -> judge.edit { it.copy(payeePasted = v) } }

                JudgeSwitchRow(
                    label = "Switched apps during payment checkout",
                    helper = "User minimized DemoPay to view external messages or consult caller",
                    checked = p.appSwitch
                ) { v -> judge.edit { it.copy(appSwitch = v) } }

                JudgeSwitchRow(
                    label = "Late-night transaction (11:00 PM – 6:00 AM)",
                    helper = "High-urgency off-hours payment window typical of panic extortion",
                    checked = p.night
                ) { v -> judge.edit { it.copy(night = v) } }
            }
        }

        // Card 3: Payee Trust & Relationship Velocity
        item {
            SectionCard(
                title = "Payee Trust & Relationship Velocity",
                icon = AppIcons.Person,
                subtitle = "Recipient familiarity, categorization, and rapid velocity spikes"
            ) {
                JudgeChoiceRow(
                    label = "Payee Trust Status",
                    helper = "Historical relationship with this recipient",
                    options = listOf("known", "new", "recent", "warned", "flagged"),
                    selected = p.payeeStatus
                ) { v -> judge.edit { it.copy(payeeStatus = v) } }

                JudgeChoiceRow(
                    label = "Payee Classification",
                    helper = "Merchant, biller, or person-to-person transfer",
                    options = listOf("person", "merchant", "biller"),
                    selected = p.payeeKind
                ) { v -> judge.edit { it.copy(payeeKind = v) } }

                JudgeNumberField(
                    label = "Earlier payments to this payee today",
                    helper = "Repeat transfers to this unfamiliar recipient in past 24 hours",
                    value = p.samePayee24h.toLong()
                ) { v -> judge.edit { it.copy(samePayee24h = v.toInt()) } }

                JudgeNumberField(
                    label = "Other new payees paid in last hour",
                    helper = "Rapid consecutive payments to multiple new accounts",
                    value = p.newPayees60m.toLong()
                ) { v -> judge.edit { it.copy(newPayees60m = v.toInt()) } }

                JudgeNumberField(
                    label = "Total ₹ sent to new payees today",
                    helper = "Daily cumulative volume transferred to unverified recipients",
                    value = p.sentNew24h.toLong()
                ) { v -> judge.edit { it.copy(sentNew24h = v.toDouble()) } }

                JudgeNumberField(
                    label = "Money received from stranger in last 48h (₹)",
                    helper = "Recent incoming transfer from unknown account (0 = none)",
                    value = p.creditUnknown48h.toLong()
                ) { v -> judge.edit { it.copy(creditUnknown48h = v.toDouble()) } }

                JudgeSwitchRow(
                    label = "Payee is the stranger who sent that money",
                    helper = "Simulates reverse 'wrong credit' refund scam",
                    checked = p.payeeIsSender
                ) { v -> judge.edit { it.copy(payeeIsSender = v) } }
            }
        }

        // Card 4: Psychological Coercion Screening (Safety Guardrail Questions)
        item {
            SectionCard(
                title = "Psychological Coercion Screening",
                icon = AppIcons.HelpOutline,
                subtitle = "During a Pause reflection step, DemoPay prompts the user with these two questions:"
            ) {
                JudgeAnswerRow(
                    label = "Q1: Were you told this payment is required to verify, unfreeze, or protect funds?",
                    value = judge.q1
                ) { judge.setAnswers(it, judge.q2) }

                HorizontalDivider(color = LineBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                JudgeAnswerRow(
                    label = "Q2: Were you instructed to keep this transaction secret from family or bank?",
                    value = judge.q2
                ) { judge.setAnswers(judge.q1, it) }
            }
        }

        // Card 5: Demo Hardware Simulation (For Emulators & Hackathon Demos)
        item {
            SectionCard(
                title = "Demo Hardware Simulation",
                icon = AppIcons.Science,
                subtitle = "Inject simulated device states across the entire DemoPay app for live testing:"
            ) {
                JudgeSwitchRow(
                    label = "Simulate Live Phone Call (Whole App)",
                    helper = "Enables ongoing call state across all screens for Android emulators",
                    checked = simulateCall,
                    onChange = onSimulateCall
                )

                JudgeSwitchRow(
                    label = "Simulate Screen Sharing (Whole App)",
                    helper = "Enables remote screen capture detection on older Android devices",
                    checked = simulateScreenShare,
                    onChange = onSimulateScreenShare
                )
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

// ---------------------------------------------------------------------------------- Presets Row
@Composable
private fun QuickPresetsRow(
    currentInputs: Payment,
    onSelect: (Payment, Boolean?, Boolean?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Presets for Judges",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Swipe for more",
                    fontSize = 12.sp,
                    color = FaintText
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = null,
                    tint = FaintText,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(PRESET_SCENARIOS, key = { it.id }) { preset ->
                val isSelected = currentInputs.amount == preset.payment.amount &&
                    currentInputs.callNow == preset.payment.callNow &&
                    currentInputs.screenShare == preset.payment.screenShare &&
                    currentInputs.creditUnknown48h == preset.payment.creditUnknown48h &&
                    currentInputs.payeeStatus == preset.payment.payeeStatus

                val (badgeFg, badgeBg) = levelColors(preset.badgeColorType)

                Surface(
                    color = if (isSelected) PanelBg else AppSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) AppPurple else LineBorder
                    ),
                    shadowElevation = if (isSelected) 2.dp else 0.5.dp,
                    modifier = Modifier
                        .width(228.dp)
                        .height(144.dp)
                        .clickable { onSelect(preset.payment, preset.q1, preset.q2) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = preset.icon,
                                contentDescription = null,
                                tint = if (isSelected) AppPurple else badgeFg,
                                modifier = Modifier.size(24.dp)
                            )
                            Surface(
                                color = badgeBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = preset.expectedBadge,
                                    color = badgeFg,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = preset.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark,
                            maxLines = 1
                        )
                        Text(
                            text = preset.subtitle,
                            fontSize = 12.sp,
                            color = MutedText,
                            maxLines = 2,
                            lineHeight = 16.sp
                        )

                        Spacer(Modifier.weight(1f))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) AppPurple else AppBlue.copy(alpha = 0.4f))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isSelected) "Active Preset" else "Tap to Select",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) AppPurple else AppBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------- Result Card
@Composable
private fun ResultCard(
    r: Result?,
    modelVersion: String,
    story: String?,
    onRunInApp: () -> Unit,
    onReset: () -> Unit,
    canRun: Boolean,
) {
    if (r == null) {
        Surface(
            color = CritRedTint,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, CritRed),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.Warning,
                        contentDescription = null,
                        tint = CritRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Model Offline", fontWeight = FontWeight.Bold, color = CritRed, fontSize = 16.sp)
                }
                Text("Fail-open protection active: payments are allowed through safely.", color = MutedText, fontSize = 14.sp)
            }
        }
        return
    }

    val (fg, bg) = levelColors(r.level)
    val levelTitle = when (r.level) {
        "CRITICAL" -> "CRITICAL SCAM BLOCKED"
        "PAUSE" -> "PAUSE & REFLECT (10s)"
        "NUDGE" -> "NUDGE ADVISORY"
        else -> "SAFE TO PROCEED"
    }
    val levelIcon = when (r.level) {
        "CRITICAL" -> AppIcons.Warning
        "PAUSE" -> AppIcons.HourglassEmpty
        "NUDGE" -> AppIcons.Info
        else -> AppIcons.VerifiedUser
    }
    val levelDesc = when (r.level) {
        "CRITICAL" -> "Transaction blocked by safety policies. Immediate intervention triggered."
        "PAUSE" -> "10-second cognitive delay with psychological safety screening active."
        "NUDGE" -> "Non-blocking contextual advisory banner presented before payment."
        else -> "Zero high-risk signals detected. Payment processed instantly without friction."
    }

    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, fg.copy(alpha = 0.6f)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Story subtitle if loaded from stories
            if (story != null) {
                Surface(
                    color = PanelBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.MenuBook,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = story,
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }
                }
            }

            // Top Status Badge & Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = bg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, fg.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = levelIcon,
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = levelTitle,
                            color = fg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    color = PanelBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, LineBorder)
                ) {
                    Text(
                        text = "Sentinel AI · Offline",
                        fontSize = 11.sp,
                        color = MutedText,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = levelDesc,
                fontSize = 13.sp,
                color = MutedText,
                lineHeight = 18.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Risk Gauge Meter
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "AI Scam Risk Score",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MutedText
                    )
                    Text(
                        text = String.format(Locale.ENGLISH, "%.1f%%", r.score * 100),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                }

                Spacer(Modifier.height(2.dp))

                LinearProgressIndicator(
                    progress = { r.score.toFloat() },
                    color = fg,
                    trackColor = PanelBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                )

                Spacer(Modifier.height(4.dp))

                // Threshold Markers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0% Safe", fontSize = 11.sp, color = FaintText)
                    Text("30% Nudge", fontSize = 11.sp, color = FaintText)
                    Text("65% Pause", fontSize = 11.sp, color = FaintText)
                    Text("85% Critical", fontSize = 11.sp, color = FaintText)
                }
            }

            // Reasons Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AI Detection Insights",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
                if (r.reasons.isEmpty()) {
                    Surface(
                        color = PanelBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, LineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.VerifiedUser,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Zero risk signals triggered. Normal spending behavior.",
                                fontSize = 13.sp,
                                color = SafeGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    r.reasons.forEach { reason ->
                        Surface(
                            color = PanelBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, LineBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = reasonIcon(reason),
                                    contentDescription = null,
                                    tint = AppPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = reason,
                                    fontSize = 13.sp,
                                    color = InkDark,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // Guardrails Section
            if (r.guardrails.isNotEmpty()) {
                Surface(
                    color = CritRedTint.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CritRed.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = AppIcons.Security,
                                contentDescription = null,
                                tint = CritRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Deterministic Safety Rule Override",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CritRed
                            )
                        }
                        r.guardrails.forEach { g ->
                            val expl = when (g) {
                                "G1" -> "Rule G1: Payee is on Bank/RBI Fraud Watchlist (Mandatory Block)"
                                "G2" -> "Rule G2: Payee was previously paid despite security warning"
                                "G4" -> "Rule G4: User confirmed psychological coercion"
                                else -> "$g: Safety rule approved by banking policy"
                            }
                            Text("• $expl", fontSize = 12.sp, color = InkDark, lineHeight = 16.sp)
                        }
                    }
                }
            }

            HorizontalDivider(color = LineBorder.copy(alpha = 0.6f))

            // Balanced Single-Line Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DemoPrimaryButton(
                    text = "Run in App",
                    onClick = onRunInApp,
                    modifier = Modifier.weight(1.2f),
                    enabled = canRun
                )
                DemoSecondaryButton(
                    text = "Reset",
                    onClick = onReset,
                    modifier = Modifier.weight(0.8f)
                )
            }

            Text(
                text = "AI Engine: $modelVersion · Evaluated on this phone offline",
                fontSize = 11.sp,
                color = FaintText,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp)
            )
        }
    }
}

private fun reasonIcon(text: String): ImageVector = when {
    text.contains("call", ignoreCase = true) -> AppIcons.Phone
    text.contains("screen", ignoreCase = true) -> AppIcons.ScreenShare
    text.contains("amount", ignoreCase = true) || text.contains("higher", ignoreCase = true) -> AppIcons.TrendingUp
    text.contains("payee", ignoreCase = true) || text.contains("new", ignoreCase = true) -> AppIcons.Person
    text.contains("secret", ignoreCase = true) -> AppIcons.Lock
    text.contains("verify", ignoreCase = true) || text.contains("protect", ignoreCase = true) -> AppIcons.Warning
    text.contains("night", ignoreCase = true) -> AppIcons.DarkMode
    text.contains("pasted", ignoreCase = true) -> AppIcons.ContentPaste
    text.contains("switch", ignoreCase = true) -> AppIcons.SwapHoriz
    else -> AppIcons.Info
}

@Composable
private fun levelColors(level: String): Pair<Color, Color> = when (level.uppercase()) {
    "NUDGE" -> NudgeAmber to NudgeAmberTint
    "PAUSE" -> PauseOrange to PauseOrangeTint
    "CRITICAL" -> CritRed to CritRedTint
    else -> SafeGreen to SafeGreenTint
}

// ---------------------------------------------------------------------------------- Form Components
@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LineBorder),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MutedText,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            HorizontalDivider(color = LineBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))
            content()
        }
    }
}

@Composable
private fun JudgeNumberField(
    label: String,
    helper: String,
    value: Long,
    onChange: (Long) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkDark
        )
        if (helper.isNotEmpty()) {
            Text(
                text = helper,
                fontSize = 12.sp,
                color = MutedText,
                lineHeight = 16.sp
            )
        }
        Spacer(Modifier.height(2.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { s ->
                val d = s.filter(Char::isDigit).take(10)
                text = d
                onChange(d.toLongOrNull() ?: 0L)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = InkDark,
                unfocusedTextColor = InkDark,
                cursorColor = AppPurple,
                focusedBorderColor = AppPurple,
                unfocusedBorderColor = LineBorder,
                focusedContainerColor = AppSurface,
                unfocusedContainerColor = AppSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun JudgeSwitchRow(
    label: String,
    helper: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = InkDark
            )
            if (helper.isNotEmpty()) {
                Text(
                    text = helper,
                    fontSize = 12.sp,
                    color = MutedText,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppPurple,
                uncheckedTrackColor = PanelBg
            )
        )
    }
}

@Composable
private fun JudgeChoiceRow(
    label: String,
    helper: String,
    options: List<String>,
    selected: String,
    onPick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkDark
        )
        if (helper.isNotEmpty()) {
            Text(
                text = helper,
                fontSize = 12.sp,
                color = MutedText,
                lineHeight = 16.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { opt ->
                val isSelected = opt.equals(selected, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onPick(opt) },
                    label = {
                        Text(
                            text = opt.replaceFirstChar { it.uppercase() },
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = PanelBg,
                        labelColor = InkDark,
                        selectedContainerColor = AppPurpleLight,
                        selectedLabelColor = AppPurple
                    ),
                    border = BorderStroke(1.dp, if (isSelected) AppPurple else LineBorder)
                )
            }
        }
    }
}

@Composable
private fun JudgeAnswerRow(
    label: String,
    value: Boolean?,
    onPick: (Boolean?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = InkDark,
            lineHeight = 18.sp
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("Not Asked", null, MutedText),
                Triple("Yes (Coerced)", true, CritRed),
                Triple("No (Safe)", false, SafeGreen)
            ).forEach { (title, v, _) ->
                val isSelected = value == v
                FilterChip(
                    selected = isSelected,
                    onClick = { onPick(v) },
                    label = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = PanelBg,
                        labelColor = InkDark,
                        selectedContainerColor = if (v == true) CritRedTint else if (v == false) SafeGreenTint else AppPurpleLight,
                        selectedLabelColor = if (v == true) CritRed else if (v == false) SafeGreen else AppPurple
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) (if (v == true) CritRed else if (v == false) SafeGreen else AppPurple) else LineBorder
                    )
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------- Stories Tab
@Composable
private fun StoriesTab(judge: JudgeMode) {
    var selectedKind by remember { mutableStateOf("all") }
    val filteredStories = remember(judge.stories, selectedKind) {
        when (selectedKind) {
            "scam" -> judge.stories.filter { it.kind == "scam" }
            "boundary" -> judge.stories.filter { it.kind == "boundary" }
            "legit" -> judge.stories.filter { it.kind == "legit" }
            else -> judge.stories
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // Explanatory Banner
        item {
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.MenuBook,
                            contentDescription = null,
                            tint = AppPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "28 Behavioral Scam Test Stories",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )
                    }
                    Text(
                        text = "Curated real-world fraud cases withheld from model training. Tap any scenario to load its exact inputs into the sandbox and test live.",
                        fontSize = 13.sp,
                        color = MutedText,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "all" to "All (${judge.stories.size})",
                    "scam" to "Scams (${judge.stories.count { it.kind == "scam" }})",
                    "boundary" to "Edge Cases (${judge.stories.count { it.kind == "boundary" }})",
                    "legit" to "Genuine (${judge.stories.count { it.kind == "legit" }})"
                ).forEach { (kind, label) ->
                    val isSelected = selectedKind == kind
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedKind = kind },
                        label = { Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = PanelBg,
                            labelColor = InkDark,
                            selectedContainerColor = AppPurpleLight,
                            selectedLabelColor = AppPurple
                        ),
                        border = BorderStroke(1.dp, if (isSelected) AppPurple else LineBorder)
                    )
                }
            }
        }

        items(filteredStories, key = { it.id }) { s ->
            val aiLevel = judge.levelOf(s)
            val isMatch = aiLevel == s.round1Level
            val (aiFg, aiBg) = levelColors(aiLevel)
            val (kindFg, kindBg) = when (s.kind) {
                "scam" -> CritRed to CritRedTint
                "boundary" -> PauseOrange to PauseOrangeTint
                else -> SafeGreen to SafeGreenTint
            }

            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, LineBorder),
                shadowElevation = 0.5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { judge.loadStory(s) }
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = kindBg, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = s.kind.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = kindFg,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(s.id, fontSize = 12.sp, color = FaintText, fontWeight = FontWeight.SemiBold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isMatch) {
                                Surface(color = SafeGreenTint, shape = RoundedCornerShape(6.dp)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Check,
                                            contentDescription = null,
                                            tint = SafeGreen,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text("Match", fontSize = 10.sp, color = SafeGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Surface(color = NudgeAmberTint, shape = RoundedCornerShape(6.dp)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Warning,
                                            contentDescription = null,
                                            tint = NudgeAmber,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text("Adjusted", fontSize = 10.sp, color = NudgeAmber, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(color = PanelBg, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "₹${s.payment.amount.toLong()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InkDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = s.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark,
                        lineHeight = 20.sp
                    )

                    HorizontalDivider(color = LineBorder.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Target: ", fontSize = 12.sp, color = MutedText)
                            Text(s.round1Level, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                            Spacer(Modifier.width(8.dp))
                            Text("AI: ", fontSize = 12.sp, color = MutedText)
                            Surface(color = aiBg, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = aiLevel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = aiFg,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Test Story", fontSize = 12.sp, color = AppPurple, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(3.dp))
                            Icon(
                                imageVector = AppIcons.ChevronRight,
                                contentDescription = null,
                                tint = AppPurple,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

// ---------------------------------------------------------------------------------- Batch Test Tab
@Composable
private fun FileTab(judge: JudgeMode, onPickFile: () -> Unit, onLoadSample: () -> Unit) {
    val kit = judge.kit
    var filterTab by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // Evaluator Intro Card
        item {
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.Folder,
                            contentDescription = null,
                            tint = AppPurple,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Offline Batch Benchmark Evaluator",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )
                    }
                    Text(
                        text = "Score hundreds of transactions offline on this phone in real time. Validate detection rate, false friction rate, and Wilson 95% confidence intervals.",
                        fontSize = 13.sp,
                        color = MutedText,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Action Buttons
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DemoPrimaryButton(
                    text = "Run 400-Payment Benchmark",
                    onClick = onLoadSample,
                    enabled = !judge.kitLoading
                )
                DemoSecondaryButton(
                    text = "Upload Custom Test File",
                    onClick = onPickFile,
                    enabled = !judge.kitLoading
                )
            }
        }

        if (judge.kitLoading) {
            item {
                Surface(
                    color = AppSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = AppPurple, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Evaluating Payments Offline...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = InkDark)
                            Text("Running on-device neural model with 0 network latency", fontSize = 12.sp, color = MutedText)
                        }
                    }
                }
            }
        }

        if (kit != null && !judge.kitLoading) {
            item { KitSummaryCard(kit) }

            // Filter Tabs for rows
            item {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "All Payments (${kit.rows.size})" to 0,
                        "Missed Scams (${kit.missedScams.size})" to 1,
                        "False Alarms (${kit.falseAlarms.size})" to 2
                    ).forEach { (label, idx) ->
                        val isSelected = filterTab == idx
                        FilterChip(
                            selected = isSelected,
                            onClick = { filterTab = idx },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = PanelBg,
                                labelColor = InkDark,
                                selectedContainerColor = AppPurpleLight,
                                selectedLabelColor = AppPurple
                            ),
                            border = BorderStroke(1.dp, if (isSelected) AppPurple else LineBorder)
                        )
                    }
                }
            }

            val displayedRows = when (filterTab) {
                1 -> kit.missedScams
                2 -> kit.falseAlarms
                else -> kit.rows.take(200)
            }

            if (displayedRows.isEmpty()) {
                item {
                    Surface(
                        color = AppSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
                            Text("No transactions in this category", fontSize = 14.sp, color = MutedText)
                        }
                    }
                }
            } else {
                items(displayedRows) { RowLine(it) }
            }

            if (kit.problems.isNotEmpty()) {
                item {
                    Text(
                        text = "Unparseable Rows (${kit.problems.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CritRed
                    )
                }
                items(kit.problems.take(20)) { Text(it, fontSize = 12.sp, color = CritRed) }
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

private fun pct(x: Double) = String.format(Locale.ENGLISH, "%.1f%%", x * 100)
private fun per1000(x: Double) = String.format(Locale.ENGLISH, "%.1f", x * 1000)

@Composable
private fun KitSummaryCard(k: KitResult) {
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LineBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(k.fileName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = InkDark)
                    Text("${k.rows.size} payments evaluated offline", fontSize = 12.sp, color = MutedText)
                }
                Surface(color = SafeGreenTint, shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Check,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Completed",
                            color = SafeGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Stat Cards Row
            if (k.hasLabels) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        title = "Scams Flagged",
                        value = pct(k.scamsFlagged.rate),
                        subtitle = "${k.scamsFlagged.k} of ${k.scamsFlagged.n}",
                        accentColor = CritRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        title = "Legit Protected",
                        value = pct(1.0 - k.genuineInterrupted.rate),
                        subtitle = "Smooth checkout",
                        accentColor = SafeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        title = "False Alarms",
                        value = per1000(k.genuineInterrupted.rate),
                        subtitle = "per 1,000 txns",
                        accentColor = NudgeAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Level Distribution Breakdown
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Decision Level Distribution", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    k.levelCounts.forEach { (level, count) ->
                        val (fg, bg) = levelColors(level)
                        Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(level, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                                Spacer(Modifier.width(4.dp))
                                Text("$count", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkDark)
                            }
                        }
                    }
                }
            }

            if (k.hasLabels && k.byWorkflow.isNotEmpty()) {
                HorizontalDivider(color = LineBorder.copy(alpha = 0.5f))
                Text("Detection Rate by Fraud Type", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                k.byWorkflow.forEach { (workflow, rate) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(workflow, fontSize = 13.sp, color = InkDark)
                        Text("${pct(rate.rate)} (${rate.k}/${rate.n})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CritRed)
                    }
                }
            }

            Text("Confidence intervals calculated using Wilson 95% score interval.", fontSize = 11.sp, color = FaintText)
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = PanelBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LineBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Text(subtitle, fontSize = 10.sp, color = FaintText)
        }
    }
}

@Composable
private fun RowLine(r: KitRow) {
    val (fg, bg) = levelColors(r.level)
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LineBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${r.index}",
                fontSize = 12.sp,
                color = FaintText,
                modifier = Modifier.width(42.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (r.label) { 1 -> "Scam"; 0 -> "Genuine"; else -> "Unlabeled" } + (r.workflow?.let { " · $it" } ?: ""),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkDark
                )
                Text(
                    text = "Amount: ₹${r.amount.toLong()}",
                    fontSize = 11.sp,
                    color = MutedText
                )
            }

            Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = r.level,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = String.format(Locale.ENGLISH, "%.0f%%", r.score * 100),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}
