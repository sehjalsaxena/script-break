# Judge questions about the AI, with answers (Deepak, learn these)

**What model is it, and why?**
Gradient boosting: 200 small decision trees, each correcting the previous ones. We trained three
(logistic regression, random forest, gradient boosting) and picked the one that caught the most
scams on validation data at the same false-alarm rate: 86% vs about 83%. Tree models are strong on
table data like ours, and small enough to run on a phone.

**Why not just if/else rules?**
We had rules in Round 1. On the same unseen test data, the model flags 80% of in-scope scam sessions
vs 60% for our rules, with fewer false alarms (7.3 vs 7.7 per 1,000). Rules have hard cut-offs
(5× the usual amount); the model weighs all the signals together. We keep a few fixed rules as
safety guardrails, because some things should never depend on a model.

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
Yes. A 91 KB JSON file plus about 300 lines of Kotlin, 0.1 ms per payment, offline. The Kotlin
output matches Python exactly on 500 test payments (parity test).

**What happens if the model is wrong?**
False alarm: one extra screen, and "Continue anyway" always works. Missed scam: report to 1930.
Model crash: the payment goes through (fail-open).

**What would a bank need for a pilot?**
Real consented fraud reports to retrain, 8 weeks of shadow mode (scores computed, nothing shown)
to measure false alarms, then an opt-in group of customers over 55.
