// Scan & Pay. Prototype: no camera; the presenter taps one of the demo QR codes instead.
import { Image, QrCode, X } from 'lucide-react'
import type { Payee } from '../scenarios/presets'
import { IconButton } from './parts'

export function ScanScreen({ codes, onPick, onBack, onSoon }: { codes: Payee[]; onPick: (p: Payee) => void; onBack: () => void; onSoon: (label: string) => void }) {
  return (
    <div className="flex min-h-0 flex-1 flex-col bg-black text-white">
      <div className="flex flex-none items-center gap-2 px-3 py-1">
        <IconButton label="Close scanner" onClick={onBack}><X size={24} /></IconButton>
        <span className="text-[17px] font-semibold">Scan any QR code</span>
      </div>
      <div className="flex flex-1 flex-col items-center justify-center gap-4">
        <div className="relative h-56 w-56">
          {['left-0 top-0 border-l-4 border-t-4 rounded-tl-2xl', 'right-0 top-0 border-r-4 border-t-4 rounded-tr-2xl',
            'bottom-0 left-0 border-b-4 border-l-4 rounded-bl-2xl', 'bottom-0 right-0 border-b-4 border-r-4 rounded-br-2xl'].map((c) => (
            <span key={c} className={`absolute h-10 w-10 border-white ${c}`} />
          ))}
          <span className="sb-scanline absolute inset-x-4 h-0.5 rounded bg-[#8C6BFF] shadow-[0_0_12px_#8C6BFF]" />
        </div>
        <p className="text-sm text-white/70">Align the QR code inside the frame</p>
        <button type="button" onClick={() => onSoon('Upload from gallery')} className="flex items-center gap-2 rounded-full bg-white/10 px-4 py-2 text-sm">
          <Image size={16} /> Upload from gallery
        </button>
      </div>
      <div className="flex-none rounded-t-2xl bg-white px-4 pb-5 pt-3 text-ink">
        <h2 className="text-xs font-bold uppercase tracking-wider text-muted">Demo QR codes (prototype: no camera)</h2>
        <ul className="mt-2 space-y-1">
          {codes.map((p) => (
            <li key={p.id}>
              <button type="button" onClick={() => onPick(p)} className="flex w-full items-center gap-3 rounded-xl px-2 py-2 text-left active:bg-app-tint">
                <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-app-tint text-app"><QrCode size={22} /></span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-[15px] font-medium">{p.name}</span>
                  <span className="block truncate text-xs text-muted">{p.vpa}</span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
