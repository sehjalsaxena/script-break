"""Ablation study: what each signal buys (scams flagged) and costs (legit interruptions).

Runs rule pack v0.2 on generated sessions with one feature switched off at a time.
All numbers depend on the generator's stated assumptions (generator.py).
"""
import json
from collections import Counter
from pathlib import Path

import engine
from engine import RANK, assess, load_pack
from generator import SCAMS, legit_stream, scam_stream

IN_SCOPE = ["digital_arrest", "customer_care", "kyc_official", "courier", "voice_clone", "hang_up_and_pay"]
N_LEGIT, N_SCAM = 200_000, 3_000


def measure(pack):
    legit = sum(RANK[assess(s, pack).tier] >= 2 for s in legit_stream(N_LEGIT, seed=11)) / N_LEGIT * 1000
    rates = {}
    for w in IN_SCOPE:
        rates[w] = sum(RANK[assess(s, pack).tier] >= 2 for s in scam_stream(w, N_SCAM, seed=21)) / N_SCAM
    return legit, sum(rates.values()) / len(rates), rates


def main():
    pack = load_pack("0.2")
    base_legit, base_scam, base_rates = measure(pack)
    rows = [{"feature": "rule pack v0.2 as shipped", "legit_per_1000": base_legit, "scam_flagged": base_scam}]
    for f in list(engine.FEATURES):
        on = engine.FEATURES[f]
        engine.FEATURES[f] = not on
        l, sc, rates = measure(pack)
        engine.FEATURES[f] = on
        # value of the feature = (with it) - (without it)
        with_l, with_sc, without_l, without_sc = (base_legit, base_scam, l, sc) if on else (l, sc, base_legit, base_scam)
        rows.append({"feature": f, "in_pack": on, "legit_per_1000": l, "scam_flagged": sc,
                     "buys_scam_pts": (with_sc - without_sc) * 100, "costs_per_1000": with_l - without_l,
                     "by_workflow_pts": {w: round(abs(base_rates[w] - rates[w]) * 100, 1) for w in rates}})
    for r in rows:
        print(f"{r['feature']:26} legit/1000 {r['legit_per_1000']:5.2f}  scams flagged {r['scam_flagged']:6.1%}"
              + (f"   in_pack={r['in_pack']!s:5} buys {r['buys_scam_pts']:5.1f} pts  costs {r['costs_per_1000']:5.2f}/1000  {r['by_workflow_pts']}" if 'buys_scam_pts' in r else ""))
    out = Path(__file__).parent / "results"
    out.mkdir(exist_ok=True)
    (out / "ablation.json").write_text(json.dumps(rows, indent=1))


if __name__ == "__main__":
    main()
