# ScriptBreak

**Break the scam script before the PIN.**

ScriptBreak is an on-device layer for UPI apps that spots payments matching a coached-scam pattern,
shows the plain facts before the PIN, and brings in family with one tap.

Team RUNTIME, Chitkara University: **Sambhav Jain, Sehjal Saxena, Deepak Rawat**.
RAKSHAM AI Cybersecurity Hackathon finale (Amazon), IIT Delhi. Track 02: AI-Driven Scam Pattern Recognition.

> Demo app with no real money. All data is synthetic, and all names, numbers, banks and UPI IDs are fictional.
> Results test our assumptions on synthetic data; they are not real-world accuracy.

---

## What it does

A scammer on a call ("CBI digital arrest", "bank KYC", "courier customs") talks the victim through a UPI payment.
Just before the PIN, ScriptBreak checks the payment **on the phone** and shows one of four levels:

| Level | What the person sees |
|---|---|
| None | Nothing: the normal payment flow |
| Nudge | One amber line on the Amount screen, no extra tap |
| Pause | "Before you pay": the facts, two questions, "Ask family", Next |
| Critical | Full red screen, a 10-second wait before "Continue anyway", "Ask family", call 1930 |

It **never blocks**: the person always decides. "Ask family" alerts a trusted contact's phone.

---

## How to run the whole project

The project has two parts that run separately:

| Part | Folder | What you need |
|---|---|---|
| The Android app (DemoPay + ScriptBreak) | `andriod app/` | Android Studio, and an Android phone (8.0 or newer) or an emulator |
| The AI kit: data, training, tests | `ml/` | Python 3.10 or newer |

The trained model is already inside the app, so **you can run the app without running the AI kit**.

### Part 1: Run the app

1. Get the code:
   ```bash
   git clone https://github.com/sehjalsaxena/script-break.git
   ```
2. Open Android Studio → **Open** → choose the `script-break/andriod app` folder (not the top folder).
   Wait for "Gradle sync" to finish (the first time takes a few minutes).
3. Connect a phone with **USB debugging** on (Settings → About phone → tap "Build number" 7 times →
   Developer options → USB debugging), or start an emulator.
4. Press the green **Run ▶** button. The app is called **DemoPay**.

From the command line instead (Windows; on macOS/Linux use `./gradlew`):

```bash
cd "andriod app"
gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The "Ask family" alert and money between the demo people go through Firebase, so for those the phones need internet.
Everything else, including the AI check, works offline.

### Part 2: Log in

Tap **Select demo profile** on the login screen, or type these:

| Username | PIN | Who | UPI PIN |
|---|---|---|---|
| `sambhav` | 1111 | Sambhav Jain, the person paying (`sambhav.jain@demoupi`) | 4821 |
| `deepak` | 2222 | Deepak Rawat, a second payer (`deepak.rawat@demoupi`) | 7316 |
| `sehjal` | 3333 | Sehjal Saxena: pays like the others, and is the trusted contact who gets the "Ask family" alerts | 2580 |
| `judge` | 0000 | Judge mode: change any input and watch the AI decide | |
| `analyst` | 9999 | Analyst console: approve the rules the AI proposes | |

### Part 3: Try it (one phone, 5 minutes)

1. Log in as **judge**, switch on **Simulate Live Phone Call (Whole App)**, then log out.
2. Log in as **sambhav** → **Pay UPI ID** → `ramesh.k@demoupi` → Pay → **20000** → Continue.
3. You get **Pause**. Answer **Yes** to the first question → Next → **Critical**.
4. Tap **Ask Sehjal to call me**. Log out, log in as **sehjal**, open **Notifications** (the bell on Home)
   and tap the alert: "Call Sambhav now" / "I've called".
5. Compare a genuine payment: as sambhav, pay **Suresh Verma** (the usual landlord) ₹18,000. Nothing extra appears.

Before rehearsing again, use **Settings → Reset Demo**: every payment you make becomes part of the history the AI learns from.

**Demo QR codes** are in [`docs/demo_qr/`](docs/demo_qr/). Scan one from Home → QR icon, or Judge mode → **Fill from a QR code**:

| Image | Payment | What happens |
|---|---|---|
| `1_scam_refund_qr.png` | ₹20,000 to `ramesh.k@demoupi`, QR says "Bank Refund Desk" | Warns that the QR's name is not the banking name; on a call: Pause |
| `2_tv_gupta_electronics_qr.png` | ₹90,000 TV at a new shop | No call: only the amber line, then the PIN |
| `3_rent_suresh_qr.png` | ₹18,000 rent to the usual landlord | Nothing extra |
| `4/5/6_pay_*_qr.png` | Pay Sambhav, Deepak or Sehjal | The money arrives on that person's phone |
| `7_electrician_manoj_qr.png` | ₹6,000 to a new electrician | On a call: Pause, a genuine false alarm the person gets past with Next |

### Part 4: Run the AI tests

One command rebuilds the data and the model from zero and checks every number we report (about 5 to 8 minutes):

```bash
cd ml
python -m venv .venv
.venv\Scripts\activate              # macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
python run_all_tests.py             # add --android to also run the phone-vs-Python check
```

Expected last line: **`21 passed, 0 failed`**. Results go to `ml/results/TEST_SUMMARY.md` and `ml/results/REPORT.md`.

The Android unit tests (91, including the checks that the phone gives the same scores and decisions as Python):

```bash
cd "andriod app"
gradlew.bat testDebugUnitTest
```

To run the AI steps one by one instead:

| Step | Command | What it does |
|---|---|---|
| 1 | `python make_dataset.py` | Makes the synthetic payments (train, validation, test) |
| 2 | `python train.py` | Trains 3 models, sets the score lines on validation data, picks the best |
| 3 | `python export_model.py` | Saves the model as `scam_model.json` for the phone |
| 4 | `python evaluate.py` | The full test report: per scam type, false alarms, AI vs rules, unseen scam types, 28 stories |
| 5 | `python analyst_discovery.py --unseen sent_by_mistake` | The Analyst console data |

Details, dataset checksums and what each check means: [TESTING.md](TESTING.md).

---

## How it works

```
phone signals + payment history ──► 16 facts ──► AI model (gradient boosting, 200 trees, on the phone)
                                                      │ scam score 0-1
                                                      ▼
                                safety layer: C1 coaching check, guardrails G1-G5
                                                      │
                                         None / Nudge / Pause / Critical + plain reasons
```

- **Signals, read only while the app is open:** a call is active (the phone's audio mode), the screen is shared
  or recorded (Android 15+) or cast to another display, the UPI ID was pasted, the person left the app mid-payment,
  night time, and the payment history (usual amount, new payees, money sent today).
- **Never read:** call audio, the caller's number, the call log, messages, contacts, location or clipboard contents.
- **Real or simulated:** calls, pasting, leaving the app and night are real. Screen sharing is real on Android 15+
  (and casting on any Android). Money from a stranger and bank-flagged payees exist only in Judge mode.
- **The model:** gradient boosting trained only on our synthetic data. It is exported to a 91 KB JSON file and
  scored by our own Kotlin code: no network, no ML library. The phone gives the same scores as Python on 500 test
  payments and the same decisions on 4,960 cases.
- **The safety layer** ([`ml/policy.py`](ml/policy.py), the same logic as `ScamModel.assess` in Kotlin):
  - C1: a Pause or Critical needs at least one sign of coaching. A big purchase alone in a shop gets only the amber line.
  - G1: a bank-flagged payee is always Critical.
  - G2: a payee the bank has warned about gets at least a Nudge, and Critical during a call or screen share.
  - G3: a rule an analyst approved gives at least a Pause.
  - G4: "Yes" to either question gives Critical.
  - G5: a first payment to a new person of 30× the usual amount and ₹20,000 or more gives at least a Pause.
  - Answers only raise the level. If scoring fails, the payment goes through (fail-open).
- **Analyst console:** scam reports the model missed are grouped by what they have in common (clustering),
  a rule is proposed for each group with its false-alarm cost, and a person approves it.
- **QR codes:** read on the phone with ZXing (not an AI model). The name inside a QR is never trusted:
  the payee page shows the banking name and warns if they differ.

---

## Results (synthetic test set: 30,000 genuine + 3,600 scam payments, never seen in training)

| | AI model | Our Round 1 rules | Bank rule (new person ≥ ₹10,000) |
|---|---|---|---|
| Scams stopped, 6 coached-scam types | **79.9%** (95% range 78.3–81.3) | 60% | 24% |
| Genuine payments interrupted per 1,000 | **7.4** (6.5–8.4) | 7.7 | 3.6 |

Per scam type, unseen scam types, the 28 Round 1 stories and the known failures:
[`ml/results/REPORT.md`](ml/results/REPORT.md). All checks with error bars: [`ml/results/TEST_SUMMARY.md`](ml/results/TEST_SUMMARY.md).

---

## Project structure

| Path | What |
|---|---|
| [`andriod app/`](andriod%20app/) | The Android app (Kotlin + Jetpack Compose). Open this folder in Android Studio |
| `andriod app/app/src/main/java/com/example/scriptbreak/ml/` | `ScamModel.kt`: the on-phone scorer and safety layer |
| `andriod app/app/src/main/java/com/example/scriptbreak/logic/` | Payment flow, phone signals, Judge mode, Analyst mode, family alert, QR |
| `andriod app/app/src/main/java/com/example/scriptbreak/ui/` | All screens |
| `andriod app/app/src/test/` | 91 unit tests |
| [`ml/`](ml/) | The AI kit: data generator, training, export, evaluation, analyst discovery, one-command test runner |
| [`reference/`](reference/) | Round 1 Python code (rules engine, generator), reused as declared |
| [`tests/cases.json`](tests/cases.json) | The 28 Round 1 behaviour stories (never used for training) |
| [`docs/`](docs/) | Round 1 deck and the demo QR images |

Key files in `ml/`: `features.py` (the 24 numbers the model sees), `make_dataset.py`, `train.py`, `export_model.py`,
`scorer.py` (scores without any ML library, like the phone), `policy.py` (safety layer), `evaluate.py`,
`analyst_discovery.py`, `run_all_tests.py`, `scam_model.json` (the model inside the app).

---

## What we promised on the disclosure form, and where it is

Paths are under `andriod app/app/src/main/java/com/example/scriptbreak/` unless they start with `ml/`.

| Promise | Where |
|---|---|
| AI scam-score model trained on synthetic sessions | `ml/make_dataset.py`, `ml/train.py`, `ml/export_model.py`; on the phone `ml/ScamModel.kt` |
| Automatic feature extraction from payment history and device state | `logic/PaymentFlow.kt` (`currentPayment`), `logic/Signals.kt` |
| Rules stay as safety guardrails | `ml/policy.py` = `ml/ScamModel.kt` `assess` (C1, G1–G5) |
| Plain reasons for every payment | `ScamModel` reasons, shown on Nudge, Pause and Critical, and in the Safety check on the Amount screen |
| Ask family: alert to a second phone | `logic/FamilyLink.kt` (Firebase), `logic/FamilyBuzz.kt`, `ui/screens/FamilyAlertScreen.kt` |
| Analyst console: AI clusters new scam reports, proposes rules, a human approves | `ml/analyst_discovery.py`; `logic/AnalystMode.kt`, `ui/screens/AnalystScreen.kt` |
| Live scoreboard | Judge mode → Scoreboard: the 28 stories and 400 test payments scored live on the phone |
| Report packet for 1930 | `ui/screens/ReportAndContactScreens.kt` (Success screen → Report payment) |
| Safeguards: answers only raise, Continue anyway, fail-open, humans approve rules, ≤ 8 false alarms per 1,000, OTP for the trusted contact | `ml/policy.py`, `logic/PaymentFlow.kt`; checked in `ml/results/TEST_SUMMARY.md` and the unit tests |
| Judges can vary every input | Judge mode: 16 inputs, 28 stories, load a JSON/CSV/JSONL test file, "Run in app" |

---

## More documents

| Document | What |
|---|---|
| [TESTING.md](TESTING.md) | How we test, how to reproduce every number, known failures and limits |
| [ml/MODEL_CARD.md](ml/MODEL_CARD.md) | One-page description of the model |
| [DISCLOSURE.md](DISCLOSURE.md) | Check-in commit `f14de57`, reused Round 1 code, libraries, AI assistants |
| [docs/ScriptBreak_Round1_deck.pdf](docs/ScriptBreak_Round1_deck.pdf) | Our Round 1 submission |
