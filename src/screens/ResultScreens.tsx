// After the PIN: processing, success; or "not sent" after "Wait 30 minutes"; and the balance result.
import { Check, Clock, Landmark, Share2 } from 'lucide-react'
import { useEffect, useRef } from 'react'
import { BANK } from './AmountScreen'
import { CRITICAL_COPY, DONE_COPY } from './copy'
import { rupees } from './format'
import { Button } from './parts'

export interface Receipt {
  payeeName: string
  bankingName: string
  amount: number
  note: string
  txnId: string
  wasWarned: boolean // a Pause or Critical screen was shown for this payment
}

const PROCESSING_MS = 1400

export function ProcessingScreen({ onDone }: { onDone: () => void }) {
  // Keep the latest callback without restarting the timer on every render.
  const done = useRef(onDone)
  useEffect(() => {
    done.current = onDone
  })
  useEffect(() => {
    const id = setTimeout(() => done.current(), PROCESSING_MS)
    return () => clearTimeout(id)
  }, [])
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 bg-white">
      <span className="h-14 w-14 animate-spin rounded-full border-4 border-app-tint border-t-app" aria-hidden />
      <p className="text-[15px] font-semibold text-ink">Processing payment…</p>
      <p className="text-xs text-muted">Please don't press back</p>
    </div>
  )
}

export function SuccessScreen({ receipt, onDone, onSoon }: { receipt: Receipt; onDone: () => void; onSoon: (label: string) => void }) {
  return (
    <div className="flex min-h-0 flex-1 flex-col bg-app-bg">
      <div className="flex flex-none flex-col items-center bg-success px-6 pb-8 pt-6 text-center text-white">
        <span className="sb-pop flex h-16 w-16 items-center justify-center rounded-full bg-white text-success"><Check size={38} strokeWidth={3} /></span>
        <p className="mt-3 text-sm font-semibold text-white/90">Payment successful</p>
        <p className="mt-1 text-[40px] font-bold leading-none">{rupees(receipt.amount)}</p>
        <p className="mt-2 text-sm text-white/90">Paid to {receipt.payeeName}</p>
        <p className="text-xs text-white/75">Today, 12:52 pm</p>
      </div>
      <div className="min-h-0 flex-1 space-y-3 overflow-y-auto px-4 pt-4">
        {receipt.wasWarned && (
          <p className="rounded-xl border border-crit/20 bg-crit-tint px-3 py-2.5 text-sm font-semibold text-crit">{CRITICAL_COPY.en.report}</p>
        )}
        <dl className="space-y-2.5 rounded-2xl bg-white p-4 text-sm shadow-sm">
          <div><dt className="text-xs text-muted">Banking name</dt><dd className="font-semibold text-ink">{receipt.bankingName}</dd></div>
          <div><dt className="text-xs text-muted">UPI transaction ID</dt><dd className="font-mono font-semibold text-ink">{receipt.txnId}</dd></div>
          <div><dt className="text-xs text-muted">Debited from</dt><dd className="font-semibold text-ink">{BANK.name} {BANK.masked}</dd></div>
          {receipt.note && <div><dt className="text-xs text-muted">Note</dt><dd className="font-semibold text-ink">{receipt.note}</dd></div>}
        </dl>
        <p className="text-center text-xs text-faint">{DONE_COPY.demo}</p>
      </div>
      <div className="flex flex-none gap-3 px-4 pb-5 pt-3">
        <button type="button" onClick={() => onSoon('Share receipt')} className="flex w-full items-center justify-center gap-2 rounded-xl border-2 border-app py-3 text-[15px] font-bold text-app">
          <Share2 size={18} /> Share
        </button>
        <Button variant="app" onClick={onDone}>Done</Button>
      </div>
    </div>
  )
}

export function WaitingScreen({ onHome }: { onHome: () => void }) {
  return (
    <div className="flex flex-1 flex-col items-center bg-white px-6 pt-20 text-center">
      <span className="flex h-16 w-16 items-center justify-center rounded-full bg-ink text-white"><Clock size={32} /></span>
      <h1 className="mt-4 text-xl font-bold text-ink">{DONE_COPY.waitingTitle}</h1>
      <p className="mt-2 text-sm text-muted">{DONE_COPY.waitingBody}</p>
      <div className="mt-auto w-full pb-5"><Button variant="app" onClick={onHome}>{DONE_COPY.home}</Button></div>
    </div>
  )
}

export function BalanceScreen({ balance, onHome }: { balance: number; onHome: () => void }) {
  return (
    <div className="flex flex-1 flex-col bg-app-bg px-4 pt-8">
      <div className="sb-rise rounded-2xl bg-white p-5 text-center shadow-sm">
        <span className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-app-tint text-app"><Landmark size={24} /></span>
        <p className="mt-3 text-sm font-semibold text-ink">{BANK.name} {BANK.masked}</p>
        <p className="text-xs text-muted">Savings account</p>
        <p className="mt-4 text-xs uppercase tracking-wider text-muted">Available balance</p>
        <p className="text-[34px] font-bold text-ink">{rupees(balance)}</p>
      </div>
      <p className="mt-3 text-center text-xs text-faint">Demo balance. No real account.</p>
      <div className="mt-auto pb-5"><Button variant="app" onClick={onHome}>{DONE_COPY.home}</Button></div>
    </div>
  )
}
