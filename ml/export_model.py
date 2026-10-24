"""Export the selected model to scam_model.json, the file the Android app loads.

The JSON holds everything the phone needs, with no Python and no ML library:
feature order, preprocessing constants, the model itself (trees or weights),
the thresholds and the reason texts. ScamModel.kt reads it; scorer.py is the
same logic in Python, used to check that the two agree (test_vectors.json).

Run:  python export_model.py            (exports the model chosen in models/selection.json)
      python export_model.py random_forest   (export a different candidate)
"""
from __future__ import annotations

import json
import sys
from datetime import datetime, timezone
from pathlib import Path

import joblib
import numpy as np

import features as F

HERE = Path(__file__).parent
MODELS = HERE / "models"


def tree_to_json(tree, value_fn) -> dict:
    t = tree.tree_
    return {
        "feature": [int(x) for x in t.feature],          # -2 = leaf
        "threshold": [round(float(x), 9) for x in t.threshold],
        "left": [int(x) for x in t.children_left],        # -1 = leaf
        "right": [int(x) for x in t.children_right],
        "value": [round(float(value_fn(t.value[i])), 9) for i in range(t.node_count)],
    }


def export(name: str) -> dict:
    sel = json.loads((MODELS / "selection.json").read_text())
    model = joblib.load(MODELS / f"{name}.joblib")
    th = sel["candidates"][name]["thresholds"]
    out = {
        "format": "scriptbreak-scam-model/1",
        "model_version": datetime.now(timezone.utc).strftime("%Y%m%d-%H%M"),
        "model_type": name,
        "features": F.FEATURES,
        "preprocessing": {"min_history": F.MIN_HISTORY, "credit_match": F.CREDIT_MATCH, "ratio_cap": F.RATIO_CAP,
                          "count_cap": 5, "payee_statuses": F.PAYEE_STATUSES, "payee_kinds": F.PAYEE_KINDS,
                          "raw_defaults": F.RAW_DEFAULTS},
        "thresholds": th,
        "nudge_min_amount": 1000,
        "reasons": F.REASONS,
        "trained_on": "synthetic sessions only (make_dataset.py); see MODEL_CARD.md",
    }
    if name == "gradient_boosting":
        p0 = float(model.init_.class_prior_[1])
        out["gb"] = {"init_raw": float(np.log(p0 / (1 - p0))), "learning_rate": float(model.learning_rate),
                     "trees": [tree_to_json(est[0], lambda v: v[0][0]) for est in model.estimators_]}
    elif name == "random_forest":
        out["rf"] = {"trees": [tree_to_json(est, lambda v: v[0][1] / v[0].sum()) for est in model.estimators_]}
    elif name == "logistic_regression":
        sc, lr = model.named_steps["standardscaler"], model.named_steps["logisticregression"]
        out["lr"] = {"mean": sc.mean_.tolist(), "scale": sc.scale_.tolist(),
                     "coef": lr.coef_[0].tolist(), "intercept": float(lr.intercept_[0])}
    else:
        raise SystemExit(f"unknown model {name}")
    return out


def main():
    name = sys.argv[1] if len(sys.argv) > 1 else json.loads((MODELS / "selection.json").read_text())["selected"]
    out = export(name)
    path = HERE / "scam_model.json"
    path.write_text(json.dumps(out, separators=(",", ":")))
    print(f"wrote {path.name}: {name}, {path.stat().st_size / 1024:.0f} KB, thresholds {out['thresholds']}")

    # parity check against the trained sklearn model, then write test vectors for Kotlin
    import scorer
    from make_test_vectors import write_vectors
    write_vectors(name)


if __name__ == "__main__":
    main()
