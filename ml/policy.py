"""The safety layer: turns the model's score into a level, then applies fixed guardrails.

The MODEL decides how risky a payment looks. This small, explicit POLICY decides what the
app does about it. These guardrails are what the disclosure form calls "rules stay as
safety guardrails"; they protect the user if the model is wrong. G1-G5 never lower a level;
only the C1 coaching check can (a popup with no sign of coaching becomes the amber line).

  Score -> level      score > critical -> CRITICAL; > pause -> PAUSE;
                      > nudge and amount >= Rs 1,000 -> NUDGE; else NONE
  C1 coaching check   a full-screen Pause/Critical needs at least ONE sign that someone may be
                      steering the payment (call now or in the last 30 min, screen share, pasted
                      UPI ID, left the app, new payees in the last hour, money already sent to new
                      payees today, money received from a stranger, repeat payment to a new payee).
                      With none of them, a big or unusual amount alone gets the amber NUDGE line
                      (e.g. buying a TV in a shop). This is the ONLY step that can lower a level.
                      Measured on the test set: 0 scams lost, 1 genuine false alarm removed.
  G1 flagged payee    bank-flagged account -> always CRITICAL
  G2 warned payee     paid after an earlier warning -> at least NUDGE;
                      with a live call or screen share -> CRITICAL
  G3 approved rules   a rule approved by analysts (analyst console) -> at least PAUSE
  G5 big first payment (added 25 Oct) a first payment to a new PERSON of at least 30x the usual amount
                      and at least Rs 20,000 -> at least PAUSE, even with no coaching sign (C1 does not
                      apply to it). Money for scams goes to people's accounts; shops are not included,
                      so a TV bought in a shop still gets only the amber line. Measured on the test set:
                      +4 scams caught, genuine interrupted 7.27 -> 7.40 per 1,000 (budget 8).
  G4 answers          at PAUSE or above, "Yes" to Q1 or Q2 -> CRITICAL. "No" changes nothing,
                      so answers can only RAISE the level. G1-G5 run after C1, so they still raise
  (in the app)        "Continue anyway" always available; if scoring fails, the payment goes through
"""
from __future__ import annotations

RANK = {"NONE": 0, "NUDGE": 1, "PAUSE": 2, "CRITICAL": 3}


def _at_least(level: str, floor: str) -> str:
    return floor if RANK[floor] > RANK[level] else level


def coaching_signs(raw: dict) -> list:
    """The signs that someone may be steering this payment (C1). Same list as ScamModel.kt."""
    signs = [k for k in ("call_now", "call_seen_30m", "screen_share", "payee_pasted", "app_switch") if raw.get(k)]
    signs += [k for k in ("new_payees_60m", "sent_new_24h", "credit_unknown_48h", "same_payee_24h") if (raw.get(k) or 0) > 0]
    return signs


def big_first_payment_to_person(raw: dict) -> bool:
    """G5: a first payment to a new person, at least 30x the usual amount and at least Rs 20,000. Same as ScamModel.kt."""
    median = raw.get("median_90d") or 0
    amount = raw.get("amount") or 0
    has_history = (raw.get("payments_90d") or 0) >= 10 and median > 0
    return (raw.get("payee_status") in ("new", "recent") and raw.get("payee_kind", "person") == "person"
            and has_history and amount >= 30 * median and amount >= 20000)


def matches_rule(tokens: set, rule: dict) -> bool:
    return all(t in tokens for t in rule["all"]) and not any(t in tokens for t in rule.get("none", []))


def decide(raw: dict, score: float, m: dict, q1=None, q2=None, approved_rules=(), tokens: set | None = None) -> dict:
    th = m["thresholds"]
    amount = raw.get("amount", 0)
    if score > th["critical"]:
        level = "CRITICAL"
    elif score > th["pause"]:
        level = "PAUSE"
    elif score > th["nudge"] and amount >= m.get("nudge_min_amount", 1000):
        level = "NUDGE"
    else:
        level = "NONE"
    fired = []
    big = big_first_payment_to_person(raw)
    if RANK[level] > RANK["NUDGE"] and not coaching_signs(raw) and not big:   # C1: no sign of coaching -> no popup
        level = "NUDGE" if amount >= m.get("nudge_min_amount", 1000) else "NONE"; fired.append("C1")
    live = raw.get("call_now") or raw.get("call_seen_30m") or raw.get("screen_share")
    if raw.get("payee_status") == "flagged":
        level = "CRITICAL"; fired.append("G1")
    if raw.get("payee_status") == "warned":
        level = _at_least(level, "CRITICAL" if live else "NUDGE"); fired.append("G2")
    for r in approved_rules:
        if tokens is not None and matches_rule(tokens, r):
            level = _at_least(level, "PAUSE"); fired.append(r["id"])
    if big and RANK[level] < RANK["PAUSE"]:                             # G5: big first payment to a new person
        level = "PAUSE"; fired.append("G5")
    if RANK[level] >= RANK["PAUSE"] and (q1 is True or q2 is True):
        level = "CRITICAL"; fired.append("G4")
    return {"level": level, "guardrails": fired}
