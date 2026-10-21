// Scenario panel: picks a demo preset, simulates the phone signals (prototype only: deck page 10,
// "OS signals simulated") and shows what assess() returned, so judges can follow the decision.
import type { ReactNode } from 'react'
import type { Assessment, PayeeKind, PayeeStatus, Tier } from '../engine/engine'
import type { Scenario } from '../scenarios/presets'
import type { Context, FamilyAlertRequest, Step } from '../screens/flow'
import { rupees } from '../screens/format'

export interface Live {
  step: Step
  payeeName: string | null
  assessment: Assessment | null
}

const TIER_STYLE: Record<Tier, string> = {
  NONE: 'bg-panel text-muted',
  NUDGE: 'bg-nudge-tint text-nudge',
  PAUSE: 'bg-pause-tint text-pause',
  CRITICAL: 'bg-crit-tint text-crit',
}

const STEP_LABEL: Record<Step, string> = {
  home: 'Home', search: 'Search', scan: 'Scan', chat: 'Payee', amount: 'Amount', pause: 'Pause screen',
  critical: 'Critical screen', pin: 'UPI PIN', processing: 'Processing', success: 'Paid', waiting: 'Waiting 30 minutes',
  history: 'History', 'balance-pin': 'Balance PIN', balance: 'Balance',
}

/** What the presenter taps, in order, for this scenario. */
function howTo(s: Scenario, status: PayeeStatus): string[] {
  const p = s.payee
  const isNew = status === 'new' || status === 'recent'
  const find = p.kind === 'merchant'
    ? `Tap Scan, then the "${p.name}" QR code`
    : isNew
      ? s.context.payee_pasted
        ? 'Tap To Mobile Number, then "Paste UPI ID"'
        : `Tap To Mobile Number, type "${p.name.split(' ')[0].toLowerCase()}", tap ${p.name}`
      : `Tap ${p.name.split(' (')[0]} under People`
  const steps = [find, `Tap Pay, type ${rupees(s.amount)}, tap Pay`]
  const a = s.expect.answers
  if (a) {
    const yn = (v: boolean | null) => (v === null ? 'skip' : v ? 'Yes' : 'No')
    steps.push(`Q1: ${yn(a.q1)} · Q2: ${yn(a.q2)} · Next`)
  }
  return steps
}

interface Props {
  scenarios: Scenario[]
  selected: Scenario
  onChoose: (s: Scenario) => void
  onRestart: () => void
  onJump: () => void
  context: Context
  onContext: (c: Context) => void
  packVersion: '0.2' | '0.3'
  onPack: (v: '0.2' | '0.3') => void
  live: Live | null
  alerts: FamilyAlertRequest[]
}

function Card({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-2xl bg-white p-4 shadow-sm">
      <h2 className="mb-3 text-xs font-bold uppercase tracking-wider text-muted">{title}</h2>
      {children}
    </section>
  )
}

export function ScenarioPanel({ scenarios, selected, onChoose, onRestart, onJump, context, onContext, packVersion, onPack, live, alerts }: Props) {
  const item = (s: Scenario) => (
    <li key={s.id}>
      <button
        type="button"
        onClick={() => onChoose(s)}
        className={`flex w-full items-baseline gap-2 rounded-lg px-2 py-1.5 text-left text-sm ${s.id === selected.id ? 'bg-brand-tint font-semibold text-brand' : 'text-ink hover:bg-panel'}`}
      >
        {s.demoStep && <span className="w-4 flex-none font-bold">{s.demoStep}.</span>}
        <span className="flex-1">{s.title}</span>
        <span className="font-mono text-xs text-faint">{s.caseId}</span>
      </button>
    </li>
  )
  const toggle = (key: 'call_now' | 'call_seen_30m' | 'screen_share', label: string) => (
    <label className="flex items-center gap-2 text-sm text-ink">
      <input type="checkbox" checked={!!context[key]} onChange={(e) => onContext({ ...context, [key]: e.target.checked })} />
      {label}
    </label>
  )
  const a = live?.assessment

  return (
    <aside className="mx-auto mt-4 w-full max-w-[430px] space-y-4 px-4 pb-10 lg:mt-0 lg:w-[400px] lg:max-w-none lg:px-0">
      <Card title="Demo script">
        <ol className="space-y-0.5">{scenarios.filter((s) => s.demoStep).map(item)}</ol>
        <h3 className="mb-1 mt-3 text-xs font-semibold text-muted">More scenarios</h3>
        <ul className="space-y-0.5">{scenarios.filter((s) => !s.demoStep).map(item)}</ul>
        <div className="mt-3 rounded-lg bg-panel p-3 text-sm text-body">
          <p>{selected.blurb}</p>
          <ol className="mt-2 list-decimal space-y-0.5 pl-5 text-ink">
            {howTo(selected, context.payee_status ?? 'known').map((x) => <li key={x}>{x}</li>)}
          </ol>
        </div>
        <div className="mt-3 flex gap-4 text-sm font-semibold">
          <button type="button" onClick={onRestart} className="text-brand">↺ Restart app</button>
          <button type="button" onClick={onJump} className="text-brand">⇢ Jump to the payment</button>
        </div>
      </Card>

      <Card title="Engine output: assess()">
        {a && live ? (
          <div className="space-y-2 text-sm">
            <div className="flex flex-wrap items-center gap-2">
              <span className={`rounded-md px-2 py-0.5 font-mono text-sm font-bold ${TIER_STYLE[a.tier]}`}>{a.tier}</span>
              <span className="text-muted">{live.payeeName} · {STEP_LABEL[live.step]} · pack v{a.packVersion}</span>
            </div>
            <div className="font-mono text-xs text-muted">
              rules: [{a.rules.join(', ')}] · groups: [{a.groups.join(', ')}]
            </div>
            {a.reasons.length > 0 && (
              <ul className="list-disc space-y-0.5 pl-5 text-body">
                {a.reasons.map((r) => <li key={r}>{r}</li>)}
              </ul>
            )}
          </div>
        ) : (
          <p className="text-sm text-muted">No payee chosen yet. The engine runs once a payee and amount are entered.</p>
        )}
      </Card>

      <Card title="Simulated phone signals">
        <div className="space-y-2">
          {toggle('call_now', 'On a call right now')}
          {toggle('call_seen_30m', 'Call seen in this app, last 30 min')}
          {toggle('screen_share', 'Screen shared / remote-access app')}
          <label className="flex items-center justify-between gap-2 text-sm text-ink">
            {selected.payee.name}
            <select
              value={context.payee_status ?? 'known'}
              onChange={(e) => onContext({ ...context, payee_status: e.target.value as PayeeStatus })}
              className="rounded-md border border-line px-2 py-1"
            >
              <option value="known">Paid before</option>
              <option value="new">New</option>
              <option value="recent">First paid &lt; 7 days ago</option>
              <option value="warned">Paid after a warning</option>
              <option value="flagged">Bank-flagged</option>
            </select>
          </label>
          <label className="flex items-center justify-between gap-2 text-sm text-ink">
            Payee kind
            <select
              value={context.payee_kind ?? 'person'}
              onChange={(e) => onContext({ ...context, payee_kind: e.target.value as PayeeKind })}
              className="rounded-md border border-line px-2 py-1"
            >
              <option value="person">Person</option>
              <option value="merchant">Merchant</option>
              <option value="biller">Allow-listed biller</option>
            </select>
          </label>
          <div className="flex items-center justify-between gap-2 text-sm text-ink">
            Rule pack
            <div className="flex overflow-hidden rounded-md border border-line">
              {(['0.2', '0.3'] as const).map((v) => (
                <button
                  key={v}
                  type="button"
                  aria-pressed={packVersion === v}
                  onClick={() => onPack(v)}
                  className={`px-3 py-1 ${packVersion === v ? 'bg-ink text-white' : 'text-ink'}`}
                >
                  v{v}
                </button>
              ))}
            </div>
          </div>
          <p className="text-xs text-faint">
            Usual payment (90-day median): {context.median_90d ? rupees(context.median_90d) : 'little history'}.
            Other contacts count as paid before. Prototype only: real apps read these signals from the OS.
          </p>
        </div>
      </Card>

      <Card title="Family alerts (simulated)">
        {alerts.length === 0 ? (
          <p className="text-sm text-muted">None yet. Tap "Ask … to call me" on Pause or Critical.</p>
        ) : (
          <ul className="space-y-1 text-sm text-ink">
            {alerts.map((x, i) => (
              <li key={i}>
                📳 {x.contactName}: please call. About to pay {rupees(x.amount)} to {x.payeeName} ({x.tier}).
              </li>
            ))}
          </ul>
        )}
        <p className="mt-2 text-xs text-faint">The relay to the second phone goes in src/family/.</p>
      </Card>

      <p className="text-center text-xs font-semibold text-muted">All data synthetic</p>
    </aside>
  )
}
