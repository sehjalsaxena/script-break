/**
 * ScriptBreak decision engine: TypeScript port of reference/engine.py (the source of truth).
 *
 * assess(session, pack) -> Assessment is a pure function:
 *   * runs on the phone: no network call, no call audio, no message text, no contacts
 *   * every input is observable by the host bank/UPI app while it is open (see PDF page 12)
 *   * the user's answers can only RAISE the level, never lower it
 *   * learned rules come from the pattern-discovery loop (discover.py) and are plain data in
 *     the rule pack; a rule enters a pack only after analysts approve it
 *
 * Rule pack v0.2 = hand-written rules after the adversarial review of v0.1.
 * Rule pack v0.3 = v0.2 + rules found by the discovery loop and approved by analysts.
 *
 * Rules: SPEC.md. Interface: CONTRACT.md.
 */
import { PACK_V02, PACK_V03 } from './rulepacks'

export type Tier = 'NONE' | 'NUDGE' | 'PAUSE' | 'CRITICAL'
export type Group = 'C' | 'P' | 'A'
export type PayeeStatus = 'known' | 'new' | 'recent' | 'warned' | 'flagged'
export type PayeeKind = 'person' | 'merchant' | 'biller'

export const RANK: Record<Tier, number> = { NONE: 0, NUDGE: 1, PAUSE: 2, CRITICAL: 3 }

// ---- thresholds (constants of the rule pack; calibrated in bank shadow mode) ----
export const AMOUNT_MULTIPLE = 5      // amount at least 5x the user's 90-day median payment ...
export const AMOUNT_FLOOR = 2000      // ... and at least Rs 2,000
export const MIN_HISTORY = 10         // fewer payments than this in 90 days = "no reliable history"
export const NO_HISTORY_FLOOR = 5000  // fixed threshold for users without history
export const CREDIT_MATCH = 0.02      // outgoing amount within 2% of a recent credit = "matches"
export const NUDGE_FLOOR = 1000       // no inline notes on payments below Rs 1,000

// Feature switches for the ablation study (ablation.py). "repeat_same_payee" is OFF in the rule
// pack: the ablation showed it cost more false alarms than it caught scams; adding up today's
// payments to new payees ("cumulative") covers splitting to the same account instead.
export const FEATURES = {
  call_seen_30m: true, merchants_count: true, repeat_same_payee: false,
  cumulative: true, fan_out: true, no_history_floor: true, screen_share_rule: true,
} as const

export interface Session {
  amount: number                 // this payment, in rupees
  median_90d?: number
  payments_90d?: number
  call_now?: boolean
  call_seen_30m?: boolean
  screen_share?: boolean
  payee_status?: PayeeStatus
  payee_kind?: PayeeKind
  same_payee_24h?: number
  new_payees_60m?: number
  sent_new_24h?: number
  credit_unknown_48h?: number
  payee_is_sender?: boolean
  payee_pasted?: boolean
  app_switch?: boolean
  night?: boolean
  q1?: boolean | null
  q2?: boolean | null
}

export interface LearnedRule {
  id: string
  all: string[]
  none?: string[]
  explain: string
  name?: string
  then?: string
  source?: Record<string, number>
  approved_by?: string[]
}

export interface RulePack {
  version: string
  based_on?: string
  learned: LearnedRule[]
}

export interface Assessment {
  tier: Tier
  groups: Group[]
  rules: string[]
  reasons: string[]
  packVersion: string
}

// The Python dataclass defaults, applied to a plain JSON session.
type FullSession = Required<Omit<Session, 'q1' | 'q2'>> & { q1: boolean | null; q2: boolean | null }

const DEFAULTS: Omit<FullSession, 'amount'> = {
  median_90d: 0,           // user's median payment over 90 days (computed on the phone)
  payments_90d: 50,        // number of payments in 90 days
  // live-guidance context, sampled only while the host app is open
  call_now: false,         // a phone or internet call is active during this payment
  call_seen_30m: false,    // a call was active while this app was open in the last 30 min
  screen_share: false,     // screen sharing / recording, or a remote-access app, is active
  // payee, from the host app's own payment records
  payee_status: 'known',   // known | new | recent (first paid < 7 days ago) | warned | flagged
  payee_kind: 'person',    // person | merchant | biller (allow-listed large biller)
  // payment pattern, from the host app's own records
  same_payee_24h: 0,       // earlier payments to this new/recent payee in the last 24 h
  new_payees_60m: 0,       // earlier payments to other new payees in the last 60 min
  sent_new_24h: 0,         // rupees already sent to new/recent payees in the last 24 h
  credit_unknown_48h: 0,   // a credit from a sender never transacted with, last 48 h (0 = none)
  payee_is_sender: false,  // this payee is the person who sent that credit
  // recorded for incident reports; no hand-written rule uses them
  payee_pasted: false,     // payee ID was pasted rather than picked from history
  app_switch: false,       // user left the app and came back during the payment flow
  night: false,            // 23:00 to 06:00
  // the two questions, asked only at Pause; null = not asked or skipped
  q1: null,                // told the payment is needed to verify, protect or release money
  q2: null,                // someone asked them to keep this payment secret (family, bank)
}

// Fill in defaults for fields the caller left out (undefined). Never mutates the input.
function full(s: Session): FullSession {
  const out: Record<string, unknown> = { ...DEFAULTS }
  for (const [k, v] of Object.entries(s)) if (v !== undefined) out[k] = v
  return out as FullSession
}

export function hasHistory(session: Session): boolean {
  const s = full(session)
  return s.payments_90d >= MIN_HISTORY && s.median_90d > 0
}

export function isBig(session: Session, amount: number): boolean {
  const s = full(session)
  if (hasHistory(s)) {
    return amount >= AMOUNT_FLOOR && amount >= AMOUNT_MULTIPLE * s.median_90d
  }
  // no reliable history: fixed floor (switched off only in the ablation study)
  return FEATURES.no_history_floor && amount >= NO_HISTORY_FLOOR
}

export function creditMatch(session: Session): boolean {
  const s = full(session)
  const c = s.credit_unknown_48h
  return c > 0 && Math.abs(s.amount - c) <= CREDIT_MATCH * c
}

/**
 * The event vocabulary written to the on-phone incident record (and to opt-in reports).
 * It is deliberately wider than what the hand-written rules use, so the discovery loop
 * can find patterns no rule covers yet.
 */
export function tokens(session: Session): Set<string> {
  const s = full(session)
  const t = new Set<string>()
  const payeeNew = s.payee_status === 'new' || s.payee_status === 'recent'
  if (s.call_now) t.add('call_now')
  if (s.call_seen_30m) t.add('call_seen_30m')
  if (s.screen_share) t.add('screen_share')
  if (payeeNew) t.add('payee_new')
  if (s.payee_status === 'warned') t.add('payee_warned')
  if (s.payee_status === 'flagged') t.add('payee_flagged')
  if (s.payee_kind === 'merchant') t.add('payee_merchant')
  if (s.payee_kind === 'biller') t.add('payee_biller')
  if (isBig(s, s.amount)) t.add('amount_big')
  if (payeeNew && isBig(s, s.amount + s.sent_new_24h) && !isBig(s, s.amount)) t.add('amount_cumulative')
  if (payeeNew && s.same_payee_24h >= 1) t.add('repeat_same_payee')
  if (s.new_payees_60m >= 2) t.add('many_new_payees')
  if (!hasHistory(s)) t.add('low_history')
  if (s.credit_unknown_48h > 0) t.add('credit_unknown_48h')
  if (creditMatch(s)) t.add('amount_matches_credit')
  if (s.payee_is_sender) t.add('payee_is_sender')
  if (s.payee_pasted) t.add('payee_pasted')
  if (s.app_switch) t.add('app_switch')
  if (s.night) t.add('night')
  return t
}

// Python f"{x:.0f}": round to nearest, exact halves to even. (JS toFixed(0) rounds halves up.)
function formatRatio(x: number): string {
  const f = Math.floor(x)
  const d = x - f
  const n = d > 0.5 ? f + 1 : d < 0.5 ? f : f % 2 === 0 ? f : f + 1
  return String(n)
}

export interface Evidence {
  C: boolean
  strongC: boolean
  P: boolean
  strongP: boolean
  A: boolean
  reasons: string[]
}

export function evidence(session: Session): Evidence {
  const s = full(session)
  const reasons: string[] = []
  const c = s.call_now || (s.call_seen_30m && FEATURES.call_seen_30m) || s.screen_share
  const strongC = s.screen_share
  if (s.call_now) reasons.push('You are on a call right now')
  else if (s.call_seen_30m) reasons.push('You were on a call while using this app in the last 30 minutes')
  if (s.screen_share) reasons.push('Your screen is being shared or a remote-access app is active')

  const p = (s.payee_status === 'new' || s.payee_status === 'recent') && s.payee_kind !== 'biller' &&
    (FEATURES.merchants_count || s.payee_kind !== 'merchant')
  const strongP = s.payee_status === 'warned' || s.payee_status === 'flagged'
  if (s.payee_status === 'new' && p) reasons.push('You have never paid this payee before')
  if (s.payee_status === 'recent' && p) reasons.push('You first paid this payee less than 7 days ago')
  if (s.payee_status === 'warned') reasons.push('You paid this payee after an earlier warning')
  if (s.payee_status === 'flagged') reasons.push('This account appears in a fraud report shared with your bank')

  const big = isBig(s, s.amount)
  const cumulative = FEATURES.cumulative && p && !big && isBig(s, s.amount + s.sent_new_24h)
  const repeat = (FEATURES.repeat_same_payee && p && s.same_payee_24h >= 1) ||
    (FEATURES.fan_out && s.new_payees_60m >= 2)
  const a = big || cumulative || repeat
  if (big) {
    if (hasHistory(s)) {
      reasons.push(`The amount is ${formatRatio(s.amount / s.median_90d)} times your usual payment`)
    } else {
      reasons.push('This is a large payment and you have little payment history')
    }
  }
  if (cumulative) reasons.push("Together with today's other payments to new payees, this is unusually large")
  if (repeat) reasons.push('This is a repeat payment to a payee you only started paying today')
  return { C: c, strongC, P: p, strongP, A: a, reasons }
}

export function assess(session: Session, pack: RulePack): Assessment {
  const s = full(session)
  const ev = evidence(s)
  const present: Record<Group, boolean> = { C: ev.C, P: ev.P || ev.strongP, A: ev.A }
  const groups = (['C', 'P', 'A'] as const).filter((g) => present[g])
  const reasons = [...ev.reasons]
  const rules: string[] = []
  if (ev.C && ev.P && ev.A) rules.push('R1')
  if (ev.strongC && ev.P && FEATURES.screen_share_rule) rules.push('R2')
  if (ev.C && ev.strongP) rules.push('R3')
  if (s.payee_status === 'flagged') rules.push('R4')

  const toks = tokens(s)
  for (const r of pack.learned) {
    if (r.all.every((t) => toks.has(t)) && !(r.none ?? []).some((t) => toks.has(t))) {
      rules.push(r.id)
      reasons.push(r.explain)
    }
  }

  const packVersion = pack.version
  if (rules.length > 0) {
    const crit: string[] = []
    if (s.q1 === true) { crit.push('K1'); reasons.push('You said you were told this is needed to verify, protect or release money') }
    if (s.q2 === true) { crit.push('K2'); reasons.push('You said someone asked you to keep this payment secret') }
    if (ev.strongP) crit.push('K3')
    if (ev.strongC && ev.A) crit.push('K4')
    return { tier: crit.length > 0 ? 'CRITICAL' : 'PAUSE', groups, rules: [...rules, ...crit], reasons, packVersion }
  }
  // Nudge: one inline line, no extra tap. Never for payees you have paid before, never for
  // everyday small amounts: those are most of life, and notes on them breed alert fatigue.
  if ((ev.P && (ev.C || ev.A) && s.amount >= NUDGE_FLOOR) || s.payee_status === 'warned') {
    return { tier: 'NUDGE', groups, rules: ['N1'], reasons, packVersion }
  }
  return { tier: 'NONE', groups, rules: [], reasons, packVersion }
}

/** A common bank-style rule, for comparison: warn on a first payment to a person of Rs 10,000+. */
export function naiveBaseline(session: Session): boolean {
  const s = full(session)
  return s.payee_status === 'new' && s.payee_kind === 'person' && s.amount >= 10000
}

// Python reads rulepack_v{version}.json from disk; here the packs are bundled (no file system on the phone).
export function loadPack(version: '0.2' | '0.3'): RulePack {
  return version === '0.2' ? PACK_V02 : PACK_V03
}
