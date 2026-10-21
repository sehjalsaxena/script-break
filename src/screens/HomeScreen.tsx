// Home of the mock UPI app: transfer shortcuts, people, bills, and the bottom bar with Scan.
import {
  ArrowLeftRight, Banknote, Bell, Building, CircleQuestionMark, CreditCard, Droplets, Flame, House,
  Landmark, QrCode, Receipt, ScanLine, Search, ShieldCheck, Smartphone, Tv, Wallet, Zap,
} from 'lucide-react'
import type { ReactNode } from 'react'
import type { Payee } from '../scenarios/presets'
import { Avatar, IconButton } from './parts'

export type Transfer = 'mobile' | 'upi' | 'self' | 'balance'

interface Props {
  people: Payee[]
  onTransfer: (t: Transfer) => void
  onPerson: (p: Payee) => void
  onScan: () => void
  onHistory: () => void
  onSoon: (label: string) => void
}

function Tile({ icon, label, onClick, solid }: { icon: ReactNode; label: string; onClick: () => void; solid?: boolean }) {
  return (
    <button type="button" onClick={onClick} className="flex flex-col items-center gap-1.5 rounded-xl p-1 text-center active:bg-app-tint">
      <span className={`flex h-12 w-12 items-center justify-center rounded-2xl ${solid ? 'bg-app text-white' : 'bg-app-tint text-app'}`}>{icon}</span>
      <span className="text-[11.5px] font-medium leading-tight text-ink">{label}</span>
    </button>
  )
}

function Card({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mx-3 rounded-2xl bg-white p-3 shadow-sm">
      <h2 className="mb-2.5 px-1 text-[15px] font-bold text-ink">{title}</h2>
      {children}
    </section>
  )
}

export function HomeScreen({ people, onTransfer, onPerson, onScan, onHistory, onSoon }: Props) {
  const bills: [ReactNode, string][] = [
    [<Smartphone size={22} />, 'Mobile Recharge'], [<Tv size={22} />, 'DTH'], [<Zap size={22} />, 'Electricity'], [<CreditCard size={22} />, 'Credit Card'],
    [<Building size={22} />, 'Rent'], [<Flame size={22} />, 'Book Gas'], [<Droplets size={22} />, 'Water'], [<Banknote size={22} />, 'Loan Repay'],
  ]
  return (
    <>
      <div className="flex flex-none items-center gap-3 bg-app px-4 pb-4 pt-1 text-white">
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-white/20 text-sm font-bold">MS</div>
        <div className="min-w-0 flex-1">
          <div className="text-[15px] font-semibold">Hi, Mr. Sharma</div>
          <div className="text-xs text-white/75">Pune · DemoPay</div>
        </div>
        <IconButton label="My QR code" onClick={() => onSoon('My QR code')}><QrCode size={22} /></IconButton>
        <IconButton label="Notifications" onClick={() => onSoon('Notifications')}><Bell size={22} /></IconButton>
        <IconButton label="Help" onClick={() => onSoon('Help')}><CircleQuestionMark size={22} /></IconButton>
      </div>

      <div className="min-h-0 flex-1 space-y-3 overflow-y-auto bg-app-bg pb-4 pt-3">
        <Card title="Transfer Money">
          <div className="grid grid-cols-4 gap-1">
            <Tile solid icon={<Smartphone size={22} />} label="To Mobile Number" onClick={() => onTransfer('mobile')} />
            <Tile solid icon={<Landmark size={22} />} label="To Bank / UPI ID" onClick={() => onTransfer('upi')} />
            <Tile solid icon={<ArrowLeftRight size={22} />} label="To Self Account" onClick={() => onTransfer('self')} />
            <Tile solid icon={<Wallet size={22} />} label="Check Balance" onClick={() => onTransfer('balance')} />
          </div>
        </Card>

        <div className="mx-3 flex items-start gap-3 rounded-2xl border border-safe/20 bg-safe-tint px-3.5 py-3">
          <ShieldCheck size={22} className="mt-0.5 flex-none text-safe" />
          <div className="text-[12.5px] leading-snug text-[#0B4D38]">
            <b>ScriptBreak is on.</b> Each payment is checked for common scam patterns before your PIN, on this phone. It never reads your calls or messages.
          </div>
        </div>

        <Card title="People">
          <div className="grid grid-cols-4 gap-y-3">
            {people.map((p) => (
              <button key={p.id} type="button" onClick={() => onPerson(p)} className="flex flex-col items-center gap-1 rounded-xl p-1 active:bg-app-tint">
                <Avatar initials={p.initials} color={p.color} />
                <span className="w-full truncate text-center text-[11.5px] text-ink">{p.name.split(' (')[0]}</span>
              </button>
            ))}
          </div>
        </Card>

        <Card title="Recharge & Pay Bills">
          <div className="grid grid-cols-4 gap-1">
            {bills.map(([icon, label]) => <Tile key={label} icon={icon} label={label} onClick={() => onSoon(label)} />)}
          </div>
        </Card>

        <p className="text-center text-[11px] text-faint">Demo app. All names, numbers and amounts are synthetic.</p>
      </div>

      <nav className="flex flex-none items-end justify-around border-t border-line bg-white px-2 pb-3 pt-1.5 text-[11px] text-muted">
        <button type="button" className="flex flex-col items-center gap-0.5 font-semibold text-app"><House size={22} />Home</button>
        <button type="button" onClick={() => onTransfer('mobile')} className="flex flex-col items-center gap-0.5"><Search size={22} />Search</button>
        <button type="button" onClick={onScan} className="-mt-7 flex flex-col items-center gap-1 font-semibold text-app" aria-label="Scan QR code">
          <span className="flex h-14 w-14 items-center justify-center rounded-full bg-app text-white shadow-lg ring-4 ring-white"><ScanLine size={26} /></span>
          Scan
        </button>
        <button type="button" onClick={() => onSoon('Alerts')} className="flex flex-col items-center gap-0.5"><Bell size={22} />Alerts</button>
        <button type="button" onClick={onHistory} className="flex flex-col items-center gap-0.5"><Receipt size={22} />History</button>
      </nav>
    </>
  )
}
