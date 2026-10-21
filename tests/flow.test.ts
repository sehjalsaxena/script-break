// The mock UPI flow and the demo presets. The presets must reproduce the behaviour checks they
// are built from, and the demo script (deck page 10) must behave as promised.
import { describe, expect, it } from 'vitest'
import { assess, type Session, type Tier } from '../src/engine/engine'
import { PACK_V02, PACK_V03 } from '../src/engine/rulepacks'
import { ADDRESS_BOOK, SCENARIOS } from '../src/scenarios/presets'
import { PAUSE_COPY } from '../src/screens/copy'
import { buildSession, contextFor, NO_ANSWERS, stepAfterPause, stepAfterPay } from '../src/screens/flow'
import { nudgeParts, rupees, sentences } from '../src/screens/format'
import cases from './cases.json'

const caseById = new Map((cases as { id: string; session: Session }[]).map((c) => [c.id, c.session]))

describe('screen after "Pay"', () => {
  it.each<[Tier, string]>([
    ['NONE', 'pin'],
    ['NUDGE', 'pin'],
    ['PAUSE', 'pause'],
    ['CRITICAL', 'critical'],
  ])('%s goes to %s', (tier, step) => {
    expect(stepAfterPay(tier)).toBe(step)
  })

  it('after the questions, only Critical leads to the Critical screen', () => {
    expect(stepAfterPause('CRITICAL')).toBe('critical')
    expect(stepAfterPause('PAUSE')).toBe('pin')
  })
})

describe('demo presets', () => {
  it('cover the four engine steps of the demo script, in order', () => {
    const steps = SCENARIOS.filter((s) => s.demoStep).map((s) => [s.demoStep, s.caseId])
    expect(steps).toEqual([[1, 'S01'], [2, 'L02'], [3, 'S02'], [4, 'S15']])
  })

  for (const s of SCENARIOS) {
    describe(`${s.id} (${s.caseId})`, () => {
      it('uses exactly the signals and amount of its behaviour check', () => {
        const { q1: _q1, q2: _q2, ...withoutAnswers } = caseById.get(s.caseId)!
        expect(buildSession(s.context, s.amount)).toEqual({ ...withoutAnswers, q1: null, q2: null })
      })

      it(`is ${s.expect.first} on "Pay" (v0.3)${s.expect.firstV02 ? ` and ${s.expect.firstV02} on v0.2` : ''}`, () => {
        const session = buildSession(s.context, s.amount, NO_ANSWERS)
        expect(assess(session, PACK_V03).tier).toBe(s.expect.first)
        expect(assess(session, PACK_V02).tier).toBe(s.expect.firstV02 ?? s.expect.first)
      })

      if (s.expect.answers) {
        it(`answers lead to ${s.expect.afterAnswers}`, () => {
          const tier = assess(buildSession(s.context, s.amount, s.expect.answers), PACK_V03).tier
          expect(tier).toBe(s.expect.afterAnswers)
          expect(stepAfterPay(s.expect.first)).toBe('pause')
          expect(stepAfterPause(tier)).toBe(tier === 'CRITICAL' ? 'critical' : 'pin')
        })
      }
    })
  }
})

describe('paying someone else while a scenario is running', () => {
  it('each scenario payee has the payee kind of its behaviour check', () => {
    for (const s of SCENARIOS) expect(s.payee.kind, s.id).toBe(s.context.payee_kind ?? 'person')
  })

  it('paying the scenario payee uses the case signals unchanged', () => {
    for (const s of SCENARIOS) expect(contextFor(s.context, true, s.payee.kind)).toBe(s.context)
  })

  it('paying a known contact during the digital-arrest call shows nothing (like L07)', () => {
    const s = SCENARIOS.find((x) => x.id === 'digital-arrest')!
    const rohit = ADDRESS_BOOK.find((p) => p.id === 'rohit')!
    const ctx = contextFor(s.context, false, rohit.kind)
    expect(ctx.call_now).toBe(true)
    expect(assess(buildSession(ctx, 30000), PACK_V03).tier).toBe('NONE')
  })
})

describe('screen text helpers', () => {
  it('formats rupees with Indian grouping', () => {
    expect(rupees(150000)).toBe('₹1,50,000')
    expect(rupees(48000)).toBe('₹48,000')
  })

  it('joins engine reasons into sentences without doubling full stops', () => {
    expect(sentences(['You are on a call right now', 'Real ones are reversed by the bank.'])).toBe(
      'You are on a call right now. Real ones are reversed by the bank.',
    )
  })

  it('builds the deck\'s Nudge line for a new landlord (L04) without repeating "never paid"', () => {
    const s = SCENARIOS.find((x) => x.id === 'new-landlord')!
    const a = assess(buildSession(s.context, s.amount), PACK_V03)
    expect(nudgeParts(a.reasons, s.payee.name, 'new')).toEqual({
      headline: 'First payment to Anil Mehta.',
      body: 'The amount is 44 times your usual payment. Check you know who this is.',
    })
  })

  it('does not double the full stop after a name ending in "."', () => {
    expect(nudgeParts([], 'Vikram S.', 'new').headline).toBe('First payment to Vikram S.')
    expect(PAUSE_COPY.sends('₹48,000', 'Ramesh K.')).toBe('This sends ₹48,000 out of your account to Ramesh K.')
  })
})
