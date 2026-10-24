"""Feature engineering for the ScriptBreak scam-score model.

ONE place defines how a raw payment record becomes the numbers the model sees.
The Android app (ScamModel.kt) must compute exactly the same numbers in exactly
the same order. export_model.py writes this order into scam_model.json, and the
parity test (test_vectors.json) checks that Kotlin and Python agree.

Raw record fields (schema.json):
  amount, median_90d, payments_90d, call_now, call_seen_30m, screen_share,
  payee_status, payee_kind, same_payee_24h, new_payees_60m, sent_new_24h,
  credit_unknown_48h, payee_is_sender, payee_pasted, app_switch, night
Not model inputs: scenario_id, workflow, label, and the answers q1/q2 (the
safety layer handles the answers, after the model).
"""
from __future__ import annotations

import math
from typing import Dict, List

MIN_HISTORY = 10          # fewer payments than this in 90 days = no reliable history
CREDIT_MATCH = 0.02       # outgoing amount within 2% of a recent credit = "matches"
RATIO_CAP = 500.0         # cap for amount / median, so one huge value can't dominate

PAYEE_STATUSES = ["known", "new", "recent", "warned", "flagged"]
PAYEE_KINDS = ["person", "merchant", "biller"]

RAW_DEFAULTS = {
    "amount": 0, "median_90d": 0, "payments_90d": 50,
    "call_now": False, "call_seen_30m": False, "screen_share": False,
    "payee_status": "known", "payee_kind": "person",
    "same_payee_24h": 0, "new_payees_60m": 0, "sent_new_24h": 0,
    "credit_unknown_48h": 0, "payee_is_sender": False,
    "payee_pasted": False, "app_switch": False, "night": False,
}

# Model input columns, in order. Never reorder without re-exporting the model.
FEATURES: List[str] = [
    "log_amount",            # log(1 + amount)
    "has_history",           # 1 if payments_90d >= 10 and median_90d > 0
    "log_ratio",             # log(1 + amount / median_90d), 0 without history
    "log_ratio_24h",         # log(1 + (amount + sent_new_24h) / median_90d), 0 without history
    "log_sent_new_24h",      # log(1 + sent_new_24h)
    "call_now",
    "call_seen_30m",
    "screen_share",
    "status_known", "status_new", "status_recent", "status_warned", "status_flagged",
    "kind_person", "kind_merchant", "kind_biller",
    "same_payee_24h",        # capped at 5
    "new_payees_60m",        # capped at 5
    "credit_unknown",        # 1 if money arrived from a stranger in the last 48 h
    "amount_matches_credit", # 1 if this amount is within 2% of that money
    "payee_is_sender",
    "payee_pasted",
    "app_switch",
    "night",
]

# Plain-language reason for each feature, used when it pushes the score UP.
# None = never shown as a reason (it describes context, not a warning sign).
REASONS: Dict[str, str | None] = {
    "log_amount": "This is a large amount",
    "has_history": None,
    "log_ratio": "The amount is much higher than your usual payment",
    "log_ratio_24h": "Together with today's payments to new payees, this is unusually large",
    "log_sent_new_24h": "You already sent money to new payees today",
    "call_now": "You are on a call right now",
    "call_seen_30m": "You were on a call in the last 30 minutes",
    "screen_share": "Your screen is being shared or a remote-access app is active",
    "status_known": None,
    "status_new": "You have never paid this payee before",
    "status_recent": "You first paid this payee less than 7 days ago",
    "status_warned": "You paid this payee after an earlier warning",
    "status_flagged": "This account appears in a fraud report shared with your bank",
    "kind_person": None,
    "kind_merchant": None,
    "kind_biller": None,
    "same_payee_24h": "This is a repeat payment to a payee you only started paying today",
    "new_payees_60m": "You paid several new accounts in the last hour",
    "credit_unknown": "You recently received money from someone you have never paid",
    "amount_matches_credit": "This sends back the same amount you received from a stranger",
    "payee_is_sender": None,
    "payee_pasted": "The payee's UPI ID was pasted from another app",
    "app_switch": "You left the app and came back during this payment",
    "night": "It is late at night",
}


def _b(x) -> float:
    return 1.0 if bool(x) else 0.0


def featurize(raw: Dict) -> List[float]:
    r = {**RAW_DEFAULTS, **{k: v for k, v in raw.items() if v is not None}}
    amount = float(r["amount"])
    median = float(r["median_90d"])
    hist = r["payments_90d"] >= MIN_HISTORY and median > 0
    sent = float(r["sent_new_24h"])
    credit = float(r["credit_unknown_48h"])
    ratio = min(amount / median, RATIO_CAP) if hist else 0.0
    ratio24 = min((amount + sent) / median, RATIO_CAP) if hist else 0.0
    f = {
        "log_amount": math.log1p(amount),
        "has_history": _b(hist),
        "log_ratio": math.log1p(ratio),
        "log_ratio_24h": math.log1p(ratio24),
        "log_sent_new_24h": math.log1p(sent),
        "call_now": _b(r["call_now"]),
        "call_seen_30m": _b(r["call_seen_30m"]),
        "screen_share": _b(r["screen_share"]),
        "same_payee_24h": float(min(r["same_payee_24h"], 5)),
        "new_payees_60m": float(min(r["new_payees_60m"], 5)),
        "credit_unknown": _b(credit > 0),
        "amount_matches_credit": _b(credit > 0 and abs(amount - credit) <= CREDIT_MATCH * credit),
        "payee_is_sender": _b(r["payee_is_sender"]),
        "payee_pasted": _b(r["payee_pasted"]),
        "app_switch": _b(r["app_switch"]),
        "night": _b(r["night"]),
    }
    for s in PAYEE_STATUSES:
        f[f"status_{s}"] = _b(r["payee_status"] == s)
    for k in PAYEE_KINDS:
        f[f"kind_{k}"] = _b(r["payee_kind"] == k)
    return [f[name] for name in FEATURES]
