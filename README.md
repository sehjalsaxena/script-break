# ScriptBreak

**Break the scam script before the PIN.**

ScriptBreak is an on-device layer for UPI apps that spots payments matching a coached-scam pattern,
shows the plain facts before the PIN, and brings in family with one tap.

Team RUNTIME, Chitkara University: Sambhav Jain, Sehjal Saxena, Deepak Rawat.
RAKSHAM AI Cybersecurity Hackathon finale (Amazon), IIT Delhi. Track 02: AI-Driven Scam Pattern Recognition.

> Demo app with no real money. All data is synthetic, and all names, numbers, banks and UPI IDs are fictional.
> Results test our assumptions on synthetic data; they are not real-world accuracy.

## What it does

A scammer on a call ("CBI digital arrest", "bank KYC", "courier customs") talks the victim through a UPI payment.
Just before the PIN, ScriptBreak checks the payment on the phone and shows one of four levels:

| Level | What the person sees |
|---|---|
| None | Nothing: the normal payment flow |
| Nudge | One amber line on the amount screen, no extra tap |
| Pause | "Before you pay": the facts, two questions, "Ask family", Next |
| Critical | Full red screen, a 10-second wait before "Continue anyway", "Ask family", call 1930 |

It **never blocks**: the person always decides. "Ask family" alerts a trusted relative's phone through Firebase.

## How it works

```
phone signals + payment history ──► 16 facts ──► AI model (gradient boosting, 200 trees, on the phone)
                                                      │ scam score 0-1
                                                      ▼
                                safety layer: C1 coaching check, guardrails G1-G4
                                                      │
                                         None / Nudge / Pause / Critical + plain reasons
```

- **Signals (read only while the app is open):**
  - a call is active (audio mode only)
  - screen sharing or recording (Android 15+), or the screen mirrored to another display
  - the UPI ID was pasted
  - the person left the app mid-payment
  - night time
  - the payment history: usual amount (90-day median), new payees, money sent today
- **Never read:** call audio, the caller's number, the call log, messages, contacts, location or clipboard contents.
- **Model:** trained only on synthetic data from our generator ([ml/](ml/)). It is exported to JSON (91 KB) and scored
  in Kotlin with no network and no ML library. The Kotlin scores match Python exactly on 500 test payments.
- **Safety layer** ([ml/policy.py](ml/policy.py), same logic in Kotlin `ScamModel.assess`):
  - C1: a Pause or Critical needs at least one sign of coaching. A big purchase alone in a shop gets only the amber line.
  - G1: a bank-flagged payee is always Critical.
  - G2: a payee paid after an earlier warning gets at least a Nudge.
  - G3: an analyst-approved rule gives at least a Pause.
  - G4: "Yes" to a question gives Critical.
  - Answers only raise the level. If scoring fails, the payment goes through (fail-open).
- **Analyst console:** scam reports the model missed are clustered, a rule is proposed for each cluster, and a human approves it.

## Results (synthetic test set: 30,000 genuine + 3,600 scams, never seen in training)

| | AI model | Our Round 1 rules | Bank rule (new person ≥ ₹10,000) |
|---|---|---|---|
| Scams stopped, 6 in-scope types | **79.7%** (95% range 78.1–81.2) | 60% | 24% |
| Genuine payments interrupted per 1,000 | **7.3** (6.4–8.3) | 7.7 | 3.6 |

Per scam type, scam types the model never saw, the 28 Round 1 stories and the known failures are in
[ml/results/REPORT.md](ml/results/REPORT.md). All checks are in [ml/results/TEST_SUMMARY.md](ml/results/TEST_SUMMARY.md).

## What we promised on the disclosure form, and where it is

Paths below are under `andriod app/app/src/main/java/com/example/scriptbreak/` unless they start with `ml/`.

| Promise | Where |
|---|---|
| AI scam-score model trained on synthetic sessions | `ml/make_dataset.py`, `ml/train.py`, `ml/export_model.py`; on the phone `ml/ScamModel.kt` |
| Automatic feature extraction from payment history and device state | `logic/PaymentFlow.kt` (`currentPayment`, learned usual payment), `logic/Signals.kt` (call, screen share) |
| Rules stay as safety guardrails | `ml/policy.py` = `ml/ScamModel.kt` `assess` (C1, G1–G4) |
| Plain reasons for every payment | `ScamModel.reasons`, shown on Nudge, Pause and Critical; "What the AI sees" on the Amount screen |
| Ask family: alert to a second phone | `logic/FamilyLink.kt` (Firebase), `logic/FamilyBuzz.kt`, `ui/screens/FamilyAlertScreen.kt` |
| Analyst console: AI clusters new scam reports, proposes rules, a human approves | `ml/analyst_discovery.py`; `logic/AnalystMode.kt`, `ui/screens/AnalystScreen.kt` (log in as analyst) |
| Live scoreboard | Judge mode → Results tab: the 28 stories and 400 test payments scored live on the phone, plus the full test results |
| Report packet for 1930 | `ui/screens/ReportAndContactScreens.kt` (Success screen → Report) |
| Safeguards: answers only raise, Continue anyway, fail-open, humans approve rules, ≤ 8 false alarms per 1,000, OTP for the trusted contact | `ml/policy.py`, `logic/PaymentFlow.kt`; checked in `ml/results/TEST_SUMMARY.md` and the unit tests |
| Judges can vary every input | Judge mode: 16 inputs, 28 stories, load a JSON/CSV/JSONL test file with error bars, "Run in app" |

## Repository

| Folder | What |
|---|---|
| [`andriod app/`](andriod%20app/) | The Android app (Kotlin + Jetpack Compose). Open this folder in Android Studio |
| `andriod app/app/src/main/java/com/example/scriptbreak/ml/` | `ScamModel.kt`: the on-phone scorer and safety layer |
| `andriod app/app/src/main/java/com/example/scriptbreak/logic/` | Payment flow, phone signals, Judge mode, Analyst mode, family alert |
| `andriod app/app/src/main/java/com/example/scriptbreak/ui/` | All screens |
| `andriod app/app/src/test/` | 63 unit tests |
| [`ml/`](ml/) | Data generator, training, export, evaluation, analyst discovery, one-command test runner |
| [`reference/`](reference/) | Round 1 Python code (rules engine, generator), reused as declared |
| [`tests/cases.json`](tests/cases.json) | The 28 Round 1 behaviour stories (never used for training) |
| [`docs/`](docs/) | Demo script, manual test checklist, Round 1 deck |
| [`SPEC.md`](SPEC.md) | Exact screen wording (English and Hindi) |

## Run the app

1. Open the `andriod app` folder in Android Studio and press Run. Needs Android 8.0 (API 26) or newer.
   From the command line, `gradlew.bat assembleDebug` (Windows) or `./gradlew assembleDebug` builds
   `app/build/outputs/apk/debug/app-debug.apk`.
2. Log in (demo accounts, listed on the login screen):

| Username | PIN | Opens | UPI PIN |
|---|---|---|---|
| `sharma` | 1111 | Mr. Sharma, 64: the person paying | 4821 |
| `priya` | 2222 | Priya: a second payer | 7316 |
| `rohit` | 3333 | Rohit: the family phone that gets "Ask family" alerts | |
| `judge` | 0000 | Judge mode: change any of the 16 inputs, the 28 stories, load a test file, results | |
| `analyst` | 9999 | Analyst console: approve rules the AI proposes | |

A demo to try on one phone: log in as judge, switch on "Simulate a phone call", log out, and log in as sharma.
Then Pay to UPI ID `ramesh.k@demoupi` ₹20,000 → Pause → answer Yes → Critical → Ask Rohit.
Then log out and log in as rohit: the alert is there. For two or three phones, see [docs/DEMO_SCRIPT.md](docs/DEMO_SCRIPT.md).

## Run the tests

```bash
# Android: 63 unit tests (needs a JDK, e.g. Android Studio's)
cd "andriod app"
gradlew.bat testDebugUnitTest

# AI: rebuild data and model from zero and check every reported number (about 5 minutes)
cd ml
pip install -r requirements.txt
python run_all_tests.py
```

Step-by-step instructions, the dataset checksums and what each check means are in [TESTING.md](TESTING.md).

## Disclosure

The check-in commit is `f14de57`. The Round 1 code we reused, the libraries and the AI assistants are listed in [DISCLOSURE.md](DISCLOSURE.md).
