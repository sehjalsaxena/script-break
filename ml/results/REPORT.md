# ScriptBreak AI model: test report

Model: **gradient_boosting** (version 20261024-0637). All data synthetic. Test set: 30,000 genuine + 3,600 scam sessions, shifted settings, never seen in training.

## Headline

| | AI model | Round 1 rules (v0.3) | Bank amount rule (new person ≥ ₹10,000) |
|---|---|---|---|
| Scam sessions flagged (6 in-scope types) | **80%** | 60% | 24% |
| Scam sessions flagged (all 8 types) | **80%** | 61% | 20% |
| Genuine payments interrupted per 1,000 (budget ≤ 8) | **7.3** | 7.7 | 3.6 |

Other: Nudge on 20 per 1,000 genuine payments; ROC AUC 0.994; precision 93% on this test mix (scams are ~11% of it, far more than in real life, so real precision would be lower).

## By scam type (flagged = Pause or Critical)

| Scam type | AI model | Round 1 rules | Never seen in training* |
|---|---|---|---|
| Digital arrest | 89% | 82% | 83% |
| Fake customer care / refund | 74% | 59% | 72% |
| Fake KYC / bank official | 73% | 63% | 67% |
| Courier / customs | 82% | 69% | 81% |
| AI voice clone of a relative | 81% | 64% | 78% |
| Hang up, then pay | 79% | 23% | 70% |
| 'Sent by mistake' return | 100% | 100% | 11% |
| Task scam over chat | 64% | 31% | 38% |

*Retrained with that scam type removed from the training data, then tested on it.

## The 28 Round 1 stories (model + safety layer)

Scams flagged: 14 of 16. Genuine interrupted: 2 of 10. Same level as the Round 1 design: 18 of 28.

| ID | Story | Round 1 | AI | Score |
|---|---|---|---|---|
| S01 | Digital arrest; victim confirms the 'verification' story | CRITICAL | CRITICAL | 0.961 |
| S02 | Digital arrest; coached victim answers 'No' to both questions | PAUSE | CRITICAL ⚠ | 0.961 |
| S03 | Digital arrest, day 2: same account again after a warning | CRITICAL | CRITICAL | 0.994 |
| S04 | Fake customer care: pay Rs 4,000 to 'release' a refund | CRITICAL | CRITICAL | 0.631 |
| S05 | Fake KYC update on screen-share; Rs 9,999 kept under Rs 10,000 | CRITICAL | CRITICAL | 0.988 |
| S06 | Split payments: 3rd Rs 1,900 transfer to new accounts in an hour | PAUSE | NONE ⚠ | 0.061 |
| S07 | 'Parcel with drugs' courier call; told to keep it secret | CRITICAL | CRITICAL | 0.725 |
| S08 | Fake bank official: first Rs 3,500 'to secure your account' | NUDGE | NUDGE | 0.179 |
| S09 | Same official asks for a third Rs 3,500 to the same account | PAUSE | PAUSE | 0.539 |
| S10 | Rs 1,999 sent a 5th time to one account; the 4th was paused | CRITICAL | CRITICAL | 0.583 |
| S11 | Told to hang up and pay; had checked balance during the call | PAUSE | CRITICAL ⚠ | 0.96 |
| S12 | Told to hang up and pay; whole payment made after the call | NUDGE | CRITICAL ⚠ | 0.984 |
| S13 | First-time digital user (no history) in a digital-arrest call | PAUSE | CRITICAL ⚠ | 0.978 |
| S14 | Scammer collects through a merchant account during the call | PAUSE | PAUSE | 0.793 |
| S15 | 'Sent by mistake' scam: return Rs 5,000 to a different account | PAUSE | CRITICAL ⚠ | 0.994 |
| S16 | AI voice clone of a son: 'Rs 30,000 now, don't tell Dad' | CRITICAL | CRITICAL | 0.84 |
| B01 | Task scam coached over chat only, no call | NUDGE | NUDGE | 0.137 |
| B02 | Task scam over chat: 3rd new account within an hour | PAUSE | CRITICAL ⚠ | 0.996 |
| L01 | Rs 350 at the usual kirana store | NONE | NONE | 0.0 |
| L02 | Monthly rent Rs 18,000 to the usual landlord | NONE | NONE | 0.002 |
| L03 | First bill to an allow-listed electricity biller | NONE | NONE | 0.004 |
| L04 | Rs 40,000 deposit to a new landlord, no call | NUDGE | PAUSE ⚠ | 0.627 |
| L05 | Father on a call with his son sends Rs 500 to son's friend | NONE | NONE | 0.028 |
| L06 | Rs 25,000 first purchase at a new shop, during a call | PAUSE | PAUSE | 0.697 |
| L07 | Rs 30,000 to sister (paid before) while on a call with her | NONE | NONE | 0.006 |
| L08 | New electrician guides a Rs 6,000 payment on a call | PAUSE | NUDGE ⚠ | 0.337 |
| L09 | Returns Rs 2,000 sent by mistake to the person who sent it | NUDGE | NONE ⚠ | 0.042 |
| L10 | First-time digital user: Rs 1,200 at a new shop | NONE | NONE | 0.008 |

## Where it fails

Missed scams by type: Hang up, then pay 94, Fake customer care / refund 115, Courier / customs 80, Fake KYC / bank official 122, Task scam over chat 160, AI voice clone of a relative 86, Digital arrest 51

Most common genuine false alarms:

- new person, on call: 126
- new person, no call: 52
- new merchant, on call: 19
- new merchant, no call: 10
- recent person, on call: 3
- warned person, on call: 3

## Calibration (does a score of 0.8 mean ~80% scam?)

| Score | Payments | Mean score | Actually scam |
|---|---|---|---|
| 0.0-0.1 | 29,311 | 0.00 | 0.01 |
| 0.1-0.2 | 437 | 0.14 | 0.25 |
| 0.2-0.3 | 286 | 0.25 | 0.42 |
| 0.3-0.4 | 259 | 0.35 | 0.61 |
| 0.4-0.5 | 229 | 0.45 | 0.67 |
| 0.5-0.6 | 173 | 0.55 | 0.77 |
| 0.6-0.7 | 212 | 0.65 | 0.80 |
| 0.7-0.8 | 267 | 0.75 | 0.82 |
| 0.8-0.9 | 379 | 0.85 | 0.90 |
| 0.9-1.0 | 2,047 | 0.97 | 0.98 |

These numbers test the model under our synthetic assumptions (make_dataset.py). They are not real-world accuracy.
