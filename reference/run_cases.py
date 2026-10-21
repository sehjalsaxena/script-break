"""Behaviour checks: run every synthetic case through rule packs v0.2 and v0.3.

Expected levels describe the DESIGNED behaviour, including known limits (cases marked
"Known limit" are scams we expect to get only a Nudge). Exit code 0 only if every case
behaves as designed.
"""
import json
import sys
from pathlib import Path

from engine import RANK, Session, assess, load_pack, naive_baseline

HERE = Path(__file__).parent


def main():
    cases = json.loads((HERE / "cases.json").read_text())
    v02, v03 = load_pack("0.2"), load_pack("0.3")
    rows, ok_all = [], True
    for c in cases:
        s = Session(**c["session"])
        a2, a3 = assess(s, v02), assess(s, v03)
        ok = a3.tier == c["expected"] and a2.tier == c.get("expected_v02", c["expected"])
        ok_all &= ok
        rows.append({"id": c["id"], "kind": c["kind"], "title": c["title"], "note": c.get("note", ""),
                     "session": c["session"], "expected": c["expected"],
                     "observed_v02": a2.tier, "observed": a3.tier, "pass": ok,
                     "groups": a3.groups, "rules": a3.rules, "reasons": a3.reasons,
                     "naive": naive_baseline(s)})

    print(f"{'ID':4} {'EXPECTED':9} {'v0.2':9} {'v0.3':9} OK NAIVE TITLE")
    for r in rows:
        print(f"{r['id']:4} {r['expected']:9} {r['observed_v02']:9} {r['observed']:9} "
              f"{'Y' if r['pass'] else 'N':2} {'warn' if r['naive'] else '-':5} {r['title']}  {r['rules']}")

    def flagged(r, key="observed"): return RANK[r[key]] >= 2
    scams = [r for r in rows if r["kind"] == "scam"]
    legit = [r for r in rows if r["kind"] == "legit"]
    summary = {
        "cases": len(rows), "as_designed": sum(r["pass"] for r in rows),
        "scams": len(scams),
        "scams_flagged": sum(flagged(r) for r in scams),
        "scams_flagged_v02": sum(flagged(r, "observed_v02") for r in scams),
        "scams_critical": sum(r["observed"] == "CRITICAL" for r in scams),
        "scams_nudge_only": sum(r["observed"] == "NUDGE" for r in scams),
        "scams_naive": sum(r["naive"] for r in scams),
        "legit": len(legit),
        "legit_interrupted": sum(flagged(r) for r in legit),
        "legit_nudged": sum(r["observed"] == "NUDGE" for r in legit),
        "legit_untouched": sum(r["observed"] == "NONE" for r in legit),
        "legit_naive": sum(r["naive"] for r in legit),
    }
    print("\nSUMMARY", json.dumps(summary, indent=1))
    out = HERE / "results"
    out.mkdir(exist_ok=True)
    (out / "behaviour_checks.json").write_text(json.dumps({"summary": summary, "rows": rows}, indent=1))
    return 0 if ok_all else 1


if __name__ == "__main__":
    sys.exit(main())
