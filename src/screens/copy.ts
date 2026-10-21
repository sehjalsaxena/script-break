// Screen wording. English is from the Round 1 deck (docs/ScriptBreak_Round1_deck.pdf, page 5)
// and SPEC.md "Screen wording". Hindi: TODO: native speaker to check.
// Lines marked "proposed" are not in the deck.
import type { PayeeStatus } from '../engine/engine'

export type Lang = 'en' | 'hi'

/** End a sentence with a name without doubling the full stop ("Ramesh K." not "Ramesh K.."). */
const endWith = (name: string) => (name.endsWith('.') ? name : `${name}.`)

export const PAUSE_COPY = {
  title: 'Before you pay',
  sends: (amount: string, payee: string) => `This sends ${amount} out of your account to ${endWith(payee)}`,
  q1: 'Were you told this payment is needed to verify, protect or release your money?',
  q2: 'Has anyone asked you to keep this payment secret from your family or bank?',
  yes: 'Yes',
  no: 'No',
  notSure: 'Not sure?',
  askFamily: (name: string) => `Ask ${name} to call me`,
  asked: (name: string) => `${name} has been asked to call you`, // proposed
  next: 'Next',
}

export const CRITICAL_COPY: Record<Lang, {
  title: string
  calm: string
  never: string
  toPayee: (amount: string, payee: string, isNew: boolean) => string
  last: string
  wait: string
  askFamily: (contact: string) => string
  asked: (name: string) => string
  continueIn: (secs: number) => string
  continueNow: string
  report: string
}> = {
  en: {
    title: 'Stop. This matches a common scam.',
    calm: 'You are not in trouble. You are in control of this payment.',
    never: 'Police, CBI, RBI and banks never ask you to move money to verify, protect or release it.',
    toPayee: (amount, payee, isNew) => `${amount} to ${payee}${isNew ? ', a new payee' : ''}`,
    last: 'If someone is telling you to ignore this screen, that is the scam.',
    wait: 'Wait 30 minutes',
    askFamily: (contact) => `Ask ${contact} to call me`,
    asked: (name) => `${name} has been asked to call you`, // proposed
    continueIn: (secs) => `Continue anyway (${secs} s)`,
    continueNow: 'Continue anyway',
    report: 'Already paid? Report now: call 1930',
  },
  // TODO: native speaker to check every Hindi line.
  hi: {
    title: 'रुकिए। यह एक आम धोखाधड़ी जैसा है।',
    calm: 'आप किसी मुसीबत में नहीं हैं। इस भुगतान का फ़ैसला आपके हाथ में है।',
    never: 'पुलिस, CBI, RBI और बैंक कभी भी पैसे की जाँच, सुरक्षा या उसे छुड़ाने के लिए पैसे भेजने को नहीं कहते।',
    toPayee: (amount, payee, isNew) => `${amount}, ${payee} को${isNew ? ' (नया प्राप्तकर्ता)' : ''}`,
    last: 'अगर कोई आपसे इस स्क्रीन को अनदेखा करने को कह रहा है, तो वही धोखाधड़ी है।',
    wait: '30 मिनट रुकें',
    askFamily: (contact) => `${contact} से कहें कि मुझे कॉल करें`,
    asked: (name) => `${name} से कॉल करने को कहा गया है`,
    continueIn: (secs) => `फिर भी जारी रखें (${secs} सेकंड)`,
    continueNow: 'फिर भी जारी रखें',
    report: 'भुगतान कर चुके हैं? अभी रिपोर्ट करें: 1930 पर कॉल करें',
  },
}

export const NUDGE_COPY = {
  firstPayment: (payee: string) => `First payment to ${endWith(payee)}`,
  check: 'Check you know who this is',
}

export const DONE_COPY = {
  paid: (amount: string, payee: string) => `${amount} paid to ${payee}`,
  demo: 'Demo payment. No real money moved.',
  waitingTitle: 'Payment not sent', // proposed
  waitingBody: 'Nothing has left your account. Come back in 30 minutes if you still want to pay.', // proposed
  home: 'Back to home',
}

/** The line under the payee's name on the Pay screen, from the host app's own records. */
export function historyLine(status: PayeeStatus, knownHistory?: string): string {
  if (status === 'new') return 'New payee'
  if (status === 'recent') return 'First paid less than 7 days ago'
  if (status === 'warned') return 'Paid before, after a warning'
  return knownHistory ?? 'Paid before'
}
