"""Extra tests for completeness: error bars, safety guarantees, edge cases and speed.

They test the COMMITTED model (scam_model.json, the file inside the Android app) through
scorer.py + policy.py, which is line-for-line the same logic as ScamModel.kt on the phone.
Data: the test split (data/test.jsonl: new seeds, shifted settings, never used in training),
the 28 Round 1 stories, and hand-made edge cases.

  1. Error bars    95% Wilson intervals for every rate we report (450 scams per type is small)
  2. Safety        answers only raise the level; guardrails G1-G4 always fire and never lower;
                   no Nudge under Rs 1,000; Q1/Q2 are never model inputs; reasons only name
                   signals that are actually present; scoring is deterministic
  3. Edge cases    Rs 0, Rs 1, Rs 100 crore, no history, missing or unknown fields
  4. Speed         Python scorer time per payment (the phone is measured separately)
  5. Fairness      genuine payments interrupted per 1,000, by group (first-time users vs regular
                   users, person vs merchant payee, night vs day): reported, never tuned on test data

Run:  python extra_tests.py [path/to/test.jsonl]      (run_all_tests.py calls this for you)
"""
from __future__ import annotations

import json
import math
import random
import sys
import time
from pathlib import Path

import policy
import scorer
from features import featurize

HERE = Path(__file__).parent
sys.path.insert(0, str(HERE / "round1"))
from engine import Session, tokens  # noqa: E402

IN_SCOPE = ["digital_arrest", "customer_care", "kyc_official", "courier", "voice_clone", "hang_up_and_pay"]
FLAGGED = ("PAUSE", "CRITICAL")
R = policy.RANK


def wilson(k: int, n: int, z: float = 1.96) -> dict:
    """Share k/n with a 95% Wilson confidence interval (works well near 0% and 100%)."""
    if n == 0:
        return {"k": 0, "n": 0, "rate": None, "low": None, "high": None}
    p = k / n
    d = 1 + z * z / n
    c = (p + z * z / (2 * n)) / d
    h = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / d
    return {"k": k, "n": n, "rate": p, "low": max(0.0, c - h), "high": min(1.0, c + h)}


def decide(raw, score, m, q1=None, q2=None, rules=()):
    tk = tokens(Session(**raw)) if rules else None
    return policy.decide(raw, score, m, q1=q1, q2=q2, approved_rules=rules, tokens=tk)["level"]


def score_only_level(raw, s, m):
    th = m["thresholds"]
    if s > th["critical"]:
        return "CRITICAL"
    if s > th["pause"]:
        return "PAUSE"
    if s > th["nudge"] and raw.get("amount", 0) >= m.get("nudge_min_amount", 1000):
        return "NUDGE"
    return "NONE"


def check(name, passed, detail):
    return {"name": name, "passed": bool(passed), "detail": detail}


# ---------------------------------------------------------------- reasons vs signals
def reason_ok(text: str, f: dict) -> bool:
    """Does this reason sentence describe a signal that is really present in the payment?"""
    hist = f["payments_90d"] >= 10 and f["median_90d"] > 0
    status = f["payee_status"]
    credit = f["credit_unknown_48h"]
    if text.startswith("The amount is ") and text.endswith(" times your usual payment"):
        n = int(text.split()[3])
        return hist and n >= 3 and abs(n - f["amount"] / f["median_90d"]) <= 0.5 + 1e-9
    table = {
        "You are on a call right now": f["call_now"],
        "You were on a call in the last 30 minutes": f["call_seen_30m"] and not f["call_now"],
        "Your screen is being shared or a remote-access app is active": f["screen_share"],
        "You have never paid this payee before": status == "new",
        "You first paid this payee less than 7 days ago": status == "recent",
        "You paid this payee after an earlier warning": status == "warned",
        "This account appears in a fraud report shared with your bank": status == "flagged",
        "This is a repeat payment to a payee you only started paying today": f["same_payee_24h"] > 0,
        "You paid several new accounts in the last hour": f["new_payees_60m"] > 0,
        "You recently received money from someone you have never paid": credit > 0 and not f["payee_is_sender"],
        "This sends back the same amount you received from a stranger":
            credit > 0 and abs(f["amount"] - credit) <= 0.02 * credit and not f["payee_is_sender"],
        "The payee's UPI ID was pasted from another app": f["payee_pasted"],
        "You left the app and came back during this payment": f["app_switch"],
        "It is late at night": f["night"],
        "Together with today's payments to new payees, this is unusually large":
            hist and (f["amount"] + f["sent_new_24h"]) / f["median_90d"] >= 3,
        "This is a large payment and you have little payment history": (not hist) and f["amount"] >= 5000,
    }
    return bool(table.get(text, False))


def main(test_path: Path | None = None) -> dict:
    m = scorer.load(HERE / "scam_model.json")
    test_path = test_path or HERE / "data" / "test.jsonl"
    rows = [json.loads(l) for l in open(test_path)]
    raws = [r["features"] for r in rows]
    scored = [scorer.score(f, m) for f in raws]
    scores = [s["score"] for s in scored]
    base = [decide(f, s, m) for f, s in zip(raws, scores)]
    checks = []

    # ---- 1. error bars --------------------------------------------------------------
    by_wf = {}
    for w in sorted({r["workflow"] for r in rows}):
        idx = [i for i, r in enumerate(rows) if r["workflow"] == w]
        by_wf[w] = wilson(sum(base[i] in FLAGGED for i in idx), len(idx))
    gen = [i for i, r in enumerate(rows) if r["label"] == 0]
    ins = [i for i, r in enumerate(rows) if r["workflow"] in IN_SCOPE]
    allsc = [i for i, r in enumerate(rows) if r["label"] == 1]
    error_bars = {
        "scams_flagged_in_scope6": wilson(sum(base[i] in FLAGGED for i in ins), len(ins)),
        "scams_flagged_all8": wilson(sum(base[i] in FLAGGED for i in allsc), len(allsc)),
        "genuine_interrupted": wilson(sum(base[i] in FLAGGED for i in gen), len(gen)),
        "genuine_nudged": wilson(sum(base[i] == "NUDGE" for i in gen), len(gen)),
        "by_workflow": by_wf,
    }

    # ---- 2. safety guarantees ---------------------------------------------------------
    combos = [(False, False), (False, None), (None, False), (True, None), (None, True), (True, True), (True, False), (False, True)]
    lowered, no_changed = 0, 0
    for f, s, b in zip(raws, scores, base):
        for q1, q2 in combos:
            lv = decide(f, s, m, q1, q2)
            lowered += R[lv] < R[b]
            if q1 is not True and q2 is not True:
                no_changed += lv != b
    checks.append(check("Answers never lower the level", lowered == 0,
                        f"{len(raws):,} test payments x {len(combos)} answer combinations: {lowered} lowered"))
    checks.append(check("'No' or no answer changes nothing", no_changed == 0, f"{no_changed} payments changed level after 'No'"))

    yes_crit = [decide(f, s, m, True, None) for f, s, b in zip(raws, scores, base) if R[b] >= R["PAUSE"]]
    checks.append(check("G4: 'Yes' at Pause or above gives Critical", all(l == "CRITICAL" for l in yes_crit),
                        f"{len(yes_crit):,} paused payments answered Yes: {sum(l != 'CRITICAL' for l in yes_crit)} not Critical"))

    # Only C1 may lower a level: only to Nudge (None below Rs 1,000), only with NO coaching sign at all.
    lowered_ok, lowered_bad = 0, 0
    for f, s, b in zip(raws, scores, base):
        sl = score_only_level(f, s, m)
        if R[b] < R[sl]:
            ok = not policy.coaching_signs(f) and b in ("NUDGE", "NONE") and f.get("payee_status") not in ("flagged", "warned")
            lowered_ok += ok; lowered_bad += not ok
    checks.append(check("G1-G4 never lower the model's level; only C1 does, to Nudge, with no coaching sign", lowered_bad == 0,
                        f"{lowered_ok} lowered by C1 (no call, screen share, paste, app switch or new-payee activity), {lowered_bad} wrongly"))
    popup_without_sign = sum(R[b] >= R["PAUSE"] and not policy.coaching_signs(f) and f.get("payee_status") not in ("flagged", "warned")
                             for f, b in zip(raws, base))
    checks.append(check("C1: no full-screen popup without a sign of coaching (flagged/warned payees excepted)", popup_without_sign == 0,
                        f"{popup_without_sign} found"))

    rng = random.Random(42)
    sample = rng.sample(raws, 2000)
    g1 = [decide({**f, "payee_status": "flagged"}, scorer.score({**f, "payee_status": "flagged"}, m)["score"], m) for f in sample]
    checks.append(check("G1: bank-flagged payee is always Critical", all(l == "CRITICAL" for l in g1),
                        f"2,000 payments made flagged: {sum(l != 'CRITICAL' for l in g1)} not Critical"))
    live = [{**f, "payee_status": "warned", "call_now": True} for f in sample]
    g2a = [decide(f, scorer.score(f, m)["score"], m) for f in live]
    quiet = [{**f, "payee_status": "warned", "call_now": False, "call_seen_30m": False, "screen_share": False} for f in sample]
    g2b = [decide(f, scorer.score(f, m)["score"], m) for f in quiet]
    checks.append(check("G2: warned payee + live call is Critical", all(l == "CRITICAL" for l in g2a),
                        f"{sum(l != 'CRITICAL' for l in g2a)} of 2,000 not Critical"))
    checks.append(check("G2: warned payee without a call is at least Nudge", all(R[l] >= R["NUDGE"] for l in g2b),
                        f"{sum(R[l] < R['NUDGE'] for l in g2b)} of 2,000 below Nudge"))

    disc = json.loads((HERE / "discovery_unseen_sent_by_mistake.json").read_text())
    rules = [c["proposed_rule"] for c in disc["clusters"] if c.get("suggestion") == "approve" and c.get("proposed_rule")]
    matched = [(f, s) for f, s in zip(raws, scores) if rules and any(policy.matches_rule(tokens(Session(**f)), r) for r in rules)]
    g3 = [decide(f, s, m, rules=rules) for f, s in matched]
    checks.append(check("G3: an approved analyst rule gives at least Pause", len(g3) > 0 and all(R[l] >= R["PAUSE"] for l in g3),
                        f"rule(s) {[r['id'] for r in rules]} matched {len(g3):,} test payments: "
                        f"{sum(R[l] < R['PAUSE'] for l in g3)} below Pause"))

    small_nudge = sum(b == "NUDGE" and f["amount"] < 1000 and f["payee_status"] != "warned" for f, b in zip(raws, base))
    checks.append(check("No Nudge below Rs 1,000 (except warned payee, G2)", small_nudge == 0, f"{small_nudge} found"))

    q_input = sum(featurize({**f, "q1": True, "q2": True}) != featurize(f) for f in sample)
    checks.append(check("Q1/Q2 answers are never model inputs", q_input == 0, f"{q_input} of 2,000 changed the model input"))

    bad_reason, too_many, examples = 0, 0, []
    for f, s in zip(raws, scored):
        too_many += len(s["reasons"]) > 3
        for t in s["reasons"]:
            if not reason_ok(t, f):
                bad_reason += 1
                if len(examples) < 3:
                    examples.append({"reason": t, "payment": f})
    checks.append(check("Reasons only name signals that are present (and at most 3)", bad_reason == 0 and too_many == 0,
                        f"{sum(len(s['reasons']) for s in scored):,} reasons checked: {bad_reason} wrong, {too_many} payments with >3"
                        + (f"; examples {examples}" if examples else "")))

    again = sum(scorer.score(f, m)["score"] != s for f, s in zip(sample, [scorer.score(f, m)["score"] for f in sample]))
    checks.append(check("Scoring is deterministic", again == 0, f"{again} of 2,000 differed on a second run"))

    # the 28 stories: answers only raise, on the designed sessions
    cases = json.loads((HERE / "round1" / "cases.json").read_text())
    story_low = 0
    for c in cases:
        f = {k: v for k, v in c["session"].items() if k not in ("q1", "q2")}
        s = scorer.score(f, m)["score"]
        b = decide(f, s, m)
        story_low += any(R[decide(f, s, m, q1, q2)] < R[b] for q1, q2 in combos)
    checks.append(check("28 stories: answers never lower the level", story_low == 0, f"{story_low} stories lowered"))

    # ---- 3. edge cases ------------------------------------------------------------------
    edges = {
        "Rs 0": {"amount": 0},
        "Rs 1": {"amount": 1},
        "Rs 100 crore, tiny median": {"amount": 1_000_000_000, "median_90d": 1, "payments_90d": 500},
        "No payment history": {"amount": 20000, "median_90d": 0, "payments_90d": 0},
        "Only the amount given": {"amount": 48000},
        "Fields set to None": {"amount": 5000, "median_90d": None, "call_now": None, "payee_status": None},
        "Unknown payee status/kind": {"amount": 5000, "median_90d": 600, "payee_status": "other", "payee_kind": "other"},
        "Huge counts": {"amount": 3000, "median_90d": 600, "payee_status": "new", "same_payee_24h": 999, "new_payees_60m": 999},
        "Credit exactly equal to amount": {"amount": 5000, "median_90d": 2500, "payee_status": "new", "credit_unknown_48h": 5000},
        "Huge money already sent today": {"amount": 1000, "median_90d": 600, "payee_status": "new", "sent_new_24h": 10**9},
    }
    edge_rows, edge_fail = [], 0
    for name, raw in edges.items():
        try:
            s = scorer.score(raw, m)
            full = {**json.loads(json.dumps(m["preprocessing"]["raw_defaults"])), **{k: v for k, v in raw.items() if v is not None}}
            lv = decide(full, s["score"], m)
            ok = 0.0 <= s["score"] <= 1.0 and lv in R
            edge_rows.append({"case": name, "score": round(s["score"], 4), "level": lv, "reasons": s["reasons"], "ok": ok})
        except Exception as e:  # noqa: BLE001
            ok = False
            edge_rows.append({"case": name, "error": repr(e), "ok": False})
        edge_fail += not ok
    checks.append(check("Edge cases score without crashing (score 0-1, valid level)", edge_fail == 0,
                        f"{len(edges)} cases, {edge_fail} failed"))
    try:
        scorer.score({"amount": -500}, m)
        negative = "scored without error"
    except ValueError as e:
        negative = f"rejected with {type(e).__name__} (the app must only score amounts > 0; on any error it lets the payment through)"

    # ---- 4. fairness: who gets interrupted by mistake? (Round 1 PDF: compare first-time users) ----
    hist = lambda f: f["payments_90d"] >= 10 and f["median_90d"] > 0
    groups = {
        "First-time users (little history)": lambda f: not hist(f),
        "Regular users": hist,
        "Paying a person": lambda f: f["payee_kind"] == "person",
        "Paying a merchant": lambda f: f["payee_kind"] == "merchant",
        "Night (23:00-06:00)": lambda f: f["night"],
        "Day": lambda f: not f["night"],
    }
    fairness = {}
    for name, fn in groups.items():
        idx = [i for i in gen if fn(raws[i])]
        fairness[name] = wilson(sum(base[i] in FLAGGED for i in idx), len(idx))

    # ---- 5. speed (Python reference scorer; the phone is measured separately) --------------
    t0 = time.perf_counter()
    for f in sample[:1000]:
        scorer.score(f, m)
    py_ms = (time.perf_counter() - t0) / 1000 * 1000

    out = {"model_version": m["model_version"], "test_file": str(test_path.name), "test_payments": len(rows),
           "error_bars_95": error_bars, "fairness_genuine_interrupted": fairness, "checks": checks, "edge_cases": edge_rows, "negative_amount": negative,
           "python_scorer_ms_per_payment": round(py_ms, 3)}
    (HERE / "results").mkdir(exist_ok=True)
    (HERE / "results" / "extra_tests.json").write_text(json.dumps(out, indent=1, default=str), encoding="utf-8")
    for c in checks:
        print(f"{'PASS' if c['passed'] else 'FAIL'}  {c['name']}  ({c['detail']})")
    eb = error_bars
    print(f"in-scope scams flagged {eb['scams_flagged_in_scope6']['rate']:.1%} "
          f"[{eb['scams_flagged_in_scope6']['low']:.1%}, {eb['scams_flagged_in_scope6']['high']:.1%}]; "
          f"genuine interrupted {eb['genuine_interrupted']['rate'] * 1000:.1f}/1000 "
          f"[{eb['genuine_interrupted']['low'] * 1000:.1f}, {eb['genuine_interrupted']['high'] * 1000:.1f}]")
    print(f"python scorer {py_ms:.2f} ms/payment; negative amount: {negative}")
    return out


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main(Path(sys.argv[1]) if len(sys.argv) > 1 else None)
