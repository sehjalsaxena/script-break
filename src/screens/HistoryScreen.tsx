// Transaction history: earlier (seeded) payments plus the ones made in this demo session.
import { rupees } from './format'
import { Avatar, Header } from './parts'

export interface Txn {
  id: string
  name: string
  initials: string
  color: string
  amount: number
  when: string
}

export function HistoryScreen({ txns, onBack }: { txns: Txn[]; onBack: () => void }) {
  return (
    <>
      <Header title="History" onBack={onBack} />
      <ul className="min-h-0 flex-1 divide-y divide-line overflow-y-auto bg-white">
        {txns.map((t) => (
          <li key={t.id} className="flex items-center gap-3 px-4 py-3">
            <Avatar initials={t.initials} color={t.color} />
            <span className="min-w-0 flex-1">
              <span className="block truncate text-[15px] font-medium text-ink">Paid to {t.name}</span>
              <span className="block text-xs text-muted">{t.when}</span>
            </span>
            <span className="text-right">
              <span className="block text-[15px] font-bold text-ink">{rupees(t.amount)}</span>
              <span className="block text-xs text-muted">Debited</span>
            </span>
          </li>
        ))}
      </ul>
    </>
  )
}
