import type { RulePack } from './engine'
import v03 from './rulepack_v0.3.json'

// Rule pack v0.2 = hand-written rules after the adversarial review of v0.1.
export const PACK_V02: RulePack = { version: '0.2', learned: [] }

// Rule pack v0.3 = v0.2 + rules found by the discovery loop and approved by analysts.
// Copied unchanged from reference/rulepack_v0.3.json.
export const PACK_V03: RulePack = v03
