# ScriptBreak live demo: three phones

All data is synthetic and all names are fictional. "Demo app, no real money."

## Preliminary round (30 → 10): 12 minutes, demo + conversation, no slides

Judges use the product and ask questions; a technical team checks the code (plagiarism, completeness) at the same time.
Stage 1 marks: works end to end on judge-picked inputs (25), **spot-check results with error rates on unseen data and
named failures (30)**, own engineering that any member can explain (15), alert and response flow with safeguards (15),
delivery against the Round 1 plan (15).

| Time | Who | What |
|---|---|---|
| 0:00–0:45 | Sambhav | The one-liner (README, first line). "A scammer on a call talks the victim through the payment. Banks check amounts; we check the pattern, on the phone, before the PIN." |
| 0:45–4:00 | All three | **Scenario 1, legitimate:** rent to the usual landlord → nothing; ₹90,000 TV in a shop → amber line only (scenes 1, 1b). **Scenario 2, obviously suspicious:** scam call → ₹20,000 to a new payee → Pause ("What the AI sees" shows why) → "No, No" stays → Yes → Critical → Sehjal's phone buzzes → Sehjal calls → "I've called" (scenes 2–5). Keep it moving: no explanations mid-flow |
| 4:00–6:30 | Judge + Deepak | Hand Phone C (judge mode) to a judge: "Change anything." Inputs tab, then **Run in app**. Then Test file → load the sample, or their own file: error rates with 95% ranges |
| 6:30–8:30 | Deepak (or Sambhav) | **AI vs rules:** Judge → Stories → S12 "hang up and pay": Round 1 rules Nudge, AI Critical; hang-up scams 23% (rules) vs 79% (AI) on the same test set. Results tab: 80% vs 60% (our rules) vs 24% (bank rule) at 7.4 per 1,000 false alarms, with 95% ranges. Scam types never seen (sent by mistake only 11%). **Named failures:** S06 (small splits to several new accounts, missed), S08 (first small payment, Nudge only), L04 (₹40,000 new-landlord deposit, wrongly Paused), most false alarms = genuine first payment to a new person during a call, first-time users get 1.8× more false alarms (12.5 vs 7.1 per 1,000). "We report these instead of tuning on test data." Reproduce: `python run_all_tests.py`, 20 checks |
| 8:30–9:30 | Sambhav | **Scenario 3, a scam the model misses:** analyst on Phone C → **Run in app first: the "sent by mistake" payment goes straight to the PIN (missed)** → the AI grouped 89 such missed reports, all one pattern, and proposes one rule (catches 100%, +0.05 false alarms per 1,000) → a human taps **Approve** → Run in app again → Pause. "The AI proposes, a human approves before anything reaches phones." No architecture talk |
| 9:30–12:00 | All | Questions. Changes from Round 1: web app → native Android (a website can't see calls); rules decide → trained AI decides, rules stay as guardrails. Deliberate cuts: no push to a fully closed app (needs a server); screen-share detection is Android 15+ |

Who answers what (but everyone must manage the 30-second version of everything):
- **Sambhav:** payment flow, signals, safety layer (C1, G1–G5), Firebase alert, Judge and Analyst modes.
- **Sehjal:** screens, wording (SPEC.md), Hindi, read-aloud, big text and buttons.
- **Deepak:** data generator, model choice, thresholds, results, failures ([ml/JUDGE_QA.md](../ml/JUDGE_QA.md)).

For the technical team, have ready:
- the repo link and the check-in commit `f14de57`
- [README.md](../README.md), which maps every form promise to the file that implements it
- [DISCLOSURE.md](../DISCLOSURE.md)
- a laptop with the repo, where `gradlew.bat testDebugUnitTest` (90 tests) and `python run_all_tests.py` (21 passed) were already run, with the output on screen
- the APK at `andriod app/app/build/outputs/apk/debug/app-debug.apk` on the laptop and a USB stick

## Who holds what

| Phone | Person | Logged in as | Role in the story |
|---|---|---|---|
| A | Sambhav Jain | `sambhav` / 1111 (UPI PIN 4821) | Sambhav Jain, the person being scammed |
| B | Sehjal Saxena | `sehjal` / 3333 (UPI PIN 2580) | Sehjal, the trusted contact ("Ask family"); can also pay like the others |
| C | Deepak Rawat | first the "scammer" calling A, then `judge` / 0000 and `analyst` / 9999 | The caller, then the judges' phone |

Other logins: `deepak` / 2222 (UPI PIN 7316).

## Before the judges arrive (5 minutes)

1. Install the **same build** on all three phones. Turn on internet (Wi-Fi or data) on all three. Set the clock to automatic.
2. Phone B: log in as sehjal and allow notifications. It opens on Home like the others; alerts arrive as a phone notification and in Notifications (the bell). Keep the app open if you can. If it was closed or logged out, the alert is still there when Sehjal logs in (up to 24 hours), marked with the time it was sent.
3. Phone A: log in as sambhav. Open Settings (Home → Settings) → **Reset demo**. The usual payment should read ₹650.
4. Phone C: save Phone A's number so Deepak can call it. Phone B: save Phone A's number as "Sambhav".
5. Screen share: check Phone A's Android version (Settings → About phone).
   - **Android 15 or newer:** real screen sharing in a Meet or WhatsApp video call is detected.
   - **Older:** casting or mirroring to a TV is detected. Otherwise use Judge mode → **Simulate screen sharing** on Phone A (log in as judge, switch it on, log out, log in as sambhav).
6. Between runs: Phone A → Settings → **Reset demo**. Earlier payments change the AI's inputs (new payees today, usual payment), so every run should start the same.

## The story (about 6 minutes)

**1. Normal day: no interruption (30 s).** Phone A, no call: pay **Suresh Verma ₹18,000** (rent, the usual landlord). No warning, PIN 4821, paid.
*Say:* "Normal payments are never interrupted. The AI only speaks up when the pattern looks like a coached scam."

**1b. A big genuine purchase: no popup (30 s).** Still no call: pay **Gupta Electronics ₹90,000** (a TV).
The amber line appears ("…138× your usual payment"), tap **What the AI sees**: score 0.54 but **Nudge**, then straight to the PIN.
*Say:* "The model alone would pause this. Our coaching check says a full-screen stop needs a sign that someone is steering
the payment: a call, screen sharing, a pasted ID, switching apps. In a shop there is none, so there's just one line."

**1c. Optional, if a judge asks about QR codes (40 s).** On a call, Home → QR icon → the camera opens → point it at
`1_scam_refund_qr.png` shown on Phone C or a laptop (or tap "Pick a QR image instead").
The payee page shows the real banking name (RAMESH KUMAR) and warns that the QR says "Bank Refund Desk". Pay → ₹20,000 is
already filled in → Pause: "This sends ₹20,000 **out of** your account".
*Say:* "The classic trick is 'scan this to receive your refund'. A QR is just another way to enter a payee, so the same AI
checks it, and we never trust the name written inside the QR."

**1d. Optional: real money flow between two phones (20 s).** Phone A pays **Deepak Rawat** ₹500. Phone C (logged in as
`deepak`) shows "Received ₹500 from Sambhav Jain", and the balance goes up. Known person, normal amount: no warning at all.

**2. The scam call (1 min).** Deepak calls Phone A from Phone C as a "CBI officer" (digital-arrest script). Phone A's Home shows **"Active phone call detected"**.
Pay to UPI ID → type `ramesh.k@demoupi` → amount **₹20,000**.
- The amber line appears while typing: "First payment to Ramesh K. ₹20,000 is 31× your usual payment…"
- Tap **What the AI sees**: on a call, new payee, usual ₹650 from 42 payments, score 0.88 (88%).

Tap Continue → **Pause** ("Before you pay", the reasons, Q1 and Q2).

**3. Coached answers don't help the scammer (20 s).** The caller says "press No". Answer No to both: the level stays Pause. Answers can only raise the level.

**4. The truth comes out → Critical (30 s).** Change Q1 to **Yes** → Next → **Critical** (red). Point at the last line: "If someone is telling you to ignore this screen, that is the scam." Show EN | हिंदी and read-aloud.

**5. Family breaks the script (1 min).** On Phone A tap **Ask Sehjal to call me**. Phone A shows "Sehjal has been alerted".
- Phone B **vibrates** and shows a phone notification: "Sambhav may be on a scam call". Sehjal **taps the notification** and the alert opens: Sambhav, ₹20,000, Ramesh K., the reasons, and "Sehjal can't see the account or stop the payment". (It is also in Sehjal's Notifications, the bell on Home.)
- Sehjal taps **Call Sambhav now**. Phone A shows "Sehjal is calling you now".
- Sambhav hangs up on the "officer" (Phone C) and answers Sehjal.
- Sehjal taps **I've called**. Phone A shows "Sehjal says they have called you", and the alert is deleted from Firebase.
- Phone A: **Wait 30 minutes**, so the payment is not made.

**6. Screen sharing (40 s).** Phone A starts a video call with screen sharing (or uses the simulate switch). Home shows **both** banners: call and screen sharing. Pay Ramesh **₹2,000** → **Critical**, with both reasons listed.
*Say:* "Even a small amount: screen sharing plus a new payee is the remote-access pattern."

**7. A genuine user gets past a false alarm (40 s).** Reset demo first. On a call, pay **Manoj Electricals** (`manoj.elec@demoupi`) **₹6,000** → Pause → **Continue anyway** → PIN → paid in two taps.
*Say:* "It never blocks. About 7 in 1,000 genuine payments get a Pause, and getting past one takes seconds."

**8. Judges take over (Phone C, open-ended).** Log in as **judge**.
- **Inputs:** change any of the 16 inputs and see the level and score live. **Run in app** puts those inputs through the real payment screens on Phone C.
- **Stories:** the 28 Round 1 stories.
- **Test file:** load the 400-payment sample or the judges' own JSON/CSV and get error rates with 95% intervals.
- **Results:** AI vs rules vs bank rule, per-type numbers, known failures.

**9. The AI learns a new scam wave (Phone C, 1 min).** Log out and log in as **analyst**. The model never saw "sent by mistake" scams.
1. Tap **Run in app** BEFORE approving: the stranger's "please return my ₹5,000" payment goes straight to the PIN. The model misses it.
2. Back on the analyst screen: the AI grouped the missed reports, found one pure group of 89, and proposes one rule (catches 100%, +0.05 false alarms per 1,000).
3. Tap **Approve** → **Run in app** → now it's a Pause.
4. Show that returning the money to the real sender still passes (the rule excludes it).
*Say:* "The AI proposes, a human approves. It never changes the phone by itself."

## If something goes wrong

| Problem | Fix |
|---|---|
| Phone B didn't buzz | Is B online and showing **● Connected**? Notifications allowed for DemoPay? If B was logged out or the app was closed, log in as sehjal: the alert is in Notifications (the bell) with "Sent at …"; tap it. |
| No "on call" banner | The call must be active while DemoPay is open. Plan B: Judge mode → Simulate a phone call. |
| Levels differ from the rehearsal | Tap **What the AI sees** on the Amount screen: one of the facts differs (call, pasted ID, left the app, payments made earlier). Reset demo. |
| No internet at the venue | Same-phone fallback: Phone A asks Sehjal, log out, log in as sehjal on Phone A: the alert is in Notifications (the bell). |

## Honest limits (say them if asked)

- Sehjal's phone buzzes only while DemoPay is running on it. If the app is closed, the alert waits in Firebase (up to 24 hours, deleted once Sehjal taps "I've called") and opens when Sehjal logs in. A buzz on a fully closed app needs Firebase Cloud Messaging and a server, which we did not build.
- Screen-share detection is real on Android 15+ (this app being recorded or shared) and for casting/mirroring on any Android. On older phones we use the Judge-mode switch.
- Analyst-approved rules apply on the phone where they were approved. In production they would ship as a signed rules update.
- The trusted-contact change (OTP + 24 hours) is shown on the phone, but alerts in this demo always go to Sehjal's demo account.
- All results are on synthetic data. They test our assumptions, not real-world accuracy.
