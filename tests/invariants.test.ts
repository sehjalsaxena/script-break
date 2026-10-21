import { describe, expect, it } from 'vitest'
import { assess, RANK, type Session } from '../src/engine/engine'
import { PACK_V02, PACK_V03 } from '../src/engine/rulepacks'
import cases from './cases.json'

const sessions = (cases as { id: string; session: Session }[]).map((c) => ({ id: c.id, session: c.session }))
const packs = [PACK_V02, PACK_V03]
const answers = [undefined, null, false, true] as const

function unanswered(s: Session): Session {
  const { q1: _q1, q2: _q2, ...rest } = s
  return rest
}

describe('answers can only raise the level', () => {
  for (const { id, session } of sessions) {
    it(`${id}: no answer combination gives a lower tier than leaving both unset`, () => {
      for (const pack of packs) {
        const base = RANK[assess(unanswered(session), pack).tier]
        for (const q1 of answers) {
          for (const q2 of answers) {
            const tier = assess({ ...unanswered(session), q1, q2 }, pack).tier
            expect(RANK[tier], `${id} v${pack.version} q1=${q1} q2=${q2}`).toBeGreaterThanOrEqual(base)
            // "No" behaves exactly like not answering
            if (q1 !== true && q2 !== true) expect(RANK[tier]).toBe(base)
          }
        }
      }
    })
  }
})

describe('assess is deterministic and pure', () => {
  it('same input gives the same output, and the input is not modified', () => {
    for (const { session } of sessions) {
      for (const pack of packs) {
        const before = JSON.stringify(session)
        const packBefore = JSON.stringify(pack)
        const first = assess(session, pack)
        const second = assess(structuredClone(session), pack)
        expect(second).toEqual(first)
        expect(JSON.stringify(session)).toBe(before)
        expect(JSON.stringify(pack)).toBe(packBefore)
      }
    }
  })
})

describe('performance', () => {
  it('runs in under 5 ms per call on average over 10,000 calls', () => {
    const n = 10_000
    const start = performance.now()
    for (let i = 0; i < n; i++) assess(sessions[i % sessions.length].session, PACK_V03)
    const avg = (performance.now() - start) / n
    console.log(`assess: ${(avg * 1000).toFixed(1)} µs per call on average`)
    expect(avg).toBeLessThan(5)
  })
})
