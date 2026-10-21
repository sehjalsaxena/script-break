# ScriptBreak: code behind the Round 1 PDF

Everything here runs on synthetic data. Python 3.10+, with numpy, scipy and scikit-learn.

| File | What it does | PDF |
|---|---|---|
| `engine.py` | The decision engine: evidence groups C/P/A, decision table R1 to R4, learned rules, the two questions | Slide 6 |
| `cases.json`, `run_cases.py` | 28 behaviour checks (16 scams, 2 chat-only, 10 genuine), run on rule packs v0.2 and v0.3 | Slide 8, Appendix A |
| `generator.py` | Synthetic session generator; every assumption is written at the top | Appendix B |
| `stress_test.py` | 2,00,000 genuine + 3,000 per scam workflow; sensitivity and the Rs 5,000 fallback | Slide 8, Appendix B |
| `fair_compare.py` | ScriptBreak vs amount rules at the same false-alarm rate | Slide 8, Appendix B |
| `ablation.py` | What each signal buys and costs | Appendix B |
| `discover.py` | The AI loop: cluster missed scams, propose rules, backtest, approve, held-out test; writes `rulepack_v0.3.json` | Slide 7, Appendix C |
| `results/` | The JSON outputs the PDF is built from | |

Run order (about one minute in total):

```
python3 discover.py      # writes rulepack_v0.3.json and results/discovery.json
python3 run_cases.py     # exit code 0 only if all 28 cases behave as designed
python3 stress_test.py
python3 fair_compare.py
python3 ablation.py
```

For the finale: port `engine.py` to TypeScript and keep `cases.json` as the test suite.
Never edit a case's expected level just to make a test pass.

Key numbers to know by heart (all under the stated synthetic assumptions):
- 28 of 28 checks as designed; 14 of 16 scams flagged; 2 of 10 genuine interrupted (accepted).
- 6.6 interruptions per 1,000 genuine payments (budget 8); amount rule at a similar rate flags 39% vs our 68%.
- Budget breaks if genuine on-call payments to new payees are 1.35x our assumption; the Rs 5,000 fallback holds it to 2.6x.
- Q2 is now the secrecy question: AI voice-clone sessions reaching Critical rose from 6% to 39%.
- Discovery: 449 misses -> 7 clusters -> R5 and R6 approved; 'sent by mistake' 17% -> 100% on held-out sessions.
- In this run, "approval" is the team applying the written policy in `discover.py`; the `approved_by` names in the rule pack are role placeholders.
