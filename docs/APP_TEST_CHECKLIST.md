# ScriptBreak app: manual test checklist

Work through this on a real Android phone (best) or the emulator. For each row, do the steps, compare
with **Expected**, and mark ✅ or ❌. For every ❌, write down: the row number, what you did, what you saw,
a screenshot, and the Logcat error (see section 0). Expected levels come from the real AI model.

Sambhav's profile: usual payment ₹650, 42 payments in 90 days, trusted contact Sehjal.
Demo UPI IDs: `ramesh.k@demoupi` (new), `anil.mehta@demoupi` (new), `manoj.elec@demoupi` (new),
`guptaelectronics@demoupi` (new shop), `suresh.verma@demoupi` (landlord, paid 14 times).

## 0. Setup (once per phone)

| # | Steps | Expected | ✅/❌ |
|---|---|---|---|
| 0.1 | Phone: Settings → About phone → tap Build number 7 times. Developer options → USB debugging ON, **Install via USB ON** (Oppo/OnePlus/Xiaomi), "Verify apps over USB" OFF | Developer options available | |
| 0.2 | Pull the latest code. Android Studio → File → Open → `script-break\andriod app` → wait for Gradle sync | No red errors; top bar shows **app** | |
| 0.3 | Plug in the phone, pick it in the device list, press ▶ Run. Accept "Install" on the phone if asked | App opens on the profile picker | |
| 0.4 | Android Studio → **Logcat** tab → filter `package:com.example.scriptbreak level:error` | Keep it open: any crash appears here in red | |
| 0.5 | Optional, all logic tests: `cd "andriod app"` then `gradlew testDebugUnitTest` | BUILD SUCCESSFUL (25 tests) | |

## 1a. Login, UPI PIN, limit

Demo accounts (also listed on the login screen): sambhav 1111 (UPI PIN 4821), deepak 2222 (UPI PIN 7316),
sehjal 3333, judge 0000, analyst 9999.

| # | Steps | Expected | ✅/❌ |
|---|---|---|---|
| 1a.1 | Open the app | Login screen with "Demo accounts (tap to fill)" | |
| 1a.2 | Username `sambhav`, PIN `9999` → Log in | "Wrong username or PIN"; stays on login | |
| 1a.3 | Tap Sambhav in Demo accounts → Log in | Sambhav's Home | |
| 1a.4 | Pay Suresh ₹500 → PIN `7316` (Deepak's) → Submit | "Incorrect UPI PIN. 2 attempts left" | |
| 1a.5 | Two more wrong PINs | "Wrong UPI PIN 3 times. Payment cancelled…" → Home, nothing paid | |
| 1a.6 | Pay again → PIN `4821` | Success | |
| 1a.7 | Amount: type 100000, then one more 0 | Stays ₹1,00,000; message "UPI allows up to ₹1,00,000 per payment" | |
| 1a.8 | Back on Home | Logs out to the login screen | |

## 1. Start and profiles

| # | Steps | Expected | ✅/❌ |
|---|---|---|---|
| 1.1 | Open the app | Profile picker with 5 cards; "Demo app – no real money" visible | |
| 1.2 | Tap **Sehjal** before any alert was sent | Message "No alert yet…"; stays on picker | |
| 1.3 | Tap **Analyst** | Analyst console opens (see section 7b) | |
| 1.4 | Tap **Sambhav** | Home: "Sambhav", people row (Sehjal, Suresh, Sunita, Kirana), recent payments | |
| 1.5 | Press the phone's Back button on Home | Back to the profile picker (no crash) | |

## 2. Payments with no warning (no call)

| # | Steps | Expected | ✅/❌ |
|---|---|---|---|
| 2.1 | Sambhav → tap **Suresh** in people row | Payee page: SURESH KUMAR VERMA, "Paid 14 times before" | |
| 2.2 | Pay → type **18000** | No amber line | |
| 2.3 | Tap Pay | **Straight to UPI PIN** (no Pause) | |
| 2.4 | Tap 4 digits → Submit | Success: ₹18,000, UPI ref, **no** "report" line | |
| 2.5 | Done | Home; the payment appears at the top of Recent payments | |
| 2.6 | Pay UPI ID → type `ramesh.k@demoupi` → Continue → Pay → **3000** → Pay | Straight to PIN (small amount, no call) | |
| 2.7 | Kirana → ₹**350** → Pay | Straight to PIN | |

## 3. Nudge (one amber line, no extra tap)

| # | Steps | Expected | ✅/❌ |
|---|---|---|---|
| 3.1 | No call. Pay UPI ID → **type** `anil.mehta@demoupi` → Continue | Payee: ANIL MEHTA, "New payee – never paid" | |
| 3.2 | Pay → type **40000** | Amber line: "First payment to Anil Mehta. ₹40,000 is 62× your usual payment. Check you know who this is." | |
| 3.3 | Delete digits down to 4000 | Amber line disappears | |
| 3.4 | Type back to 40000 → Pay | **Straight to PIN** (Nudge needs no extra tap) | |

## 4. Pause and Critical (needs a call)

Make a **real call** to the demo phone from another phone and answer it, **then** open the app
(or: Judge mode → "Simulate a phone call in the whole app" ON). Don't switch apps mid-payment.

| # | Steps | Expected | ✅/❌ |
|---|---|---|---|
| 4.1 | Home during the call | Green **On call** banner appears within ~1 second | |
| 4.2 | Pay UPI ID → **type** `ramesh.k@demoupi` → Continue → Pay → **20000** → Pay | **Pause**: "Before you pay", "This sends ₹20,000 out of your account to Ramesh K.", reasons incl. "You are on a call right now", Q1, Q2, footer "AI model 20261024-0701" | |
| 4.3 | Answer Q1 **No**, Q2 **No** → Next | Goes to **PIN** (not Critical): "No" never raises or lowers | |
| 4.4 | Back to Home, repeat 4.2, answer Q1 **Yes** → Next | **Critical** (red badge): "Stop. This matches a common scam." … last line "If someone is telling you to ignore this screen, that is the scam." | |
| 4.5 | Look at "Continue anyway (10 s)" | Disabled; counts down 10 → 0, then "Continue anyway" enabled | |
| 4.6 | Tap **Ask Sehjal to call me** | Message "Sehjal has been alerted"; button changes to "Sehjal has been asked to call you" | |
| 4.7 | Tap **EN \| हिंदी** | All Critical text switches to Hindi and back | |
| 4.8 | Tap the speaker icon | Phone reads the warning aloud (Hindi needs a Hindi voice installed) | |
| 4.9 | Tap **Wait 30 minutes** | "Payment cancelled. Nothing has left your account." → Back to home | |
| 4.10 | Repeat 4.4, wait for countdown, **Continue anyway** → PIN → Submit | Success with **"Report payment" / 1930** line | |
| 4.11 | Tap Report on Success | Phone dialer opens with **1930** | |
| 4.12 | Pay **Ramesh** again, ₹**2000**, still on the call | **Critical straight away** (paid after a warning: guardrail G2) | |
| 4.13 | On call: Pay UPI ID → type `manoj.elec@demoupi` → ₹**6000** → Pay | **Pause** (a genuine electrician: accepted false alarm) → Next → PIN in 2 taps | |
| 4.14 | On call: type `ramesh.k@demoupi`, ₹**48000** | **Critical straight away** (score above 0.942) | |
| 4.15 | On call: Pay UPI ID → **Paste** (copy `ramesh.k@demoupi` in another app first) → ₹20,000 | Critical (pasted ID + left the app to copy it) | |
| 4.16 | During 4.2, press Back on Pause / Critical / PIN | Returns to Amount; no crash | |
| 4.17 | Rotate the phone on the Pause screen | Same screen, answers kept (no reset) | |

## 5. Hang up, screen share

| # | Steps | Expected | ✅/❌ |
|---|---|---|---|
| 5.1 | Be on a call with the app open for a few seconds, **hang up**, then pay Ramesh ₹20,000 | Still warned: reason "You were on a call in the last 30 minutes" | |
| 5.2 | Android 15+: start **Screen record** (quick settings), open the app, pay Ramesh ₹9,999 | Critical (screen-share signal; reason mentions screen being shared) | |
| 5.3 | Android 15+: video call (Meet/WhatsApp) and **share screen**, open DemoPay | Home shows BOTH banners: "Active phone call detected" and "Screen sharing detected" | |
| 5.4 | Same, pay Ramesh ₹20,000 | Critical; reasons include BOTH "You are on a call right now" and "Your screen is being shared…" | |
| 5.5 | Any Android: **Cast / Screen mirror** to a TV, open DemoPay | "Screen sharing detected" banner | |
| 5.6 | Older phone: log in as judge → Inputs → **Simulate screen sharing** on → log out → sambhav → pay Ramesh ₹20,000 | Pause, reason "Your screen is being shared…"; "What the AI sees" says "(simulated in Judge mode)". Reset demo turns it off | |

## 6a. Family alert on two or three phones (internet on all)

| # | Steps | Expected | ✅/❌ |
|---|---|---|---|
| 6a.1 | Phone B: log in as **sehjal** (3333); allow notifications | "Waiting for alerts from Sambhav", **● Connected** | |
| 6a.2 | Phone A: log in as **sambhav**, on a call, pay Ramesh ₹20,000 → Pause → **Ask Sehjal to call me** | Phone A: "Sehjal has been alerted" | |
| 6a.3 | Phone B, within a few seconds | **Vibrates**, shows the alert: Sambhav, ₹20,000, Ramesh K., reasons | |
| 6a.4 | Phone B with the app in the background, repeat 6a.2 | Notification "Sambhav may be on a scam call"; tap opens the alert | |
| 6a.5 | Phone B: **Call Sambhav now** | Dialer opens on B; Phone A shows "Sehjal is calling you now" | |
| 6a.6 | Phone B: **I've called** | B back to waiting; A shows "Sehjal says they have called you" | |
| 6a.7 | Turn off internet on A, ask Sehjal, turn internet back on | Alert arrives on B once A is back online | |
| 6a.8 | Phone B **logged out** (login screen). Phone A asks Sehjal | B vibrates + notification on the login screen. Log in as sehjal → the alert opens straight away | |
| 6a.9 | Phone B logged in as **judge**. Phone A asks Sehjal | B does NOT buzz (someone else is using it). Log out → log in as sehjal → the alert opens | |
| 6a.10 | Phone B: **close the app** (swipe it away). Phone A asks Sehjal. Wait 15 minutes, open the app on B, log in as sehjal | The alert opens, marked **"Missed alert · Sent at 10:42 PM · 15 min ago"** | |
| 6a.12 | Phone B: on the alert, tap Back (logs out), log in as sehjal again | The same alert is shown again (no second buzz) until **I've called** is tapped | |
| 6a.11 | Phone C also logged out while B taps **I've called** | C forgets the alert: logging in as sehjal on C shows "Waiting" | |

## 6. Family phone (same phone)

| # | Steps | Expected | ✅/❌ |
|---|---|---|---|
| 6.1 | After 4.6, Back to the profile picker → **Sehjal** | Alert screen: "Sambhav may be on a scam call", amount ₹20,000, Ramesh K., reasons, "Sehjal can't see the account or stop the payment" | |
| 6.2 | Tap **Call Sambhav now** | Phone dialer opens | |
| 6.3 | Tap **I've called** | Back to the picker | |

## 7. Judge mode

| # | Steps | Expected | ✅/❌ |
|---|---|---|---|
| 7.1 | Picker → **Judge Mode** | 3 tabs; result card shows **PAUSE**, score ≈ 0.877 | |
| 7.2 | Turn **On a call** off and set status **known** | Card changes live to **NONE** | |
| 7.3 | Set status **flagged** | **CRITICAL**, "G1: bank-flagged payee is always Critical" | |
| 7.4 | Reset; set Q1 **Yes** | **CRITICAL**, "G4…" | |
| 7.5 | Reset; set Q1 **No**, Q2 **No** | Still **PAUSE** (answers never lower) | |
| 7.6 | Type amount **48000** | CRITICAL; type **500** → NONE | |
| 7.7 | **Run in app** | Amount screen with the same amount → Pay shows the same level as the card → finish → back in Judge mode | |
| 7.8 | Tab **28 stories** | 28 cards, Round 1 vs AI level, "⚠ differs" on some (e.g. S06, L04) | |
| 7.9 | Tap **S01** | Loads into Try inputs; card CRITICAL; story name shown | |
| 7.10 | Tab **Test file** → **Our sample file** | 400 scored; scams flagged ~80%; genuine interrupted per 1,000 with 95% range; per type; missed scams list | |
| 7.11 | Put the CSV below on the phone (e.g. save to Downloads) → **Choose a file** | 3 rows: PAUSE, CRITICAL, NONE | |
| 7.12 | Choose a non-JSON/CSV file (e.g. a photo) | Error message, no crash | |
| 7.13 | Switch **Simulate a phone call** ON → Back → Sambhav → Home | "On call" banner shows (turn it OFF afterwards) | |

CSV for 7.11 (save as `judge_test.csv`):
```
amount,median_90d,payments_90d,call_now,payee_status,payee_kind,label,q1
20000,650,42,true,new,person,1,
20000,650,42,true,new,person,1,yes
18000,900,50,false,known,person,0,
```

## 7b. Analyst console (the AI learning a new scam)

| # | Steps | Expected | ✅/❌ |
|---|---|---|---|
| 7b.1 | Picker → **Analyst** | "A new scam wave has started": 1,200 reports, model missed 270 | |
| 7b.2 | Scroll the groups | 5 groups; **C1 · 89 reports**, AI suggests **approve**, rule L1 "same amount as that money + new payee, not payee is that sender", catches 100%, adds 0.05 per 1,000; the others: no rule cheap enough, AI suggests reject | |
| 7b.3 | Turn on **Show the true scam types** | C1 is 100% "sent by mistake"; other groups are mixed | |
| 7b.4 | "Try it on a phone" before approving | Scam payment: **NONE (no warning)** | |
| 7b.5 | Tap **Approve** on C1 | "✓ Approved…"; Try it now shows **PAUSE** (L1); genuine return to the real sender stays **NONE** | |
| 7b.6 | **Run the scam payment in the app** → Pay | **Pause** with the reason "This sends back money you received from someone you have never paid…"; footer shows model "…-without-sent_by_mistake" → finish → back in the Analyst console | |
| 7b.7 | Tap **Shadow mode**, then **Reject** on C1 | Try it goes back to **NONE** (only approved rules reach phones) | |

## 7c. Report packet, trusted contact, scoreboard

| # | Steps | Expected | ✅/❌ |
|---|---|---|---|
| 7c.1 | Sambhav, on a call: pay Ramesh ₹20,000 → Pause → Next → PIN 4821 → Success → **Report payment** | Report packet: amount, Ramesh K., ramesh.k@demoupi, 12-digit UPI ref, date/time, "Warning shown: PAUSE" + reasons | |
| 7c.2 | Tap **Call 1930** / **Report online** / **Share** | Dialer with 1930 / browser at cybercrime.gov.in / share sheet with the packet text | |
| 7c.3 | Home → **Settings** (bottom bar) | Trusted contact: Sehjal (friend), +91 98••••3210, Active | |
| 7c.4 | Change contact: name, relation, mobile `9811122233` → Send code | Code screen with "Demo: no SMS is sent. The code is …" | |
| 7c.5 | Enter a wrong code, then the right one | "Wrong code"; then "Change requested … Active in 24 hours"; Sehjal still active | |
| 7c.6 | Pay Ramesh on a call → Pause | "Ask **Sehjal** to call me" (the new contact is not active yet) | |
| 7c.7 | Settings → **Cancel this change** | Pending change removed | |
| 7c.8 | Log in as judge → **Results** tab | Live check runs: 28 stories (14/16, 2/10), 400 payments scored with ms per payment, error ranges; full test table AI vs rules vs bank rule; per-type bars; fairness; where it fails | |

## 7d. No fixed amount limits: the AI learns the usual payment

- [ ] Settings (trusted contact screen) → "Demo" card shows "Usual payment the AI sees: ₹650 (median of 42 payments in 90 days)".
- [ ] On the Amount screen, tap "What the AI sees": it lists the call, payee, usual payment, pasted ID, left the app, night, and the score.
      Every time the level differs between two tries, one of these facts differs. Check it there first.
- [ ] Type ₹2,000 then ₹20,000 to Ramesh on a call: the amber line stays at both amounts (it no longer disappears at Pause level).
- [ ] Pay Suresh ₹20,000 ten times → the usual payment rises (about ₹1,225 over 52 payments). Reset demo → back to ₹650.
- [ ] Type a UPI ID with keyboard suggestions on: NOT counted as pasted. Paste a whole ID: counted as pasted.
- [ ] Let the screen turn off on the Amount screen, turn it on again: "Left the app" is NOT listed.

## 7e. Coaching check (C1): big genuine purchases are not interrupted

- [ ] No call: pay **Gupta Electronics ₹90,000** → amber line, "What the AI sees" shows **Nudge**, Continue goes straight to the PIN.
- [ ] Same payment while on a call → Pause/Critical as before.
- [ ] Same payment with a **pasted** UPI ID, or after leaving the app and coming back → Pause.

## 7f. Pay from a QR image

Put the three images from `docs/demo_qr/` on the phone (for "Pick a QR image") or show them on another screen (for the camera).

- [ ] Home → QR icon in the search bar → the camera scanner opens and asks for camera permission once. Allow it.
- [ ] Show `1_scam_refund_qr.png` on a laptop or another phone and point the camera at it → the payee page opens by itself.
- [ ] A non-UPI QR (e.g. a website QR) → "This QR code is not a UPI payment code", and the scanner keeps scanning.
- [ ] Deny the camera → "Camera permission is needed…" with **Allow camera**; **Pick a QR image instead** still works.
- [ ] Judge mode → Sandbox → **Fill from a QR code** → scan or pick a QR → back in Judge mode with the amount and payee filled in, a note saying the QR's name is not verified, and the verdict updated.
- [ ] Pick **3_rent_suresh_qr** → payee Suresh Verma, "Paid 14 times before", amber "From a QR code. Check the name and the amount…" → Pay → amount ₹18,000 and note "Rent" filled in → Continue → straight to the PIN.
- [ ] Pick **2_tv_gupta_electronics_qr**, no call → ₹90,000 filled in, amber line only → Continue → straight to the PIN.
- [ ] On a call, pick **1_scam_refund_qr** → payee **Ramesh K.**, banking name RAMESH KUMAR, amber "The QR says "Bank Refund Desk", but the banking name below is different…" → Pay → ₹20,000 → Continue → **Pause** ("This sends ₹20,000 out of your account…").
- [ ] Pick a photo with no QR → "No QR code found in that image". A real shop QR (not @demoupi) → "This QR is for a real UPI ID… DemoPay only pays @demoupi IDs".

## 7g. Money between Sambhav and Deepak (two phones, internet on both)

- [ ] Phone A `sambhav`, Phone C `deepak`. Each sees the other in the people row and Contacts, never themselves.
- [ ] Phone A: Check Balance → ₹2,48,250. Phone C: ₹1,86,400.
- [ ] Phone A pays **Deepak Rawat** ₹500 (UPI PIN 4821) → Phone C shows **"Received ₹500 from Sambhav Jain"**; Home and History show +₹500 in green; Phone C's balance is ₹1,86,900.
- [ ] Phone C pays **Sambhav Jain** ₹200 (UPI PIN 7316) → Phone A shows "Received ₹200 from Deepak Rawat".
- [ ] Phone C logged out while Phone A pays → log in as deepak → the money is there.
- [ ] Pay your own UPI ID (type `sambhav.jain@demoupi` as Sambhav) → "That's your own UPI ID".
- [ ] Pay more than the balance → after the PIN: "Payment failed: not enough balance… Nothing left your account".
- [ ] Settings → Reset demo → this person's balance is back to the start.

## 8. Not built yet (don't report as bugs)

- Hindi on screens other than Critical; Deepak has the same trusted contact (Sehjal).
