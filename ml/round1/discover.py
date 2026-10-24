"""Pattern discovery: find scam workflows the current rules miss, and propose rules for analysts.

Pipeline (all offline, never on the payment path):
  1. Consented, redacted incident reports arrive as sets of event tokens (engine.tokens):
     no names, phone numbers, UPI IDs, message text or audio.
  2. Take confirmed-scam reports that the current rule pack did NOT pause ("misses").
  3. Cluster the misses: average-linkage hierarchical clustering on Jaccard distance between
     token sets; the cut height is the one with the best silhouette score among cuts that put
     at least 80% of misses into clusters big enough (20+) for an analyst to act on.
  4. For each sizeable cluster, induce a candidate rule: the conjunction of the cluster's
     common tokens (plus at most one exclusion) that catches the most misses while adding at
     most NEW_RULE_BUDGET interruptions per 1,000 genuine payments in a backtest.
  5. Analyst policy decides: approve into the next rule pack, run in shadow mode, or reject.
     Every approved rule must also pass every behaviour check (run_cases.py).
  6. Held-out test: fresh sessions of the workflow, generated with different parameters, and
     fresh genuine sessions, compare rule pack v0.2 with v0.3.

All data is synthetic (generator.py). Real deployments replace step 4's backtest with
silent counting on phones: the candidate rule runs without showing anything and phones
report only how often it would have fired.
"""
from __future__ import annotations

import itertools
import json
from collections import Counter
from pathlib import Path

import numpy as np
from scipy.cluster.hierarchy import fcluster, linkage
from scipy.spatial.distance import pdist, squareform
from sklearn.metrics import silhouette_score

from engine import RANK, Session, assess, load_pack, tokens
from generator import SCAMS, legit_stream, scam_stream

HERE = Path(__file__).parent
OUT = HERE / "results"

REPORTS = {  # consented scam reports received in one month (synthetic)
    "digital_arrest": 300, "customer_care": 200, "kyc_official": 150, "courier": 150,
    "voice_clone": 100, "hang_up_and_pay": 120, "sent_by_mistake": 90, "chat_task": 90,
}
MIN_CLUSTER = 20
MIN_ACTIONABLE = 0.8       # share of misses that must fall in clusters of MIN_CLUSTER or more
NEW_RULE_BUDGET = 0.5          # max added interruptions per 1,000 genuine payments per new rule
MIN_RECALL_TO_APPROVE = 0.7
N_BACKTEST = 100_000

EXPLAIN = {  # plain-language copy an analyst attaches to an approved rule, keyed by its tokens
    frozenset({"app_switch", "many_new_payees", "payee_pasted"}):
        "This is your third payment to a new account within an hour, using details copied from "
        "another app. Online 'task' and 'investment' scams ask for payments exactly like this.",
    frozenset({"amount_matches_credit"}):
        "This sends back money you received from someone you have never paid, but to a "
        "different account. Real mistaken payments are reversed by the sender's bank, not by you.",
    frozenset({"credit_unknown_48h", "amount_matches_credit"}):
        "This sends back money you received from someone you have never paid, but to a "
        "different account. Real mistaken payments are reversed by the sender's bank, not by you.",
}
TOKEN_TEXT = {
    "call_now": "on a call now", "call_seen_30m": "call in the last 30 min", "screen_share": "screen shared",
    "payee_new": "new payee", "payee_merchant": "merchant payee", "amount_big": "unusual amount",
    "amount_cumulative": "adds up to an unusual amount", "repeat_same_payee": "repeat to same payee",
    "many_new_payees": "3rd new payee in an hour", "low_history": "little payment history",
    "credit_unknown_48h": "money received from an unknown sender", "amount_matches_credit": "same amount as that money",
    "payee_is_sender": "payee is that sender", "payee_pasted": "payee ID pasted",
    "app_switch": "left and returned to the app", "night": "late at night",
}


def build_reports(pack):
    reports = []
    for i, (w, n) in enumerate(REPORTS.items()):
        for s in scam_stream(w, n, seed=101 + i):
            a = assess(s, pack)
            reports.append({"workflow": w, "tokens": tokens(s), "tier": a.tier})
    return reports


def cluster(token_sets, vocab):
    X = np.array([[t in ts for t in vocab] for ts in token_sets], dtype=bool)
    d = pdist(X, metric="jaccard")
    Z = linkage(d, method="average")
    D = squareform(d)
    best = None
    for h in np.arange(0.30, 0.71, 0.05):
        labels = fcluster(Z, t=h, criterion="distance")
        sizes = Counter(labels)
        actionable = sum(v for v in sizes.values() if v >= MIN_CLUSTER) / len(labels)
        if len(sizes) < 2 or actionable < MIN_ACTIONABLE:
            continue  # most misses must land in clusters big enough for an analyst to act on
        sc = silhouette_score(D, labels, metric="precomputed")
        if best is None or sc > best[0]:
            best = (sc, round(float(h), 2), labels)
    return best


def induce_rule(members, legit_sets, legit_flagged):
    """Best conjunction of common tokens (+ at most one exclusion) within the new-rule budget."""
    n = len(members)
    freq = Counter(t for m in members for t in m)
    common = [t for t, c in freq.items() if c / n >= 0.6]
    candidates = []
    for k in range(2, min(4, len(common)) + 1):
        for combo in itertools.combinations(sorted(common), k):
            req = set(combo)
            recall = sum(req <= m for m in members) / n
            fires = [i for i, ls in enumerate(legit_sets) if req <= ls and not legit_flagged[i]]
            options = [((), recall, len(fires))]
            # one exclusion: a token common among the genuine fires but rare in the cluster
            fire_freq = Counter(t for i in fires for t in legit_sets[i])
            for t, c in fire_freq.most_common(6):
                if t in req or freq.get(t, 0) / n > 0.05 or not fires or c / len(fires) < 0.3:
                    continue
                r2 = sum(req <= m and t not in m for m in members) / n
                f2 = sum(1 for i in fires if t not in legit_sets[i])
                options.append(((t,), r2, f2))
            for excl, r, f in options:
                added = f / len(legit_sets) * 1000
                candidates.append({"all": sorted(req), "none": list(excl), "recall": r, "added_per_1000": added})
    feasible = [c for c in candidates if c["added_per_1000"] <= NEW_RULE_BUDGET]
    # most scams caught, then fewest false alarms, then the simplest rule
    key = lambda c: (-round(c["recall"], 3), round(c["added_per_1000"], 3), len(c["all"]) + len(c["none"]))
    best_feasible = min(feasible, key=key) if feasible else None
    reach = [c for c in candidates if c["recall"] >= MIN_RECALL_TO_APPROVE]
    cheapest_reach = min(reach, key=lambda c: (c["added_per_1000"], -c["recall"])) if reach else None
    return best_feasible, cheapest_reach, common


def main():
    OUT.mkdir(exist_ok=True)
    v02 = load_pack("0.2")
    reports = build_reports(v02)
    misses = [r for r in reports if RANK[r["tier"]] < 2]
    vocab = sorted({t for r in reports for t in r["tokens"]})
    sil, height, labels = cluster([r["tokens"] for r in misses], vocab)

    legit = list(legit_stream(N_BACKTEST, seed=31))
    legit_sets = [tokens(s) for s in legit]
    legit_flagged = [RANK[assess(s, v02).tier] >= 2 for s in legit]
    base_legit = sum(legit_flagged) / N_BACKTEST * 1000

    clusters, approved = [], []
    for lab in sorted(set(labels)):
        idx = [i for i, l in enumerate(labels) if l == lab]
        if len(idx) < MIN_CLUSTER:
            continue
        members = [misses[i]["tokens"] for i in idx]
        comp = Counter(misses[i]["workflow"] for i in idx)
        rule, best_any, common = induce_rule(members, legit_sets, legit_flagged)
        if rule and rule["recall"] >= MIN_RECALL_TO_APPROVE:
            decision = "approve"
        elif rule:
            decision = "shadow mode"
        else:
            decision = "reject"
        c = {"size": len(idx), "top_workflow": comp.most_common(1)[0][0],
             "purity": comp.most_common(1)[0][1] / len(idx), "composition": dict(comp),
             "common_tokens": sorted(common), "rule": rule, "best_unconstrained": best_any,
             "decision": decision}
        clusters.append(c)
    clusters.sort(key=lambda c: -c["size"])

    legit_cases = [Session(**c["session"]) for c in json.loads((HERE / "cases.json").read_text())
                   if c["kind"] == "legit"]
    rid = 5
    for c in clusters:
        if c["decision"] != "approve":
            continue
        r = c["rule"]
        probe = {"learned": [{"id": "probe", "all": r["all"], "none": r["none"], "explain": ""}]}
        changed = [i for i, s in enumerate(legit_cases)
                   if RANK[assess(s, probe).tier] >= 2 and RANK[assess(s, v02).tier] < 2]
        if changed:
            c["decision"] = "reject (changes a genuine behaviour check)"
            continue
        key = frozenset(t for t in r["all"] if t not in ("payee_new", "call_now"))
        if key not in EXPLAIN and key - {"credit_unknown_48h"} in EXPLAIN:
            key = key - {"credit_unknown_48h"}
        explain = EXPLAIN.get(key, "This payment matches a scam pattern reported by other customers.")
        rule = {"id": f"R{rid}", "name": " + ".join(TOKEN_TEXT.get(t, t) for t in r["all"])
                + ("".join(f", not {TOKEN_TEXT.get(t, t)}" for t in r["none"])),
                "all": r["all"], "none": r["none"], "then": "PAUSE", "explain": explain,
                "source": {"cluster_size": c["size"], "recall_on_cluster": r["recall"],
                           "backtest_added_per_1000": r["added_per_1000"]},
                "approved_by": ["analyst 1", "analyst 2"]}
        approved.append(rule)
        c["rule_id"] = rule["id"]
        rid += 1
    pack = {"version": "0.3", "based_on": "0.2", "learned": approved}
    (HERE / "rulepack_v0.3.json").write_text(json.dumps(pack, indent=1))

    # ---- held-out test: fresh sessions with shifted parameters, fresh genuine sessions ----
    v03 = load_pack("0.3")
    SHIFT = {"sent_by_mistake": {"call_now": 0.4, "ratio": (np.log(4.0), 0.9), "pasted": 0.6},
             "hang_up_and_pay": {"call_seen_30m": 0.25, "ratio": (np.log(15), 1.0)},
             "chat_task": {"many_new": 0.3}}
    held = {}
    for w in SCAMS:
        sessions = list(scam_stream(w, 2000, seed=401, shift=SHIFT.get(w)))
        held[w] = {
            "label": SCAMS[w]["label"],
            "v02_flagged": sum(RANK[assess(s, v02).tier] >= 2 for s in sessions) / len(sessions),
            "v03_flagged": sum(RANK[assess(s, v03).tier] >= 2 for s in sessions) / len(sessions),
            "v03_nudge": sum(assess(s, v03).tier == "NUDGE" for s in sessions) / len(sessions),
            "v03_none": sum(assess(s, v03).tier == "NONE" for s in sessions) / len(sessions),
            "shifted": w in SHIFT,
        }
    fresh = list(legit_stream(200_000, seed=51))
    legit_v02 = sum(RANK[assess(s, v02).tier] >= 2 for s in fresh) / len(fresh) * 1000
    legit_v03 = sum(RANK[assess(s, v03).tier] >= 2 for s in fresh) / len(fresh) * 1000

    result = {
        "reports": len(reports), "paused_by_v02": len(reports) - len(misses), "misses": len(misses),
        "vocab_size": len(vocab), "cut_height": height, "silhouette": sil,
        "n_clusters_all": len(set(labels)), "clusters": clusters, "approved": approved,
        "backtest_base_per_1000": base_legit, "held_out": held,
        "legit_fresh_v02_per_1000": legit_v02, "legit_fresh_v03_per_1000": legit_v03,
    }
    (OUT / "discovery.json").write_text(json.dumps(result, indent=1, default=float))

    print(f"reports {len(reports)}  paused by v0.2 {len(reports)-len(misses)}  misses {len(misses)}  "
          f"vocab {len(vocab)}  cut {height}  silhouette {sil:.2f}  clusters {len(set(labels))}")
    for c in clusters:
        r, b = c["rule"], c["best_unconstrained"]
        print(f"- n={c['size']:3} {c['top_workflow']:16} purity {c['purity']:.0%}  common={c['common_tokens']}")
        if r:
            print(f"    rule all={r['all']} none={r['none']} recall {r['recall']:.0%} added {r['added_per_1000']:.2f}/1000 -> {c['decision']}")
        else:
            print(f"    no rule within budget; cheapest rule reaching {MIN_RECALL_TO_APPROVE:.0%}: all={b['all']} none={b['none']} "
                  f"recall {b['recall']:.0%} added {b['added_per_1000']:.2f}/1000 -> {c['decision']}")
    print("approved:", [(r["id"], r["name"]) for r in approved])
    for w, h in held.items():
        print(f"held-out {w:16} v0.2 {h['v02_flagged']:5.1%} -> v0.3 {h['v03_flagged']:5.1%}")
    print(f"genuine (fresh 200k): v0.2 {legit_v02:.2f}/1000 -> v0.3 {legit_v03:.2f}/1000")


if __name__ == "__main__":
    main()
