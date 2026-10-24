"""Level parity: the phone's DECISION must equal the Python safety layer, not only the score.

test_vectors.json (make_test_vectors.py) proves ScamModel.kt gives the same scores and reasons as
scorer.py. This file proves ScamModel.assess gives the same LEVEL and the same safety-layer steps
(C1, G1-G5, the analyst rule) as policy.decide, which is what every reported number is based on.

Cases: 600 test payments (fixed random sample, all levels), plus hand-made edge cases around the
C1 and G5 boundaries, each with 4 answer combinations, with and without the approved analyst rule
(the "sent by mistake" rule from discovery_unseen_sent_by_mistake.json): 5,000+ decisions.
Writes level_vectors.json, checked by PolicyParityTest.kt in the Android unit tests.
"""
from __future__ import annotations

import json
import random
import sys
from pathlib import Path

import policy
import scorer

HERE = Path(__file__).parent
sys.path.insert(0, str(HERE / "round1"))
from engine import Session, tokens  # noqa: E402

BASE = {"amount": 1000, "median_90d": 650, "payments_90d": 42, "call_now": False, "call_seen_30m": False,
        "screen_share": False, "payee_status": "new", "payee_kind": "person", "same_payee_24h": 0,
        "new_payees_60m": 0, "sent_new_24h": 0, "credit_unknown_48h": 0, "payee_is_sender": False,
        "payee_pasted": False, "app_switch": False, "night": False}

# Hand-made edge cases: the exact C1 / G5 boundaries and every guardrail
EDGES = [
    {"amount": 20000},                                    # G5: 30.8x, Rs 20,000
    {"amount": 19500},                                    # exactly 30x but under Rs 20,000: not G5
    {"amount": 20000, "median_90d": 667},                 # Rs 20,000 but 29.99x: not G5
    {"amount": 35000, "night": True},                     # the "stranger, no call" case
    {"amount": 35000, "payee_status": "recent"},          # G5 also for a payee first paid this week
    {"amount": 35000, "payee_status": "known"},           # paid before: not G5
    {"amount": 90000, "payee_kind": "merchant"},          # the TV in a shop: C1, amber line only
    {"amount": 90000, "payee_kind": "merchant", "payee_pasted": True},
    {"amount": 90000, "payee_kind": "biller"},
    {"amount": 40000, "payments_90d": 5},                 # little history: G5 needs history
    {"amount": 800, "call_now": True, "screen_share": True},          # under Rs 1,000
    {"amount": 600, "payee_status": "flagged"},           # G1
    {"amount": 5000, "payee_status": "warned"},           # G2 without a call
    {"amount": 5000, "payee_status": "warned", "call_now": True},     # G2 with a call
    {"amount": 5000, "credit_unknown_48h": 5000, "call_now": True},   # "sent by mistake" (rule)
    {"amount": 5000, "credit_unknown_48h": 5000, "payee_is_sender": True},   # paying back the real sender
    {"amount": 10000, "credit_unknown_48h": 10000, "median_90d": 1200, "payments_90d": 40},
    {"amount": 25000, "app_switch": True},
    {"amount": 3000, "new_payees_60m": 2, "payee_pasted": True},
    {"amount": 3500, "same_payee_24h": 2, "sent_new_24h": 7000, "call_now": True},
]
COMBOS = [(None, None), (True, None), (None, True), (False, False)]


def main(n: int = 600):
    m = scorer.load()
    disc = json.loads((HERE / "discovery_unseen_sent_by_mistake.json").read_text(encoding="utf-8"))
    pr = next(c["proposed_rule"] for c in disc["clusters"] if c.get("proposed_rule"))
    rule = {"id": pr["id"], "all": pr["all"], "none": pr["none"]}
    rows = [json.loads(line) for line in open(HERE / "data" / "test.jsonl")]
    raws = [r["features"] for r in random.Random(2026).sample(rows, n)] + [{**BASE, **e} for e in EDGES]
    vectors = []
    for raw in raws:
        s = scorer.score(raw, m)["score"]
        tk = tokens(Session(**raw))
        results = []
        for q1, q2 in COMBOS:
            for with_rule in (False, True):
                d = policy.decide(raw, s, m, q1, q2, approved_rules=[rule] if with_rule else [], tokens=tk if with_rule else None)
                results.append({"q1": q1, "q2": q2, "rule": with_rule, "level": d["level"], "guardrails": d["guardrails"]})
        vectors.append({"features": raw, "score": s, "results": results})
    out = {"model_version": m["model_version"], "rule": rule, "vectors": vectors}
    (HERE / "level_vectors.json").write_text(json.dumps(out), encoding="utf-8")
    levels = sorted({r["level"] for v in vectors for r in v["results"]})
    steps = sorted({g for v in vectors for r in v["results"] for g in r["guardrails"]})
    print(f"wrote level_vectors.json: {len(vectors)} payments x {len(COMBOS) * 2} = "
          f"{len(vectors) * len(COMBOS) * 2} decisions; levels {levels}; steps seen {steps}")


if __name__ == "__main__":
    main()
