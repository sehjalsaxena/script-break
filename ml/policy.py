"""The safety layer: turns the model's score into a level, then applies fixed guardrails.

The MODEL decides how risky a payment looks. This small, explicit POLICY decides what the
app does about it. These guardrails are what the disclosure form calls "rules stay as
safety guardrails"; they protect the user if the model is wrong, and never lower a level.

  Score -> level      score > critical -> CRITICAL; > pause -> PAUSE;
                      > nudge and amount >= Rs 1,000 -> NUDGE; else NONE
  G1 flagged payee    bank-flagged account -> always CRITICAL
  G2 warned payee     paid after an earlier warning -> at least NUDGE;
                      with a live call or screen share -> CRITICAL
  G3 approved rules   a rule approved by analysts (analyst console) -> at least PAUSE
  G4 answers          at PAUSE or above, "Yes" to Q1 or Q2 -> CRITICAL. "No" changes nothing,
                      so answers can only RAISE the level
  (in the app)        "Continue anyway" always available; if scoring fails, the payment goes through
"""
from __future__ import annotations

RANK = {"NONE": 0, "NUDGE": 1, "PAUSE": 2, "CRITICAL": 3}


def _at_least(level: str, floor: str) -> str:
    return floor if RANK[floor] > RANK[level] else level


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
    live = raw.get("call_now") or raw.get("call_seen_30m") or raw.get("screen_share")
    if raw.get("payee_status") == "flagged":
        level = "CRITICAL"; fired.append("G1")
    if raw.get("payee_status") == "warned":
        level = _at_least(level, "CRITICAL" if live else "NUDGE"); fired.append("G2")
    for r in approved_rules:
        if tokens is not None and matches_rule(tokens, r):
            level = _at_least(level, "PAUSE"); fired.append(r["id"])
    if RANK[level] >= RANK["PAUSE"] and (q1 is True or q2 is True):
        level = "CRITICAL"; fired.append("G4")
    return {"level": level, "guardrails": fired}
