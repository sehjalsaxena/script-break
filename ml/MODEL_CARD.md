# Model card: ScriptBreak scam-score model

**Team RUNTIME (Sambhav Jain, Sehjal Saxena, Deepak Rawat). Built at the RAKSHAM finale, IIT Delhi.**
Code drafted with an AI coding assistant (Claude); trained, run and checked by the team.

## What it is
A gradient-boosted decision-tree classifier (scikit-learn, 200 trees, depth 3) that gives one
UPI payment a **scam probability from 0 to 1** and the **top 3 reasons**. It runs on the phone:
exported as a 91 KB JSON file, scored by about 300 lines of Kotlin, in about 0.1 ms, with no network
and no ML library.

## Inputs (16 facts, turned into 24 numbers)
Amount, usual (median) payment, payment count, call now, call in the last 30 minutes, screen share,
payee status (known / new / recent / warned / flagged), payee kind (person / merchant / biller),
repeat payments to this payee today, other new payees in the last hour, money sent to new payees
today, money received from a stranger, whether this payee sent it, pasted UPI ID, left and
returned to the app, night.
**Never used:** call audio, caller number, messages, contacts, location, clipboard contents.

## Training data
Synthetic only: no bank, customer or platform data. 2,00,000 genuine sessions (+10,000 extra
tricky lookalikes in training) and 3,000 sessions for each of 8 scam types, generated from written
assumptions based on public police and government advisories (`round1/generator.py`).
Labels come from the generated scenario, never from rules.

## How it decides (model + safety layer)
| Score | Level |
|---|---|
| above 0.942 | Critical |
| above 0.486 | Pause (facts + two questions) |
| above 0.071 and ₹1,000+ | Nudge (one line) |
| otherwise | None |

The thresholds are set on validation data so that about 6.5 per 1,000 genuine payments are
interrupted (budget ≤ 8).

**Fixed guardrails on top** (`policy.py`, same in Kotlin):
- G1: a bank-flagged payee is always Critical.
- G2: a warned payee gets at least a Nudge, or Critical with a live call.
- G3: a rule approved by analysts gives at least a Pause.
- G4: "Yes" to Q1 or Q2 at Pause or above gives Critical. Answers can only raise the level.
- In the app: "Continue anyway" is always available, and if scoring fails the payment goes through.

## Results (test set: 30,000 genuine + 3,600 scams, shifted settings, never seen in training)
| | AI model | Round 1 rules | Bank amount rule |
|---|---|---|---|
| Scam sessions flagged, 6 in-scope types | **80%** | 60% | 24% |
| Genuine payments interrupted per 1,000 | **7.3** | 7.7 | 3.6 |

By scam type: digital arrest 89%, courier 82%, voice clone 81%, hang up then pay 79%, fake customer
care 74%, fake KYC 73%, task scam over chat 64%, "sent by mistake" 100%. ROC AUC 0.994.

**A scam type it never saw:** retrained without each type in turn, it still flags 67–83% of the
five call-based types and 70% of "hang up, then pay". But it flags only **11% of "sent by mistake"**
and 38% of chat task scams: a new *mechanism* needs new examples. That is what the analyst loop
is for. Given a month of reports, it groups the 89 unseen "sent by mistake" reports into one pure
cluster and proposes a rule that catches 100% of them for +0.05 false alarms per 1,000; an analyst approves it.

**28 Round 1 stories:** 14 of 16 scams flagged, 2 of 10 genuine interrupted (the same totals as
the Round 1 rules, but different cases).

## Known failures (we show these)
- **Splitting into small payments** to several new accounts during a call (story S06, ₹1,900): score 0.06, missed.
- **First small payment** to a fake bank official (S08): Nudge only.
- **New landlord deposit**, ₹40,000, no call (L04): wrongly Paused (genuine user taps Continue).
- Most false alarms: a genuine first payment to a new person during a call.
- Missed scams mostly look like normal payments: small amounts, no call, known-looking patterns.

## Limits
- The numbers test the model **under our synthetic assumptions**, not real-world accuracy.
  Precision on the test mix (93%) is optimistic because scams are about 11% of it, far more than in real life.
- A bank would retrain on consented, labelled fraud reports and run 8 weeks of **shadow mode**
  (scores computed, nothing shown) before going live.
- Device signals (call, screen share) depend on Android version and permissions.
