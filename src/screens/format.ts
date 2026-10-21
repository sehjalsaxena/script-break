import type { PayeeStatus } from '../engine/engine'
import { NUDGE_COPY } from './copy'

const inr = new Intl.NumberFormat('en-IN')

/** ₹1,50,000 (Indian digit grouping). */
export function rupees(n: number): string {
  return `₹${inr.format(n)}`
}

/** Engine reasons are short lines without a full stop; join them into sentences. */
export function sentences(lines: string[]): string {
  return lines.map((l) => (/[.!?]$/.test(l) ? l : `${l}.`)).join(' ')
}

// Same text as the engine's P reason for a new payee (engine.ts, evidence()).
const NEVER_PAID = 'You have never paid this payee before'

/**
 * The inline Nudge line, e.g. "First payment to Anil Mehta." + "The amount is 44 times your
 * usual payment. Check you know who this is." The bold headline replaces the engine's
 * "never paid" reason so it isn't said twice.
 */
export function nudgeParts(reasons: string[], payeeName: string, status: PayeeStatus): { headline: string; body: string } {
  const isNew = status === 'new'
  const rest = isNew ? reasons.filter((r) => r !== NEVER_PAID) : reasons
  return {
    headline: isNew ? NUDGE_COPY.firstPayment(payeeName) : '',
    body: sentences([...rest, NUDGE_COPY.check]),
  }
}
