// Behaviour checks, as in reference/run_cases.py: every synthetic case is run through
// rule packs v0.2 and v0.3. Expected levels describe the DESIGNED behaviour, including known
// limits. Never edit a case's expected level to make a test pass.
import { describe, expect, it } from 'vitest'
import { assess, type Session, type Tier } from '../src/engine/engine'
import { PACK_V02, PACK_V03 } from '../src/engine/rulepacks'
import cases from './cases.json'

interface Case {
  id: string
  kind: string
  title: string
  session: Session
  expected: Tier
  expected_v02?: Tier
  note?: string
}

const all = cases as Case[]

describe('behaviour checks (cases.json)', () => {
  it('has 28 cases', () => {
    expect(all).toHaveLength(28)
  })

  for (const c of all) {
    it(`${c.id}: ${c.title}`, () => {
      const a3 = assess(c.session, PACK_V03)
      const a2 = assess(c.session, PACK_V02)
      expect(a3.tier, `${c.id} on v0.3`).toBe(c.expected)
      expect(a2.tier, `${c.id} on v0.2`).toBe(c.expected_v02 ?? c.expected)
    })
  }
})
