// "To Mobile Number" / "To Bank / UPI ID": find a payee by name, number or UPI ID.
// A payee never paid before is found only by searching, as in a real UPI app.
import { ClipboardPaste, Search } from 'lucide-react'
import { useState } from 'react'
import type { PayeeStatus } from '../engine/engine'
import type { Payee } from '../scenarios/presets'
import { Avatar, Header } from './parts'

interface Props {
  mode: 'mobile' | 'upi'
  recent: Payee[]
  unsaved: Payee | null    // the scenario's payee when it is new: shown only when searched for
  clipboard: string | null // a UPI ID "copied from another app" (scenarios where the ID was pasted)
  statusOf: (p: Payee) => PayeeStatus
  onPick: (p: Payee) => void
  onBack: () => void
}

const norm = (s: string) => s.toLowerCase().replace(/[^a-z0-9@.]/g, '')

function matches(p: Payee, q: string): boolean {
  const n = norm(q)
  return n.length > 0 && [p.name, p.vpa, p.phone ?? ''].some((f) => norm(f).includes(n))
}

export function SearchScreen({ mode, recent, unsaved, clipboard, statusOf, onPick, onBack }: Props) {
  const [query, setQuery] = useState('')
  const results = query
    ? [...recent.filter((p) => matches(p, query)), ...(unsaved && norm(query).length >= 3 && matches(unsaved, query) ? [unsaved] : [])]
    : recent

  return (
    <>
      <Header title={mode === 'mobile' ? 'To Mobile Number' : 'To Bank / UPI ID'} onBack={onBack}>
        <label className="mt-2 flex items-center gap-2 rounded-xl bg-white px-3 py-2.5 text-ink">
          <Search size={18} className="text-muted" />
          <input
            autoFocus
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={mode === 'mobile' ? 'Search name or mobile number' : 'Enter UPI ID'}
            className="min-w-0 flex-1 bg-transparent text-[15px] outline-none placeholder:text-faint"
            aria-label="Search payee"
          />
        </label>
      </Header>
      <div className="min-h-0 flex-1 overflow-y-auto bg-white">
        {clipboard && !query && (
          <button type="button" onClick={() => setQuery(clipboard)} className="mx-4 mt-3 flex items-center gap-2 rounded-full border border-app/30 bg-app-tint px-3 py-1.5 text-sm font-medium text-app">
            <ClipboardPaste size={16} /> Paste UPI ID: {clipboard}
          </button>
        )}
        <h2 className="px-4 pb-1 pt-4 text-xs font-bold uppercase tracking-wider text-muted">{query ? 'Results' : 'Recent'}</h2>
        {results.length === 0 && (
          <p className="px-4 py-6 text-center text-sm text-muted">No match. Check the name, number or UPI ID.</p>
        )}
        <ul>
          {results.map((p) => {
            const status = statusOf(p)
            const isNew = status === 'new' || status === 'recent'
            return (
              <li key={p.id}>
                <button type="button" onClick={() => onPick(p)} className="flex w-full items-center gap-3 px-4 py-2.5 text-left active:bg-app-tint">
                  <Avatar initials={p.initials} color={p.color} />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-[15px] font-medium text-ink">{p.name}</span>
                    <span className="block truncate text-xs text-muted">{mode === 'mobile' && p.phone ? p.phone : p.vpa}</span>
                  </span>
                  {isNew && <span className="rounded-md bg-app-tint px-1.5 py-0.5 text-[11px] font-semibold text-app">New</span>}
                </button>
              </li>
            )
          })}
        </ul>
      </div>
    </>
  )
}
