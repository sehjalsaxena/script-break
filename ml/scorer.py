"""Reference scorer: reads scam_model.json and scores one payment, with reasons.

This is line-for-line the same logic as ScamModel.kt (the Android scorer). It needs
no ML library, which is how we know the JSON file alone is enough for the phone.

score(raw_features) -> {"score": 0.92, "contributions": {feature: value}, "reasons": [...]}
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from features import featurize

HERE = Path(__file__).parent
_MODEL = None


def load(path: Path | None = None) -> dict:
    global _MODEL
    _MODEL = json.loads(Path(path or HERE / "scam_model.json").read_text())
    return _MODEL


def _walk(tree: dict, x: list, contrib: list, scale: float) -> float:
    """Follow one tree to its leaf. Each split adds (child value - parent value) to the
    feature it split on (path attribution), so contributions sum to leaf - root."""
    node = 0
    while tree["left"][node] != -1:
        f = tree["feature"][node]
        child = tree["left"][node] if x[f] <= tree["threshold"][node] else tree["right"][node]
        contrib[f] += scale * (tree["value"][child] - tree["value"][node])
        node = child
    return tree["value"][node]


def sigmoid(z: float) -> float:
    return 1.0 / (1.0 + math.exp(-z)) if z >= 0 else math.exp(z) / (1.0 + math.exp(z))


def score_vector(x: list, m: dict | None = None) -> tuple[float, list]:
    m = m or _MODEL or load()
    contrib = [0.0] * len(m["features"])
    t = m["model_type"]
    if t == "gradient_boosting":
        gb = m["gb"]
        raw = gb["init_raw"]
        for tree in gb["trees"]:
            raw += gb["learning_rate"] * _walk(tree, x, contrib, gb["learning_rate"])
            # the root values of GB trees add to the bias, not to any feature
        return sigmoid(raw), contrib            # contributions in log-odds units
    if t == "random_forest":
        trees = m["rf"]["trees"]
        p = sum(_walk(tree, x, contrib, 1.0 / len(trees)) for tree in trees) / len(trees)
        return p, contrib                       # contributions in probability units
    if t == "logistic_regression":
        lr = m["lr"]
        z = lr["intercept"]
        for i, xi in enumerate(x):
            c = lr["coef"][i] * (xi - lr["mean"][i]) / lr["scale"][i]
            contrib[i] = c
            z += c
        return sigmoid(z), contrib
    raise ValueError(t)


# Features are grouped the way a person thinks about a payment. A group's push on the score is
# the sum of its features' contributions; the top groups become the reasons on screen.
GROUPS = {
    "amount": ["log_amount", "has_history", "log_ratio", "log_ratio_24h", "log_sent_new_24h"],
    "payee": ["status_known", "status_new", "status_recent", "status_warned", "status_flagged",
              "kind_person", "kind_merchant", "kind_biller"],
    "call": ["call_now", "call_seen_30m"],
    "screen_share": ["screen_share"],
    "repeat": ["same_payee_24h"],
    "fan_out": ["new_payees_60m"],
    "credit": ["credit_unknown", "amount_matches_credit", "payee_is_sender"],
    "pasted": ["payee_pasted"],
    "app_switch": ["app_switch"],
    "night": ["night"],
}


def _group_text(group: str, f: dict, texts: dict) -> str | None:
    """The sentence for a group, or None if nothing in it is actually present."""
    if group == "amount":
        if f["has_history"] and f["log_ratio"] >= math.log1p(3.0):
            n = math.expm1(f["log_ratio"])
            return f"The amount is {n:.0f} times your usual payment"
        if f["has_history"] and f["log_ratio_24h"] >= math.log1p(3.0):
            return texts["log_ratio_24h"]
        if not f["has_history"] and f["log_amount"] >= math.log1p(5000):
            return "This is a large payment and you have little payment history"
        return None
    if group == "payee":
        for s in ("flagged", "warned", "new", "recent"):
            if f[f"status_{s}"]:
                return texts[f"status_{s}"]
        return None
    if group == "call":
        if f["call_now"]:
            return texts["call_now"]
        return texts["call_seen_30m"] if f["call_seen_30m"] else None
    if group == "credit":
        if f["amount_matches_credit"] and not f["payee_is_sender"]:
            return texts["amount_matches_credit"]
        return texts["credit_unknown"] if f["credit_unknown"] and not f["payee_is_sender"] else None
    name = GROUPS[group][0]
    return texts[name] if f[name] > 0 else None


def reasons_from(x: list, contrib: list, m: dict, top: int = 3) -> list:
    """Plain-language reasons: groups that pushed the score UP and are actually present."""
    feats, texts = m["features"], m["reasons"]
    f = dict(zip(feats, x))
    c = dict(zip(feats, contrib))
    pushes = {g: sum(c[n] for n in names) for g, names in GROUPS.items()}
    out = []
    for g in sorted(GROUPS, key=lambda g: -pushes[g]):
        if pushes[g] <= 0 or len(out) >= top:
            break
        t = _group_text(g, f, texts)
        if t:
            out.append(t)
    return out


def score(raw: dict, m: dict | None = None) -> dict:
    m = m or _MODEL or load()
    x = featurize(raw)
    p, contrib = score_vector(x, m)
    return {"score": p, "contributions": dict(zip(m["features"], contrib)), "reasons": reasons_from(x, contrib, m)}
