"""Make the small labelled sample test file bundled in the Android app (Judge mode > Test file > Our sample file).

It is a fixed, seeded sample of the TEST split (new seeds, shifted settings, never used in training):
200 genuine payments + 25 of each of the 8 scam types = 400 payments, in the schema.json record format.

Run (after make_dataset.py):  python make_sample_kit.py [path/to/test.jsonl]
Output: sample_kit.jsonl   (copy it to "andriod app/app/src/main/assets/sample_kit.jsonl")
"""
from __future__ import annotations

import json
import random
import sys
from pathlib import Path

HERE = Path(__file__).parent
N_GENUINE, N_PER_SCAM = 200, 25


def main():
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else HERE / "data" / "test.jsonl"
    rows = [json.loads(l) for l in open(src)]
    rng = random.Random(2024)
    genuine = [r for r in rows if r["label"] == 0]
    out = rng.sample(genuine, N_GENUINE)
    for w in sorted({r["workflow"] for r in rows if r["label"] == 1}):
        out += rng.sample([r for r in rows if r["workflow"] == w], N_PER_SCAM)
    rng.shuffle(out)
    with open(HERE / "sample_kit.jsonl", "w", newline="\n") as f:
        for r in out:
            f.write(json.dumps({"scenario_id": r["scenario_id"], "workflow": r["workflow"],
                                "features": r["features"], "label": r["label"]}) + "\n")
    print(f"wrote sample_kit.jsonl: {len(out)} payments ({N_GENUINE} genuine + {N_PER_SCAM} x scam types) from {src.name}")


if __name__ == "__main__":
    main()
