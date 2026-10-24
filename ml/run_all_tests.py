"""ONE command to reproduce and test everything. Judges: start here (see ../TESTING.md).

    python run_all_tests.py              full run, about 8 minutes
    python run_all_tests.py --quick      skips the slow hidden-scam-type retraining
    python run_all_tests.py --android    also runs the Kotlin parity test with Gradle

What it does
  1. Rebuild from zero in ml/_repro/ (a scratch copy, so the committed model is never overwritten):
     generate the synthetic data, train the 3 candidate models, export, evaluate, run the analyst
     discovery (normal and with "sent by mistake" hidden).
  2. Reproducibility: checksums of the generated datasets (must match TESTING.md), the rebuilt model
     against the committed scam_model.json (the one inside the app), and every number in the rebuilt
     report against the committed results/report.json.
  3. Extra tests on the committed model (extra_tests.py): error bars, safety guarantees, edge cases, speed.
  4. Optional: the Android parity test (Kotlin must equal Python on 500 payments).
  5. Writes results/TEST_SUMMARY.md and results/test_summary.json. Exit code 0 only if nothing FAILED.
"""
from __future__ import annotations

import hashlib
import json
import os
import platform
import shutil
import subprocess
import sys
import time
from importlib import metadata
from pathlib import Path

HERE = Path(__file__).parent
WORK = HERE / "_repro"
RESULTS = HERE / "results"
SOURCES = ["features.py", "make_dataset.py", "train.py", "export_model.py", "scorer.py", "policy.py",
           "make_test_vectors.py", "evaluate.py", "analyst_discovery.py", "schema.json", "requirements.txt"]

# SHA-256 of the generated datasets (line endings normalised to \n). The generator uses only
# Python's built-in random module, so these are the same on every computer and library version.
EXPECTED_DATA_SHA256 = {
    "train.jsonl": "7615cedab94db55269777d6dd63c61426247f2a2f66a47c6b24488f72d193041",
    "val.jsonl": "7df16c984d0f2d33aae75f4725f5667a25de315cf71e6376dc941d9f223ab506",
    "test.jsonl": "76534b568f8c2f1285ace4bb940b8642c712591b6ee3ab93d1b069526599a93d",
}


def sha256_text(path: Path) -> str:
    return hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()


def run(args: list, cwd: Path) -> float:
    t0 = time.time()
    env = {**os.environ, "PYTHONIOENCODING": "utf-8", "PYTHONDONTWRITEBYTECODE": "1"}
    print(f"\n$ {' '.join(args)}", flush=True)
    subprocess.run([sys.executable, *args], cwd=cwd, env=env, check=True)
    return round(time.time() - t0, 1)


def model_without_version(m: dict) -> dict:
    return {k: v for k, v in m.items() if k != "model_version"}


def numbers_diff(a, b, path="") -> list:
    """Every leaf value that differs between two report.json files (floats compared to 1e-9)."""
    out = []
    if isinstance(a, dict) and isinstance(b, dict):
        for k in sorted(set(a) | set(b)):
            if k != "model_version":
                out += numbers_diff(a.get(k), b.get(k), f"{path}/{k}")
    elif isinstance(a, list) and isinstance(b, list) and len(a) == len(b):
        for i, (x, y) in enumerate(zip(a, b)):
            out += numbers_diff(x, y, f"{path}[{i}]")
    elif isinstance(a, (int, float)) and isinstance(b, (int, float)) and not isinstance(a, bool):
        if abs(a - b) > 1e-9:
            out.append(f"{path}: {b} -> {a}")
    elif a != b:
        out.append(f"{path}: {b!r} -> {a!r}")
    return out


def versions() -> dict:
    v = {"python": platform.python_version(), "platform": platform.platform()}
    for pkg in ("numpy", "scipy", "scikit-learn", "joblib"):
        try:
            v[pkg] = metadata.version(pkg)
        except metadata.PackageNotFoundError:
            v[pkg] = "not installed"
    return v


def android_parity() -> dict:
    app = HERE.parent / "andriod app"
    gradlew = app / ("gradlew.bat" if os.name == "nt" else "gradlew")
    if not gradlew.exists():
        return {"name": "Android: Kotlin = Python on 500 payments", "passed": False, "detail": f"{gradlew} not found"}
    env = dict(os.environ)
    studio_jbr = Path(r"C:\Program Files\Android\Android Studio\jbr")
    if "JAVA_HOME" not in env and studio_jbr.exists():
        env["JAVA_HOME"] = str(studio_jbr)
    print("\n$ gradlew :app:testDebugUnitTest", flush=True)
    r = subprocess.run([str(gradlew), ":app:testDebugUnitTest", "--console=plain"], cwd=app, env=env)
    xml = app / "app/build/test-results/testDebugUnitTest/TEST-com.example.scriptbreak.ml.ScamModelParityTest.xml"
    detail = "Gradle failed" if r.returncode else "ScamModelParityTest: matchesPython, answersOnlyRaise"
    if xml.exists():
        import re
        head = xml.read_text(encoding="utf-8")[:600]
        counts = dict(re.findall(r'(tests|failures|errors|skipped)="(\d+)"', head))
        detail += f" | tests {counts.get('tests')}, failures {counts.get('failures')}, errors {counts.get('errors')}"
    return {"name": "Android: Kotlin = Python on 500 payments", "passed": r.returncode == 0 and xml.exists(), "detail": detail}


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    quick, android = "--quick" in sys.argv, "--android" in sys.argv
    t_start = time.time()

    # ---- 1. rebuild from zero in a scratch copy -------------------------------------------
    if WORK.exists():
        shutil.rmtree(WORK)
    WORK.mkdir()
    for f in SOURCES:
        shutil.copy2(HERE / f, WORK / f)
    shutil.copytree(HERE / "round1", WORK / "round1", ignore=shutil.ignore_patterns("__pycache__"))
    timings = {
        "make_dataset": run(["make_dataset.py"], WORK),
        "train": run(["train.py"], WORK),
        "export_model": run(["export_model.py"], WORK),
        "evaluate": run(["evaluate.py"] + (["--quick"] if quick else []), WORK),
        "discovery_unseen": run(["analyst_discovery.py", "--unseen", "sent_by_mistake"], WORK),
        "discovery": run(["analyst_discovery.py"], WORK),
    }

    # ---- 2. reproducibility -----------------------------------------------------------------
    checks = []
    data_sha = {f: sha256_text(WORK / "data" / f) for f in EXPECTED_DATA_SHA256}
    if all(v is None for v in EXPECTED_DATA_SHA256.values()):
        checks.append({"name": "Datasets match the published checksums", "passed": True,
                       "detail": "no checksums published yet; computed: " + json.dumps(data_sha)})
    else:
        bad = [f for f, v in EXPECTED_DATA_SHA256.items() if v != data_sha[f]]
        checks.append({"name": "Datasets match the published checksums", "passed": not bad,
                       "detail": "all 3 identical" if not bad else f"different: {bad} {data_sha}"})

    sys.path.insert(0, str(HERE))
    import scorer
    from features import featurize
    committed = json.loads((HERE / "scam_model.json").read_text())
    rebuilt = json.loads((WORK / "scam_model.json").read_text())
    if model_without_version(committed) == model_without_version(rebuilt):
        model_detail, model_ok = "identical to the committed model (apart from the version label)", True
    else:
        ta, tb = committed.get("gb", {}).get("trees", []), rebuilt.get("gb", {}).get("trees", [])
        trees_diff = sum(x != y for x, y in zip(ta, tb)) + abs(len(ta) - len(tb))
        rows = [json.loads(l) for l in open(WORK / "data" / "test.jsonl")]
        diff = max(abs(scorer.score_vector(featurize(r["features"]), committed)[0]
                       - scorer.score_vector(featurize(r["features"]), rebuilt)[0]) for r in rows)
        th = max(abs(committed["thresholds"][k] - rebuilt["thresholds"][k]) for k in committed["thresholds"])
        model_ok = diff < 1e-9 and th < 1e-9
        model_detail = (f"{trees_diff} of {len(ta)} trees written differently (library versions break exact ties "
                        f"differently); max score difference on all {len(rows):,} test payments = {diff:.2e}; "
                        f"max threshold difference = {th:.2e}")
    checks.append({"name": "Rebuilt model gives the same scores as the committed model", "passed": model_ok, "detail": model_detail})

    rep_diff = numbers_diff(json.loads((WORK / "results" / "report.json").read_text(encoding="utf-8")),
                            json.loads((HERE / "results" / "report.json").read_text(encoding="utf-8")))
    if quick:
        rep_diff = [d for d in rep_diff if not d.startswith("/hidden_scam_type")]
    checks.append({"name": "Every number in the rebuilt report equals the committed report", "passed": not rep_diff,
                   "detail": "all identical" + (" (hidden-type test skipped: --quick)" if quick else "")
                   if not rep_diff else f"{len(rep_diff)} differ: {rep_diff[:8]}"})

    for name in ("discovery.json", "discovery_unseen_sent_by_mistake.json"):
        a = json.loads((WORK / name).read_text()); b = json.loads((HERE / name).read_text())
        a.pop("model", None); b.pop("model", None)
        checks.append({"name": f"Analyst discovery ({name}) reproduces", "passed": a == b,
                       "detail": "identical" if a == b else "different clusters or rules"})

    # ---- 3. extra tests on the committed model -----------------------------------------------
    import extra_tests
    extra = extra_tests.main(WORK / "data" / "test.jsonl")
    checks += extra["checks"]

    # ---- 4. Android ------------------------------------------------------------------------------
    if android:
        checks.append(android_parity())

    # ---- 5. summary ------------------------------------------------------------------------------
    report = json.loads((HERE / "results" / "report.json").read_text(encoding="utf-8"))
    summary = {"when_utc": time.strftime("%Y-%m-%d %H:%M", time.gmtime()), "environment": versions(),
               "seconds": round(time.time() - t_start), "step_seconds": timings, "dataset_sha256": data_sha,
               "committed_model_version": committed["model_version"], "checks": checks,
               "passed": sum(c["passed"] for c in checks), "failed": sum(not c["passed"] for c in checks),
               "error_bars_95": extra["error_bars_95"], "fairness": extra["fairness_genuine_interrupted"],
               "edge_cases": extra["edge_cases"],
               "negative_amount": extra["negative_amount"], "python_scorer_ms_per_payment": extra["python_scorer_ms_per_payment"]}
    (RESULTS / "test_summary.json").write_text(json.dumps(summary, indent=1, default=str), encoding="utf-8")
    write_markdown(summary, report)
    print(f"\n{summary['passed']} passed, {summary['failed']} failed. See results/TEST_SUMMARY.md")
    sys.exit(1 if summary["failed"] else 0)


def pct(x):
    return f"{x * 100:.1f}%"


def ci(w, per_1000=False):
    if per_1000:
        return f"{w['rate'] * 1000:.1f} per 1,000 (95% range {w['low'] * 1000:.1f}–{w['high'] * 1000:.1f})"
    return f"{pct(w['rate'])} (95% range {pct(w['low'])}–{pct(w['high'])})"


def write_markdown(s, report):
    eb = s["error_bars_95"]
    labels = {k: v["label"] for k, v in report["model"]["by_workflow"].items()}
    L = ["# ScriptBreak test summary\n",
         f"Run {s['when_utc']} UTC on {s['environment']['platform']}, Python {s['environment']['python']}, "
         f"scikit-learn {s['environment']['scikit-learn']}, numpy {s['environment']['numpy']}, scipy {s['environment']['scipy']}. "
         f"Took {s['seconds'] // 60} min {s['seconds'] % 60} s. Committed model version {s['committed_model_version']}.\n",
         f"**{s['passed']} checks passed, {s['failed']} failed.** All data synthetic (generation code in this folder); "
         "these numbers test our assumptions, not real-world accuracy.\n",
         "## Checks\n", "| Result | Check | Detail |", "|---|---|---|"]
    for c in s["checks"]:
        L.append(f"| {'✅ PASS' if c['passed'] else '❌ FAIL'} | {c['name']} | {str(c['detail']).replace('|', '/')} |")
    L += ["\n## Results with 95% error bars (test set: shifted settings, never used in training)\n",
          f"- Scams flagged, 6 in-scope types: **{ci(eb['scams_flagged_in_scope6'])}**",
          f"- Scams flagged, all 8 types: {ci(eb['scams_flagged_all8'])}",
          f"- Genuine payments interrupted: **{ci(eb['genuine_interrupted'], True)}** (budget ≤ 8)",
          f"- Genuine payments given a Nudge: {ci(eb['genuine_nudged'], True)}\n",
          "| Type | Flagged (Pause or Critical) | 95% range | Sessions |", "|---|---|---|---|"]
    for w, v in eb["by_workflow"].items():
        name = labels.get(w, "Genuine (should be low)" if w == "genuine" else w)
        L.append(f"| {name} | {pct(v['rate'])} | {pct(v['low'])}–{pct(v['high'])} | {v['n']:,} |")
    L += ["\n## Fairness: genuine payments interrupted by mistake, by group\n",
          "Reported, not tuned: thresholds are set on validation data only. Budget: 8 per 1,000.\n",
          "| Group | Genuine payments | Interrupted per 1,000 | 95% range |", "|---|---|---|---|"]
    for g, w in s["fairness"].items():
        L.append(f"| {g} | {w['n']:,} | {w['rate'] * 1000:.1f} | {w['low'] * 1000:.1f}–{w['high'] * 1000:.1f} |")
    L += ["\n## Edge cases\n", "| Case | Score | Level | Reasons |", "|---|---|---|---|"]
    for e in s["edge_cases"]:
        L.append(f"| {e['case']} | {e.get('score', e.get('error'))} | {e.get('level', '-')} | {'; '.join(e.get('reasons', []))} |")
    L += [f"\nNegative amount: {s['negative_amount']}.",
          f"\nPython reference scorer: {s['python_scorer_ms_per_payment']} ms per payment (the phone is measured separately).",
          "\n## Dataset checksums (SHA-256, line endings normalised)\n"]
    for f, h in s["dataset_sha256"].items():
        L.append(f"- `{f}`: `{h}`")
    L.append("\nFull model results (per type, AI vs rules vs bank rule, hidden scam types, the 28 stories, failures, "
             "calibration): [REPORT.md](REPORT.md).")
    (RESULTS / "TEST_SUMMARY.md").write_text("\n".join(L) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
