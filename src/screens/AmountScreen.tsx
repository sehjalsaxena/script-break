// Screens 1 and 2 of 4: None (nothing extra) and Nudge (one inline line, no extra tap).
// The host app calls assess() as soon as the payee and amount are entered.
import { ChevronRight, Landmark } from 'lucide-react'
import { useEffect } from 'react'
import type { Assessment, PayeeStatus } from '../engine/engine'
import type { Payee } from '../scenarios/presets'
import { nudgeParts, rupees } from './format'
import { Avatar, Button, Header, Keypad } from './parts'

export const MAX_AMOUNT = 999999
export const BANK = { name: 'Demo Bank', masked: '•• 4821' }

interface Props {
  payee: Payee
  status: PayeeStatus
  amount: number
  note: string
  assessment: Assessment
  onAmount: (amount: number) => void
  onNote: (note: string) => void
  onBack: () => void
  onPay: () => void
  onSoon: (label: string) => void
}

export function AmountScreen({ payee, status, amount, note, assessment, onAmount, onNote, onBack, onPay, onSoon }: Props) {
  const nudge = assessment.tier === 'NUDGE' && amount > 0 ? nudgeParts(assessment.reasons, payee.name, status) : null
  const digit = (d: number) => onAmount(Math.min(MAX_AMOUNT, amount * 10 + d))
  const del = () => onAmount(Math.floor(amount / 10))

  // A physical keyboard works too (handy when presenting from a laptop).
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement) return
      if (/^\d$/.test(e.key)) onAmount(Math.min(MAX_AMOUNT, amount * 10 + Number(e.key)))
      else if (e.key === 'Backspace') onAmount(Math.floor(amount / 10))
      else if (e.key === 'Enter' && amount > 0) onPay()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [amount, onAmount, onPay])

  return (
    <>
      <Header
        onBack={onBack}
        title={<span className="flex items-center gap-2"><Avatar size="sm" initials={payee.initials} color={payee.color} />Paying {payee.name}</span>}
        subtitle={<span className="pl-11">Banking name: {payee.bankingName}</span>}
      />
      <div className="flex min-h-0 flex-1 flex-col items-center overflow-y-auto bg-white px-4 pt-6">
        <div className="text-xs font-semibold uppercase tracking-wider text-muted">Enter amount</div>
        <div className="mt-1 flex items-baseline text-[46px] font-bold tracking-tight text-ink" aria-live="polite" aria-label={`Amount ${rupees(amount)}`}>
          {amount > 0 ? rupees(amount) : <span className="text-faint">₹0</span>}
          <span className="sb-caret ml-0.5 h-10 w-0.5 self-center bg-app" />
        </div>
        <input
          value={note}
          onChange={(e) => onNote(e.target.value)}
          placeholder="Add a note"
          maxLength={40}
          className="mt-2 w-48 rounded-full bg-app-bg px-4 py-1.5 text-center text-sm text-ink outline-none placeholder:text-faint"
          aria-label="Note"
        />
        {nudge && (
          <div role="note" className="sb-rise mt-4 w-full rounded-lg border-l-4 border-nudge-bar bg-nudge-tint px-3 py-2.5 text-sm text-nudge-ink">
            {nudge.headline && <b>{nudge.headline} </b>}
            {nudge.body}
          </div>
        )}
      </div>
      <div className="flex-none space-y-2.5 border-t border-line bg-white px-4 pb-2 pt-2.5">
        <button type="button" onClick={() => onSoon('Choosing another bank account')} className="flex w-full items-center gap-3 text-left">
          <span className="flex h-9 w-9 items-center justify-center rounded-full bg-app-tint text-app"><Landmark size={18} /></span>
          <span className="flex-1 text-sm">
            <span className="block font-semibold text-ink">{BANK.name} {BANK.masked}</span>
            <span className="block text-xs text-muted">Savings account</span>
          </span>
          <ChevronRight size={18} className="text-muted" />
        </button>
        <Button variant="app" onClick={onPay} disabled={amount <= 0}>{amount > 0 ? `Pay ${rupees(amount)}` : 'Pay'}</Button>
      </div>
      <Keypad variant="amount" onDigit={digit} onDelete={del} />
    </>
  )
}
