"""Stress test of rule pack v0.3 on generated sessions, with a sensitivity analysis.

These numbers test our ASSUMPTIONS (generator.py), not real-world accuracy. The sensitivity
analysis shows which assumption drives the false-alarm rate, so bank shadow mode can
measure it first. Budget: at most 8 interruptions per 1,000 genuine payments (someone
making 40 payments a month would see about one Pause every three months).
"""
import json
from collections import Counter
from pathlib import Path

import engine
from engine import RANK, assess, load_pack, naive_baseline
from generator import SCAMS, legit_stream, scam_stream

HERE = Path(__file__).parent
BUDGET = 8.0
N_LEGIT, N_SCAM = 200_000, 3_000


def legit_rates(pack, **kw):
    c, naive, rules = Counter(), 0, Counter()
    for s in legit_stream(N_LEGIT, seed=51, **kw):
        a = assess(s, pack)
        c[a.tier] += 1
        naive += naive_baseline(s)
        if RANK[a.tier] >= 2:
            for r in a.rules:
                if r.startswith("R"):
                    rules[r] += 1
    per = lambda x: x / N_LEGIT * 1000
    return {"interrupt_per_1000": per(c["PAUSE"] + c["CRITICAL"]), "nudge_per_1000": per(c["NUDGE"]),
            "naive_per_1000": per(naive), "by_rule_per_1000": {k: per(v) for k, v in rules.items()}}


def scam_rates(pack, w):
    c, naive = Counter(), 0
    for s in scam_stream(w, N_SCAM, seed=61):
        c[assess(s, pack).tier] += 1
        naive += naive_baseline(s)
    n = N_SCAM
    return {"label": SCAMS[w]["label"], "flagged": (c["PAUSE"] + c["CRITICAL"]) / n,
            "critical": c["CRITICAL"] / n, "nudge": c["NUDGE"] / n, "none": c["NONE"] / n, "naive": naive / n}


def main():
    v03 = load_pack("0.3")
    base = legit_rates(v03)
    scams = {w: scam_rates(v03, w) for w in SCAMS}

    sens = []
    for k in (0.5, 1.0, 1.5, 2.0, 3.0):
        sens.append({"call_scale": k, "interrupt_per_1000": legit_rates(v03, call_scale=k)["interrupt_per_1000"]})
    # break-even multiplier where the budget is reached (linear interpolation)
    breakeven = None
    for a, b in zip(sens, sens[1:]):
        if a["interrupt_per_1000"] <= BUDGET < b["interrupt_per_1000"]:
            f = (BUDGET - a["interrupt_per_1000"]) / (b["interrupt_per_1000"] - a["interrupt_per_1000"])
            breakeven = a["call_scale"] + f * (b["call_scale"] - a["call_scale"])
    screen5 = legit_rates(v03, screen_scale=5.0)["interrupt_per_1000"]

    # pre-planned fallback if shadow mode finds the driver is high: raise the Rs 2,000 floor
    engine.AMOUNT_FLOOR = 5000
    fb_legit = {k: legit_rates(v03, call_scale=k)["interrupt_per_1000"] for k in (1.0, 2.0, 3.0)}
    fb_scams = {w: scam_rates(v03, w)["flagged"] for w in SCAMS}
    engine.AMOUNT_FLOOR = 2000

    fb_breakeven = None
    ks = [1.0, 2.0, 3.0]
    for a, b in zip(ks, ks[1:]):
        if fb_legit[a] <= BUDGET < fb_legit[b]:
            fb_breakeven = a + (BUDGET - fb_legit[a]) / (fb_legit[b] - fb_legit[a]) * (b - a)
    blanket = sum(s.payee_status in ("new", "recent") for s in legit_stream(N_LEGIT, seed=51)) / N_LEGIT
    out = {"budget_per_1000": BUDGET, "legit": base, "scams": scams, "sensitivity_call_scale": sens,
           "fallback_breakeven_call_scale": fb_breakeven, "blanket_new_payee_notice_share": blanket,
           "breakeven_call_scale": breakeven, "screen_share_x5_interrupt_per_1000": screen5,
           "fallback_floor_5000": {"legit_by_call_scale": fb_legit, "scams_flagged": fb_scams}}
    (HERE / "results").mkdir(exist_ok=True)
    (HERE / "results" / "stress_test.json").write_text(json.dumps(out, indent=1))

    print(f"genuine: interrupt {base['interrupt_per_1000']:.2f}/1000 (budget {BUDGET}), nudge {base['nudge_per_1000']:.1f}/1000, "
          f"naive {base['naive_per_1000']:.2f}/1000  by rule {({k: round(v, 2) for k, v in base['by_rule_per_1000'].items()})}")
    for w, r in scams.items():
        print(f"{w:16} flagged {r['flagged']:5.1%} (critical {r['critical']:5.1%})  nudge {r['nudge']:5.1%}  none {r['none']:5.1%}  naive {r['naive']:5.1%}"
              f"   | floor 5000: {fb_scams[w]:5.1%}")
    print("sensitivity:", [(x['call_scale'], round(x['interrupt_per_1000'], 2)) for x in sens], "break-even", breakeven)
    print("screen-share x5:", round(screen5, 2), " fallback floor 5000:", {k: round(v, 2) for k, v in fb_legit.items()})


if __name__ == "__main__":
    main()
