# ScriptBreak live demo: three phones

All data is synthetic and all names are fictional. "Demo app, no real money."

## Who holds what

| Phone | Person | Logged in as | Role in the story |
|---|---|---|---|
| A | Sambhav Jain | `sharma` / 1111 (UPI PIN 4821) | Mr. Sharma, 64, the person being scammed |
| B | Sehjal Saxena | `rohit` / 3333 | Rohit, his son and trusted contact |
| C | Deepak Rawat | first the "scammer" calling A, then `judge` / 0000 and `analyst` / 9999 | The caller, then the judges' phone |

Other logins: `priya` / 2222 (UPI PIN 7316).

## Before the judges arrive (5 minutes)

1. Install the **same build** on all three phones. Turn on internet (Wi-Fi or data) on all three. Set the clock to automatic.
2. Phone B: log in as rohit and allow notifications. It should say "Waiting for alerts from Papa" and **● Connected**. Keep the app open if you can. If it was closed or logged out, the alert is still there when Rohit logs in (up to 24 hours), marked with the time it was sent.
3. Phone A: log in as sharma. Open Settings (Home → Settings) → **Reset demo**. The usual payment should read ₹650.
4. Phone C: save Phone A's number so Deepak can call it. Phone B: save Phone A's number as "Papa".
5. Screen share: check Phone A's Android version (Settings → About phone).
   - **Android 15 or newer:** real screen sharing in a Meet or WhatsApp video call is detected.
   - **Older:** casting or mirroring to a TV is detected. Otherwise use Judge mode → **Simulate screen sharing** on Phone A (log in as judge, switch it on, log out, log in as sharma).
6. Between runs: Phone A → Settings → **Reset demo**. Earlier payments change the AI's inputs (new payees today, usual payment), so every run should start the same.

## The story (about 6 minutes)

**1. Normal day: no interruption (30 s).** Phone A, no call: pay **Suresh Verma ₹18,000** (rent, the usual landlord). No warning, PIN 4821, paid.
*Say:* "Normal payments are never interrupted. The AI only speaks up when the pattern looks like a coached scam."

**1b. A big genuine purchase: no popup (30 s).** Still no call: pay **Gupta Electronics ₹90,000** (a TV).
The amber line appears ("…138× your usual payment"), tap **What the AI sees**: score 0.54 but **Nudge**, then straight to the PIN.
*Say:* "The model alone would pause this. Our coaching check says a full-screen stop needs a sign that someone is steering
the payment: a call, screen sharing, a pasted ID, switching apps. In a shop there is none, so he just sees one line."

**2. The scam call (1 min).** Deepak calls Phone A from Phone C as a "CBI officer" (digital-arrest script). Phone A's Home shows **"Active phone call detected"**.
Pay to UPI ID → type `ramesh.k@demoupi` → amount **₹20,000**.
- The amber line appears while typing: "First payment to Ramesh K. ₹20,000 is 31× your usual payment…"
- Tap **What the AI sees**: on a call, new payee, usual ₹650 from 42 payments, score 0.88 (88%).

Tap Continue → **Pause** ("Before you pay", the reasons, Q1 and Q2).

**3. Coached answers don't help the scammer (20 s).** The caller says "press No". Answer No to both: the level stays Pause. Answers can only raise the level.

**4. The truth comes out → Critical (30 s).** Change Q1 to **Yes** → Next → **Critical** (red). Point at the last line: "If someone is telling you to ignore this screen, that is the scam." Show EN | हिंदी and read-aloud.

**5. Family breaks the script (1 min).** On Phone A tap **Ask Rohit to call me**. Phone A shows "Rohit has been alerted".
- Phone B **vibrates** and shows: Papa, ₹20,000, Ramesh K., the reasons, and "Rohit can't see the account or stop the payment".
- Sehjal taps **Call Papa now**. Phone A shows "Rohit is calling you now".
- Sambhav hangs up on the "officer" (Phone C) and answers Rohit.
- Sehjal taps **I've called**. Phone A shows "Rohit says they have called you", and the alert is deleted from Firebase.
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

**9. The AI learns a new scam wave (Phone C, 1 min).** Log out and log in as **analyst**. The model never saw "sent by mistake" scams. Clustering found a pure group of 89 missed reports and proposed one rule (catches 100%, +0.05 false alarms per 1,000). Tap **Approve** → **Run in app** → the payment now gets a Pause.
*Say:* "The AI proposes, a human approves. It never changes the phone by itself."

## If something goes wrong

| Problem | Fix |
|---|---|
| Phone B didn't buzz | Is B online and showing **● Connected**? If B was logged out or the app was closed, log in as rohit: the alert opens with "Sent at …". |
| No "on call" banner | The call must be active while DemoPay is open. Plan B: Judge mode → Simulate a phone call. |
| Levels differ from the rehearsal | Tap **What the AI sees** on the Amount screen: one of the facts differs (call, pasted ID, left the app, payments made earlier). Reset demo. |
| No internet at the venue | Same-phone fallback: Phone A asks Rohit, log out, log in as rohit on Phone A: the alert is there. |

## Honest limits (say them if asked)

- Rohit's phone buzzes only while DemoPay is running on it. If the app is closed, the alert waits in Firebase (up to 24 hours, deleted once Rohit taps "I've called") and opens when he logs in. A buzz on a fully closed app needs Firebase Cloud Messaging and a server, which we did not build.
- Screen-share detection is real on Android 15+ (this app being recorded or shared) and for casting/mirroring on any Android. On older phones we use the Judge-mode switch.
- Analyst-approved rules apply on the phone where they were approved. In production they would ship as a signed rules update.
- The trusted-contact change (OTP + 24 hours) is shown on the phone, but alerts in this demo always go to Rohit's demo account.
- All results are on synthetic data. They test our assumptions, not real-world accuracy.
