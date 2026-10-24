"""Synthetic session generator for ScriptBreak.

Every number below is an ASSUMPTION, written down in this file (some were added in later
revisions, e.g. the secrecy-answer rates). None of it comes from bank data (we have none,
and the hackathon rules forbid it).
The point of the generator is to make our assumptions explicit and testable: the
stress test shows which assumption drives the false-alarm rate, so bank shadow mode
knows what to measure first.
"""
from __future__ import annotations

import math
import random
from typing import Dict, Iterator, Tuple

from engine import Session

LEGIT = {
    "low_history_share": 0.06,              # payments made by users with < 10 payments in 90 days
    "median_lognormal": (math.log(600), 0.6),
    "payee_mix": {                          # share of payments by payee type
        "known_person": 0.22, "known_merchant": 0.50, "biller": 0.08,
        "new_person": 0.09, "new_merchant": 0.09, "recent_person": 0.02,
    },
    "amount_ratio": {                       # amount / user's median, lognormal (mu, sigma)
        "known_person": (0.3, 1.0), "known_merchant": (-0.2, 0.9), "biller": (0.6, 0.7),
        "new_person": (0.5, 1.2), "new_merchant": (0.2, 1.1), "recent_person": (0.4, 1.0),
    },
    "call_now": {                           # on a phone/internet call while paying
        "known_person": 0.10, "known_merchant": 0.05, "biller": 0.05,
        "new_person": 0.20, "new_merchant": 0.08, "recent_person": 0.15,
    },
    "call_seen_30m": 0.06,                  # was on a call while in the app in the last 30 min
    "screen_share": 0.002,
    "repeat_same_payee_if_new": 0.05,       # second payment to a payee first paid today
    "many_new_payees": 0.005,               # third new payee within 60 minutes
    "sent_new_24h_share": 0.08,             # already paid other new payees today
    "credit_unknown_48h": 0.015,            # received money from someone never transacted with
    "return_to_sender": 0.30,               # ... and this payment returns it to that sender
    "coincidental_credit_match": 0.01,      # ... or pays a different new payee the same amount
    "payee_pasted": {"new_person": 0.40, "recent_person": 0.20, "other": 0.05},
    "app_switch": 0.15, "night": 0.05,
    "warned_payee": 0.0005, "flagged_payee": 0.0001,
    "q1_yes": 0.02, "q2_yes": 0.01,          # q2: asked to keep the payment secret
}

# Scam workflows. Shapes follow public advisories (MHA/I4C, state police, DoT).
# q1 = told the payment verifies, protects or releases money; q2 = told to keep it secret
# (FTC on family-emergency and voice-clone scams; Gujarat Police on digital arrest: 'You cannot tell family').
SCAMS = {
    "digital_arrest": dict(
        label="Digital arrest", call_now=0.92, call_seen_30m=0.05, screen_share=0.05,
        payee={"new_person": 0.65, "recent_person": 0.15, "new_merchant": 0.10, "warned": 0.10},
        ratio=(math.log(30), 0.9), low_history=0.15, repeat_same=0.25, many_new=0.05,
        pasted=0.5, app_switch=0.5, night=0.15, q1=0.6, q2=0.6),
    "customer_care": dict(
        label="Fake customer care / refund", call_now=0.85, call_seen_30m=0.05, screen_share=0.35,
        payee={"new_person": 0.7, "new_merchant": 0.3},
        ratio=(math.log(6), 0.9), low_history=0.10, repeat_same=0.15, many_new=0.05,
        pasted=0.6, app_switch=0.6, night=0.05, q1=0.5, q2=0.15),
    "kyc_official": dict(
        label="Fake KYC / bank official", call_now=0.80, call_seen_30m=0.05, screen_share=0.45,
        payee={"new_person": 0.8, "new_merchant": 0.2},
        ratio=(math.log(4), 1.0), low_history=0.12, repeat_same=0.15, many_new=0.05,
        pasted=0.5, app_switch=0.5, night=0.05, q1=0.55, q2=0.2),
    "courier": dict(
        label="Courier / customs", call_now=0.90, call_seen_30m=0.05, screen_share=0.10,
        payee={"new_person": 0.85, "new_merchant": 0.15},
        ratio=(math.log(15), 0.9), low_history=0.10, repeat_same=0.20, many_new=0.05,
        pasted=0.5, app_switch=0.5, night=0.05, q1=0.5, q2=0.5),
    "voice_clone": dict(
        label="AI voice clone of a relative", call_now=0.95, call_seen_30m=0.02, screen_share=0.0,
        payee={"new_person": 0.95, "recent_person": 0.05},
        ratio=(math.log(12), 0.8), low_history=0.10, repeat_same=0.10, many_new=0.02,
        pasted=0.6, app_switch=0.5, night=0.10, q1=0.05, q2=0.5),
    "hang_up_and_pay": dict(
        label="Hang up, then pay (adaptation)", call_now=0.0, call_seen_30m=0.35, screen_share=0.0,
        payee={"new_person": 0.9, "new_merchant": 0.1},
        ratio=(math.log(25), 0.9), low_history=0.10, repeat_same=0.15, many_new=0.05,
        pasted=0.85, app_switch=0.8, night=0.10, q1=0.4, q2=0.4),
    "sent_by_mistake": dict(
        label="'Sent by mistake' return", call_now=0.6, call_seen_30m=0.1, screen_share=0.0,
        payee={"new_person": 1.0}, credit=True,
        ratio=(math.log(3), 0.7), low_history=0.08, repeat_same=0.05, many_new=0.02,
        pasted=0.4, app_switch=0.4, night=0.10, q1=0.2, q2=0.1),
    "chat_task": dict(
        label="Task scam over chat (out of scope)", call_now=0.05, call_seen_30m=0.05, screen_share=0.0,
        payee={"new_person": 0.8, "new_merchant": 0.2},
        ratio=(math.log(2.5), 1.1), low_history=0.10, repeat_same=0.2, many_new=0.45,
        pasted=0.9, app_switch=0.9, night=0.10, q1=0.1, q2=0.3),
}


def _pick(rng: random.Random, dist: Dict[str, float]) -> str:
    x, acc = rng.random(), 0.0
    for k, p in dist.items():
        acc += p
        if x < acc:
            return k
    return k


def _amount(rng: random.Random, median: int, mu: float, sigma: float, low_history: bool) -> int:
    if low_history:
        a = rng.lognormvariate(math.log(400), 1.1) * math.exp(mu - 0.3)
    else:
        a = median * rng.lognormvariate(mu, sigma)
    return max(10, int(round(a, -1)))


def legit_session(rng: random.Random, call_scale: float = 1.0, screen_scale: float = 1.0) -> Session:
    L = LEGIT
    low = rng.random() < L["low_history_share"]
    median = 0 if low else int(round(min(10000, max(100, rng.lognormvariate(*L["median_lognormal"]))), -1))
    payments = rng.randint(1, 9) if low else rng.randint(10, 200)
    kind = _pick(rng, L["payee_mix"])
    mu, sigma = L["amount_ratio"][kind]
    amount = _amount(rng, median, mu, sigma, low)
    status = {"known_person": "known", "known_merchant": "known", "biller": "known",
              "new_person": "new", "new_merchant": "new", "recent_person": "recent"}[kind]
    if kind == "biller" and rng.random() < 0.1:
        status = "new"
    pkind = "merchant" if "merchant" in kind else ("biller" if kind == "biller" else "person")
    if status == "known":
        r = rng.random()
        if r < L["flagged_payee"]: status = "flagged"
        elif r < L["flagged_payee"] + L["warned_payee"]: status = "warned"
    is_new = status in ("new", "recent")
    call_p = L["call_now"][kind] * (call_scale if is_new else 1.0)
    s = Session(
        amount=amount, median_90d=median, payments_90d=payments,
        call_now=rng.random() < min(1.0, call_p),
        call_seen_30m=rng.random() < L["call_seen_30m"],
        screen_share=rng.random() < L["screen_share"] * screen_scale,
        payee_status=status, payee_kind=pkind,
        same_payee_24h=1 if (is_new and rng.random() < L["repeat_same_payee_if_new"]) else 0,
        new_payees_60m=2 if rng.random() < L["many_new_payees"] else 0,
        sent_new_24h=int(rng.lognormvariate(math.log(800), 1.0)) if rng.random() < L["sent_new_24h_share"] else 0,
        payee_pasted=rng.random() < L["payee_pasted"].get(kind, L["payee_pasted"]["other"]),
        app_switch=rng.random() < L["app_switch"], night=rng.random() < L["night"],
        q1=rng.random() < L["q1_yes"], q2=rng.random() < L["q2_yes"],
    )
    if s.call_now:
        s.call_seen_30m = False
    if s.same_payee_24h:  # an earlier payment to this same new payee today
        s.sent_new_24h += int(s.amount * rng.uniform(0.3, 1.5))
    if rng.random() < L["credit_unknown_48h"]:
        credit = max(100, int(round(rng.lognormvariate(math.log(1500), 1.0), -1)))
        s.credit_unknown_48h = credit
        if status == "new" and pkind == "person":
            r = rng.random()
            if r < L["return_to_sender"]:
                s.payee_is_sender, s.amount = True, credit
            elif r < L["return_to_sender"] + L["coincidental_credit_match"]:
                s.amount = credit
    return s


def scam_session(rng: random.Random, workflow: str, shift: Dict = None) -> Session:
    w = dict(SCAMS[workflow])
    if shift:
        w.update(shift)
    low = rng.random() < w["low_history"]
    median = 0 if low else int(round(min(10000, max(100, rng.lognormvariate(math.log(600), 0.6))), -1))
    payments = rng.randint(1, 9) if low else rng.randint(10, 200)
    kind = _pick(rng, w["payee"])
    status = {"new_person": "new", "recent_person": "recent", "new_merchant": "new", "warned": "warned"}[kind]
    pkind = "merchant" if kind == "new_merchant" else "person"
    amount = _amount(rng, median, *w["ratio"], low)
    call_now = rng.random() < w["call_now"]
    s = Session(
        amount=amount, median_90d=median, payments_90d=payments,
        call_now=call_now, call_seen_30m=(not call_now) and rng.random() < w["call_seen_30m"],
        screen_share=rng.random() < w["screen_share"],
        payee_status=status, payee_kind=pkind,
        same_payee_24h=1 if (status in ("new", "recent") and rng.random() < w["repeat_same"]) else 0,
        new_payees_60m=2 if rng.random() < w["many_new"] else 0,
        payee_pasted=rng.random() < w["pasted"], app_switch=rng.random() < w["app_switch"],
        night=rng.random() < w["night"], q1=rng.random() < w["q1"], q2=rng.random() < w["q2"],
    )
    if s.same_payee_24h:  # split payments: earlier transfers to the same account today
        s.sent_new_24h += int(amount * rng.uniform(0.5, 2.5))
    if s.new_payees_60m:
        s.sent_new_24h += int(amount * rng.uniform(0.5, 1.5) * s.new_payees_60m)
    if w.get("credit"):
        s.credit_unknown_48h = amount
        s.amount = max(10, int(round(amount * rng.uniform(0.99, 1.01))))
    return s


def legit_stream(n: int, seed: int, **kw) -> Iterator[Session]:
    rng = random.Random(seed)
    for _ in range(n):
        yield legit_session(rng, **kw)


def scam_stream(workflow: str, n: int, seed: int, shift: Dict = None) -> Iterator[Session]:
    rng = random.Random(seed)
    for _ in range(n):
        yield scam_session(rng, workflow, shift)
