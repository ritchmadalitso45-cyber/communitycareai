# Reporting-quality AI model card

Version: reporting-iforest-v1. Updated 23 November 2026.

This nonclinical assistant reviews reporting patterns from the authenticated worker's assigned organisation in the owned DHIS2 instance. It does not diagnose, prescribe, determine triage or assess staff performance.

The backend fits scikit-learn IsolationForest with 100 trees, random seed 42 and one worker to the current snapshot of up to 200 reports. Features are log arrival delay, cyclic reporting hour and weekday, and the recorded reporting category. At least five valid reports are required. Scores are relative anomaly scores, not probabilities. The review threshold of 0.65 is an unvalidated heuristic. The five highest scores may be displayed even when none meets that threshold.

The verified live snapshot contained six engineering test reports on 23 November 2026. The system labels it EXPERIMENTAL_SMALL_SAMPLE and PILOT_INCLUDES_ENGINEERING_TEST_RECORDS. This proves the integration operates on stored DHIS2 records; it does not establish clinical effectiveness or real-patient validation. There is no held-out evaluation or accuracy claim. Above 29 valid records the model remains experimental and unvalidated.

Responses include generation time, training count, model version and a deterministic dataset fingerprint. The backend caches results for ten seconds. The Android dashboard refreshes while open every thirty seconds and supports manual refresh. Missing data and source failures produce explicit states or errors; no fabricated results replace them.

Bearer authentication and signed facility assignments restrict access. A mismatched original facility is rejected before analysis. Only report IDs, organisation/program/stage IDs, dates and the necessary category value are requested. The model is fit in memory; no model pickle is stored. Authorised prospective reporting data, governance review and a held-out evaluation are required before operational decision use.
