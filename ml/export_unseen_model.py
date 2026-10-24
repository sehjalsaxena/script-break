"""Export the "before the new scam wave" model, used ONLY by the Analyst demo in the app.

The phone's real model (scam_model.json) was trained on all 8 scam types, so it already catches
"sent by mistake". To show the analyst loop honestly (missed -> AI finds the pattern -> analyst
approves -> caught), the Analyst tab uses the exact model analyst_discovery.py --unseen builds:
the same settings, trained WITHOUT one scam type, thresholds set on validation the same way.

Self-check: scored with the exported JSON + policy.py, this model must miss exactly the reports
listed in discovery_unseen_<type>.json (same total, same count per scam type).

Run (after make_dataset.py):  python export_unseen_model.py [scam_type]     default: sent_by_mistake
Output: scam_model_unseen_<type>.json  (copy to "andriod app/app/src/main/assets/")
"""
from __future__ import annotations

import json
import sys
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

import numpy as np
from sklearn.ensemble import GradientBoostingClassifier

import policy
import scorer
from analyst_discovery import REPORTS
from export_model import tree_to_json
from features import featurize
from train import TARGET_INTERRUPT, load, threshold_for

HERE = Path(__file__).parent
sys.path.insert(0, str(HERE / "round1"))
from generator import scam_stream  # noqa: E402


def main(unseen: str = "sent_by_mistake"):
    Xtr, ytr, wtr = load("train")
    Xva, yva, wva = load("val")
    keep = wtr != unseen
    # identical to analyst_discovery.py --unseen
    model = GradientBoostingClassifier(n_estimators=200, max_depth=3, learning_rate=0.1, subsample=0.8,
                                       random_state=7).fit(Xtr[keep], ytr[keep])
    pv = model.predict_proba(Xva[wva == "genuine"])[:, 1]
    thresholds = {"nudge": threshold_for(pv, 33.0), "pause": threshold_for(pv, TARGET_INTERRUPT),
                  "critical": threshold_for(pv, 0.5)}

    base = json.loads((HERE / "scam_model.json").read_text())   # same features, preprocessing, reasons
    p0 = float(model.init_.class_prior_[1])
    out = {**base,
           "model_version": datetime.now(timezone.utc).strftime("%Y%m%d-%H%M") + f"-without-{unseen}",
           "thresholds": thresholds,
           "trained_on": f"synthetic sessions WITHOUT the '{unseen}' scam type. Analyst demo only: "
                         "a scam wave this model has never seen. Not the phone's real model.",
           "gb": {"init_raw": float(np.log(p0 / (1 - p0))), "learning_rate": float(model.learning_rate),
                  "trees": [tree_to_json(est[0], lambda v: v[0][0]) for est in model.estimators_]}}
    path = HERE / f"scam_model_unseen_{unseen}.json"
    path.write_text(json.dumps(out, separators=(",", ":")))

    # parity: the JSON gives the same scores as the trained model
    sk = model.predict_proba(Xva[:2000])[:, 1]
    js = np.array([scorer.score_vector(list(x), out)[0] for x in Xva[:2000]])
    print(f"wrote {path.name} ({path.stat().st_size / 1024:.0f} KB); parity max diff {np.abs(sk - js).max():.1e}")

    # self-check against the discovery file: same reports, same misses
    disc = json.loads((HERE / f"discovery_unseen_{unseen}.json").read_text())
    missed = Counter()
    for i, (w, n) in enumerate(REPORTS.items()):
        for s in scam_stream(w, n, seed=701 + i):
            raw = s.__dict__
            lv = policy.decide(raw, scorer.score_vector(featurize(raw), out)[0], out)["level"]
            if policy.RANK[lv] < 2:
                missed[w] += 1
    ok = dict(missed) == disc["missed_by_type"]
    print(f"misses {sum(missed.values())} {dict(missed)}")
    print(f"discovery file {disc['missed_by_model']} {disc['missed_by_type']}")
    print("SELF-CHECK", "PASSED: same misses as the discovery file" if ok else "FAILED")
    if not ok:
        raise SystemExit(1)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "sent_by_mistake")
