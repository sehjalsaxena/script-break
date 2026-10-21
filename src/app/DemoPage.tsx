// Demo page: the mock UPI app (a full phone screen on mobile) and the scenario panel beside it.
import { useCallback, useState } from 'react'
import { loadPack, type Assessment } from '../engine/engine'
import { SCENARIOS, type Scenario } from '../scenarios/presets'
import type { Context, FamilyAlertRequest, Step } from '../screens/flow'
import { MockUpiApp } from '../screens/MockUpiApp'
import { ScenarioPanel, type Live } from './ScenarioPanel'

export function DemoPage() {
  const [scenario, setScenario] = useState<Scenario>(SCENARIOS[0])
  const [context, setContext] = useState<Context>(SCENARIOS[0].context)
  const [packVersion, setPackVersion] = useState<'0.2' | '0.3'>('0.3')
  const [run, setRun] = useState(0) // bumping it restarts the app
  const [startAtPayment, setStartAtPayment] = useState(false)
  const [live, setLive] = useState<Live | null>(null)
  const [alerts, setAlerts] = useState<FamilyAlertRequest[]>([])

  // Placeholder until the second-phone relay exists (src/family/): log the alert in the panel.
  const onAskFamily = useCallback((alert: FamilyAlertRequest) => setAlerts((xs) => [alert, ...xs]), [])
  const onChange = useCallback((s: { step: Step; payeeName: string | null; assessment: Assessment | null }) => setLive(s), [])

  const restart = (atPayment: boolean) => {
    setStartAtPayment(atPayment)
    setRun((r) => r + 1)
  }
  const choose = (s: Scenario) => {
    setScenario(s)
    setContext(s.context)
    setPackVersion('0.3')
    setAlerts([])
    restart(false)
  }

  return (
    <div className="lg:flex lg:items-start lg:justify-center lg:gap-10 lg:px-8 lg:py-6">
      <MockUpiApp
        key={run} scenario={scenario} context={context} pack={loadPack(packVersion)} startAtPayment={startAtPayment}
        onAskFamily={onAskFamily} onChange={onChange}
      />
      <ScenarioPanel
        scenarios={SCENARIOS} selected={scenario} onChoose={choose}
        onRestart={() => restart(false)} onJump={() => restart(true)}
        context={context} onContext={setContext} packVersion={packVersion} onPack={setPackVersion}
        live={live} alerts={alerts}
      />
    </div>
  )
}
