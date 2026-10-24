# ScriptBreak: how we test, and how to reproduce every number

Team RUNTIME (Sambhav Jain, Sehjal Saxena, Deepak Rawat). All data is synthetic.
The numbers below test our model **under our written assumptions**; they are not real-world accuracy.

## 1. Reproduce everything with one command

Needs Python 3.10 or newer (we used 3.13.14). About 5 minutes on a laptop.

```bash
cd ml
python -m venv .venv
.venv\Scripts\activate            # Windows   (macOS/Linux: source .venv/bin/activate)
pip install -r requirements.txt
python run_all_tests.py           # add --android to also run the Kotlin parity test (needs Android Studio's JDK)
```

Expected last line: **`20 passed, 0 failed`** (`21 passed, 0 failed` with `--android`).
Results: `ml/results/TEST_SUMMARY.md` (all checks, error bars, edge cases) and `ml/results/REPORT.md` (full model report).

What the command does:
1. Rebuilds everything from zero in `ml/_repro/` (a scratch copy, so the committed model is never overwritten):
   generates the data, trains three models, exports the chosen one, evaluates it, runs the analyst discovery.
2. Checks the rebuild against what we committed: dataset checksums, model scores, and every number in the report.
3. Runs the extra tests on the committed model (the exact file inside the Android app).
4. Writes a pass/fail summary. Exit code 0 only if nothing failed.

Library versions we ran with: scikit-learn 1.9.1, numpy 2.5.4, scipy 1.18.1 (the runner prints yours).
The datasets are byte-identical on any computer (the generator uses only Python's built-in `random`).
With different scikit-learn versions, 3 of the 200 trees can break exact ties differently; the scores and
every reported number stay identical on all 33,600 test payments, and the runner checks exactly that.

## 2. The data, and why it is synthetic

We are not aware of any public dataset that links a UPI payment to the phone's state at that moment
(an active call, screen sharing, a pasted payee ID), which is exactly what a coached scam looks like.
Public payment datasets (for example PaySim) have no call or screen-share signals, and real bank data
is private. So we generate labelled sessions from written assumptions, and publish the generator.

| Item | Where |
|---|---|
| Generator and every assumption behind it | `ml/round1/generator.py` (Round 1 code, unchanged; same as `reference/generator.py`) |
| Dataset builder (splits, seeds, shifted test settings) | `ml/make_dataset.py` |
| Record format (16 inputs, label) | `ml/schema.json` |

| Split | Genuine | Scams | Used for |
|---|---|---|---|
| train | 1,40,000 + 10,000 genuine lookalikes | 2,100 × 8 scam types | learning |
| val | 30,000 | 450 × 8 | choosing the model and thresholds |
| test | 30,000 | 450 × 8, **new seeds, shifted scam settings, 20% more genuine calls** | final numbers only |

Labels come from the generated scenario (scam = 1, genuine = 0), never from rules. The model never
sees the scenario ID, the scam type, the label, or the answers to the two questions.
The 28 Round 1 behaviour stories (`tests/cases.json`) are never training data.

Dataset checksums (SHA-256, line endings normalised to `\n`):

| File | SHA-256 |
|---|---|
| `train.jsonl` | `7615cedab94db55269777d6dd63c61426247f2a2f66a47c6b24488f72d193041` |
| `val.jsonl` | `7df16c984d0f2d33aae75f4725f5667a25de315cf71e6376dc941d9f223ab506` |
| `test.jsonl` | `76534b568f8c2f1285ace4bb940b8642c712591b6ee3ab93d1b069526599a93d` |

## 3. What we test (completeness)

| Area | Test | Where |
|---|---|---|
| Accuracy | Scams flagged per scam type (8 types), with 95% error bars | `REPORT.md`, `TEST_SUMMARY.md` |
| False alarms | Genuine payments interrupted and nudged per 1,000, with 95% error bars | same |
| Fair comparison | AI vs Round 1 rules vs a bank-style amount rule, on the same test data | `REPORT.md` |
| Unseen scams | Retrain without each scam type, test on it | `REPORT.md` |
| Behaviour stories | The 28 Round 1 stories through model + safety layer | `REPORT.md` |
| Failures | Missed scams by type, false-alarm patterns, named failure stories | `REPORT.md` |
| Calibration | Does a score of 0.8 mean about 80% scam? | `REPORT.md` |
| Safety | Answers only raise the level; "No" changes nothing; guardrails G1–G4 always fire and never lower; only the C1 coaching check lowers, to Nudge, and only with no coaching sign; no popup without a coaching sign; no Nudge under ₹1,000; Q1/Q2 never reach the model | `TEST_SUMMARY.md` |
| Explanations | Every reason shown names a signal that is really present (25,041 reasons checked) | same |
| Fairness | Genuine interruptions per 1,000: first-time vs regular users, person vs merchant, night vs day | `TEST_SUMMARY.md` |
| Robustness of code | Edge cases: ₹0, ₹1, ₹100 crore, no history, missing or unknown fields; determinism | same |
| Phone = training | Kotlin scorer gives the same score and reasons as Python on 500 payments | `andriod app/.../ScamModelParityTest.kt` |
| Reproducibility | Dataset checksums; rebuilt model and report equal the committed ones | `TEST_SUMMARY.md` |

## 4. Headline results (test set, synthetic)

| | AI model | Round 1 rules | Bank rule (new person ≥ ₹10,000) |
|---|---|---|---|
| Scams flagged, 6 in-scope types | **79.7%** (95% range 78.1–81.2%) | 60% | 24% |
| Genuine payments interrupted | **7.3 per 1,000** (95% range 6.4–8.3) | 7.7 | 3.6 |

Per type, hidden-type results and the 28 stories: `ml/results/REPORT.md`.

## 5. Known failures and limits (we show these openly)

- **Splitting** into small payments to several new accounts during a call (story S06, ₹1,900): missed.
- **A scammer's first small payment** (S08): only a Nudge.
- **A genuine ₹40,000 deposit to a new landlord**, no call (L04): wrongly paused; the user taps Continue.
- Most false alarms are genuine first payments to a new person during a call.
- **A scam mechanism it has never seen** ("sent by mistake" removed from training): only 11% caught.
  The analyst loop then finds it as one pure cluster and proposes a rule that catches 100% of it.
- The false-alarm rate is 7.3 per 1,000; the **upper end of its 95% range (8.3) is slightly above our budget of 8**.
- **Fairness finding: first-time users (little payment history) are interrupted 12.5 per 1,000
  (95% range 8.3–18.9) vs 7.0 for regular users**, and payments to a new *person* far more than to
  merchants. With no history, the model leans on the amount alone. We report it rather than tune it on
  test data; the fix would be a separate threshold for low-history users, set on validation data and
  checked in shadow mode. Table: `ml/results/TEST_SUMMARY.md`.
- Precision (93%) is optimistic: scams are about 11% of the test set, far more than in real life.
- A negative amount is rejected by the scorer; the app only scores amounts above ₹0 and lets the
  payment go through if scoring ever fails.
- Everything is synthetic. A bank pilot would retrain on consented fraud reports and run 8 weeks of
  shadow mode (scores computed, nothing shown) before showing any screen.

## 6. Still being added

- Attack tests (split payments, hang up first, merchant accounts, removed signals).
- Stress tests (more genuine calls, missed call detection).
- Time per payment measured on the demo phone.
