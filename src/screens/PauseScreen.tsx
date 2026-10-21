// Screen 3 of 4: Pause. Facts first, then two optional questions. The answers go back into
// assess(); they can only raise the level (to Critical), never lower it.
import { ChevronLeft, ShieldCheck } from 'lucide-react'
import type { Assessment } from '../engine/engine'
import type { Contact, Payee } from '../scenarios/presets'
import { PAUSE_COPY as T } from './copy'
import type { Answers } from './flow'
import { rupees, sentences } from './format'
import { Button } from './parts'

interface Props {
  payee: Payee
  amount: number
  assessment: Assessment // before the answers
  answers: Answers
  contact: Contact
  askedFamily: boolean
  onAnswer: (answers: Answers) => void
  onAskFamily: () => void
  onBack: () => void
  onNext: () => void
}

function Question({ text, value, onChange }: { text: string; value: boolean | null; onChange: (v: boolean | null) => void }) {
  const option = (label: string, v: boolean) => (
    <button
      type="button"
      aria-pressed={value === v}
      onClick={() => onChange(value === v ? null : v)}
      className={`flex-1 rounded-lg border py-2 text-sm font-semibold ${value === v ? 'border-ink bg-ink text-white' : 'border-[#C8CEDA] text-ink'}`}
    >
      {label}
    </button>
  )
  return (
    <div className="rounded-xl border border-line px-3 py-2.5 text-sm font-medium text-ink">
      {text}
      <div className="mt-2 flex gap-2">
        {option(T.yes, true)}
        {option(T.no, false)}
      </div>
    </div>
  )
}

export function PauseScreen({ payee, amount, assessment, answers, contact, askedFamily, onAnswer, onAskFamily, onBack, onNext }: Props) {
  return (
    <>
      <div className="flex flex-none items-center gap-2 border-b border-line bg-white px-3 pb-2.5 pt-1">
        <button type="button" onClick={onBack} aria-label="Back" className="rounded-full p-1.5 text-ink hover:bg-panel"><ChevronLeft size={24} /></button>
        <span className="flex-1 text-[17px] font-bold text-ink">{T.title}</span>
        <span className="flex items-center gap-1 rounded-full bg-safe-tint px-2 py-0.5 text-[11px] font-semibold text-safe"><ShieldCheck size={13} /> ScriptBreak</span>
      </div>
      <div className="sb-rise flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto bg-white px-4 py-3">
        <div className="rounded-lg border-l-4 border-pause bg-pause-tint px-3 py-2.5 text-sm text-pause-ink">
          <span className="mb-1 block text-[15px] font-bold leading-snug text-[#3A1A08]">{T.sends(rupees(amount), payee.name)}</span>
          {sentences(assessment.reasons)}
        </div>
        <Question text={T.q1} value={answers.q1} onChange={(q1) => onAnswer({ ...answers, q1 })} />
        <Question text={T.q2} value={answers.q2} onChange={(q2) => onAnswer({ ...answers, q2 })} />
        <p className="text-center text-sm">
          {askedFamily ? (
            <span className="font-semibold text-safe">✓ {T.asked(contact.name)}</span>
          ) : (
            <>
              {T.notSure}{' '}
              <button type="button" onClick={onAskFamily} className="font-bold text-app">{T.askFamily(contact.name)}</button>
            </>
          )}
        </p>
      </div>
      <div className="flex-none bg-white px-4 pb-4 pt-2">
        <Button variant="outline" onClick={onNext}>{T.next}</Button>
        <p className="mt-2 text-center text-[11px] text-faint">Checked on this phone · rule pack v{assessment.packVersion}</p>
      </div>
    </>
  )
}
