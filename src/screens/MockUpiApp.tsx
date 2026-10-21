// The mock UPI app ("DemoPay"). ScriptBreak sits between the amount and the PIN:
// assess() runs on the phone as soon as the payee and amount are entered (CONTRACT.md).
import { useCallback, useEffect, useMemo, useState } from 'react'
import { assess, type Assessment, type PayeeStatus, type RulePack } from '../engine/engine'
import { ADDRESS_BOOK, type Payee, type Scenario } from '../scenarios/presets'
import { AmountScreen } from './AmountScreen'
import { ChatScreen } from './ChatScreen'
import { CriticalScreen } from './CriticalScreen'
import { buildSession, contextFor, NO_ANSWERS, stepAfterPause, stepAfterPay, type Answers, type Context, type FamilyAlertRequest, type Step } from './flow'
import { HistoryScreen, type Txn } from './HistoryScreen'
import { HomeScreen, type Transfer } from './HomeScreen'
import { PauseScreen } from './PauseScreen'
import { PhoneFrame, Toast, type Tone } from './parts'
import { PinScreen } from './PinScreen'
import { BalanceScreen, ProcessingScreen, SuccessScreen, WaitingScreen, type Receipt } from './ResultScreens'
import { ScanScreen } from './ScanScreen'
import { SearchScreen } from './SearchScreen'

const OPENING_BALANCE = 124560
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
const dayKey = (when: string) => {
  const [d, m] = when.split(' ')
  return MONTHS.indexOf(m) * 100 + Number(d)
}

// Earlier payments from the address book, newest first.
const SEEDED: Txn[] = ADDRESS_BOOK
  .flatMap((p) => (p.past ?? []).map((t, i) => ({ id: `${p.id}-${i}`, name: p.name, initials: p.initials, color: p.color, amount: t.amount, when: t.when })))
  .sort((a, b) => dayKey(b.when) - dayKey(a.when))

const TONE: Record<Step, Tone> = {
  home: 'app', search: 'app', chat: 'app', amount: 'app', history: 'app', scan: 'dark',
  pause: 'light', critical: 'crit', pin: 'light', processing: 'light', success: 'success',
  waiting: 'light', 'balance-pin': 'light', balance: 'light',
}

const isNewStatus = (s: PayeeStatus) => s === 'new' || s === 'recent'

interface Props {
  scenario: Scenario
  context: Context // the scenario's signals (simulated phone state); may change while the app is open
  pack: RulePack
  startAtPayment: boolean // open straight on the amount screen for the scenario's payee
  onAskFamily: (alert: FamilyAlertRequest) => void
  onChange: (state: { step: Step; payeeName: string | null; assessment: Assessment | null }) => void
}

export function MockUpiApp({ scenario, context, pack, startAtPayment, onAskFamily, onChange }: Props) {
  const [stack, setStack] = useState<Step[]>(startAtPayment ? ['home', 'chat', 'amount'] : ['home'])
  const [payee, setPayee] = useState<Payee | null>(startAtPayment ? scenario.payee : null)
  const [amount, setAmount] = useState(startAtPayment ? scenario.amount : 0)
  const [note, setNote] = useState(startAtPayment ? scenario.note : '')
  const [answers, setAnswers] = useState<Answers>(NO_ANSWERS)
  const [askedFamily, setAskedFamily] = useState(false)
  const [warned, setWarned] = useState(false) // a Pause or Critical screen was shown for this payment
  const [searchMode, setSearchMode] = useState<'mobile' | 'upi'>('mobile')
  const [txns, setTxns] = useState<Txn[]>(SEEDED)
  const [spent, setSpent] = useState(0)
  const [receipt, setReceipt] = useState<Receipt | null>(null)
  const [toast, setToast] = useState<string | null>(null)

  const step = stack[stack.length - 1]

  // ---- ScriptBreak: the signals for this payee, and the engine's decision ----
  const contextOf = useCallback((p: Payee) => contextFor(context, p.id === scenario.payee.id, p.kind), [context, scenario.payee.id])
  const statusOf = useCallback((p: Payee) => contextOf(p).payee_status ?? 'known', [contextOf])
  const payeeContext = useMemo(() => (payee ? contextOf(payee) : null), [payee, contextOf])
  const before = useMemo(() => (payeeContext ? assess(buildSession(payeeContext, amount), pack) : null), [payeeContext, amount, pack])
  const current = useMemo(() => (payeeContext ? assess(buildSession(payeeContext, amount, answers), pack) : null), [payeeContext, amount, answers, pack])
  useEffect(() => onChange({ step, payeeName: payee?.name ?? null, assessment: current }), [step, payee, current, onChange])

  useEffect(() => {
    if (!toast) return
    const id = setTimeout(() => setToast(null), 1800)
    return () => clearTimeout(id)
  }, [toast])
  const soon = useCallback((label: string) => setToast(`${label} is not part of this demo`), [])

  // ---- navigation ----
  const push = (s: Step) => setStack((st) => [...st, s])
  const back = () => setStack((st) => (st.length > 1 ? st.slice(0, -1) : st))
  const replaceTop = (s: Step) => setStack((st) => [...st.slice(0, -1), s])
  const backToAmount = () => {
    setAnswers(NO_ANSWERS)
    setStack((st) => st.slice(0, st.lastIndexOf('amount') + 1))
  }
  const goHome = () => {
    setStack(['home'])
    setPayee(null)
    setAmount(0)
    setNote('')
    setAnswers(NO_ANSWERS)
    setAskedFamily(false)
    setWarned(false)
    setReceipt(null)
  }

  // ---- who can be paid ----
  const target = scenario.payee
  const targetIsNew = isNewStatus(statusOf(target))
  const inBook = ADDRESS_BOOK.some((p) => p.id === target.id)
  const people = [
    ...ADDRESS_BOOK.filter((p) => !(p.id === target.id && targetIsNew)),
    ...(!targetIsNew && !inBook ? [target] : []),
  ]
  const qrCodes = [...people, ...(targetIsNew ? [target] : [])].filter((p) => p.kind === 'merchant')

  const openPayee = (p: Payee) => {
    setPayee(p)
    setAmount(0)
    setNote('')
    setAnswers(NO_ANSWERS)
    setAskedFamily(false)
    setWarned(false)
    push('chat')
  }
  const onTransfer = (t: Transfer) => {
    if (t === 'mobile' || t === 'upi') {
      setSearchMode(t)
      push('search')
    } else if (t === 'balance') push('balance-pin')
    else soon('Transfer to your own account')
  }

  // ---- paying ----
  const pay = () => {
    if (!before || amount <= 0) return
    const next = stepAfterPay(before.tier)
    if (next !== 'pin') setWarned(true)
    push(next)
  }
  const afterQuestions = () => {
    if (!current) return
    const next = stepAfterPause(current.tier)
    if (next === 'critical') replaceTop('critical')
    else push('pin')
  }
  const askFamily = () => {
    if (askedFamily || !payee || !current) return
    setAskedFamily(true)
    onAskFamily({ contactName: scenario.contact.name, payeeName: payee.name, amount, tier: current.tier })
  }
  const finishPayment = () => {
    if (!payee) return
    const n = txns.length - SEEDED.length + 1
    const txnId = `T26102112${String(n).padStart(2, '0')}${String(amount).padStart(6, '0')}`
    setTxns((t) => [{ id: txnId, name: payee.name, initials: payee.initials, color: payee.color, amount, when: 'Today, 12:52 pm' }, ...t])
    setSpent((s) => s + amount)
    setReceipt({ payeeName: payee.name, bankingName: payee.bankingName, amount, note, txnId, wasWarned: warned })
    replaceTop('success')
  }

  const status = payee ? statusOf(payee) : 'known'

  return (
    <PhoneFrame tone={TONE[step]} callNow={!!context.call_now} screenShare={!!context.screen_share}>
      {step === 'home' && (
        <HomeScreen people={people} onTransfer={onTransfer} onPerson={openPayee} onScan={() => push('scan')} onHistory={() => push('history')} onSoon={soon} />
      )}
      {step === 'search' && (
        <SearchScreen
          mode={searchMode} recent={people} unsaved={targetIsNew ? target : null}
          clipboard={targetIsNew && context.payee_pasted ? target.vpa : null}
          statusOf={statusOf} onPick={openPayee} onBack={back}
        />
      )}
      {step === 'scan' && <ScanScreen codes={qrCodes} onPick={openPayee} onBack={back} onSoon={soon} />}
      {step === 'chat' && payee && <ChatScreen payee={payee} status={status} onBack={back} onPay={() => push('amount')} onSoon={soon} />}
      {step === 'amount' && payee && before && (
        <AmountScreen
          payee={payee} status={status} amount={amount} note={note} assessment={before}
          onAmount={setAmount} onNote={setNote} onBack={back} onPay={pay} onSoon={soon}
        />
      )}
      {step === 'pause' && payee && before && (
        <PauseScreen
          payee={payee} amount={amount} assessment={before} answers={answers} contact={scenario.contact} askedFamily={askedFamily}
          onAnswer={setAnswers} onAskFamily={askFamily} onBack={backToAmount} onNext={afterQuestions}
        />
      )}
      {step === 'critical' && payee && current && (
        <CriticalScreen
          payee={payee} status={status} amount={amount} assessment={current} contact={scenario.contact} askedFamily={askedFamily}
          onWait={() => replaceTop('waiting')} onAskFamily={askFamily} onContinue={() => push('pin')}
        />
      )}
      {step === 'pin' && payee && (
        <PinScreen purpose="pay" payeeName={payee.name} amount={amount} onBack={backToAmount} onSubmit={() => replaceTop('processing')} />
      )}
      {step === 'processing' && <ProcessingScreen onDone={finishPayment} />}
      {step === 'success' && receipt && <SuccessScreen receipt={receipt} onDone={goHome} onSoon={soon} />}
      {step === 'waiting' && <WaitingScreen onHome={goHome} />}
      {step === 'history' && <HistoryScreen txns={txns} onBack={back} />}
      {step === 'balance-pin' && <PinScreen purpose="balance" onBack={back} onSubmit={() => replaceTop('balance')} />}
      {step === 'balance' && <BalanceScreen balance={OPENING_BALANCE - spent} onHome={goHome} />}
      <Toast text={toast} />
    </PhoneFrame>
  )
}
