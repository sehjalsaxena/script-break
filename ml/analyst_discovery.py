"""Analyst console data: the AI groups scam reports the model MISSED and proposes rules.

This is the "AI finds new scam tricks, people approve" part promised on the form.
  1. A month of consented, redacted scam reports arrives (synthetic, new seeds).
     Each report is only a set of event tokens: no names, numbers, IDs or text.
  2. Keep the reports the current model + safety layer did NOT pause (the misses).
  3. Cluster the misses (average-linkage hierarchical clustering on Jaccard distance,
     cut height chosen by silhouette score). The AI never sees the scam type.
  4. For each cluster, propose a rule: the combination of common events that catches the
     most of the cluster while adding at most 0.5 false alarms per 1,000 genuine payments
     (backtested on 1,00,000 fresh genuine sessions scored by the model).
  5. Write discovery.json. In the app, the analyst taps Approve or Reject; an approved
     rule becomes a guardrail (policy G3: at least Pause) on the phone.

Run:  python analyst_discovery.py                      (current model)
      python analyst_discovery.py --unseen sent_by_mistake
         demo mode: uses a model retrained WITHOUT that scam type, i.e. a scam trick
         the bank has never seen before, so the console shows the AI discovering it.
"""
from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path

import joblib
import numpy as np
from sklearn.ensemble import GradientBoostingClassifier

import policy
import scorer
from features import featurize
from train import TARGET_INTERRUPT, load, threshold_for

HERE = Path(__file__).parent
sys.path.insert(0, str(HERE / "round1"))
from discover import MIN_CLUSTER, MIN_RECALL_TO_APPROVE, NEW_RULE_BUDGET, TOKEN_TEXT, cluster, induce_rule  # noqa: E402
from engine import tokens  # noqa: E402
from generator import legit_stream, scam_stream  # noqa: E402

REPORTS = {"digital_arrest": 300, "customer_care": 200, "kyc_official": 150, "courier": 150,
           "voice_clone": 100, "hang_up_and_pay": 120, "sent_by_mistake": 90, "chat_task": 90}
EXPLAIN = {
    "amount_matches_credit": "This sends back money you received from someone you have never paid, but to a "
                             "different account. Real mistaken payments are reversed by the sender's bank, not by you.",
    "many_new_payees": "This is another payment to a new account within an hour. Online 'task' and "
                       "'investment' scams ask for payments exactly like this.",
}


def main():
    unseen = sys.argv[sys.argv.index("--unseen") + 1] if "--unseen" in sys.argv else None
    m = scorer.load()
    if unseen:
        Xtr, ytr, wtr = load("train")
        Xva, _, wva = load("val")
        model = GradientBoostingClassifier(n_estimators=200, max_depth=3, learning_rate=0.1, subsample=0.8,
                                           random_state=7).fit(Xtr[wtr != unseen], ytr[wtr != unseen])
        pv = model.predict_proba(Xva[wva == "genuine"])[:, 1]
        m = {**m, "thresholds": {"nudge": threshold_for(pv, 33.0), "pause": threshold_for(pv, TARGET_INTERRUPT),
                                 "critical": threshold_for(pv, 0.5)}}
    else:
        model = joblib.load(HERE / "models" / f"{m['model_type']}.joblib")

    def levels(sessions):
        X = np.array([featurize(s.__dict__) for s in sessions])
        p = model.predict_proba(X)[:, 1]
        return [policy.decide(s.__dict__, pi, m)["level"] for s, pi in zip(sessions, p)]

    reports = []
    for i, (w, n) in enumerate(REPORTS.items()):
        ss = list(scam_stream(w, n, seed=701 + i))
        for s, lv in zip(ss, levels(ss)):
            reports.append({"workflow": w, "tokens": tokens(s), "level": lv})
    misses = [r for r in reports if policy.RANK[r["level"]] < 2]
    vocab = sorted({t for r in reports for t in r["tokens"]})
    sil, height, labels = cluster([r["tokens"] for r in misses], vocab)

    legit = list(legit_stream(100_000, seed=31))
    legit_sets = [tokens(s) for s in legit]
    legit_flagged = [policy.RANK[lv] >= 2 for lv in levels(legit)]

    clusters = []
    rid = 1
    for lab in sorted(set(labels)):
        idx = [i for i, l in enumerate(labels) if l == lab]
        if len(idx) < MIN_CLUSTER:
            continue
        members = [misses[i]["tokens"] for i in idx]
        comp = Counter(misses[i]["workflow"] for i in idx)
        rule, cheapest, common = induce_rule(members, legit_sets, legit_flagged)
        suggestion = "approve" if rule and rule["recall"] >= MIN_RECALL_TO_APPROVE else ("shadow mode" if rule else "reject")
        c = {"cluster_id": f"C{rid}", "size": len(idx),
             "pattern": [TOKEN_TEXT.get(t, t) for t in sorted(common)],
             "ground_truth_mix_for_judges": dict(comp.most_common()),
             "suggestion": suggestion}
        if rule:
            key = next((k for k in EXPLAIN if k in rule["all"]), None)
            c["proposed_rule"] = {
                "id": f"L{rid}", "all": rule["all"], "none": rule["none"],
                "name": " + ".join(TOKEN_TEXT.get(t, t) for t in rule["all"]) + "".join(f", not {TOKEN_TEXT.get(t, t)}" for t in rule["none"]),
                "explain": EXPLAIN.get(key, "This payment matches a scam pattern reported by other customers."),
                "catches_share_of_cluster": round(rule["recall"], 3),
                "added_false_alarms_per_1000": round(rule["added_per_1000"], 3)}
        elif cheapest:
            c["cheapest_rule_reaching_70pct"] = {"all": cheapest["all"], "added_false_alarms_per_1000": round(cheapest["added_per_1000"], 2)}
        clusters.append(c)
        rid += 1
    clusters.sort(key=lambda c: -c["size"])
    out = {"model": "unseen:" + unseen if unseen else m["model_version"], "reports": len(reports),
           "missed_by_model": len(misses), "missed_by_type": dict(Counter(r["workflow"] for r in misses)),
           "clustering": {"method": "average-linkage, Jaccard distance", "cut_height": height, "silhouette": round(float(sil), 3)},
           "policy": {"new_rule_budget_per_1000": NEW_RULE_BUDGET, "min_share_caught": MIN_RECALL_TO_APPROVE, "min_cluster": MIN_CLUSTER},
           "clusters": clusters}
    name = f"discovery_unseen_{unseen}.json" if unseen else "discovery.json"
    (HERE / name).write_text(json.dumps(out, indent=1))
    print(json.dumps({k: out[k] for k in ("reports", "missed_by_model", "missed_by_type", "clustering")}, indent=1))
    for c in clusters:
        r = c.get("proposed_rule")
        print(f"{c['cluster_id']} size {c['size']:3} {c['suggestion']:12} mix {c['ground_truth_mix_for_judges']}")
        if r:
            print(f"     rule: {r['name']}  catches {r['catches_share_of_cluster']:.0%}  +{r['added_false_alarms_per_1000']}/1000")
    print(f"wrote {name}")


if __name__ == "__main__":
    main()
