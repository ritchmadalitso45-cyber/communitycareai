# Submission status on 23 November 2026

## Verified now

Read-only hosted checks at 08:52 UTC / 10:52 South Africa time confirmed API/database health UP, rejection of unauthenticated profile requests, correct OIDC issuer discovery and owned DHIS2 version 2.43.2.1. No encounters were created by this check.

The DHIS2 organisation is `CommunityCare Pilot` and the program is `CommunityCare assessments`. The queue contains six FORWARDED receipts. These aggregate rows are not evidence of clinical use. Previous documented live tests used synthetic timestamp/category reports.

The reporting app uses actual saved reports and authenticated API calls. Its device counts are calculated, not fabricated. It uses asynchronous offline reporting, not live streaming or an all-device national dashboard. DHIS2 is the owned Contabo instance, not the public demo or the national Malawi instance.

## Engineering evidence

- 36 backend tests passed with a separate PostgreSQL test database on 23 November.
- Earlier Android verification passed ten JVM tests, build and lint.
- Live native OAuth/PKCE and synthetic API-to-DHIS2 delivery/readback checks passed on 22 November.
- Original-facility binding prevents reassigned credentials from silently moving an existing offline report.
- Real-phone callback, offline persistence and background-scheduling acceptance are still pending.

## Material gaps before the requested submission

There is no trained AI model or inference endpoint. The timestamp/category report cannot establish clinical labels or support a validated diagnostic model. The founder must choose the first intended AI task and provide or arrange permitted representative data where required. A reporting-quality assistant and clinical triage AI are different products with different evidence needs.

DHIS2 contains zero configured dashboards and no model/protocol packages are published. Offsite backup configuration remains incomplete. These facts must not be concealed in product documentation or a competition application.

The updated pitch and business plan accurately describe AI as proposed development. They do not establish ITU eligibility. Active operating history, external funding, company website/social presence and the final funding request remain unconfirmed. No application submission or successful attachment is claimed.

## Documentation

`FRONTEND_GUIDE.md` explains Android modules, identity, encrypted storage, delivery and device acceptance. `BACKEND_GUIDE.md` explains the API, queue, DHIS2 delivery, secrets and operations. `API_CONTRACT.md` defines the exact integration and failure handling. No credentials belong in a source or submission bundle.

## Acceptance before describing working AI

Select the intended task; implement its actual model and input/output path; record model/dataset provenance; test on held-out representative data; validate integration and failure behaviour; show the model version and evaluation limits. Use clearly labelled synthetic data only for software checks. Clinical performance claims require separate qualified oversight and validation.
