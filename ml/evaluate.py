"""Honest evaluation of the scam-score model. Writes results/report.json and results/REPORT.md.

Everything here uses data the model NEVER trained on:
  1. Test set (shifted settings, new seeds): scams flagged per type, genuine payments
     interrupted per 1,000, precision, calibration, Nudge rate.
  2. Same test set, compared with the Round 1 hand-written rules (v0.3) and a bank-style
     amount rule, so "AI vs rules" is a fair, same-data comparison.
  3. Hidden-scam-type test: retrain WITHOUT one scam type, then test on that type.
     Shows whether the model catches a kind of scam it has never seen.
  4. The 28 Round 1 behaviour stories (cases.json), through model + safety layer.
  5. Failure list: scams it misses and genuine payments it wrongly interrupts.

Run:  python evaluate.py           (about 3 minutes; add --quick to skip the hidden-type test)
"""
from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path

import joblib
import numpy as np
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.metrics import brier_score_loss, roc_auc_score

import policy
import scorer
from features import featurize
from train import TARGET_INTERRUPT, load, threshold_for

HERE = Path(__file__).parent
sys.path.insert(0, str(HERE / "round1"))
import engine as rules  # noqa: E402  (Round 1 hand-written rules, for comparison only)
from engine import Session  # noqa: E402

OUT = HERE / "results"
LABEL = {"digital_arrest": "Digital arrest", "customer_care": "Fake customer care / refund",
         "kyc_official": "Fake KYC / bank official", "courier": "Courier / customs",
         "voice_clone": "AI voice clone of a relative", "hang_up_and_pay": "Hang up, then pay",
         "sent_by_mistake": "'Sent by mistake' return", "chat_task": "Task scam over chat"}
IN_SCOPE = ["digital_arrest", "customer_care", "kyc_official", "courier", "voice_clone", "hang_up_and_pay"]


def read_rows(name):
    with open(HERE / "data" / f"{name}.jsonl") as f:
        return [json.loads(l) for l in f]


def level_of(raw, p, m):
    return policy.decide(raw, p, m)["level"]


def main(quick=False):
    OUT.mkdir(exist_ok=True)
    m = scorer.load()
    model = joblib.load(HERE / "models" / f"{m['model_type']}.joblib")
    rows = read_rows("test")
    X = np.array([featurize(r["features"]) for r in rows])
    y = np.array([r["label"] for r in rows])
    wf = np.array([r["workflow"] for r in rows])
    p = model.predict_proba(X)[:, 1]
    levels = np.array([level_of(r["features"], pi, m) for r, pi in zip(rows, p)])
    flagged = np.isin(levels, ["PAUSE", "CRITICAL"])
    g = y == 0

    # ---- 1. model on the test set ----
    per_wf = {}
    for w in LABEL:
        k = wf == w
        c = Counter(levels[k])
        per_wf[w] = {"label": LABEL[w], "n": int(k.sum()), "flagged": float(flagged[k].mean()),
                     "critical_by_score": c["CRITICAL"] / k.sum(), "nudge_only": c["NUDGE"] / k.sum(), "nothing": c["NONE"] / k.sum()}
    bins = np.linspace(0, 1, 11)
    calib = []
    for lo, hi in zip(bins[:-1], bins[1:]):
        k = (p >= lo) & (p < hi if hi < 1 else p <= hi)
        if k.sum():
            calib.append({"bin": f"{lo:.1f}-{hi:.1f}", "n": int(k.sum()), "mean_score": float(p[k].mean()), "share_scam": float(y[k].mean())})
    model_res = {
        "scams_flagged_all8": float(flagged[y == 1].mean()),
        "scams_flagged_in_scope6": float(np.mean([per_wf[w]["flagged"] for w in IN_SCOPE])),
        "genuine_interrupted_per_1000": float(flagged[g].mean() * 1000),
        "genuine_nudged_per_1000": float((levels[g] == "NUDGE").mean() * 1000),
        "precision_on_this_test_mix": float(y[flagged].mean()) if flagged.any() else None,
        "roc_auc": float(roc_auc_score(y, p)), "brier": float(brier_score_loss(y, p)),
        "by_workflow": per_wf, "calibration": calib,
    }

    # ---- 2. same test set: Round 1 rules and the bank-style amount rule ----
    pack = json.loads((HERE / "round1" / "rulepack_v0.3.json").read_text())
    r_lv = np.array([rules.assess(Session(**r["features"]), pack).tier for r in rows])
    r_fl = np.isin(r_lv, ["PAUSE", "CRITICAL"])
    naive = np.array([rules.naive_baseline(Session(**r["features"])) for r in rows])
    def summary(fl):
        return {"scams_flagged_all8": float(fl[y == 1].mean()),
                "scams_flagged_in_scope6": float(np.mean([fl[wf == w].mean() for w in IN_SCOPE])),
                "genuine_interrupted_per_1000": float(fl[g].mean() * 1000),
                "by_workflow": {w: float(fl[wf == w].mean()) for w in LABEL}}
    comparison = {"ai_model": {k: model_res[k] for k in ("scams_flagged_all8", "scams_flagged_in_scope6", "genuine_interrupted_per_1000")}
                  | {"by_workflow": {w: per_wf[w]["flagged"] for w in LABEL}},
                  "round1_rules_v0.3": summary(r_fl), "bank_amount_rule_10k": summary(naive)}

    # ---- 3. hidden-scam-type test ----
    hidden = {}
    if not quick:
        Xtr, ytr, wtr = load("train")
        Xva, yva, wva = load("val")
        for w in LABEL:
            keep = wtr != w
            gb = GradientBoostingClassifier(n_estimators=120, max_depth=3, learning_rate=0.1, subsample=0.8, random_state=7)
            gb.fit(Xtr[keep], ytr[keep])
            t = threshold_for(gb.predict_proba(Xva[wva == "genuine"])[:, 1], TARGET_INTERRUPT)
            ph = gb.predict_proba(X[wf == w])[:, 1]
            hidden[w] = {"label": LABEL[w], "flagged_when_never_seen": float((ph > t).mean()),
                         "flagged_when_seen": per_wf[w]["flagged"]}
            print(f"hidden {w:16} never seen {hidden[w]['flagged_when_never_seen']:5.1%}  seen {per_wf[w]['flagged']:5.1%}")

    # ---- 4. the 28 Round 1 stories ----
    cases = json.loads((HERE / "round1" / "cases.json").read_text())
    stories = []
    for c in cases:
        s = dict(c["session"]); q1, q2 = s.pop("q1", None), s.pop("q2", None)
        sc = scorer.score(s, m)
        lv = policy.decide(s, sc["score"], m, q1=q1, q2=q2)["level"]
        stories.append({"id": c["id"], "kind": c["kind"], "title": c["title"], "round1_expected": c["expected"],
                         "ai_level": lv, "score": round(sc["score"], 3), "reasons": sc["reasons"],
                         "same_as_round1": lv == c["expected"]})
    def fl(lv): return lv in ("PAUSE", "CRITICAL")
    story_sum = {"same_level_as_round1": sum(s["same_as_round1"] for s in stories),
                 "scams_flagged": sum(fl(s["ai_level"]) for s in stories if s["kind"] == "scam"),
                 "scams": sum(s["kind"] == "scam" for s in stories),
                 "genuine_interrupted": sum(fl(s["ai_level"]) for s in stories if s["kind"] == "legit"),
                 "genuine": sum(s["kind"] == "legit" for s in stories)}

    # ---- 5. failure list ----
    def brief(r, pi):
        f = r["features"]
        return {"workflow": r["workflow"], "score": round(float(pi), 3), "amount": f["amount"], "median_90d": f["median_90d"],
                "call_now": f["call_now"], "call_seen_30m": f["call_seen_30m"], "screen_share": f["screen_share"],
                "payee_status": f["payee_status"], "payee_kind": f["payee_kind"], "pasted": f["payee_pasted"]}
    missed = [i for i in np.where((y == 1) & ~flagged)[0]]
    missed_by = Counter(wf[missed])
    false_alarms = sorted(np.where(g & flagged)[0], key=lambda i: -p[i])
    fa_pattern = Counter((rows[i]["features"]["payee_status"], rows[i]["features"]["payee_kind"],
                          "on call" if rows[i]["features"]["call_now"] else "no call") for i in false_alarms)
    failures = {"missed_scams_by_type": dict(missed_by),
                "missed_examples": [brief(rows[i], p[i]) for i in missed[:12]],
                "false_alarm_patterns": [{"payee": k[0], "kind": k[1], "call": k[2], "count": v} for k, v in fa_pattern.most_common(6)],
                "false_alarm_examples": [brief(rows[i], p[i]) for i in false_alarms[:8]]}

    report = {"model_type": m["model_type"], "model_version": m["model_version"], "thresholds": m["thresholds"],
              "test_set": {"genuine": int(g.sum()), "scams": int((y == 1).sum()), "note": "shifted settings, never used in training or selection"},
              "model": model_res, "comparison": comparison, "hidden_scam_type": hidden, "stories_28": {"summary": story_sum, "rows": stories},
              "failures": failures}
    (OUT / "report.json").write_text(json.dumps(report, indent=1))
    write_markdown(report)
    print((OUT / "REPORT.md").read_text(encoding="utf-8"))


def pct(x): return f"{x:.0%}"


def write_markdown(r):
    m, c = r["model"], r["comparison"]
    L = [f"# ScriptBreak AI model: test report\n",
         f"Model: **{r['model_type']}** (version {r['model_version']}). All data synthetic. "
         f"Test set: {r['test_set']['genuine']:,} genuine + {r['test_set']['scams']:,} scam sessions, shifted settings, never seen in training.\n",
         "## Headline\n",
         "| | AI model | Round 1 rules (v0.3) | Bank amount rule (new person ≥ ₹10,000) |", "|---|---|---|---|",
         f"| Scam sessions flagged (6 in-scope types) | **{pct(c['ai_model']['scams_flagged_in_scope6'])}** | {pct(c['round1_rules_v0.3']['scams_flagged_in_scope6'])} | {pct(c['bank_amount_rule_10k']['scams_flagged_in_scope6'])} |",
         f"| Scam sessions flagged (all 8 types) | **{pct(c['ai_model']['scams_flagged_all8'])}** | {pct(c['round1_rules_v0.3']['scams_flagged_all8'])} | {pct(c['bank_amount_rule_10k']['scams_flagged_all8'])} |",
         f"| Genuine payments interrupted per 1,000 (budget ≤ 8) | **{c['ai_model']['genuine_interrupted_per_1000']:.1f}** | {c['round1_rules_v0.3']['genuine_interrupted_per_1000']:.1f} | {c['bank_amount_rule_10k']['genuine_interrupted_per_1000']:.1f} |",
         f"\nOther: Nudge on {m['genuine_nudged_per_1000']:.0f} per 1,000 genuine payments; ROC AUC {m['roc_auc']:.3f}; "
         f"precision {pct(m['precision_on_this_test_mix'])} on this test mix (scams are ~11% of it, far more than in real life, so real precision would be lower).\n",
         "## By scam type (flagged = Pause or Critical)\n", "| Scam type | AI model | Round 1 rules | Never seen in training* |", "|---|---|---|---|"]
    for w, v in m["by_workflow"].items():
        h = r["hidden_scam_type"].get(w, {}).get("flagged_when_never_seen")
        L.append(f"| {v['label']} | {pct(v['flagged'])} | {pct(c['round1_rules_v0.3']['by_workflow'][w])} | {pct(h) if h is not None else '-'} |")
    L.append("\n*Retrained with that scam type removed from the training data, then tested on it.\n")
    s = r["stories_28"]["summary"]
    L += ["## The 28 Round 1 stories (model + safety layer)\n",
          f"Scams flagged: {s['scams_flagged']} of {s['scams']}. Genuine interrupted: {s['genuine_interrupted']} of {s['genuine']}. "
          f"Same level as the Round 1 design: {s['same_level_as_round1']} of 28.\n",
          "| ID | Story | Round 1 | AI | Score |", "|---|---|---|---|---|"]
    for x in r["stories_28"]["rows"]:
        mark = "" if x["same_as_round1"] else " ⚠"
        L.append(f"| {x['id']} | {x['title']} | {x['round1_expected']} | {x['ai_level']}{mark} | {x['score']} |")
    f = r["failures"]
    L += ["\n## Where it fails\n", "Missed scams by type: " + ", ".join(f"{LABEL[k]} {v}" for k, v in f["missed_scams_by_type"].items()) + "\n",
          "Most common genuine false alarms:\n"]
    for x in f["false_alarm_patterns"]:
        L.append(f"- {x['payee']} {x['kind']}, {x['call']}: {x['count']}")
    L.append("\n## Calibration (does a score of 0.8 mean ~80% scam?)\n")
    L += ["| Score | Payments | Mean score | Actually scam |", "|---|---|---|---|"]
    for b in m["calibration"]:
        L.append(f"| {b['bin']} | {b['n']:,} | {b['mean_score']:.2f} | {b['share_scam']:.2f} |")
    L.append("\nThese numbers test the model under our synthetic assumptions (make_dataset.py). They are not real-world accuracy.")
    # utf-8: the report contains ₹, ≥ and ⚠, which Windows' default encoding cannot write
    (OUT / "REPORT.md").write_text("\n".join(L) + "\n", encoding="utf-8")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")  # Windows consoles default to cp1252
    main(quick="--quick" in sys.argv)
