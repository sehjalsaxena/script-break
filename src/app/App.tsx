// Minimal check page: runs all 28 behaviour checks live in the browser, the same way as
// reference/run_cases.py (pack v0.3 must give `expected`; pack v0.2 must give
// `expected_v02` if present, otherwise `expected`).
import { assess, type Session, type Tier } from '../engine/engine'
import { PACK_V02, PACK_V03 } from '../engine/rulepacks'
import cases from '../../tests/cases.json'

interface Case {
  id: string
  kind: string
  title: string
  session: Session
  expected: Tier
  expected_v02?: Tier
  note?: string
}

const rows = (cases as Case[]).map((c) => {
  const a2 = assess(c.session, PACK_V02)
  const a3 = assess(c.session, PACK_V03)
  const expectedV02 = c.expected_v02 ?? c.expected
  const pass = a3.tier === c.expected && a2.tier === expectedV02
  return { c, a2, a3, expectedV02, pass }
})

const passed = rows.filter((r) => r.pass).length

export default function App() {
  const allPass = passed === rows.length
  return (
    <main className="p-6 font-sans text-sm">
      <h1 className="text-2xl font-bold">ScriptBreak behaviour checks</h1>
      <p className="mt-1 font-semibold text-gray-700">All data synthetic</p>
      <p className={`mt-4 text-xl font-bold ${allPass ? 'text-green-700' : 'text-red-700'}`}>
        {passed} / {rows.length} checks pass
      </p>
      <p className="mt-1 text-gray-600">
        Computed live in this browser with rule packs v{PACK_V02.version} and v{PACK_V03.version}.
      </p>
      <div className="mt-4 overflow-x-auto">
        <table className="border-collapse">
          <thead>
            <tr className="bg-gray-100 text-left">
              <th className="border px-2 py-1">ID</th>
              <th className="border px-2 py-1">Title</th>
              <th className="border px-2 py-1">Expected</th>
              <th className="border px-2 py-1">v0.2 result</th>
              <th className="border px-2 py-1">v0.3 result</th>
              <th className="border px-2 py-1">Pass</th>
              <th className="border px-2 py-1">Reasons (v0.3)</th>
            </tr>
          </thead>
          <tbody>
            {rows.map(({ c, a2, a3, expectedV02, pass }) => (
              <tr key={c.id} className="align-top">
                <td className="border px-2 py-1 font-mono">{c.id}</td>
                <td className="border px-2 py-1">
                  {c.title}
                  {c.note && <div className="text-xs text-gray-500">{c.note}</div>}
                </td>
                <td className="border px-2 py-1 font-mono">
                  {c.expected}
                  {c.expected_v02 && <div className="text-xs text-gray-500">v0.2: {expectedV02}</div>}
                </td>
                <td className="border px-2 py-1 font-mono">{a2.tier}</td>
                <td className="border px-2 py-1 font-mono">
                  {a3.tier}
                  <div className="text-xs text-gray-500">{a3.rules.join(', ')}</div>
                </td>
                <td className={`border px-2 py-1 font-bold ${pass ? 'text-green-700' : 'text-red-700'}`}>
                  {pass ? 'PASS' : 'FAIL'}
                </td>
                <td className="border px-2 py-1">
                  <ul className="list-disc pl-4">
                    {a3.reasons.map((r, i) => <li key={i}>{r}</li>)}
                  </ul>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </main>
  )
}
