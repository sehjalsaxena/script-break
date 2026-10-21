// Screen 4 of 4: Critical. Warning, family option and a 10-second confirmation delay.
// The message ends with the anti-coaching line; "Continue anyway" always stays available.
import { useEffect, useState } from 'react'
import type { Assessment, PayeeStatus } from '../engine/engine'
import type { Contact, Payee } from '../scenarios/presets'
import { CRITICAL_COPY, type Lang } from './copy'
import { rupees } from './format'
import { Button } from './parts'

const DELAY_SECONDS = 10

interface Props {
  payee: Payee
  status: PayeeStatus
  amount: number
  assessment: Assessment // including the answers
  contact: Contact
  askedFamily: boolean
  onWait: () => void
  onAskFamily: () => void
  onContinue: () => void
}

export function CriticalScreen({ payee, status, amount, assessment, contact, askedFamily, onWait, onAskFamily, onContinue }: Props) {
  const [lang, setLang] = useState<Lang>('en')
  const [secs, setSecs] = useState(DELAY_SECONDS)
  useEffect(() => {
    if (secs <= 0) return
    const id = setTimeout(() => setSecs((s) => s - 1), 1000)
    return () => clearTimeout(id)
  }, [secs])

  const t = CRITICAL_COPY[lang]
  const isNew = status === 'new' || status === 'recent'
  return (
    <>
      <div className="flex-none bg-crit px-4 pb-3.5 pt-3 text-white">
        <div className="flex items-start justify-between gap-2">
          <h1 className="text-[21px] font-bold leading-tight">{t.title}</h1>
          <button
            type="button"
            onClick={() => setLang(lang === 'en' ? 'hi' : 'en')}
            className="flex-none whitespace-nowrap rounded-lg border border-white/60 px-1.5 py-0.5 text-xs"
            aria-label="Switch language"
          >
            <span className={lang === 'en' ? 'font-bold' : 'opacity-70'}>EN</span> | <span className={lang === 'hi' ? 'font-bold' : 'opacity-70'}>हिंदी</span>
          </button>
        </div>
        <p className="mt-1 text-sm opacity-90">{t.calm}</p>
      </div>
      <div className="sb-rise flex min-h-0 flex-1 flex-col gap-2.5 overflow-y-auto bg-white px-4 pb-2 pt-3">
        <p className="text-[15px] font-medium leading-snug text-ink">{t.never}</p>
        <div className="rounded-lg border border-line px-3 py-2 text-sm text-muted">
          <b className="text-ink">{t.toPayee(rupees(amount), payee.name, isNew)}</b>
          {/* Engine reasons are English only for now (Hindi: TODO). */}
          {assessment.reasons.map((r) => <div key={r}>{r}</div>)}
        </div>
        <p className="rounded-lg bg-ink px-3 py-2.5 text-[15px] font-bold leading-snug text-white">{t.last}</p>
        <Button variant="primary" onClick={onWait}>{t.wait}</Button>
        <Button variant="outline" onClick={onAskFamily} disabled={askedFamily}>
          {askedFamily ? `✓ ${t.asked(contact.name)}` : t.askFamily(`${contact.name} (${contact.relation})`)}
        </Button>
        <Button variant="ghost" onClick={onContinue} disabled={secs > 0}>
          {secs > 0 ? t.continueIn(secs) : t.continueNow}
        </Button>
      </div>
      <p className="flex-none bg-white py-3 text-center text-xs text-muted">{t.report}</p>
    </>
  )
}
