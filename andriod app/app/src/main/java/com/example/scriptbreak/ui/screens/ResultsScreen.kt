package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.logic.KitResult
import com.example.scriptbreak.logic.Rate
import com.example.scriptbreak.logic.ResultsData
import com.example.scriptbreak.ui.components.DemoSecondaryButton
import com.example.scriptbreak.ui.theme.AppPurple
import com.example.scriptbreak.ui.theme.AppSurface
import com.example.scriptbreak.ui.theme.FaintText
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.LineBorder
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.PanelBg
import com.example.scriptbreak.ui.theme.SafeGreen
import java.util.Locale

/** What the live check on this phone produced. */
data class LiveCheck(
    val storyScamsFlagged: Int, val storyScams: Int,
    val storyGenuineInterrupted: Int, val storyGenuine: Int,
    val sample: KitResult,
    val msTotal: Double,
)

/** The scoreboard: a live check on this phone, then the full test results. All data synthetic. */
@Composable
fun ResultsTab(
    data: ResultsData?,
    live: LiveCheck?,
    liveRunning: Boolean,
    sessionPaid: Int,
    sessionAfterWarning: Int,
    onRunLive: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(4.dp)) }
        item {
            Card {
                Title("Live on this phone")
                if (liveRunning) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = AppPurple, modifier = Modifier.width(24.dp).height(24.dp))
                    Spacer(Modifier.width(10.dp)); Text("Checking…", fontSize = 15.sp)
                }
                if (live != null) {
                    Line("28 Round 1 stories: ${live.storyScamsFlagged} of ${live.storyScams} scams flagged, " +
                        "${live.storyGenuineInterrupted} of ${live.storyGenuine} genuine payments interrupted")
                    val n = live.sample.rows.size
                    Line("$n test payments read and scored on this phone, offline, in ${num(live.msTotal, 0)} ms " +
                        "(${num(live.msTotal / n.coerceAtLeast(1), 2)} ms each)")
                    Line("Scams flagged: ${pctRange(live.sample.scamsFlagged)}")
                    Line("Genuine interrupted: ${per1000Range(live.sample.genuineInterrupted)}")
                }
                Line("This session: $sessionPaid payment${if (sessionPaid == 1) "" else "s"} made, $sessionAfterWarning after a warning")
                DemoSecondaryButton("Run the live check again", onRunLive, enabled = !liveRunning)
            }
        }
        if (data == null) {
            item { Text("Full test results could not be loaded.", fontSize = 15.sp, color = MutedText) }
        } else {
            item {
                Card {
                    Title("Full test: ${fmt(data.testGenuine + data.testScams)} payments the model never trained on")
                    Small("${fmt(data.testGenuine)} genuine + ${fmt(data.testScams)} scams, shifted settings and new seeds")
                    CompareRow("", "AI model", "Round 1 rules", "Bank rule*", header = true)
                    CompareRow("Scams flagged (6 in-scope types)", pct(data.aiInScope), pct(data.rulesInScope), pct(data.bankInScope))
                    CompareRow("Scams flagged (all 8 types)", pct(data.aiAll8), pct(data.rulesAll8), pct(data.bankAll8))
                    CompareRow("Genuine interrupted per 1,000 (budget 8)", num(data.aiGenuinePer1000, 1), num(data.rulesGenuinePer1000, 1), num(data.bankGenuinePer1000, 1))
                    Small("*Bank rule: warn on a first payment to a person of ₹10,000 or more. Same test data for all three. " +
                        "Nudge on ${num(data.nudgedPer1000, 0)} per 1,000 genuine payments; ROC AUC ${num(data.rocAuc, 3)}.")
                }
            }
            item {
                Card {
                    Title("By scam type (flagged = Pause or Critical)")
                    data.byType.forEach { t ->
                        Text(t.label, fontSize = 15.sp, color = InkDark, fontWeight = FontWeight.SemiBold)
                        Bar("AI", t.ai, AppPurple)
                        Bar("Rules", t.rules, FaintText)
                        t.neverSeen?.let { Bar("AI, type never seen", it, SafeGreen) }
                    }
                    Small("\"Never seen\": retrained with that scam type removed, then tested on it.")
                }
            }
            if (data.fairness.isNotEmpty()) item {
                Card {
                    Title("Fairness: genuine payments interrupted by mistake")
                    data.fairness.forEach { (g, r) -> Line("$g: ${per1000Range(r)}") }
                    Small("First-time users (little payment history) are interrupted more often: the model leans on the amount alone. " +
                        "We report this instead of tuning on test data; the fix is a separate threshold for them, checked in shadow mode.")
                }
            }
            item {
                Card {
                    Title("Where it fails")
                    Line("Missed scams: " + data.missedByType.entries.sortedByDescending { it.value }.joinToString(", ") { "${label(data, it.key)} ${it.value}" })
                    Line("Most common false alarms: " + data.falseAlarmPatterns.take(3).joinToString("; "))
                    Line("Named stories: S06 (splitting into small payments on a call) is missed; S08 (a scammer's first small payment) gets only a Nudge; " +
                        "L04 (₹40,000 deposit to a new landlord) is wrongly paused, and the person taps Continue.")
                }
            }
        }
        item {
            Text("Synthetic data: these numbers test our assumptions, not real-world accuracy. Reproduce everything with ml/run_all_tests.py.",
                fontSize = 13.sp, color = FaintText)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(color = AppSurface, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

@Composable private fun Title(t: String) = Text(t, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkDark)
@Composable private fun Line(t: String) = Text(t, fontSize = 15.sp, color = InkDark)
@Composable private fun Small(t: String) = Text(t, fontSize = 13.sp, color = MutedText)

@Composable
private fun CompareRow(label: String, a: String, b: String, c: String, header: Boolean = false) {
    val w = if (header) FontWeight.Bold else FontWeight.Normal
    Column {
        if (label.isNotEmpty()) Text(label, fontSize = 14.sp, color = MutedText)
        Row(Modifier.fillMaxWidth()) {
            Text(a, Modifier.weight(1f), fontSize = 16.sp, fontWeight = if (header) w else FontWeight.Bold, color = AppPurple)
            Text(b, Modifier.weight(1f), fontSize = 16.sp, fontWeight = w, color = InkDark)
            Text(c, Modifier.weight(1f), fontSize = 16.sp, fontWeight = w, color = InkDark)
        }
        if (!header) HorizontalDivider(color = LineBorder, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Bar(label: String, value: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = MutedText, modifier = Modifier.width(120.dp))
        Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(PanelBg)) {
            Box(Modifier.fillMaxWidth(value.toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(color))
        }
        Text(pct(value), fontSize = 13.sp, color = InkDark, modifier = Modifier.width(48.dp).padding(start = 6.dp))
    }
}

private fun label(d: ResultsData, key: String) = d.byType.firstOrNull { it.key == key }?.label ?: key
private fun pct(x: Double) = String.format(Locale.ENGLISH, "%.0f%%", x * 100)
private fun num(x: Double, digits: Int) = String.format(Locale.ENGLISH, "%.${digits}f", x)
private fun fmt(n: Int) = String.format(Locale.ENGLISH, "%,d", n)
private fun pctRange(r: Rate) = "${r.k}/${r.n} = ${num(r.rate * 100, 1)}% (95%: ${num(r.low * 100, 1)}–${num(r.high * 100, 1)}%)"
private fun per1000Range(r: Rate) = "${num(r.rate * 1000, 1)} per 1,000 (95%: ${num(r.low * 1000, 1)}–${num(r.high * 1000, 1)}, n = ${fmt(r.n)})"
