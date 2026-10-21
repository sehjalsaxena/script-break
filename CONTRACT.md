# Engine contract

This is the one interface everyone codes against. The engine lives in `src/engine/` (owner: Sambhav).
Screens (`src/screens/`), scenarios (`src/scenarios/`), the generator (`src/data/`), the family page
(`src/family/`) and the scoreboard (`src/scoreboard/`) import **only** what is listed here.

The rules behind it are in SPEC.md. The source of truth is `reference/engine.py`.

```ts
import { assess, loadPack, naiveBaseline, RANK } from './engine/engine'
import { PACK_V02, PACK_V03 } from './engine/rulepacks'

assess(session: Session, pack: RulePack): Assessment
```

## Types

```ts
export type Tier = 'NONE' | 'NUDGE' | 'PAUSE' | 'CRITICAL'
export type Group = 'C' | 'P' | 'A'
export type PayeeStatus = 'known' | 'new' | 'recent' | 'warned' | 'flagged'
export type PayeeKind = 'person' | 'merchant' | 'biller'

// Same field names as the Python dataclass (snake_case), so cases.json and generator
// output can be passed in unchanged. Every field except `amount` is optional; a missing
// field takes the Python default (see SPEC.md section 2).
export interface Session {
  amount: number               // rupees, whole number
  median_90d?: number          // default 0
  payments_90d?: number        // default 50
  call_now?: boolean           // default false
  call_seen_30m?: boolean      // default false
  screen_share?: boolean       // default false
  payee_status?: PayeeStatus   // default 'known'
  payee_kind?: PayeeKind       // default 'person'
  same_payee_24h?: number      // default 0
  new_payees_60m?: number      // default 0
  sent_new_24h?: number        // default 0
  credit_unknown_48h?: number  // default 0 (= no such credit)
  payee_is_sender?: boolean    // default false
  payee_pasted?: boolean       // default false
  app_switch?: boolean         // default false
  night?: boolean              // default false
  q1?: boolean | null          // default null = not asked or skipped
  q2?: boolean | null          // default null = not asked or skipped
}

export interface LearnedRule {
  id: string                   // e.g. 'R5'
  all: string[]                // tokens that must all be present
  none?: string[]              // tokens that must all be absent (default [])
  explain: string              // added to reasons when the rule fires
  // carried in the pack but not read by the engine:
  name?: string
  then?: string
  source?: Record<string, number>
  approved_by?: string[]
}

export interface RulePack {
  version: string              // '0.2' | '0.3'
  based_on?: string
  learned: LearnedRule[]
}

export interface Assessment {
  tier: Tier
  groups: Group[]              // present groups, in the order C, P, A
  rules: string[]              // fired rules then escalators, e.g. ['R1', 'K1'] or ['N1'] or []
  reasons: string[]            // plain-English lines for the screens, in engine order
  packVersion: string          // pack.version of the pack used
}
```

## Other exports

| Export | Signature | Use |
|---|---|---|
| `assess` | `(s: Session, pack: RulePack) => Assessment` | The decision |
| `naiveBaseline` | `(s: Session) => boolean` | Bank-style comparison rule (new person, Rs 10,000+) |
| `loadPack` | `(version: '0.2' \| '0.3') => RulePack` | Same as Python `load_pack` |
| `PACK_V02`, `PACK_V03` | `RulePack` | The two packs (`src/engine/rulepacks.ts`) |
| `RANK` | `Record<Tier, number>` | `NONE 0, NUDGE 1, PAUSE 2, CRITICAL 3`; compare levels with this, never with string order |
| `tokens` | `(s: Session) => Set<string>` | Event vocabulary for the incident record / analyst view |
| `evidence` | `(s: Session) => Evidence` | Raw C / strong C / P / strong P / A flags plus reasons (for the analyst view and the scoreboard) |
| constants | `AMOUNT_MULTIPLE`, `AMOUNT_FLOOR`, `MIN_HISTORY`, `NO_HISTORY_FLOOR`, `CREDIT_MATCH`, `NUDGE_FLOOR`, `FEATURES` | Read-only |

## Guarantees

- **Pure.** No network calls, no randomness, no clock, no storage, no logging. The same input
  always gives the same output. The input session and the pack are never modified.
- **Answers only raise.** For any session, setting `q1` / `q2` to `false` never gives a lower tier
  than leaving them unset, and `true` never gives a lower tier than `false`.
- **Fast.** Under 5 ms per call on average (checked in `tests/invariants.test.ts`).
- **Matches Python.** All 28 cases in `tests/cases.json` give the expected tier on both packs.

## Differences from `engine.py`

| Python | TypeScript | Why |
|---|---|---|
| `Assessment(tier, groups, reasons, rules)`: field order tier, groups, reasons, rules | `{ tier, groups, rules, reasons, packVersion }` | Order in an object doesn't matter. `packVersion` is **new**: Python does not return it. It is the incident record's "rule version" |
| `assess(s, pack=None)`: pack optional, `None` acts as no learned rules | `pack` is required | Every caller must say which pack it used, so `packVersion` is always known. Pass `PACK_V02` for "no learned rules" |
| `Session` is a dataclass with defaults | `Session` is an interface with optional fields; defaults applied inside the engine | Plain JSON objects (cases.json, generator output) can be passed in directly |
| `q1` / `q2` are `Optional[bool]` (`None`) | `boolean \| null \| undefined` | `null` and `undefined` both mean "not asked or skipped" |
| `has_history`, `is_big`, `credit_match`, `naive_baseline`, `load_pack` | `hasHistory`, `isBig`, `creditMatch`, `naiveBaseline`, `loadPack` | camelCase function names |
| `load_pack(v)` reads `rulepack_v{v}.json` from disk for any `v` other than `'0.2'` | `loadPack` accepts only `'0.2'` and `'0.3'` and returns bundled data | No file system on the phone |
| `f"{x:.0f}"` in the "N times your usual payment" reason | Custom formatter that rounds exact halves to even | JavaScript's `toFixed(0)` rounds halves up, which would change the reason text for some amounts |
