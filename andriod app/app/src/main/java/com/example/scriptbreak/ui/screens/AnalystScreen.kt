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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.logic.AnalystMode
import com.example.scriptbreak.logic.Cluster
import com.example.scriptbreak.ml.Result
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.components.DemoPrimaryButton
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
 * Analyst console: the AI groups scam reports the phone missed and proposes rules; a person decides.
 * Approved rules become guardrail G3 on the phone. All data synthetic.
 */
@Composable
fun AnalystScreen(analyst: AnalystMode, onRunInApp: () -> Unit, onBack: () -> Unit) {
    val d = analyst.discovery
    Column(Modifier.fillMaxSize().background(AppBackground)) {
        AppTopBar(title = "Analyst console", onBack = onBack, showScriptBreakBadge = true, modifier = Modifier.padding(horizontal = 8.dp))
        if (d == null) {
            Text("Discovery data could not be loaded.", fontSize = 16.sp, color = CritRed, modifier = Modifier.padding(16.dp))
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card {
                    Text("A new scam wave has started", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkDark)
                    Text("${fmt(d.reports)} scam reports this month (consented, no names, numbers or messages). " +
                        "The phone's model missed ${d.missed} of them.", fontSize = 16.sp, color = InkDark)
                    Text("The AI grouped the missed reports by what happened in each payment (${d.method}; " +
                        "silhouette ${num(d.silhouette, 2)}). It is never told the scam type.", fontSize = 14.sp, color = MutedText)
                    Text("Approval policy: a rule must catch at least ${(d.minShareCaught * 100).toInt()}% of its group and add at most " +
                        "${num(d.budgetPer1000, 1)} false alarms per 1,000 genuine payments. A person always decides.", fontSize = 14.sp, color = MutedText)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable { analyst.showTrueTypes = !analyst.showTrueTypes }) {
                        Text("Show the true scam types (judges only)", fontSize = 15.sp, color = InkDark, modifier = Modifier.weight(1f))
                        Switch(checked = analyst.showTrueTypes, onCheckedChange = { analyst.showTrueTypes = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = AppPurple))
                    }
                    if (analyst.showTrueTypes) {
                        Text("Missed, by true type: " + d.missedByType.entries.sortedByDescending { it.value }
                            .joinToString(", ") { "${AnalystMode.TYPE_NAMES[it.key] ?: it.key} ${it.value}" }, fontSize = 14.sp, color = MutedText)
                    }
                }
            }
            item { Text("Groups the AI found (${d.clusters.size})", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkDark) }
            items(d.clusters, key = { it.id }) { c -> ClusterCard(c, analyst) }
            item { TryItCard(analyst, onRunInApp) }
            item {
                Text("Honest note: the app's real model was later retrained with these reports and now catches \"sent by mistake\" " +
                    "without any rule. This console uses the model from before that retraining, to show the gap between a new " +
                    "scam appearing and the model learning it, and how an approved rule closes it.", fontSize = 13.sp, color = FaintText)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun ClusterCard(c: Cluster, analyst: AnalystMode) {
    val decision = analyst.decisions[c.id]
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Group ${c.id} · ${c.size} reports", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkDark, modifier = Modifier.weight(1f))
            val (fg, bg) = when (c.suggestion) { "approve" -> SafeGreen to SafeGreenTint; "shadow mode" -> NudgeAmber to NudgeAmberTint; else -> MutedText to PanelBg }
            Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                Text("AI suggests: ${c.suggestion}", color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
        }
        Text("What these reports have in common:", fontSize = 14.sp, color = MutedText)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            c.pattern.forEach { p ->
                Surface(color = AppPurpleLight, shape = RoundedCornerShape(8.dp)) {
                    Text(p, color = AppPurple, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
        if (analyst.showTrueTypes) {
            Text("True types: " + c.trueMix.entries.joinToString(", ") { "${AnalystMode.TYPE_NAMES[it.key] ?: it.key} ${it.value}" },
                fontSize = 14.sp, color = MutedText)
        }
        HorizontalDivider(color = LineBorder)
        val r = c.rule
        if (r != null) {
            Text("Proposed rule ${r.id}: ${r.name}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
            Text("Catches ${(r.catchesShare * 100).toInt()}% of this group · adds ${num(r.addedPer1000, 2)} false alarms per 1,000 genuine payments (backtested)",
                fontSize = 14.sp, color = InkDark)
            Text("The phone will say: \"${r.explain}\"", fontSize = 14.sp, color = MutedText)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Decision("Approve", "approve", decision, SafeGreen) { analyst.decide(c.id, it) }
                Decision("Shadow mode", "shadow", decision, NudgeAmber) { analyst.decide(c.id, it) }
                Decision("Reject", "reject", decision, CritRed) { analyst.decide(c.id, it) }
            }
            when (decision) {
                "approve" -> Text("✓ Approved: phones now Pause payments that match this rule (guardrail G3).", fontSize = 14.sp, color = SafeGreen, fontWeight = FontWeight.SemiBold)
                "shadow" -> Text("Shadow mode: runs silently and only counts how often it would fire. Nothing is shown to people.", fontSize = 14.sp, color = NudgeAmber)
                "reject" -> Text("Rejected: not sent to phones.", fontSize = 14.sp, color = MutedText)
            }
        } else {
            val cost = c.cheapestAddedPer1000
            Text(if (cost != null) "No rule is cheap enough: catching 70% of this group would interrupt ${num(cost, 2)} genuine payments per 1,000."
                 else "No rule could be found for this group.", fontSize = 14.sp, color = InkDark)
            Text("These reports look too much like normal payments. They need more data and a retrained model, not a rule.", fontSize = 14.sp, color = MutedText)
        }
    }
}

@Composable
private fun Decision(label: String, value: String, current: String?, color: Color, onPick: (String) -> Unit) {
    FilterChip(selected = current == value, onClick = { onPick(value) }, label = { Text(label, fontSize = 15.sp) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color, selectedLabelColor = Color.White))
}

@Composable
private fun TryItCard(analyst: AnalystMode, onRunInApp: () -> Unit) {
    val before = analyst.tryIt(analyst.scamPayment, withApprovedRules = false)
    val now = analyst.tryIt(analyst.scamPayment, withApprovedRules = true)
    val genuine = analyst.tryIt(analyst.genuinePayment, withApprovedRules = true)
    Card {
        Text("Try it on a phone", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkDark)
        Text("A stranger sends ₹5,000 \"by mistake\", calls, and asks for it back, to a different account.", fontSize = 15.sp, color = InkDark)
        LevelLine("Before any rule", before)
        LevelLine("With the approved rules", now)
        HorizontalDivider(color = LineBorder)
        Text("A genuine person returning ₹5,000 to the stranger who actually sent it:", fontSize = 15.sp, color = InkDark)
        LevelLine("With the approved rules", genuine)
        Text("The rule says \"not paying back that sender\", so genuine returns are not interrupted.", fontSize = 13.sp, color = MutedText)
        DemoPrimaryButton("Run the scam payment in the app", onRunInApp, enabled = analyst.beforeModel != null)
    }
}

@Composable
private fun LevelLine(label: String, r: Result?) {
    val level = r?.level ?: "unavailable"
    val (fg, bg) = when (level) {
        "NUDGE" -> NudgeAmber to NudgeAmberTint; "PAUSE" -> PauseOrange to PauseOrangeTint
        "CRITICAL" -> CritRed to CritRedTint; else -> SafeGreen to SafeGreenTint
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, color = MutedText, modifier = Modifier.weight(1f))
        Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
            Text(if (level == "NONE") "NONE (no warning)" else level, color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
        }
        if (r != null && r.guardrails.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Text(r.guardrails.joinToString(","), fontSize = 13.sp, color = FaintText)
        }
    }
}

private fun fmt(n: Int) = String.format(Locale.ENGLISH, "%,d", n)
private fun num(x: Double, digits: Int) = String.format(Locale.ENGLISH, "%.${digits}f", x)
