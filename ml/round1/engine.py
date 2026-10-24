"""ScriptBreak decision engine.

assess(session, pack) -> Assessment is a pure function:
  * runs on the phone: no network call, no call audio, no message text, no contacts
  * every input is observable by the host bank/UPI app while it is open (see PDF page 12)
  * the user's answers can only RAISE the level, never lower it
  * learned rules come from the pattern-discovery loop (discover.py) and are plain data in
    the rule pack; a rule enters a pack only after analysts approve it

Rule pack v0.2 = hand-written rules after the adversarial review of v0.1.
Rule pack v0.3 = v0.2 + rules found by the discovery loop and approved by analysts.
"""
from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional

NONE, NUDGE, PAUSE, CRITICAL = "NONE", "NUDGE", "PAUSE", "CRITICAL"
RANK = {NONE: 0, NUDGE: 1, PAUSE: 2, CRITICAL: 3}

# ---- thresholds (constants of the rule pack; calibrated in bank shadow mode) ----
AMOUNT_MULTIPLE = 5        # amount at least 5x the user's 90-day median payment ...
AMOUNT_FLOOR = 2000        # ... and at least Rs 2,000
MIN_HISTORY = 10           # fewer payments than this in 90 days = "no reliable history"
NO_HISTORY_FLOOR = 5000    # fixed threshold for users without history
CREDIT_MATCH = 0.02        # outgoing amount within 2% of a recent credit = "matches"
NUDGE_FLOOR = 1000         # no inline notes on payments below Rs 1,000

HERE = Path(__file__).parent

# Feature switches for the ablation study (ablation.py). "repeat_same_payee" is OFF in the rule
# pack: the ablation showed it cost more false alarms than it caught scams; adding up today's
# payments to new payees ("cumulative") covers splitting to the same account instead.
FEATURES = {"call_seen_30m": True, "merchants_count": True, "repeat_same_payee": False,
            "cumulative": True, "fan_out": True, "no_history_floor": True, "screen_share_rule": True}


@dataclass
class Session:
    amount: int                      # this payment, in rupees
    median_90d: int = 0              # user's median payment over 90 days (computed on the phone)
    payments_90d: int = 50           # number of payments in 90 days
    # live-guidance context, sampled only while the host app is open
    call_now: bool = False           # a phone or internet call is active during this payment
    call_seen_30m: bool = False      # a call was active while this app was open in the last 30 min
    screen_share: bool = False       # screen sharing / recording, or a remote-access app, is active
    # payee, from the host app's own payment records
    payee_status: str = "known"      # known | new | recent (first paid < 7 days ago) | warned | flagged
    payee_kind: str = "person"       # person | merchant | biller (allow-listed large biller)
    # payment pattern, from the host app's own records
    same_payee_24h: int = 0          # earlier payments to this new/recent payee in the last 24 h
    new_payees_60m: int = 0          # earlier payments to other new payees in the last 60 min
    sent_new_24h: int = 0            # rupees already sent to new/recent payees in the last 24 h
    credit_unknown_48h: int = 0      # a credit from a sender never transacted with, last 48 h (0 = none)
    payee_is_sender: bool = False    # this payee is the person who sent that credit
    # recorded for incident reports; no hand-written rule uses them
    payee_pasted: bool = False       # payee ID was pasted rather than picked from history
    app_switch: bool = False         # user left the app and came back during the payment flow
    night: bool = False              # 23:00 to 06:00
    # the two questions, asked only at Pause; None = not asked or skipped
    q1: Optional[bool] = None        # told the payment is needed to verify, protect or release money
    q2: Optional[bool] = None        # someone asked them to keep this payment secret (family, bank)


@dataclass
class Assessment:
    tier: str
    groups: List[str]
    reasons: List[str] = field(default_factory=list)
    rules: List[str] = field(default_factory=list)


def has_history(s: Session) -> bool:
    return s.payments_90d >= MIN_HISTORY and s.median_90d > 0


def is_big(s: Session, amount: int) -> bool:
    if has_history(s):
        return amount >= AMOUNT_FLOOR and amount >= AMOUNT_MULTIPLE * s.median_90d
    # no reliable history: fixed floor (switched off only in the ablation study)
    return FEATURES["no_history_floor"] and amount >= NO_HISTORY_FLOOR


def credit_match(s: Session) -> bool:
    c = s.credit_unknown_48h
    return c > 0 and abs(s.amount - c) <= CREDIT_MATCH * c


def tokens(s: Session) -> set:
    """The event vocabulary written to the on-phone incident record (and to opt-in reports).
    It is deliberately wider than what the hand-written rules use, so the discovery loop
    can find patterns no rule covers yet."""
    t = set()
    if s.call_now: t.add("call_now")
    if s.call_seen_30m: t.add("call_seen_30m")
    if s.screen_share: t.add("screen_share")
    if s.payee_status in ("new", "recent"): t.add("payee_new")
    if s.payee_status == "warned": t.add("payee_warned")
    if s.payee_status == "flagged": t.add("payee_flagged")
    if s.payee_kind == "merchant": t.add("payee_merchant")
    if s.payee_kind == "biller": t.add("payee_biller")
    if is_big(s, s.amount): t.add("amount_big")
    if s.payee_status in ("new", "recent") and is_big(s, s.amount + s.sent_new_24h) and not is_big(s, s.amount):
        t.add("amount_cumulative")
    if s.payee_status in ("new", "recent") and s.same_payee_24h >= 1: t.add("repeat_same_payee")
    if s.new_payees_60m >= 2: t.add("many_new_payees")
    if not has_history(s): t.add("low_history")
    if s.credit_unknown_48h > 0: t.add("credit_unknown_48h")
    if credit_match(s): t.add("amount_matches_credit")
    if s.payee_is_sender: t.add("payee_is_sender")
    if s.payee_pasted: t.add("payee_pasted")
    if s.app_switch: t.add("app_switch")
    if s.night: t.add("night")
    return t


def evidence(s: Session) -> Dict:
    reasons = []
    c = s.call_now or (s.call_seen_30m and FEATURES["call_seen_30m"]) or s.screen_share
    strong_c = s.screen_share
    if s.call_now: reasons.append("You are on a call right now")
    elif s.call_seen_30m: reasons.append("You were on a call while using this app in the last 30 minutes")
    if s.screen_share: reasons.append("Your screen is being shared or a remote-access app is active")

    p = s.payee_status in ("new", "recent") and s.payee_kind != "biller" and \
        (FEATURES["merchants_count"] or s.payee_kind != "merchant")
    strong_p = s.payee_status in ("warned", "flagged")
    if s.payee_status == "new" and p: reasons.append("You have never paid this payee before")
    if s.payee_status == "recent" and p: reasons.append("You first paid this payee less than 7 days ago")
    if s.payee_status == "warned": reasons.append("You paid this payee after an earlier warning")
    if s.payee_status == "flagged": reasons.append("This account appears in a fraud report shared with your bank")

    big = is_big(s, s.amount)
    cumulative = FEATURES["cumulative"] and p and not big and is_big(s, s.amount + s.sent_new_24h)
    repeat = (FEATURES["repeat_same_payee"] and p and s.same_payee_24h >= 1) or \
        (FEATURES["fan_out"] and s.new_payees_60m >= 2)
    a = big or cumulative or repeat
    if big:
        if has_history(s):
            reasons.append(f"The amount is {s.amount / s.median_90d:.0f} times your usual payment")
        else:
            reasons.append("This is a large payment and you have little payment history")
    if cumulative: reasons.append("Together with today's other payments to new payees, this is unusually large")
    if repeat: reasons.append("This is a repeat payment to a payee you only started paying today")
    return dict(C=c, strongC=strong_c, P=p, strongP=strong_p, A=a, reasons=reasons)


def assess(s: Session, pack: Optional[Dict] = None) -> Assessment:
    ev = evidence(s)
    present = {"C": ev["C"], "P": ev["P"] or ev["strongP"], "A": ev["A"]}
    groups = [g for g in ("C", "P", "A") if present[g]]
    reasons = list(ev["reasons"])
    rules = []
    if ev["C"] and ev["P"] and ev["A"]: rules.append("R1")
    if ev["strongC"] and ev["P"] and FEATURES["screen_share_rule"]: rules.append("R2")
    if ev["C"] and ev["strongP"]: rules.append("R3")
    if s.payee_status == "flagged": rules.append("R4")

    toks = tokens(s)
    for r in (pack or {}).get("learned", []):
        if all(t in toks for t in r["all"]) and not any(t in toks for t in r.get("none", [])):
            rules.append(r["id"])
            reasons.append(r["explain"])

    if rules:
        crit = []
        if s.q1: crit.append("K1"); reasons.append("You said you were told this is needed to verify, protect or release money")
        if s.q2: crit.append("K2"); reasons.append("You said someone asked you to keep this payment secret")
        if ev["strongP"]: crit.append("K3")
        if ev["strongC"] and ev["A"]: crit.append("K4")
        return Assessment(CRITICAL if crit else PAUSE, groups, reasons, rules + crit)
    # Nudge: one inline line, no extra tap. Never for payees you have paid before, never for
    # everyday small amounts: those are most of life, and notes on them breed alert fatigue.
    if (ev["P"] and (ev["C"] or ev["A"]) and s.amount >= NUDGE_FLOOR) or s.payee_status == "warned":
        return Assessment(NUDGE, groups, reasons, ["N1"])
    return Assessment(NONE, groups, reasons, [])


def naive_baseline(s: Session) -> bool:
    """A common bank-style rule, for comparison: warn on a first payment to a person of Rs 10,000+."""
    return s.payee_status == "new" and s.payee_kind == "person" and s.amount >= 10000


def load_pack(version: str) -> Dict:
    if version == "0.2":
        return {"version": "0.2", "learned": []}
    return json.loads((HERE / f"rulepack_v{version}.json").read_text())
