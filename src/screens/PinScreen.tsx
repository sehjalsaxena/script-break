// Mock UPI PIN pad. Demo only: any 4 digits are accepted and nothing is stored or sent.
import { Info, Landmark } from 'lucide-react'
import { useEffect, useState } from 'react'
import { BANK } from './AmountScreen'
import { rupees } from './format'
import { Keypad } from './parts'

const PIN_LENGTH = 4

interface Props {
  purpose: 'pay' | 'balance'
  payeeName?: string
  amount?: number
  onBack: () => void
  onSubmit: () => void
}

export function PinScreen({ purpose, payeeName, amount, onBack, onSubmit }: Props) {
  const [count, setCount] = useState(0)
  const full = count >= PIN_LENGTH

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (/^\d$/.test(e.key)) setCount((c) => Math.min(PIN_LENGTH, c + 1))
      else if (e.key === 'Backspace') setCount((c) => Math.max(0, c - 1))
      else if (e.key === 'Enter' && full) onSubmit()
      else if (e.key === 'Escape') onBack()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [full, onBack, onSubmit])

  return (
    <>
      <div className="flex flex-none items-center gap-3 border-b border-line bg-white px-4 py-3">
        <span className="flex h-9 w-9 items-center justify-center rounded-full bg-app-tint text-app"><Landmark size={18} /></span>
        <span className="flex-1">
          <span className="block text-sm font-bold text-ink">{BANK.name}</span>
          <span className="block text-xs text-muted">{BANK.masked}</span>
        </span>
        <span className="text-lg font-black italic tracking-tight text-[#2B4A7E]">UPI</span>
      </div>
      <div className="flex min-h-0 flex-1 flex-col overflow-y-auto bg-white px-5 pt-4">
        {purpose === 'pay' ? (
          <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
            <dt className="text-muted">To:</dt><dd className="font-semibold text-ink">{payeeName}</dd>
            <dt className="text-muted">Sending:</dt><dd className="text-lg font-bold text-ink">{rupees(amount ?? 0)}</dd>
          </dl>
        ) : (
          <p className="text-sm font-semibold text-ink">Check balance of {BANK.name} {BANK.masked}</p>
        )}
        <div className="mt-8 text-center text-xs font-bold uppercase tracking-[0.18em] text-muted">Enter 4-digit UPI PIN</div>
        <div className="mt-4 flex justify-center gap-5" aria-label={`${count} of ${PIN_LENGTH} digits entered`}>
          {Array.from({ length: PIN_LENGTH }, (_, i) => (
            <span key={i} className={`flex h-8 w-8 items-end justify-center border-b-2 pb-1 ${i < count ? 'border-ink' : 'border-[#B8BFCC]'}`}>
              {i < count && <span className="h-3 w-3 rounded-full bg-ink" />}
            </span>
          ))}
        </div>
        <p className="mt-auto mb-3 flex items-start gap-2 rounded-lg bg-[#FFF6E0] px-3 py-2 text-xs text-[#6B4E00]">
          <Info size={15} className="mt-px flex-none" />
          Demo only: tap any 4 digits, then the tick. No real PIN, no real money.
        </p>
      </div>
      <Keypad
        variant="pin"
        onDigit={() => setCount((c) => Math.min(PIN_LENGTH, c + 1))}
        onDelete={() => setCount((c) => Math.max(0, c - 1))}
        onSubmit={onSubmit}
        canSubmit={full}
      />
    </>
  )
}
