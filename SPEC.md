# ScriptBreak rulebook (SPEC)

This is the rulebook of the ScriptBreak decision engine, written **only from the Round 1 code**:
`reference/engine.py`, `reference/rulepack_v0.3.json` and `reference/cases.json`. If this document
and `engine.py` ever disagree, `engine.py` wins and this document is wrong.

The Round 1 slide deck is in `docs/ScriptBreak_Round1_deck.pdf`. It is the reference for screen
wording, the 48-hour MVP scope (page 10) and the demo script. For engine behaviour, the code wins.

ScriptBreak runs inside the UPI app after the payee and amount are entered and before the PIN
screen. It looks for the shape of a *coached* payment: someone guiding the victim live, a payee
never paid before, and an amount out of character.

From the module docstring, `assess(session, pack)` is a pure function:

- it runs on the phone: no network call, no call audio, no message text, no contacts
- every input is observable by the host bank/UPI app while it is open
- the user's answers can only **raise** the level, never lower it
- learned rules come from the pattern-discovery loop (`discover.py`) and are plain data in the
  rule pack; a rule enters a pack only after analysts approve it

All data used anywhere in this project is synthetic.

---

## 1. Levels

| Level | Rank | Meaning |
|---|---|---|
| `NONE` | 0 | Nothing shown |
| `NUDGE` | 1 | One inline line on the payment screen, no extra tap |
| `PAUSE` | 2 | Full screen: facts first, then two questions |
| `CRITICAL` | 3 | Warning screen, family option, 10-second confirmation delay |

`RANK = {NONE: 0, NUDGE: 1, PAUSE: 2, CRITICAL: 3}`. "Flagged" or "interrupted" in the reports means
rank >= 2 (Pause or Critical).

---

## 2. Inputs: `Session`

All amounts are whole rupees. Field names are the Python names (the ML kit's `ml/schema.json`
uses the same 16 names).

| Field | Type | Default | Meaning |
|---|---|---|---|
| `amount` | int | *(required)* | This payment, in rupees |
| `median_90d` | int | `0` | User's median payment over 90 days (computed on the phone) |
| `payments_90d` | int | `50` | Number of payments in 90 days |
| `call_now` | bool | `false` | A phone or internet call is active during this payment |
| `call_seen_30m` | bool | `false` | A call was active while this app was open in the last 30 min |
| `screen_share` | bool | `false` | Screen sharing / recording, or a remote-access app, is active |
| `payee_status` | str | `"known"` | `known` \| `new` \| `recent` (first paid < 7 days ago) \| `warned` \| `flagged` |
| `payee_kind` | str | `"person"` | `person` \| `merchant` \| `biller` (allow-listed large biller) |
| `same_payee_24h` | int | `0` | Earlier payments to this new/recent payee in the last 24 h |
| `new_payees_60m` | int | `0` | Earlier payments to *other* new payees in the last 60 min |
| `sent_new_24h` | int | `0` | Rupees already sent to new/recent payees in the last 24 h |
| `credit_unknown_48h` | int | `0` | A credit from a sender never transacted with, last 48 h (0 = none) |
| `payee_is_sender` | bool | `false` | This payee is the person who sent that credit |
| `payee_pasted` | bool | `false` | Payee ID was pasted rather than picked from history |
| `app_switch` | bool | `false` | User left the app and came back during the payment flow |
| `night` | bool | `false` | 23:00 to 06:00 |
| `q1` | bool or none | `None` | Told the payment is needed to verify, protect or release money |
| `q2` | bool or none | `None` | Someone asked them to keep this payment secret (family, bank) |

Groups of fields, as commented in the code:

- **Live-guidance context** (`call_now`, `call_seen_30m`, `screen_share`) is sampled only while the
  host app is open.
- **Payee** and **payment pattern** fields come from the host app's own payment records.
- `payee_pasted`, `app_switch` and `night` are "recorded for incident reports; no hand-written
  rule uses them". They only appear as tokens, so learned rules (R6) can use them.
- `q1` / `q2` are "asked only at Pause; None = not asked or skipped".

`warned` means the user paid this payee after an earlier Pause or Critical warning (a Nudge does
not count; from the Round 1 deck). `flagged` means the account appears in a fraud report shared
with the bank.

---

## 3. Threshold constants

"Constants of the rule pack; calibrated in bank shadow mode."

| Constant | Value | Meaning |
|---|---|---|
| `AMOUNT_MULTIPLE` | `5` | Amount at least 5x the user's 90-day median payment ... |
| `AMOUNT_FLOOR` | `2000` | ... and at least Rs 2,000 |
| `MIN_HISTORY` | `10` | Fewer payments than this in 90 days = "no reliable history" |
| `NO_HISTORY_FLOOR` | `5000` | Fixed threshold for users without history |
| `CREDIT_MATCH` | `0.02` | Outgoing amount within 2% of a recent credit = "matches" |
| `NUDGE_FLOOR` | `1000` | No inline notes on payments below Rs 1,000 |

## 4. Feature switches (`FEATURES`)

These exist for the ablation study (`ablation.py`). The shipped values are:

| Switch | Value |
|---|---|
| `call_seen_30m` | `true` |
| `merchants_count` | `true` |
| `repeat_same_payee` | **`false`** |
| `cumulative` | `true` |
| `fan_out` | `true` |
| `no_history_floor` | `true` |
| `screen_share_rule` | `true` |

Why `repeat_same_payee` is OFF (code comment): the ablation showed it cost more false alarms than it
caught scams; adding up today's payments to new payees ("cumulative") covers splitting to the same
account instead.

---

## 5. Helper functions

**`has_history(s)`**: `s.payments_90d >= MIN_HISTORY and s.median_90d > 0`.
Note that the defaults (`payments_90d = 50`, `median_90d = 0`) give **no** history.

**`is_big(s, amount)`**:

- with history: `amount >= AMOUNT_FLOOR and amount >= AMOUNT_MULTIPLE * s.median_90d`
- without history: `FEATURES.no_history_floor and amount >= NO_HISTORY_FLOOR`
  (the fixed floor is switched off only in the ablation study)

**`credit_match(s)`**: with `c = s.credit_unknown_48h`: `c > 0 and abs(s.amount - c) <= CREDIT_MATCH * c`.

**`tokens(s)`**: the event vocabulary written to the on-phone incident record (and to opt-in
reports). It is deliberately wider than what the hand-written rules use, so the discovery loop can
find patterns no rule covers yet. Learned rules match against these tokens.

| Token | When present |
|---|---|
| `call_now` | `call_now` |
| `call_seen_30m` | `call_seen_30m` (not gated by the feature switch) |
| `screen_share` | `screen_share` |
| `payee_new` | `payee_status` is `new` or `recent` (any `payee_kind`, billers included) |
| `payee_warned` | `payee_status == "warned"` |
| `payee_flagged` | `payee_status == "flagged"` |
| `payee_merchant` | `payee_kind == "merchant"` |
| `payee_biller` | `payee_kind == "biller"` |
| `amount_big` | `is_big(s, amount)` |
| `amount_cumulative` | `payee_new` and `is_big(s, amount + sent_new_24h)` and not `is_big(s, amount)` (not gated by the feature switch) |
| `repeat_same_payee` | `payee_new` and `same_payee_24h >= 1` (not gated by the feature switch) |
| `many_new_payees` | `new_payees_60m >= 2` |
| `low_history` | not `has_history(s)` |
| `credit_unknown_48h` | `credit_unknown_48h > 0` |
| `amount_matches_credit` | `credit_match(s)` |
| `payee_is_sender` | `payee_is_sender` |
| `payee_pasted` | `payee_pasted` |
| `app_switch` | `app_switch` |
| `night` | `night` |

---

## 6. Evidence groups C, P, A (`evidence(s)`)

### C: live guidance

- **C** = `call_now` or (`call_seen_30m` and `FEATURES.call_seen_30m`) or `screen_share`
- **strong C** = `screen_share`

### P: new payee

- **P** = `payee_status` in (`new`, `recent`) and `payee_kind != "biller"` and
  (`FEATURES.merchants_count` or `payee_kind != "merchant"`)
- **strong P** = `payee_status` in (`warned`, `flagged`)

Strong P is **not** a superset of P: a warned or flagged payee has strong P but not P. The
reported group list counts P as present if either P or strong P holds.

### A: unusual amount

- `big` = `is_big(s, amount)`
- `cumulative` = `FEATURES.cumulative` and P and not `big` and `is_big(s, amount + sent_new_24h)`
- `repeat` = (`FEATURES.repeat_same_payee` and P and `same_payee_24h >= 1`) or
  (`FEATURES.fan_out` and `new_payees_60m >= 2`)
- **A** = `big` or `cumulative` or `repeat`

**A has no strong form.** (Fan-out does not require P.)

### Reasons, in this order

1. C:
   - if `call_now`: "You are on a call right now"
   - else if `call_seen_30m`: "You were on a call while using this app in the last 30 minutes"
     (shown even when the `call_seen_30m` switch is off)
   - if `screen_share`: "Your screen is being shared or a remote-access app is active"
2. P:
   - `new` and P: "You have never paid this payee before"
   - `recent` and P: "You first paid this payee less than 7 days ago"
   - `warned`: "You paid this payee after an earlier warning"
   - `flagged`: "This account appears in a fraud report shared with your bank"
3. A:
   - `big` with history: "The amount is {N} times your usual payment", where
     N = `amount / median_90d` formatted with Python `:.0f` (round to nearest, exact halves to even)
   - `big` without history: "This is a large payment and you have little payment history"
   - `cumulative`: "Together with today's other payments to new payees, this is unusually large"
   - `repeat`: "This is a repeat payment to a payee you only started paying today"
     (the same text is used when `repeat` comes from fan-out)

### Groups

`groups` = those of `C`, `P`, `A` that are present, in that order, where P is present if P or
strong P.

---

## 7. Decision (`assess(s, pack)`)

### Step 1: hand-written rules R1 to R4

| Rule | Fires when |
|---|---|
| **R1** | C and P and A |
| **R2** | strong C and P and `FEATURES.screen_share_rule` |
| **R3** | C and strong P |
| **R4** | `payee_status == "flagged"` (no other condition) |

R1 and R2 use plain P, so a warned/flagged payee never triggers them.

### Step 2: learned rules from the rule pack

For each rule `r` in `pack.learned`, in order: it fires if **every** token in `r.all` is in
`tokens(s)` and **no** token in `r.none` (default `[]`) is. A firing rule appends `r.id` to the
rule list and `r.explain` to the reasons.

The pack's `then`, `name`, `source` and `approved_by` fields are **not read by the engine**. A firing
learned rule has exactly the effect of a hand-written rule (Pause, or Critical with K1 to K4).

### Step 3: level

**If any rule fired** (R1 to R6), the escalators are checked:

| Escalator | Condition | Extra reason |
|---|---|---|
| **K1** | `q1` is true | "You said you were told this is needed to verify, protect or release money" |
| **K2** | `q2` is true | "You said someone asked you to keep this payment secret" |
| **K3** | strong P | *(none)* |
| **K4** | strong C and A | *(none)* |

Level = `CRITICAL` if any escalator holds, otherwise `PAUSE`. `rules` = fired rules followed by the
escalators, e.g. `["R1", "R2", "K4"]`.

**Otherwise, Nudge.** "One inline line, no extra tap. Never for payees you have paid before, never for
everyday small amounts: those are most of life, and notes on them breed alert fatigue."

Level = `NUDGE` with `rules = ["N1"]` if
(P and (C or A) and `amount >= NUDGE_FLOOR`) **or** `payee_status == "warned"`.

**Otherwise** level = `NONE`, `rules = []`.

Reasons and groups are always returned, even at `NONE`.

### Consequences worth knowing (all follow from the rules above)

- A flagged payee is always Critical (R4 always fires, K3 always holds).
- A warned payee during live guidance is Critical (R3 + K3); without live guidance it gets only a
  Nudge (S10 vs the known limit noted in the deck).
- A payment made entirely after the call has no C, so R1 cannot fire: Nudge at most, unless a
  learned rule fires (known limit S12).

---

## 8. The two questions

Asked only on the Pause screen.

- **Q1**: "Were you told this payment is needed to verify, protect or release your money?"
- **Q2**: "Has anyone asked you to keep this payment secret from your family or bank?"

(Question text from the Round 1 deck; the engine only sees the booleans.)

**Answers can only raise the level, never lower it.** In the code this holds by construction:
`q1` / `q2` are read only after a rule has fired (so the level is already at least Pause), and
only a `true` answer has an effect (it adds K1 / K2 and so raises Pause to Critical). `false` and
`None` (not asked or skipped) behave identically. A coached victim who answers "No" to both stays
at Pause (S02).

---

## 9. Rule packs

- **v0.2** = the hand-written rules after the adversarial review of v0.1. No learned rules:
  `{ "version": "0.2", "learned": [] }`.
- **v0.3** = v0.2 + rules found by the discovery loop and approved by analysts (`rulepack_v0.3.json`,
  `based_on: "0.2"`):

| Id | `all` tokens | `none` tokens | Explain (shown as a reason) |
|---|---|---|---|
| **R5** | `amount_matches_credit`, `payee_new` | `payee_is_sender` | "This sends back money you received from someone you have never paid, but to a different account. Real mistaken payments are reversed by the sender's bank, not by you." |
| **R6** | `app_switch`, `many_new_payees`, `payee_new`, `payee_pasted` | *(none)* | "This is your third payment to a new account within an hour, using details copied from another app. Online 'task' and 'investment' scams ask for payments exactly like this." |

R5 catches the "sent by mistake" scam (S15); R6 catches the chat-coached task scam (B02). Both
are NUDGE on v0.2 and PAUSE on v0.3; every other case gives the same level on both packs.
(`approved_by` names in the pack are role placeholders.)

---

## 10. Naive baseline (`naive_baseline(s)`)

"A common bank-style rule, for comparison: warn on a first payment to a person of Rs 10,000+."

`payee_status == "new"` and `payee_kind == "person"` and `amount >= 10000`.

It returns a boolean (warn or not). It is used only for comparison in reports and never affects
the level.

---

## 11. Behaviour checks

`cases.json` holds 28 synthetic cases: 16 scams (`S01` to `S16`), 2 chat-only boundary cases
(`B01`, `B02`) and 10 genuine payments (`L01` to `L10`). A case passes only if:

- with pack v0.3, `tier == expected`
- with pack v0.2, `tier == expected_v02` if present, otherwise `expected`

Expected levels describe the **designed** behaviour, including known limits (S08 and S12 are
scams we expect to get only a Nudge) and accepted false alarms (L06 and L08 are paused). A case's
expected level is never edited to make a test pass.

---

## 12. Screen wording

English text is from the Round 1 deck. `{…}` are values filled in by the UI. The engine's
`reasons` list supplies the fact lines. The Hindi column is a first draft only.

### NONE

Nothing is shown. The payment goes straight to the PIN screen.

### NUDGE: one inline line above the Pay button, no extra tap

| Element | English | Hindi (TODO: native speaker to check) |
|---|---|---|
| Line, bold part | First payment to {payee}. | {payee} को पहला भुगतान। |
| Line, rest (big amount) | {amount} is {N}× your usual payment. Check you know who this is. | {amount} आपके सामान्य भुगतान का {N} गुना है। पक्का कर लें कि आप इन्हें जानते हैं। |
| Line, rest (otherwise; proposed, not in the deck) | {first reason}. Check you know who this is. | {first reason}। पक्का कर लें कि आप इन्हें जानते हैं। |
| Button | Pay {amount} | {amount} भुगतान करें |

### PAUSE: facts first, then two questions

| Element | English | Hindi (TODO: native speaker to check) |
|---|---|---|
| Title | Before you pay | भुगतान करने से पहले |
| Facts | This sends {amount} out of your account to {payee}. | यह आपके खाते से {amount} {payee} को भेजता है। |
| Fact lines | *(each item of `reasons`)* | *(Hindi for each reason: TODO)* |
| Q1 | Were you told this payment is needed to verify, protect or release your money? | क्या आपसे कहा गया है कि आपके पैसे की जाँच, सुरक्षा या उसे छुड़ाने के लिए यह भुगतान ज़रूरी है? |
| Q2 | Has anyone asked you to keep this payment secret from your family or bank? | क्या किसी ने आपसे यह भुगतान अपने परिवार या बैंक से छिपाकर रखने को कहा है? |
| Answers | Yes / No | हाँ / नहीं |
| Family | Not sure? Ask {contact} to call me | पक्का नहीं? {contact} से कहें कि मुझे कॉल करें |
| Button | Next | आगे |

### CRITICAL: warning, family option, 10-second confirmation delay

The message body must start with the title line and **end with** the last line below; action
buttons come after the message.

| Element | English | Hindi (TODO: native speaker to check) |
|---|---|---|
| Title | Stop. This matches a common scam. | रुकिए। यह एक आम धोखाधड़ी जैसा है। |
| Line | You are not in trouble. You are in control of this payment. | आप किसी मुसीबत में नहीं हैं। इस भुगतान का फ़ैसला आपके हाथ में है। |
| Line | Police, CBI, RBI and banks never ask you to move money to verify, protect or release it. | पुलिस, CBI, RBI और बैंक कभी भी पैसे की जाँच, सुरक्षा या उसे छुड़ाने के लिए पैसे भेजने को नहीं कहते। |
| Amount | {amount} to {payee}, a new payee | {amount}, {payee} को (नया प्राप्तकर्ता) |
| Fact lines | *(each item of `reasons`)* | *(Hindi for each reason: TODO)* |
| Last line | If someone is telling you to ignore this screen, that is the scam. | अगर कोई आपसे इस स्क्रीन को अनदेखा करने को कह रहा है, तो वही धोखाधड़ी है। |
| Button | Wait 30 minutes | 30 मिनट रुकें |
| Button | Ask {contact} to call me | {contact} से कहें कि मुझे कॉल करें |
| Button | Continue anyway (10 s) | फिर भी जारी रखें (10 सेकंड) |
| Footer | Already paid? Report now: call 1930 | भुगतान कर चुके हैं? अभी रिपोर्ट करें: 1930 पर कॉल करें |

"Continue anyway" is always available: the person decides.
