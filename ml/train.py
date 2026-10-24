"""Train three candidate models, choose thresholds on validation data, pick the best.

  1. Logistic Regression  - simple baseline, easiest to explain
  2. Random Forest        - learns combinations of signals
  3. Gradient Boosting    - usually strongest on table data

For each model, the Pause threshold T2 is set on the VALIDATION set so that at most
TARGET_INTERRUPT genuine payments per 1,000 are interrupted (budget is 8; we aim lower
to leave a margin for the shifted test set). The model that catches the most validation
scams at that threshold wins. The test set is NOT used here.

Outputs (models/): <name>.joblib for each model, thresholds.json, selection.json
Run:  python train.py
"""
from __future__ import annotations

import json
import time
from pathlib import Path

import joblib
import numpy as np
from sklearn.ensemble import GradientBoostingClassifier, RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

from features import FEATURES, featurize

HERE = Path(__file__).parent
DATA, MODELS = HERE / "data", HERE / "models"

TARGET_INTERRUPT = 6.5    # genuine Pause-or-Critical per 1,000 on validation (budget: 8)
TARGET_NUDGE = 33.0       # genuine payments that get a Nudge or more, per 1,000 (about 3.3%)
TARGET_CRITICAL = 0.5     # genuine payments sent straight to Critical by the score alone, per 1,000


def load(name: str):
    X, y, wf = [], [], []
    with open(DATA / f"{name}.jsonl") as f:
        for line in f:
            r = json.loads(line)
            X.append(featurize(r["features"]))
            y.append(r["label"])
            wf.append(r["workflow"])
    return np.array(X, dtype=np.float64), np.array(y), np.array(wf)


def threshold_for(genuine_scores: np.ndarray, per_1000: float) -> float:
    """Lowest score threshold that flags at most `per_1000` genuine payments per 1,000."""
    s = np.sort(genuine_scores)[::-1]
    k = int(np.floor(per_1000 / 1000 * len(s)))
    if k >= len(s):
        return 0.0
    # flag when score > threshold; place it just above the (k+1)-th highest genuine score
    return float(np.nextafter(s[k], 1.0))


CANDIDATES = {
    "logistic_regression": lambda: make_pipeline(StandardScaler(), LogisticRegression(max_iter=2000, C=1.0)),
    "random_forest": lambda: RandomForestClassifier(n_estimators=100, max_depth=10, min_samples_leaf=20,
                                                    n_jobs=-1, random_state=7),
    "gradient_boosting": lambda: GradientBoostingClassifier(n_estimators=200, max_depth=3, learning_rate=0.1,
                                                            subsample=0.8, random_state=7),
}


def main():
    MODELS.mkdir(exist_ok=True)
    Xtr, ytr, _ = load("train")
    Xva, yva, wva = load("val")
    print(f"train {Xtr.shape}, val {Xva.shape}, {len(FEATURES)} features")
    g = wva == "genuine"
    results = {}
    for name, make in CANDIDATES.items():
        t0 = time.time()
        model = make().fit(Xtr, ytr)
        p = model.predict_proba(Xva)[:, 1]
        t_pause = threshold_for(p[g], TARGET_INTERRUPT)
        t_nudge = min(threshold_for(p[g], TARGET_NUDGE), t_pause)
        t_crit = max(threshold_for(p[g], TARGET_CRITICAL), t_pause)
        flagged = p > t_pause
        recall = float(flagged[yva == 1].mean())
        by_wf = {w: float(flagged[wva == w].mean()) for w in sorted(set(wva)) if w != "genuine"}
        results[name] = {"val_scams_flagged": recall, "val_genuine_interrupted_per_1000": float(flagged[g].mean() * 1000),
                         "thresholds": {"nudge": t_nudge, "pause": t_pause, "critical": t_crit},
                         "val_flagged_by_workflow": by_wf, "train_seconds": round(time.time() - t0, 1)}
        joblib.dump(model, MODELS / f"{name}.joblib")
        print(f"{name:20} scams flagged {recall:6.1%}  genuine interrupted {flagged[g].mean() * 1000:4.1f}/1000  "
              f"T(nudge,pause,crit)=({t_nudge:.3f}, {t_pause:.3f}, {t_crit:.3f})  {results[name]['train_seconds']}s")
    best = max(results, key=lambda n: results[n]["val_scams_flagged"])
    print(f"\nSelected on validation: {best}")
    (MODELS / "selection.json").write_text(json.dumps({"selected": best, "targets": {
        "interrupt_per_1000": TARGET_INTERRUPT, "nudge_per_1000": TARGET_NUDGE, "critical_per_1000": TARGET_CRITICAL},
        "candidates": results}, indent=1))


if __name__ == "__main__":
    main()
