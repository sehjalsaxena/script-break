"""Make the labelled synthetic datasets for the ScriptBreak scam-score model.

Labels come from the SCENARIO we generated (scam workflow = 1, genuine = 0),
never from the old rule engine. The generator and every assumption behind it
are in round1/generator.py (Round 1 code, declared on the disclosure form).

Outputs (data/):
  train.jsonl       1,40,000 genuine + 10,000 genuine lookalikes + 2,100 per scam type
  val.jsonl           30,000 genuine + 450 per scam type          (pick model + thresholds)
  test.jsonl          30,000 genuine + 450 per scam type, SHIFTED settings (final numbers, used once)
  summary.json      counts per split and workflow

Totals: 2,00,000 genuine (+10,000 extra lookalikes in training) and 3,000 per scam type
(8 types = 24,000), the sizes declared on the form.

Run:  python make_dataset.py
"""
from __future__ import annotations

import json
import math
import random
import sys
from collections import Counter
from dataclasses import asdict
from pathlib import Path

HERE = Path(__file__).parent
sys.path.insert(0, str(HERE / "round1"))
from generator import SCAMS, legit_session, scam_session  # noqa: E402

OUT = HERE / "data"
RAW_FIELDS = ["amount", "median_90d", "payments_90d", "call_now", "call_seen_30m", "screen_share",
              "payee_status", "payee_kind", "same_payee_24h", "new_payees_60m", "sent_new_24h",
              "credit_unknown_48h", "payee_is_sender", "payee_pasted", "app_switch", "night"]

# Test set: different random seeds AND shifted scam settings, so a good score means the
# model learned the pattern, not the exact numbers of the training generator.
TEST_SHIFT = {
    "digital_arrest": {"ratio": (math.log(20), 1.0), "app_switch": 0.4},
    "customer_care": {"screen_share": 0.25, "ratio": (math.log(5), 1.0)},
    "kyc_official": {"call_now": 0.7, "pasted": 0.4},
    "courier": {"ratio": (math.log(10), 1.0)},
    "voice_clone": {"call_now": 0.9, "ratio": (math.log(9), 0.9)},
    "hang_up_and_pay": {"call_seen_30m": 0.25, "ratio": (math.log(15), 1.0)},
    "sent_by_mistake": {"call_now": 0.4, "ratio": (math.log(4.0), 0.9), "pasted": 0.6},
    "chat_task": {"many_new": 0.3},
}
TEST_CALL_SCALE = 1.2   # genuine people make 20% more calls while paying new payees in the test set


def record(sid: str, workflow: str, label: int, session) -> dict:
    d = asdict(session)
    return {"scenario_id": sid, "workflow": workflow,
            "features": {k: d[k] for k in RAW_FIELDS}, "label": label}


def is_lookalike(s) -> bool:
    """A genuine payment that LOOKS risky: new payee plus a call, screen share or a big amount."""
    new = s.payee_status in ("new", "recent")
    big = s.median_90d > 0 and s.amount >= 5 * s.median_90d
    return new and (s.call_now or s.call_seen_30m or s.screen_share or big or s.payee_pasted)


def make_split(name: str, seed: int, n_legit: int, n_lookalike: int, n_scam_each: int,
               shift: dict | None = None, call_scale: float = 1.0) -> list:
    rng = random.Random(seed)
    rows = []
    for i in range(n_legit):
        rows.append(record(f"{name}_g{i:06d}", "genuine", 0, legit_session(rng, call_scale=call_scale)))
    i = 0
    while i < n_lookalike:   # extra tricky genuine payments, so the model doesn't learn "new payee = scam"
        s = legit_session(rng, call_scale=call_scale)
        if is_lookalike(s):
            rows.append(record(f"{name}_l{i:06d}", "genuine_lookalike", 0, s))
            i += 1
    for w in SCAMS:
        for j in range(n_scam_each):
            s = scam_session(rng, w, (shift or {}).get(w))
            rows.append(record(f"{name}_{w}_{j:05d}", w, 1, s))
    rng.shuffle(rows)
    return rows


def main():
    OUT.mkdir(exist_ok=True)
    splits = {
        "train": make_split("train", seed=1001, n_legit=140_000, n_lookalike=10_000, n_scam_each=2_100),
        "val": make_split("val", seed=2002, n_legit=30_000, n_lookalike=0, n_scam_each=450),
        "test": make_split("test", seed=3003, n_legit=30_000, n_lookalike=0, n_scam_each=450,
                           shift=TEST_SHIFT, call_scale=TEST_CALL_SCALE),
    }
    summary = {}
    for name, rows in splits.items():
        with open(OUT / f"{name}.jsonl", "w") as f:
            for r in rows:
                f.write(json.dumps(r) + "\n")
        c = Counter(r["workflow"] for r in rows)
        summary[name] = {"rows": len(rows), "scams": sum(r["label"] for r in rows), "by_workflow": dict(c)}
        print(f"{name:5} {len(rows):7,} rows  scams {summary[name]['scams']:6,}  {dict(c)}")
    summary["test_shift"] = {k: {kk: (list(vv) if isinstance(vv, tuple) else vv) for kk, vv in v.items()}
                             for k, v in TEST_SHIFT.items()}
    summary["test_call_scale"] = TEST_CALL_SCALE
    (OUT / "summary.json").write_text(json.dumps(summary, indent=1))


if __name__ == "__main__":
    main()
