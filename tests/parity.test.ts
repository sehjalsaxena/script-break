// Python parity: beyond the tier, every case must give the same groups, rules, reasons and naive
// baseline as the Python engine. python_behaviour_checks.json is an unchanged copy of
// reference/results/behaviour_checks.json (written by reference/run_cases.py, rule pack v0.3).
import { describe, expect, it } from 'vitest'
import { assess, naiveBaseline, type Session } from '../src/engine/engine'
import { PACK_V02, PACK_V03 } from '../src/engine/rulepacks'
import py from './python_behaviour_checks.json'

interface Row {
  id: string
  session: Session
  observed: string
  observed_v02: string
  groups: string[]
  rules: string[]
  reasons: string[]
  naive: boolean
}

describe('parity with the Python engine', () => {
  for (const r of py.rows as Row[]) {
    it(`${r.id}: groups, rules, reasons and naive baseline match Python`, () => {
      const a3 = assess(r.session, PACK_V03)
      expect(a3.tier).toBe(r.observed)
      expect(assess(r.session, PACK_V02).tier).toBe(r.observed_v02)
      expect(a3.groups).toEqual(r.groups)
      expect(a3.rules).toEqual(r.rules)
      expect(a3.reasons).toEqual(r.reasons)
      expect(a3.packVersion).toBe('0.3')
      expect(naiveBaseline(r.session)).toBe(r.naive)
    })
  }
})
