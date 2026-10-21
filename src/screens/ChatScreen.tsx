// The payee's thread: verified banking name, earlier payments, and the Pay / Request buttons.
import { BadgeCheck, CheckCheck, EllipsisVertical, Phone } from 'lucide-react'
import type { PayeeStatus } from '../engine/engine'
import type { Payee } from '../scenarios/presets'
import { historyLine } from './copy'
import { rupees } from './format'
import { Avatar, Button, Header, IconButton } from './parts'

interface Props {
  payee: Payee
  status: PayeeStatus
  onBack: () => void
  onPay: () => void
  onSoon: (label: string) => void
}

export function ChatScreen({ payee, status, onBack, onPay, onSoon }: Props) {
  const past = status === 'new' || status === 'recent' ? [] : payee.past ?? []
  return (
    <>
      <Header
        onBack={onBack}
        title={<span className="flex items-center gap-2"><Avatar size="sm" initials={payee.initials} color={payee.color} />{payee.name}</span>}
        right={
          <>
            <IconButton label="Call" onClick={() => onSoon('Calling')}><Phone size={20} /></IconButton>
            <IconButton label="More" onClick={() => onSoon('More options')}><EllipsisVertical size={20} /></IconButton>
          </>
        }
      />
      <div className="min-h-0 flex-1 space-y-3 overflow-y-auto bg-app-bg px-4 py-4">
        <div className="mx-auto w-fit max-w-full rounded-xl bg-white px-3 py-2 text-center text-xs text-muted shadow-sm">
          <span className="flex items-center justify-center gap-1 font-semibold text-ink">
            <BadgeCheck size={15} className="text-success" /> Banking name: {payee.bankingName}
          </span>
          <span className="block">{payee.vpa}{payee.phone ? ` · ${payee.phone}` : ''}</span>
          <span className="block">{historyLine(status, payee.knownHistory)}</span>
        </div>
        {past.length === 0 ? (
          <p className="pt-8 text-center text-sm text-muted">No payments with {payee.name} yet.</p>
        ) : (
          [...past].reverse().map((t, i) => (
            <div key={i} className="ml-auto w-44 rounded-2xl rounded-br-md bg-white px-3.5 py-2.5 shadow-sm">
              <div className="text-xl font-bold text-ink">{rupees(t.amount)}</div>
              <div className="mt-0.5 flex items-center gap-1 text-xs text-success"><CheckCheck size={14} /> Paid · {t.when}</div>
            </div>
          ))
        )}
      </div>
      <div className="flex flex-none gap-3 border-t border-line bg-white px-4 pb-4 pt-3">
        <Button variant="app" onClick={onPay}>Pay</Button>
        <button type="button" onClick={() => onSoon('Request money')} className="w-full rounded-xl border-2 border-app py-3 text-[15px] font-bold text-app">
          Request
        </button>
      </div>
    </>
  )
}
