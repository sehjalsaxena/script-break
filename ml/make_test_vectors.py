"""Parity check: the exported JSON (via scorer.py) must give the same score as the trained
sklearn model. Then save 500 sample payments + expected scores to test_vectors.json, so the
Android team can check ScamModel.kt gives the same numbers (ScamModelParityTest)."""
from __future__ import annotations

import json
from pathlib import Path

import joblib
import numpy as np

import scorer
from features import featurize

HERE = Path(__file__).parent


def write_vectors(name: str, n: int = 500):
    m = scorer.load()
    model = joblib.load(HERE / "models" / f"{name}.joblib")
    rows = []
    with open(HERE / "data" / "test.jsonl") as f:
        for i, line in enumerate(f):
            if i >= n:
                break
            rows.append(json.loads(line))
    X = np.array([featurize(r["features"]) for r in rows])
    sk = model.predict_proba(X)[:, 1]
    js = np.array([scorer.score_vector(list(x), m)[0] for x in X])
    worst = float(np.max(np.abs(sk - js)))
    print(f"parity: max |sklearn - json scorer| over {n} test payments = {worst:.2e}")
    if worst > 1e-6:
        raise SystemExit("PARITY FAILED: the exported model does not match the trained model")
    vectors = [{"features": r["features"], "expected_score": float(s), "expected_reasons": scorer.score(r["features"], m)["reasons"]}
               for r, s in zip(rows, js)]
    (HERE / "test_vectors.json").write_text(json.dumps({"model_version": m["model_version"], "tolerance": 1e-6, "vectors": vectors}))
    print(f"wrote test_vectors.json ({n} payments) for the Kotlin parity test")
