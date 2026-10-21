// The mock UPI flow as plain functions, so the step logic is testable without a browser.
// The engine decides the level; this file only decides which screen follows.
import type { PayeeKind, Session, Tier } from '../engine/engine'

/** Every screen of the mock UPI app. */
export type Step =
  | 'home' | 'search' | 'scan' | 'chat' | 'amount'
  | 'pause' | 'critical' | 'pin' | 'processing' | 'success' | 'waiting'
  | 'history' | 'balance-pin' | 'balance'

/** Everything about the payment except the amount and the two answers. */
export type Context = Omit<Session, 'amount' | 'q1' | 'q2'>

/** The two Pause questions; null = not answered (the questions are optional). */
export interface Answers {
  q1: boolean | null
  q2: boolean | null
}

export const NO_ANSWERS: Answers = { q1: null, q2: null }

/** What the "Ask family" button hands over. The relay to the second phone lives in src/family/. */
export interface FamilyAlertRequest {
  contactName: string
  payeeName: string
  amount: number
  tier: Tier
}

export function buildSession(context: Context, amount: number, answers: Answers = NO_ANSWERS): Session {
  return { ...context, amount, q1: answers.q1, q2: answers.q2 }
}

/**
 * The signals for a payment to `payee`. Paying the scenario's own payee uses the scenario's
 * signals unchanged (so it matches its behaviour check). Paying anyone else in the address book
 * keeps the phone and history signals (call, median, today's totals) but uses that payee's own
 * record: paid before, its kind, no repeat, not the sender of an unknown credit.
 */
export function contextFor(scenarioContext: Context, isScenarioPayee: boolean, kind: PayeeKind): Context {
  if (isScenarioPayee) return scenarioContext
  return { ...scenarioContext, payee_status: 'known', payee_kind: kind, same_payee_24h: 0, payee_is_sender: false, payee_pasted: false }
}

/** After "Pay". None and Nudge go straight to the PIN: a Nudge is one inline line, no extra tap. */
export function stepAfterPay(tier: Tier): Step {
  if (tier === 'CRITICAL') return 'critical'
  if (tier === 'PAUSE') return 'pause'
  return 'pin'
}

/**
 * After the two questions. The engine guarantees answers can only raise the level, so a
 * Pause either becomes Critical or stays a Pause, and then the person decides (on to the PIN).
 */
export function stepAfterPause(tierWithAnswers: Tier): Step {
  return tierWithAnswers === 'CRITICAL' ? 'critical' : 'pin'
}
