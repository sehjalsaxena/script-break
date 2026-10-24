package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.logic.JudgeMode
import com.example.scriptbreak.logic.KitResult
import com.example.scriptbreak.logic.KitRow
import com.example.scriptbreak.logic.Rate
import com.example.scriptbreak.ml.Payment
import com.example.scriptbreak.ml.Result
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.components.DemoSecondaryButton
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppPurple
import com.example.scriptbreak.ui.theme.AppPurpleLight
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
 * Judge mode: vary any of the 16 inputs and see the AI model + safety layer decide live; load one of
 * the 28 Round 1 stories; or load a test file and get error rates. All data synthetic.
 */
@Composable
fun JudgeScreen(
    judge: JudgeMode,
    simulateCall: Boolean,
    onSimulateCall: (Boolean) -> Unit,
    onRunInApp: () -> Unit,
    onPickFile: () -> Unit,
    onLoadSample: () -> Unit,
    onBack: () -> Unit,
    resultsContent: @Composable () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(AppBackground)) {
        AppTopBar(title = "Judge mode", onBack = onBack, showScriptBreakBadge = true, modifier = Modifier.padding(horizontal = 8.dp))
        TabRow(selectedTabIndex = judge.tab, containerColor = AppBackground, contentColor = AppPurple) {
            listOf("Inputs", "Stories", "Test file", "Results").forEachIndexed { i, t ->
                Tab(selected = judge.tab == i, onClick = { judge.tab = i },
                    text = { Text(t, fontSize = 15.sp, fontWeight = if (judge.tab == i) FontWeight.Bold else FontWeight.Normal) })
            }
        }
        when (judge.tab) {
            0 -> InputsTab(judge, simulateCall, onSimulateCall, onRunInApp)
            1 -> StoriesTab(judge)
            2 -> FileTab(judge, onPickFile, onLoadSample)
            else -> resultsContent()
        }
    }
}

// ---------------------------------------------------------------------------------- Try inputs
@Composable
private fun InputsTab(judge: JudgeMode, simulateCall: Boolean, onSimulateCall: (Boolean) -> Unit, onRunInApp: () -> Unit) {
    val p = judge.inputs
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Spacer(Modifier.height(4.dp)) }
        item {
            ResultCard(judge.result, judge.modelVersion,
                judge.stories.firstOrNull { it.id == judge.storyId }?.let { "${it.id}: ${it.title} · Round 1 design level ${it.round1Level}" })
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DemoPrimaryButton("Run in app", onRunInApp, Modifier.weight(1f), enabled = p.amount > 0)
                DemoSecondaryButton("Reset", { judge.reset() }, Modifier.weight(1f))
            }
        }
        item { Hint("\"Run in app\" opens the Amount screen with exactly these inputs; tap Pay to see the real screens.") }
        item { Section("Payment") }
        item { NumberField("Amount (₹)", "amount", p.amount.toLong()) { v -> judge.edit { it.copy(amount = v.toDouble()) } } }
        item { NumberField("Usual payment: 90-day median (₹, 0 = no history)", "median_90d", p.median90d.toLong()) { v -> judge.edit { it.copy(median90d = v.toDouble()) } } }
        item { NumberField("Payments in the last 90 days", "payments_90d", p.payments90d.toLong()) { v -> judge.edit { it.copy(payments90d = v.toInt()) } } }
        item { Section("Phone signals") }
        item { SwitchRow("On a call right now", "call_now", p.callNow) { v -> judge.edit { it.copy(callNow = v) } } }
        item { SwitchRow("Call seen in the app in the last 30 min", "call_seen_30m", p.callSeen30m) { v -> judge.edit { it.copy(callSeen30m = v) } } }
        item { SwitchRow("Screen shared or remote-access app", "screen_share", p.screenShare) { v -> judge.edit { it.copy(screenShare = v) } } }
        item { SwitchRow("Payee UPI ID was pasted", "payee_pasted", p.payeePasted) { v -> judge.edit { it.copy(payeePasted = v) } } }
        item { SwitchRow("Left the app and came back", "app_switch", p.appSwitch) { v -> judge.edit { it.copy(appSwitch = v) } } }
        item { SwitchRow("Late at night (23:00–06:00)", "night", p.night) { v -> judge.edit { it.copy(night = v) } } }
        item { Section("Payee and today's payments") }
        item { Choice("Payee status", "payee_status", listOf("known", "new", "recent", "warned", "flagged"), p.payeeStatus) { v -> judge.edit { it.copy(payeeStatus = v) } } }
        item { Choice("Payee kind", "payee_kind", listOf("person", "merchant", "biller"), p.payeeKind) { v -> judge.edit { it.copy(payeeKind = v) } } }
        item { NumberField("Earlier payments to this new payee today", "same_payee_24h", p.samePayee24h.toLong()) { v -> judge.edit { it.copy(samePayee24h = v.toInt()) } } }
        item { NumberField("Other new payees paid in the last hour", "new_payees_60m", p.newPayees60m.toLong()) { v -> judge.edit { it.copy(newPayees60m = v.toInt()) } } }
        item { NumberField("Already sent to new payees today (₹)", "sent_new_24h", p.sentNew24h.toLong()) { v -> judge.edit { it.copy(sentNew24h = v.toDouble()) } } }
        item { NumberField("Money received from a stranger, last 48 h (₹, 0 = none)", "credit_unknown_48h", p.creditUnknown48h.toLong()) { v -> judge.edit { it.copy(creditUnknown48h = v.toDouble()) } } }
        item { SwitchRow("This payee is the person who sent that money", "payee_is_sender", p.payeeIsSender) { v -> judge.edit { it.copy(payeeIsSender = v) } } }
        item { Section("The two questions (safety layer only, never model inputs)") }
        item { Answer("Q1: told it's needed to verify, protect or release money?", judge.q1) { judge.setAnswers(it, judge.q2) } }
        item { Answer("Q2: asked to keep it secret from family or bank?", judge.q2) { judge.setAnswers(judge.q1, it) } }
        item { Section("Demo") }
        item { SwitchRow("Simulate a phone call in the whole app (for emulators)", "", simulateCall, onSimulateCall) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun levelColors(level: String): Pair<Color, Color> = when (level) {
    "NUDGE" -> NudgeAmber to NudgeAmberTint
    "PAUSE" -> PauseOrange to PauseOrangeTint
    "CRITICAL" -> CritRed to CritRedTint
    else -> SafeGreen to SafeGreenTint
}

private val GUARDRAIL_TEXT = mapOf(
    "G1" to "G1: bank-flagged payee is always Critical",
    "G2" to "G2: payee paid after an earlier warning",
    "G4" to "G4: a \"Yes\" answer at Pause raises it to Critical",
)

@Composable
private fun ResultCard(r: Result?, modelVersion: String, story: String?) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (story != null) Text(story, fontSize = 14.sp, color = MutedText)
            if (r == null) {
                Text("Model unavailable: the app lets this payment through (fail-open).", fontSize = 16.sp, color = CritRed)
                return@Column
            }
            val (fg, bg) = levelColors(r.level)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                    Text(r.level, color = fg, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("AI scam score ${String.format(Locale.ENGLISH, "%.3f", r.score)}", fontSize = 16.sp, color = InkDark, fontWeight = FontWeight.SemiBold)
            }
            LinearProgressIndicator(progress = { r.score.toFloat() }, color = fg, trackColor = PanelBg, modifier = Modifier.fillMaxWidth().height(8.dp))
            if (r.reasons.isEmpty()) Text("No warning signs pushed the score up.", fontSize = 15.sp, color = MutedText)
            r.reasons.forEach { Text("• $it", fontSize = 15.sp, color = InkDark) }
            if (r.guardrails.isNotEmpty()) {
                Text("Safety rules that set the level: " + r.guardrails.joinToString("; ") { GUARDRAIL_TEXT[it] ?: "$it: rule approved by analysts" },
                    fontSize = 14.sp, color = MutedText)
            }
            Text("Model $modelVersion · on this phone, no network · synthetic training data", fontSize = 12.sp, color = FaintText)
        }
    }
}

@Composable private fun Section(t: String) = Text(t, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkDark, modifier = Modifier.padding(top = 8.dp))
@Composable private fun Hint(t: String) = Text(t, fontSize = 14.sp, color = MutedText)

@Composable
private fun FieldLabel(label: String, key: String) {
    Column {
        Text(label, fontSize = 15.sp, color = InkDark)
        if (key.isNotEmpty()) Text(key, fontSize = 12.sp, color = FaintText)
    }
}

@Composable
private fun NumberField(label: String, key: String, value: Long, onChange: (Long) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { s -> val d = s.filter(Char::isDigit).take(10); text = d; onChange(d.toLongOrNull() ?: 0L) },
        label = { Text(label, fontSize = 13.sp) },
        supportingText = { Text(key, fontSize = 12.sp, color = FaintText) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = InkDark,
            unfocusedTextColor = InkDark,
            cursorColor = AppPurple,
            focusedBorderColor = AppPurple,
            unfocusedBorderColor = Color(0xFFDDE2EA)
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SwitchRow(label: String, key: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onChange(!checked) }) {
        Column(Modifier.weight(1f)) { FieldLabel(label, key) }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = AppPurple))
    }
}

@Composable
private fun Choice(label: String, key: String, options: List<String>, selected: String, onPick: (String) -> Unit) {
    Column {
        FieldLabel(label, key)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { o ->
                FilterChip(selected = o == selected, onClick = { onPick(o) }, label = { Text(o, fontSize = 15.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppPurpleLight, selectedLabelColor = AppPurple))
            }
        }
    }
}

@Composable
private fun Answer(label: String, value: Boolean?, onPick: (Boolean?) -> Unit) {
    Column {
        Text(label, fontSize = 15.sp, color = InkDark)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf<Pair<String, Boolean?>>("Not asked" to null, "Yes" to true, "No" to false).forEach { (t, v) ->
                FilterChip(selected = value == v, onClick = { onPick(v) }, label = { Text(t, fontSize = 15.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppPurpleLight, selectedLabelColor = AppPurple))
            }
        }
    }
}

// ---------------------------------------------------------------------------------- 28 stories
@Composable
private fun StoriesTab(judge: JudgeMode) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Hint("The 28 Round 1 behaviour stories, never used to train the model. \"Round 1\" is the level the old " +
                "rules were designed to give; where the AI differs it is marked, not hidden. Tap a story to load it.")
        }
        items(judge.stories, key = { it.id }) { s ->
            val ai = judge.levelOf(s)
            Surface(color = Color.White, shape = RoundedCornerShape(14.dp), shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth().clickable { judge.loadStory(s) }) {
                Column(Modifier.padding(14.dp)) {
                    Text("${s.id} · ${s.kind}", fontSize = 13.sp, color = FaintText)
                    Text(s.title, fontSize = 16.sp, color = InkDark, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Round 1: ${s.round1Level}", fontSize = 14.sp, color = MutedText)
                        Spacer(Modifier.width(12.dp))
                        Text("AI: $ai", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = levelColors(ai).first)
                        if (ai != s.round1Level) Text("  ⚠ differs", fontSize = 14.sp, color = NudgeAmber)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------------------------------------------------------------------------- Test file
@Composable
private fun FileTab(judge: JudgeMode, onPickFile: () -> Unit, onLoadSample: () -> Unit) {
    val kit = judge.kit
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Hint("Load a test file and the model scores every payment on this phone. Formats: JSON array, " +
                "{\"records\": [...]}, JSON Lines, or CSV with a header row. Field names as in ml/schema.json " +
                "(amount, median_90d, call_now, payee_status, …), either flat or inside \"features\". Add \"label\" " +
                "(1 scam, 0 genuine) to get error rates; optional \"workflow\", \"q1\", \"q2\".")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DemoPrimaryButton("Choose a file", onPickFile, Modifier.weight(1f), enabled = !judge.kitLoading)
                DemoSecondaryButton("Our sample file", onLoadSample, Modifier.weight(1f))
            }
        }
        if (judge.kitLoading) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = AppPurple); Spacer(Modifier.width(12.dp)); Text("Scoring on this phone…", fontSize = 16.sp)
            }
        }
        if (kit != null && !judge.kitLoading) {
            item { KitSummaryCard(kit) }
            if (kit.missedScams.isNotEmpty()) {
                item { Section("Missed scams (${kit.missedScams.size})") }
                items(kit.missedScams.take(20)) { RowLine(it) }
            }
            if (kit.falseAlarms.isNotEmpty()) {
                item { Section("Genuine payments interrupted (${kit.falseAlarms.size})") }
                items(kit.falseAlarms.take(20)) { RowLine(it) }
            }
            item { Section("All rows (first 200 of ${kit.rows.size})") }
            items(kit.rows.take(200)) { RowLine(it) }
            if (kit.problems.isNotEmpty()) {
                item { Section("Rows that could not be read (${kit.problems.size})") }
                items(kit.problems.take(30)) { Text(it, fontSize = 14.sp, color = CritRed) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun pct(x: Double) = String.format(Locale.ENGLISH, "%.1f%%", x * 100)
private fun per1000(x: Double) = String.format(Locale.ENGLISH, "%.1f", x * 1000)

@Composable
private fun KitSummaryCard(k: KitResult) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(k.fileName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkDark)
            Text("${k.rows.size} payments scored, ${k.problems.size} skipped", fontSize = 15.sp, color = MutedText)
            Text(k.levelCounts.entries.joinToString("  ·  ") { "${it.key} ${it.value}" }, fontSize = 15.sp, color = InkDark)
            HorizontalDivider(color = LineBorder)
            if (!k.hasLabels) {
                Text("No labels in this file, so no error rates. Add \"label\" (1 scam, 0 genuine).", fontSize = 15.sp, color = MutedText)
            } else {
                Metric("Scams flagged (Pause or Critical)", k.scamsFlagged, false)
                Metric("Genuine payments interrupted", k.genuineInterrupted, true)
                Metric("Genuine payments given a Nudge", k.genuineNudged, true)
                if (k.byWorkflow.isNotEmpty()) {
                    Text("By scam type", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                    k.byWorkflow.forEach { (w, r) -> Text("$w: ${r.k}/${r.n} = ${pct(r.rate)}", fontSize = 14.sp, color = InkDark) }
                }
                Text("95% ranges are Wilson intervals. Synthetic data tests our assumptions, not real-world accuracy.", fontSize = 12.sp, color = FaintText)
            }
        }
    }
}

@Composable
private fun Metric(label: String, r: Rate, perThousand: Boolean) {
    val v = if (perThousand) "${per1000(r.rate)} per 1,000 (95%: ${per1000(r.low)}–${per1000(r.high)})"
            else "${pct(r.rate)} (95%: ${pct(r.low)}–${pct(r.high)})"
    Text("$label: ${r.k}/${r.n} = $v", fontSize = 15.sp, color = InkDark)
}

@Composable
private fun RowLine(r: KitRow) {
    val (fg, _) = levelColors(r.level)
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("#${r.index}", fontSize = 14.sp, color = FaintText, modifier = Modifier.width(56.dp))
        Text(when (r.label) { 1 -> "scam"; 0 -> "genuine"; else -> "-" } + (r.workflow?.let { " · $it" } ?: ""),
            fontSize = 14.sp, color = MutedText, modifier = Modifier.weight(1f))
        Text("₹${r.amount.toLong()}", fontSize = 14.sp, color = InkDark, modifier = Modifier.width(80.dp))
        Text(r.level, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.width(76.dp))
        Text(String.format(Locale.ENGLISH, "%.2f", r.score), fontSize = 14.sp, color = InkDark)
    }
}
