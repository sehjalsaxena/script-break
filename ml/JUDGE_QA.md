# Judge questions about the AI, with answers (all three of us, learn these)

## One-page technical facts

**Data** (`make_dataset.py`, using the Round 1 generator `round1/generator.py`, Python's built-in `random` only, so it is byte-identical on any computer)
- Each record is one payment: 16 facts about the payment and the phone, plus a label (1 = scam, 0 = genuine). The label comes from the scenario we generated, never from our rules.
- The generator's numbers are written assumptions, taken from police and government advisories about how each scam works (e.g. digital-arrest victims are on a call about 90% of the time).

| Split | Seed | Genuine | Scams |
|---|---|---|---|
| train | 1001 | 1,40,000 + 10,000 "lookalikes" | 2,100 × 8 types |
| validation | 2002 | 30,000 | 450 × 8 |
| test | 3003 | 30,000 | 450 × 8 |

- **Lookalikes** are tricky genuine payments, such as a first payment to a new person while on a call, so the model doesn't learn "new payee = scam".
- **How the test set differs from training:**
  - it uses new seeds, so all payments are new
  - the scam settings are shifted: digital-arrest amounts around 20× usual and more app switching; KYC callers on a call only 70% of the time but pasting more; customer care with more screen sharing; "sent by mistake" on a call only 40% of the time
  - genuine people make 20% more calls
  - the test set was used once, for the final numbers. Thresholds were set on validation.
- The **28 Round 1 stories** are never training data. They are a separate behaviour check.

**The 24 features** (`features.py`, mirrored exactly in Kotlin)

| Group | Features | Count |
|---|---|---|
| Amount | log amount, has history (≥ 10 payments in 90 days), log(amount ÷ 90-day median), the same including today's payments to new payees, log of money already sent to new payees today | 5 |
| Live signals | call now, call in the last 30 min, screen share | 3 |
| Payee status | known, new, recent (first paid < 7 days ago), warned, flagged (one-hot) | 5 |
| Payee kind | person, merchant, biller (one-hot) | 3 |
| Velocity | repeat payments to this payee today, other new payees in the last hour (both capped at 5) | 2 |
| Money from a stranger | money received from an unknown sender in 48 h, this amount matches it (within 2%), paying back the sender | 3 |
| Behaviour | UPI ID pasted, left the app mid-payment | 2 |
| Time | night (23:00–06:00) | 1 |

The model never sees the Q1/Q2 answers, the scenario label or anything personal.

**Model** (`train.py`)
- **Chosen:** gradient boosting, 200 trees of depth 3, learning rate 0.1, subsample 0.8, seed 7 (scikit-learn). Each tree corrects the errors of the ones before it.
- **Compared on validation at the same false-alarm budget:** logistic regression (scams caught 83.5%), random forest of 100 trees (83.3%), gradient boosting (86.3%), so we chose gradient boosting.
- **Thresholds**, set on validation so about 6.5 per 1,000 genuine payments are interrupted (budget ≤ 8): Nudge > 0.071 (and ₹1,000+), Pause > 0.486, Critical > 0.942.
- **On the phone:** exported to a 91 KB JSON file and scored by our own Kotlin. The scores equal Python's on 500 test payments; a unit test checks this.
- **Reasons:** each tree records how much each feature pushed the score. We add these up per group and show the top 3 groups that pushed it up.

**Results on the test set** (synthetic):
- Scams stopped: 79.9% of the 6 in-scope types (95% range 78.3–81.3); 80.4% of all 8 types
- Genuine payments interrupted: 7.4 per 1,000 (6.5–8.4, the top just above our budget of 8); Nudge only on 20 per 1,000
- Precision 93% on this test mix; ROC AUC 0.994; accuracy 97.2%. Don't lead with accuracy: calling everything genuine already scores 89%.

**AI vs our Round 1 rules, same test set**

| Scam type | Rules | AI | Why the AI wins |
|---|---|---|---|
| Hang up, then pay | 23% | **79%** | The rules need a live call; the AI also weighs a recent call, a pasted ID and the amount |
| Task scam over chat | 31% | **64%** | No call at all; the AI combines new payees in an hour, money sent today and pasting |
| Voice clone | 64% | **81%** | |
| Courier | 69% | **83%** | |
| Customer care | 59% | **74%** | |
| KYC | 63% | **73%** | |
| Digital arrest | 82% | **89%** | |
| Sent by mistake | 100% | 100% | |

Demo of the difference: Judge mode → Stories → **S12**, "Told to hang up and pay; whole payment made after the call". Round 1 rules: Nudge. AI: Critical (0.98).

**Limitations, say these yourself**
- **Synthetic data:** the results test our assumptions, not real-world accuracy. A pilot on real, consented data is the next step.
- **Unseen mechanisms are weak:** retrained without "sent by mistake", the model catches only 11% of them. That's why the analyst console exists.
- **Named failures:** S06 (split small payments to several new accounts, missed), S08 (first small payment, Nudge only), L04 (₹40,000 deposit to a new landlord, wrongly Paused).
- **Fairness:** first-time users get more false alarms, 12.5 vs 7.1 per 1,000. Most false alarms are genuine first payments to a new person during a call.
- **The score is not a real-world probability:** on our test mix 11% of payments are scams, far more than in real life.

## What the phone really detects, and what is simulated

| Signal | In our app | In a real deployment |
|---|---|---|
| Active call (phone or most internet calls) | **Real**, from the audio mode, while the app is open. Best effort: some calling apps may not set it | Inside the bank/UPI app, with the permitted phone-state API, tested per phone maker |
| Call in the last 30 minutes | Real only if our app was open during the call. We don't read the call log, by design | The same permitted API, with the user's consent |
| Screen sharing | **Real on Android 15+** (this app's screen being recorded or shared), plus casting to another display on any Android. On older phones: the Judge-mode switch | Android 15+ API, secure windows, and the payment provider's remote-access-app detection |
| Pasted UPI ID, left the app, night | **Real** | Same |
| Payment history (usual amount, new payees) | Synthetic starting history plus the real payments made in the demo | The UPI app's own transaction records |
| Money received from a stranger, flagged payee (G1) | **Not available in our app**; Judge mode only | The bank's transaction feed and the fraud registry |

Say it this way: "The app really detects calls, pasting, app switching and, on Android 15+, screen sharing. A production version would live inside the payment app with permitted signals from the payment provider. We don't claim to catch every call or screen-sharing session on every phone."

## Questions and answers

**What model is it, and why?**
Gradient boosting: 200 small decision trees, each correcting the previous ones. We trained three
(logistic regression, random forest, gradient boosting) and picked the one that caught the most
scams on validation data at the same false-alarm rate: 86% vs about 83%. Tree models are strong on
table data like ours, and small enough to run on a phone.

**Why not just if/else rules?**
We had rules in Round 1. On the same unseen test data, the model flags 80% of in-scope scam sessions
vs 60% for our rules, with fewer false alarms (7.4 vs 7.7 per 1,000). Rules have hard cut-offs
(5× the usual amount); the model weighs all the signals together. We keep a few fixed rules as
safety guardrails, because some things should never depend on a model.

**Sambhav buys a ₹90,000 TV in a shop. Is there a scary popup?**
No. The model alone scores it 0.54 (54%), a Pause: 138× the usual payment, to a new payee. But our
safety layer has a coaching check (C1): a full-screen Pause needs at least one sign that someone is
steering the payment (a call, a recent call, screen sharing, a pasted UPI ID, leaving the app, new
payees today, money from a stranger). In a shop there is none, so only the amber line shows and the payment goes
straight to the PIN. On the test set this check lost 0 scams and kept the 28 stories the same.
The risk we accept: a scam with no sign the phone can see gets only the amber line. If such a wave
appears, the analyst console can approve a rule for it (guardrail G3 still raises to Pause).

**I paid ₹35,000 to a stranger with no call. Why was that not stopped?** (fixed 25 Oct)
The model scored it 0.48, just under the Pause line (0.486): with no call, screen sharing, pasted ID or app
switch, nothing showed someone steering the payment. We added guardrail G5: a first payment to a new *person*
of at least 30× the usual amount and ₹20,000 is always at least a Pause. It catches 4 more test scams for
+0.13 false alarms per 1,000 (7.27 → 7.40, budget 8). Shops are excluded, so a TV in a shop still gets only the
amber line. We never look at the payee's name: scammers use ordinary names.

**What data did you train on?**
Synthetic only: 2 lakh genuine payments and 3,000 of each of 8 scam types, generated from
written assumptions based on police and government advisories. No real data exists that links
phone calls to UPI payments, and real bank data is private. Labels come from the scenario we
generated, not from our rules, so the model doesn't just copy the rules.

**How do you know it isn't memorising?**
Three ways: (1) the test set uses different random seeds and **shifted** scam settings; (2) we
retrain with a whole scam type removed and test on it; (3) the 28 hand-written Round 1 stories,
which were never training data.

**What does it miss?**
Splitting money into small payments to several new accounts (story S06), a scammer's first
small payment (S08), and scam types with a new mechanism it never saw ("sent by mistake": 11%
when unseen). We show these in the report.

**So how do you handle new scams?**
The analyst loop: reports of payments the model missed are clustered by the AI (no scam-type
labels), it proposes a rule per pattern, backtests the false alarms, and a human approves. It
found the unseen "sent by mistake" pattern as one pure cluster and proposed a rule catching 100%
of it for +0.05 false alarms per 1,000. Then the model is retrained with the new reports.

**What does the score mean? Is 0.9 = 90% scam?**
Roughly, on our test data (calibration table in the report): payments scored 0.8–0.9 were 90% scams.
In real life scams are far rarer than in our test mix, so the real probability would be lower.
That's why we use the score to pick a level, not as a verdict, and why the screen says
"This matches a common scam", never "This is a scam".

**Where do the reasons come from?**
Each tree split adds or subtracts from the score; we add up how much each fact pushed the score
(path attribution), group them (amount, payee, call…) and show the top 3 that pushed it up and are
actually present.

**How would a scammer beat it?**
Tell the victim to hang up first, split into small payments, use an old payee, or pay through a
merchant account. Some of these are caught (hang up then pay: 79%), and splitting isn't (S06). A
scammer can't change everything at once: someone guiding live, a new account and an unusual
amount are what the scam needs.

**Does it run on the phone?**
Yes. A 91 KB JSON file plus about 300 lines of Kotlin, offline. Speed is timed live on the phone: Judge mode → Results scores 400 test payments and shows the time. The Kotlin
output matches Python exactly on 500 test payments (parity test).

**What happens if the model is wrong?**
False alarm: one extra screen, and "Continue anyway" always works. Missed scam: report to 1930.
Model crash: the payment goes through (fail-open).

**What would a bank need for a pilot?**
Real consented fraud reports to retrain, 8 weeks of shadow mode (scores computed, nothing shown)
to measure false alarms, then an opt-in group of customers over 55.
