// Demo scenario presets. Each one takes its signals and amount from a case in tests/cases.json,
// so the demo can never drift from the 28 behaviour checks. Names, numbers and UPI IDs are fictional.
// The answers to the two questions are left out: the presenter gives them live on the Pause screen.
import type { PayeeKind, Session, Tier } from '../engine/engine'
import type { Answers, Context } from '../screens/flow'
import cases from '../../tests/cases.json'

export interface Payee {
  id: string
  name: string
  vpa: string
  phone?: string        // masked, as UPI apps show it
  bankingName: string   // the verified name UPI apps show before paying
  initials: string
  color: string
  kind: PayeeKind
  knownHistory?: string // shown for payees paid before, e.g. "Paid 23 times before"
  past?: { amount: number; when: string }[] // earlier payments, shown in the chat thread
}

export interface Contact {
  name: string     // "Rohit"
  relation: string // "son"
}

export interface Scenario {
  id: string
  caseId: string
  demoStep?: number // step in the finale demo script (deck page 10)
  title: string
  blurb: string     // what the presenter does and what it proves
  payee: Payee      // the payee this scenario is about
  amount: number
  note: string
  context: Context  // the case's signals, for a payment to `payee`
  contact: Contact
  expect: {
    first: Tier          // level on "Pay", rule pack v0.3
    firstV02?: Tier      // level on "Pay", rule pack v0.2, if different
    answers?: Answers    // answers the script gives on the Pause screen
    afterAnswers?: Tier  // level after those answers (v0.3)
  }
}

interface CaseRow {
  id: string
  session: Session
}

function fromCase(id: string): { amount: number; context: Context } {
  const c = (cases as CaseRow[]).find((x) => x.id === id)
  if (!c) throw new Error(`Unknown case ${id}`)
  const { amount, q1: _q1, q2: _q2, ...context } = c.session
  return { amount, context }
}

function scenario(s: Omit<Scenario, 'amount' | 'context'>): Scenario {
  return { ...s, ...fromCase(s.caseId) }
}

// ---- the phone owner's address book: people and shops paid before ----

const SURESH: Payee = {
  id: 'suresh', name: 'Suresh Verma', vpa: 'suresh.verma@demo', phone: '98•••••311', bankingName: 'SURESH KUMAR VERMA',
  initials: 'SV', color: '#4F7FB0', kind: 'person', knownHistory: 'Paid 14 times before',
  past: [{ amount: 18000, when: '1 Sep' }, { amount: 18000, when: '1 Aug' }],
}
const KIRANA: Payee = {
  id: 'kirana', name: 'Sharma Kirana Store', vpa: 'sharmakirana@demo', bankingName: 'SHARMA GENERAL STORE',
  initials: 'SK', color: '#4F9B7A', kind: 'merchant', knownHistory: 'Paid 23 times before',
  past: [{ amount: 420, when: '18 Oct' }, { amount: 265, when: '15 Oct' }],
}

export const ADDRESS_BOOK: Payee[] = [
  {
    id: 'rohit', name: 'Rohit (son)', vpa: 'rohit.sharma@demo', phone: '99•••••482', bankingName: 'ROHIT SHARMA',
    initials: 'RS', color: '#3D8FD1', kind: 'person', knownHistory: 'Paid 31 times before',
    past: [{ amount: 3000, when: '14 Oct' }, { amount: 1500, when: '2 Oct' }],
  },
  {
    id: 'priya', name: 'Priya (daughter)', vpa: 'priya.s@demo', phone: '97•••••260', bankingName: 'PRIYA SHARMA',
    initials: 'PS', color: '#D0679A', kind: 'person', knownHistory: 'Paid 19 times before',
    past: [{ amount: 5000, when: '10 Oct' }],
  },
  SURESH,
  KIRANA,
  {
    id: 'milk', name: 'Gopal Dairy', vpa: 'gopaldairy@demo', bankingName: 'GOPAL DAIRY FARM',
    initials: 'GD', color: '#C2913A', kind: 'merchant', knownHistory: 'Paid 40 times before',
    past: [{ amount: 1860, when: '1 Oct' }],
  },
]

// ---- scenario payees who are not (or not yet) in the address book ----

const RAMESH: Payee = {
  id: 'ramesh', name: 'Ramesh K.', vpa: 'ramesh.k.4471@demo', phone: '70•••••918', bankingName: 'RAMESH KUMAR',
  initials: 'RK', color: '#6B74D6', kind: 'person',
  past: [{ amount: 48000, when: 'Yesterday' }], // shown only once he counts as paid before (scenario S03)
}

const ROHIT: Contact = { name: 'Rohit', relation: 'son' }

export const SCENARIOS: Scenario[] = [
  scenario({
    id: 'digital-arrest', caseId: 'S01', demoStep: 1,
    title: 'Digital-arrest call',
    blurb: 'A fake police officer is on the call. Pay: the Pause screen appears. Answer "Yes" to the first question: it becomes Critical. Tap "Ask Rohit": the family phone buzzes.',
    payee: RAMESH, note: 'Verification deposit', contact: ROHIT,
    expect: { first: 'PAUSE', answers: { q1: true, q2: null }, afterAnswers: 'CRITICAL' },
  }),
  scenario({
    id: 'usual-rent', caseId: 'L02', demoStep: 2,
    title: 'Rent to the usual landlord',
    blurb: '₹18,000 to the landlord paid every month. Nothing extra is shown: no alert fatigue.',
    payee: SURESH, note: 'October rent', contact: ROHIT,
    expect: { first: 'NONE' },
  }),
  scenario({
    id: 'coached-no', caseId: 'S02', demoStep: 3,
    title: 'Coached to answer "No, No"',
    blurb: 'Same call, but the scammer says "answer No to both". Answer No, No: it stays at Pause. Answers can never lower the level.',
    payee: RAMESH, note: 'Verification deposit', contact: ROHIT,
    expect: { first: 'PAUSE', answers: { q1: false, q2: false }, afterAnswers: 'PAUSE' },
  }),
  scenario({
    id: 'sent-by-mistake', caseId: 'S15', demoStep: 4,
    title: '"Sent by mistake" return',
    blurb: '₹5,000 arrived from a stranger, who now asks for it back to a different account. Switch the rule pack: v0.2 only nudges, v0.3 (learned rule R5) pauses.',
    payee: { id: 'vikram', name: 'Vikram S.', vpa: 'vikram.s88@demo', phone: '81•••••507', bankingName: 'VIKRAM SINGH', initials: 'VS', color: '#8A5BB0', kind: 'person' },
    note: 'Returning wrong transfer', contact: ROHIT,
    expect: { first: 'PAUSE', firstV02: 'NUDGE' },
  }),
  scenario({
    id: 'kirana', caseId: 'L01',
    title: 'Kirana store (Scan & Pay)',
    blurb: '₹350 at the usual shop, by scanning its QR. Nothing extra.',
    payee: KIRANA, note: 'Groceries', contact: ROHIT,
    expect: { first: 'NONE' },
  }),
  scenario({
    id: 'new-landlord', caseId: 'L04',
    title: 'Deposit to a new landlord',
    blurb: '₹40,000 to a new payee whose UPI ID was pasted, no call: one inline Nudge line, no extra tap.',
    payee: { id: 'anil', name: 'Anil Mehta', vpa: 'anil.mehta@demo', phone: '90•••••733', bankingName: 'ANIL MEHTA', initials: 'AM', color: '#B07A3A', kind: 'person' },
    note: 'Flat deposit', contact: ROHIT,
    expect: { first: 'NUDGE' },
  }),
  scenario({
    id: 'screen-share-kyc', caseId: 'S05',
    title: 'Fake KYC update on screen-share',
    blurb: '₹9,999 (kept under ₹10,000) while the screen is shared. Straight to Critical.',
    payee: { id: 'kyc', name: 'KYC Help Desk', vpa: 'kychelp.desk@demo', bankingName: 'SHREE TRADERS', initials: 'KH', color: '#B05B5B', kind: 'person' },
    note: 'KYC update fee', contact: ROHIT,
    expect: { first: 'CRITICAL' },
  }),
  scenario({
    id: 'warned-again', caseId: 'S03',
    title: 'Day 2: same account after a warning',
    blurb: '₹1,50,000 to the account he was warned about yesterday, still on the call. Straight to Critical.',
    payee: RAMESH, note: 'Bail amount', contact: ROHIT,
    expect: { first: 'CRITICAL' },
  }),
  scenario({
    id: 'voice-clone', caseId: 'S16',
    title: 'AI voice clone of a son',
    blurb: 'A cloned voice of her son Arjun wants ₹30,000 now and says "don\'t tell Dad". Answer Yes to the secrecy question. "Ask Arjun" reaches the real son.',
    payee: { id: 'karan', name: 'Karan M.', vpa: 'karan.m.5521@demo', phone: '88•••••140', bankingName: 'KARAN MALHOTRA', initials: 'KM', color: '#5B8AB0', kind: 'person' },
    note: 'Hospital fees', contact: { name: 'Arjun', relation: 'son' },
    expect: { first: 'PAUSE', answers: { q1: false, q2: true }, afterAnswers: 'CRITICAL' },
  }),
  scenario({
    id: 'electrician', caseId: 'L08',
    title: 'New electrician on a call',
    blurb: 'A genuine ₹6,000 payment while the electrician talks him through it. Paused: an accepted false alarm. Answer No, No and continue.',
    payee: { id: 'manoj', name: 'Manoj Electricals', vpa: 'manoj.elec@demo', phone: '93•••••655', bankingName: 'MANOJ KUMAR', initials: 'ME', color: '#7A8A3A', kind: 'person' },
    note: 'Wiring repair', contact: ROHIT,
    expect: { first: 'PAUSE', answers: { q1: false, q2: false }, afterAnswers: 'PAUSE' },
  }),
]
