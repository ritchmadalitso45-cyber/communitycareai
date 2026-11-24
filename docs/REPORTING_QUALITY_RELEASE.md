# Reporting-quality preview release

23 November 2026. Python backend: 42 automated tests passed, including PostgreSQL integration. Android: 14 unit tests, debug assembly and lint passed on the server build.

Authenticated GET /api/v1/reporting/quality reads the assigned facility's owned DHIS2 records. Send Authorization: Bearer <token> and X-Organisation-Unit: <original-facility>. Unauthenticated requests return 401 and facility mismatches return 403. The live deployment check confirmed both restrictions and analysis of six stored engineering reports.

The Android preview requires Android 8 or later and an assigned CommunityCare account. Install the APK from the project's GitHub preview release, then sign in through the hosted login page. Preview signing is used; store distribution and iOS support are not included. Physical-phone browser handoff, Keystore persistence and background scheduling acceptance remain pending. Compatibility with every phone has not been demonstrated.

See MODEL_CARD.md and DATA_SOURCES.md for experimental status and data limitations. Earlier assessment documents describe the state before this release; this dated release note supersedes statements that reporting-quality AI is absent. Clinical triage AI remains unimplemented.
