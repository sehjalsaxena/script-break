// App shell: a demo page (mock UPI flow + scenario panel) and the live checks page.
// Hash routing (#/ and #/checks) so the build works on any static host.
import { useEffect, useState } from 'react'
import { CHECKS_PASSED, CHECKS_TOTAL, ChecksPage } from './ChecksPage'
import { DemoPage } from './DemoPage'

function useHashRoute(): string {
  const [hash, setHash] = useState(window.location.hash)
  useEffect(() => {
    const onChange = () => setHash(window.location.hash)
    window.addEventListener('hashchange', onChange)
    return () => window.removeEventListener('hashchange', onChange)
  }, [])
  return hash.replace(/^#\/?/, '')
}

export default function App() {
  const route = useHashRoute()
  const link = (href: string, active: boolean, label: string) => (
    <a href={href} className={active ? 'font-bold text-brand' : 'text-muted hover:text-ink'}>{label}</a>
  )
  return (
    <div className="min-h-screen">
      <header className="flex items-center gap-4 border-b border-line bg-white px-4 py-2.5 text-sm">
        <span className="font-bold text-ink">ScriptBreak</span>
        <nav className="flex gap-4">
          {link('#/', route !== 'checks', 'Demo')}
          {link('#/checks', route === 'checks', `Checks ${CHECKS_PASSED}/${CHECKS_TOTAL}`)}
        </nav>
        <span className="ml-auto text-xs font-semibold text-muted">All data synthetic</span>
      </header>
      {route === 'checks' ? <ChecksPage /> : <DemoPage />}
    </div>
  )
}
