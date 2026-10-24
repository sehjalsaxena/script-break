# ScriptBreak AI model: guide for Deepak

This folder trains ScriptBreak's own AI model and hands it to the Android app.
Everything runs on your laptop, takes about 5 minutes, and uses **synthetic data only**.

## What the model does (simple)

It looks at the facts of one payment (amount, usual amount, on a call or not, new payee or
not, and 12 more) and gives a **scam score from 0 to 1** plus the **top 3 reasons**.
A small safety layer then turns the score into None / Nudge / Pause / Critical.

```
facts about the payment ──► AI model (gradient boosting) ──► score 0.99 + reasons
                                                   │
                                                   ▼
                     safety layer (policy.py): thresholds + fixed guardrails
                                                   │
                                                   ▼
                         NONE / NUDGE / PAUSE / CRITICAL screen on the phone
```

## Setup (once)

```
cd ml
pip install -r requirements.txt        # numpy, scipy, scikit-learn, joblib
```
Python 3.10 or newer.

## Run it (in this order)

| Step | Command | Time | What it does | Output |
|---|---|---|---|---|
| 1 | `python make_dataset.py` | 5 s | Makes the labelled payments | `data/train.jsonl`, `val.jsonl`, `test.jsonl` |
| 2 | `python train.py` | 1 min | Trains 3 models, sets thresholds on validation, picks the best | `models/*.joblib`, `models/selection.json` |
| 3 | `python export_model.py` | 5 s | Saves the chosen model for the phone and checks it matches | `scam_model.json`, `test_vectors.json` |
| 4 | `python evaluate.py` | 3 min | The honest test: per scam type, false alarms, AI vs rules, unseen scam types, 28 stories, failures | `results/REPORT.md`, `results/report.json` |
| 5 | `python analyst_discovery.py --unseen sent_by_mistake` | 40 s | Analyst console data: AI groups missed scams, proposes a rule | `discovery_unseen_sent_by_mistake.json` |

Then give Sambhav: `scam_model.json`, `test_vectors.json`, `discovery_unseen_sent_by_mistake.json`
and the two Kotlin files in `android/`. Give Sehjal `results/report.json` for the Results tab.

**Important:** run steps 1 to 4 again after ANY change, and re-send `scam_model.json`.
Don't commit `data/` or `models/` to GitHub (they're big and can be regenerated; `.gitignore` handles it).

## The data

| Split | Genuine | Scams | Used for |
|---|---|---|---|
| train | 1,40,000 + 10,000 tricky lookalikes | 2,100 × 8 types | Learning |
| val | 30,000 | 450 × 8 types | Choosing the model and thresholds |
| test | 30,000 | 450 × 8 types, **shifted settings, new seeds** | Final numbers, never used for choices |

Total: 2,00,000 genuine and 3,000 per scam type, as declared on the form.
The 8 scam types: digital arrest, fake customer care, fake KYC/bank official, courier, AI voice
clone, hang up then pay, "sent by mistake", task scam over chat.
Every assumption behind the data is written at the top of `round1/generator.py` (Round 1 code).
**Labels come from the scenario we generated, never from the old rules.**

The record format is in `schema.json`. The model never sees `scenario_id`, `workflow`, `label`
or the Q1/Q2 answers.

## Files

| File | What it is |
|---|---|
| `features.py` | Turns a payment into 24 numbers (the ONLY place this is defined) |
| `make_dataset.py` | Dataset maker |
| `train.py` | Trains Logistic Regression, Random Forest, Gradient Boosting; picks the best on validation |
| `export_model.py` | Writes `scam_model.json` (the model as plain JSON) |
| `scorer.py` | Reads `scam_model.json` and scores a payment, no ML library: the same logic as the Kotlin code |
| `policy.py` | Safety layer: thresholds + C1 coaching check + guardrails G1, G2, G3, G4, G5 |
| `make_test_vectors.py` | Python-vs-JSON parity check; writes 500 test payments for Kotlin |
| `evaluate.py` | All the numbers for judges |
| `analyst_discovery.py` | AI pattern discovery for the analyst console |
| `android/ScamModel.kt` | Kotlin scorer + safety layer for the app |
| `android/ScamModelParityTest.kt` | Android unit test: Kotlin must match Python |
| `round1/` | Round 1 code reused: generator, old rules (for comparison only), clustering |
| `MODEL_CARD.md` | One-page description for judges |
| `JUDGE_QA.md` | Questions judges may ask, with answers |

## Things you can try (and how to judge them)

Change one thing, rerun steps 1 to 4, and compare `results/REPORT.md`:
- `train.py`: `n_estimators`, `max_depth`, `learning_rate` of gradient boosting
- `train.py`: `TARGET_INTERRUPT` (6.5 now). Higher catches more scams but bothers more genuine users. Keep the test result ≤ 8.
- `make_dataset.py`: number of lookalikes (10,000 now)

**Judge every change on the validation numbers in `train.py`'s output, not the test report.**
Choosing by the test set would make the test numbers dishonest.
