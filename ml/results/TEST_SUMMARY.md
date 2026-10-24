# ScriptBreak test summary

Run 2026-10-24 15:44 UTC on Windows-11-10.0.26300-SP0, Python 3.13.14, scikit-learn 1.9.1, numpy 2.5.4, scipy 1.18.1. Took 4 min 44 s. Committed model version 20261024-0701.

**19 checks passed, 0 failed.** All data synthetic (generation code in this folder); these numbers test our assumptions, not real-world accuracy.

## Checks

| Result | Check | Detail |
|---|---|---|
| ✅ PASS | Datasets match the published checksums | all 3 identical |
| ✅ PASS | Rebuilt model gives the same scores as the committed model | 3 of 200 trees written differently (library versions break exact ties differently); max score difference on all 33,600 test payments = 0.00e+00; max threshold difference = 1.11e-16 |
| ✅ PASS | Every number in the rebuilt report equals the committed report | all identical |
| ✅ PASS | Analyst discovery (discovery.json) reproduces | identical |
| ✅ PASS | Analyst discovery (discovery_unseen_sent_by_mistake.json) reproduces | identical |
| ✅ PASS | Answers never lower the level | 33,600 test payments x 8 answer combinations: 0 lowered |
| ✅ PASS | 'No' or no answer changes nothing | 0 payments changed level after 'No' |
| ✅ PASS | G4: 'Yes' at Pause or above gives Critical | 3,111 paused payments answered Yes: 0 not Critical |
| ✅ PASS | Guardrails never lower the model's level | 0 lowered |
| ✅ PASS | G1: bank-flagged payee is always Critical | 2,000 payments made flagged: 0 not Critical |
| ✅ PASS | G2: warned payee + live call is Critical | 0 of 2,000 not Critical |
| ✅ PASS | G2: warned payee without a call is at least Nudge | 0 of 2,000 below Nudge |
| ✅ PASS | G3: an approved analyst rule gives at least Pause | rule(s) ['L1'] matched 450 test payments: 0 below Pause |
| ✅ PASS | No Nudge below Rs 1,000 (except warned payee, G2) | 0 found |
| ✅ PASS | Q1/Q2 answers are never model inputs | 0 of 2,000 changed the model input |
| ✅ PASS | Reasons only name signals that are present (and at most 3) | 25,041 reasons checked: 0 wrong, 0 payments with >3 |
| ✅ PASS | Scoring is deterministic | 0 of 2,000 differed on a second run |
| ✅ PASS | 28 stories: answers never lower the level | 0 stories lowered |
| ✅ PASS | Edge cases score without crashing (score 0-1, valid level) | 10 cases, 0 failed |

## Results with 95% error bars (test set: shifted settings, never used in training)

- Scams flagged, 6 in-scope types: **79.7% (95% range 78.1%–81.2%)**
- Scams flagged, all 8 types: 80.3% (95% range 79.0%–81.6%)
- Genuine payments interrupted: **7.3 per 1,000 (95% range 6.4–8.3)** (budget ≤ 8)
- Genuine payments given a Nudge: 20.2 per 1,000 (95% range 18.7–21.9)

| Type | Flagged (Pause or Critical) | 95% range | Sessions |
|---|---|---|---|
| Task scam over chat | 64.4% | 59.9%–68.7% | 450 |
| Courier / customs | 82.2% | 78.4%–85.5% | 450 |
| Fake customer care / refund | 74.4% | 70.2%–78.3% | 450 |
| Digital arrest | 88.7% | 85.4%–91.3% | 450 |
| Genuine (should be low) | 0.7% | 0.6%–0.8% | 30,000 |
| Hang up, then pay | 79.1% | 75.1%–82.6% | 450 |
| Fake KYC / bank official | 72.9% | 68.6%–76.8% | 450 |
| 'Sent by mistake' return | 100.0% | 99.2%–100.0% | 450 |
| AI voice clone of a relative | 80.9% | 77.0%–84.3% | 450 |

## Fairness: genuine payments interrupted by mistake, by group

Reported, not tuned: thresholds are set on validation data only. Budget: 8 per 1,000.

| Group | Genuine payments | Interrupted per 1,000 | 95% range |
|---|---|---|---|
| First-time users (little history) | 1,758 | 12.5 | 8.3–18.9 |
| Regular users | 28,242 | 7.0 | 6.1–8.0 |
| Paying a person | 9,867 | 18.7 | 16.3–21.6 |
| Paying a merchant | 17,690 | 1.8 | 1.3–2.6 |
| Night (23:00-06:00) | 1,562 | 5.8 | 3.0–10.9 |
| Day | 28,438 | 7.4 | 6.5–8.4 |

## Edge cases

| Case | Score | Level | Reasons |
|---|---|---|---|
| Rs 0 | 0.0008 | NONE |  |
| Rs 1 | 0.0008 | NONE |  |
| Rs 100 crore, tiny median | 0.042 | NONE | The amount is 500 times your usual payment |
| No payment history | 0.0166 | NONE | This is a large payment and you have little payment history |
| Only the amount given | 0.0207 | NONE | This is a large payment and you have little payment history |
| Fields set to None | 0.0022 | NONE |  |
| Unknown payee status/kind | 0.0064 | NONE |  |
| Huge counts | 0.0052 | NONE | You paid several new accounts in the last hour; You have never paid this payee before; This is a repeat payment to a payee you only started paying today |
| Credit exactly equal to amount | 0.9802 | CRITICAL | This sends back the same amount you received from a stranger; You have never paid this payee before |
| Huge money already sent today | 0.0248 | NONE | You have never paid this payee before; Together with today's payments to new payees, this is unusually large |

Negative amount: rejected with ValueError (the app must only score amounts > 0; on any error it lets the payment through).

Python reference scorer: 0.139 ms per payment (the phone is measured separately).

## Dataset checksums (SHA-256, line endings normalised)

- `train.jsonl`: `7615cedab94db55269777d6dd63c61426247f2a2f66a47c6b24488f72d193041`
- `val.jsonl`: `7df16c984d0f2d33aae75f4725f5667a25de315cf71e6376dc941d9f223ab506`
- `test.jsonl`: `76534b568f8c2f1285ace4bb940b8642c712591b6ee3ab93d1b069526599a93d`

Full model results (per type, AI vs rules vs bank rule, hidden scam types, the 28 stories, failures, calibration): [REPORT.md](REPORT.md).
