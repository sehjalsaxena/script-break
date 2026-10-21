"""Fair comparison: a naive amount rule tuned to the SAME interruption rate as ScriptBreak v0.3."""
import json
from pathlib import Path

from engine import RANK, assess, load_pack
from generator import legit_stream, scam_stream

IN_SCOPE = ["digital_arrest", "customer_care", "kyc_official", "courier", "voice_clone", "hang_up_and_pay"]
HERE = Path(__file__).parent


def naive(threshold, include_merchants=False):
    def f(s):
        kinds = ("person", "merchant") if include_merchants else ("person",)
        return s.payee_status == "new" and s.payee_kind in kinds and s.amount >= threshold
    return f


def main():
    legit = list(legit_stream(200_000, seed=51))
    scams = {w: list(scam_stream(w, 3_000, seed=61)) for w in IN_SCOPE}
    v03 = load_pack("0.3")
    sb_legit = sum(RANK[assess(s, v03).tier] >= 2 for s in legit) / len(legit) * 1000
    sb_scam = {w: sum(RANK[assess(s, v03).tier] >= 2 for s in ss) / len(ss) for w, ss in scams.items()}
    rows = [{"rule": "ScriptBreak v0.3", "legit_per_1000": sb_legit, "scam_avg": sum(sb_scam.values()) / len(sb_scam), "by": sb_scam}]
    for thr in (10000, 7500, 5000, 4000, 3000, 2000):
        for merch in (False, True):
            f = naive(thr, merch)
            l = sum(f(s) for s in legit) / len(legit) * 1000
            by = {w: sum(f(s) for s in ss) / len(ss) for w, ss in scams.items()}
            rows.append({"rule": f"first payment {'to anyone' if merch else 'to a person'} >= Rs {thr:,}",
                         "legit_per_1000": l, "scam_avg": sum(by.values()) / len(by), "by": by})
    for r in rows:
        print(f"{r['rule']:44} legit {r['legit_per_1000']:5.2f}/1000  in-scope scams flagged {r['scam_avg']:5.1%}  "
              + " ".join(f"{w[:6]}={v:.0%}" for w, v in r["by"].items()))
    (HERE / "results" / "fair_compare.json").write_text(json.dumps(rows, indent=1))


if __name__ == "__main__":
    main()
