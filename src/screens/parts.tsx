// Building blocks shared by the screens: phone frame, status bar, header, avatar, buttons, keypad, toast.
import { Check, ChevronLeft, Delete } from 'lucide-react'
import type { ReactNode } from 'react'

/** Colour of the status bar, matched to the top of each screen. */
export type Tone = 'app' | 'light' | 'crit' | 'success' | 'dark'

const TONE: Record<Tone, string> = {
  app: 'bg-app text-white',
  light: 'bg-white text-ink',
  crit: 'bg-crit text-white',
  success: 'bg-success text-white',
  dark: 'bg-black text-white',
}

export function PhoneFrame({ tone, callNow, screenShare, children }: { tone: Tone; callNow: boolean; screenShare: boolean; children: ReactNode }) {
  return (
    <div className="mx-auto w-full max-w-[430px] lg:h-[844px] lg:w-[390px] lg:flex-none lg:rounded-[52px] lg:bg-bezel lg:p-3 lg:shadow-2xl">
      <div className="sb-phone relative flex h-[calc(100dvh-45px)] min-h-[620px] flex-col overflow-hidden bg-white lg:h-full lg:min-h-0 lg:rounded-[42px]">
        <div className={`flex h-10 flex-none items-center justify-between px-6 pt-1 text-[13px] font-semibold ${TONE[tone]}`}>
          <span>12:52</span>
          <span className="flex gap-1">
            {callNow && <span className="rounded-full bg-call px-2 py-0.5 text-[11px] font-bold text-white ring-1 ring-white/40">● On call</span>}
            {screenShare && <span className="rounded-full bg-crit px-2 py-0.5 text-[11px] font-bold text-white ring-1 ring-white/40">● Screen shared</span>}
          </span>
          <span className="flex items-end gap-[3px]" aria-hidden>
            {[5, 7, 9, 11].map((h) => <i key={h} className="block w-[3px] rounded-sm bg-current" style={{ height: h }} />)}
            <i className="ml-1 block h-[11px] w-[22px] rounded-[3px] border border-current p-[1.5px]"><i className="block h-full w-3/4 rounded-[1px] bg-current" /></i>
          </span>
        </div>
        <div className="flex min-h-0 flex-1 flex-col">{children}</div>
      </div>
    </div>
  )
}

/** Purple app bar. */
export function Header({ title, subtitle, onBack, right, children }: { title: ReactNode; subtitle?: ReactNode; onBack?: () => void; right?: ReactNode; children?: ReactNode }) {
  return (
    <div className="flex-none bg-app px-3 pb-3 pt-1 text-white">
      <div className="flex items-center gap-2">
        {onBack && (
          <button type="button" onClick={onBack} aria-label="Back" className="rounded-full p-1.5 hover:bg-white/10">
            <ChevronLeft size={24} />
          </button>
        )}
        <div className="min-w-0 flex-1">
          <div className="truncate text-[17px] font-semibold">{title}</div>
          {subtitle && <div className="truncate text-xs text-white/75">{subtitle}</div>}
        </div>
        {right}
      </div>
      {children}
    </div>
  )
}

export function IconButton({ label, onClick, children }: { label: string; onClick: () => void; children: ReactNode }) {
  return (
    <button type="button" onClick={onClick} aria-label={label} className="rounded-full p-2 hover:bg-white/10">
      {children}
    </button>
  )
}

const AVATAR_SIZE = { sm: 'h-9 w-9 text-xs', md: 'h-12 w-12 text-sm', lg: 'h-16 w-16 text-xl' }

export function Avatar({ initials, color, size = 'md' }: { initials: string; color: string; size?: keyof typeof AVATAR_SIZE }) {
  return (
    <div className={`flex flex-none items-center justify-center rounded-full font-bold text-white ${AVATAR_SIZE[size]}`} style={{ background: color }}>
      {initials}
    </div>
  )
}

type Variant = 'app' | 'primary' | 'outline' | 'ghost'

const VARIANTS: Record<Variant, string> = {
  app: 'bg-app text-white active:bg-app-dark disabled:bg-[#C9BEDD]',
  primary: 'bg-ink text-white',
  outline: 'border-2 border-ink text-ink disabled:border-safe disabled:text-safe',
  ghost: 'text-faint disabled:opacity-70',
}

export function Button({ variant, onClick, disabled, children }: { variant: Variant; onClick: () => void; disabled?: boolean; children: ReactNode }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`w-full rounded-xl py-3 text-center text-[15px] font-bold transition-colors ${VARIANTS[variant]}`}
    >
      {children}
    </button>
  )
}

/**
 * Number pad. "amount": digits and delete. "pin": digits, delete and a tick that submits
 * (the tick is enabled only when `canSubmit`).
 */
export function Keypad({ variant, onDigit, onDelete, onSubmit, canSubmit }: {
  variant: 'amount' | 'pin'
  onDigit: (d: number) => void
  onDelete: () => void
  onSubmit?: () => void
  canSubmit?: boolean
}) {
  const key = 'flex h-12 items-center justify-center rounded-xl text-[22px] font-medium text-ink active:bg-black/10'
  const digit = (d: number) => (
    <button key={d} type="button" className={key} onClick={() => onDigit(d)}>{d}</button>
  )
  const del = (
    <button key="del" type="button" className={key} onClick={onDelete} aria-label="Delete"><Delete size={24} /></button>
  )
  return (
    <div className="grid flex-none grid-cols-3 gap-1 bg-[#ECEAF1] px-3 pb-4 pt-2">
      {[1, 2, 3, 4, 5, 6, 7, 8, 9].map(digit)}
      {variant === 'pin' ? del : <span />}
      {digit(0)}
      {variant === 'pin' ? (
        <button
          type="button"
          onClick={onSubmit}
          disabled={!canSubmit}
          aria-label="Submit PIN"
          className="m-1 flex items-center justify-center rounded-full bg-app text-white disabled:bg-[#C9BEDD]"
        >
          <Check size={24} />
        </button>
      ) : del}
    </div>
  )
}

export function Toast({ text }: { text: string | null }) {
  if (!text) return null
  return (
    <div role="status" className="sb-rise pointer-events-none absolute inset-x-6 bottom-24 z-20 rounded-xl bg-ink/90 px-4 py-2.5 text-center text-sm text-white shadow-lg">
      {text}
    </div>
  )
}
