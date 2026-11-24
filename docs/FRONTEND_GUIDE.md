# CommunityCare Android frontend

## Current product

Kotlin and Jetpack Compose application for worker-confirmed assessment reporting. It collects an assessment timestamp and one explicitly selected triage category. It does not generate diagnoses, treatment advice or referrals. Reporting-quality anomaly review is provided by the hosted backend; no clinical model is bundled in the app.

The dashboard reads encrypted records saved on the current device and filters them by authenticated worker and facility. Counts and recent reports are derived from those records. They are not fixed examples, national totals or an all-device dashboard.

## Structure

| Source | Responsibility |
| --- | --- |
| `MainActivity.kt` | Sign-in result, report save, sync and logout actions |
| `CommunityCareApplication.kt` | Shared storage, identity, Retrofit client and sync state |
| `navigation/AppNavGraph.kt` | Dashboard requires an authenticated saved profile |
| `data/AuthManager.kt` | AppAuth discovery, Authorization Code with PKCE, refresh and encrypted session |
| `data/SecureStorage.kt` | Android Keystore AES-GCM and atomic encrypted files |
| `data/EncounterStore.kt` | Persistent worker/facility-bound outbox |
| `data/Contract.kt` | API DTOs, receipt validation and retry decisions |
| `sync/EncounterSyncWorker.kt` | Network-constrained WorkManager delivery |
| `ui/screens/DashboardScreen.kt` | Actual device counts, explicit triage entry and receipt states |

## Sign-in and configuration

Public service addresses and client identifiers are in `app/build.gradle.kts`. The API base is `https://care-api.95.111.247.118.sslip.io/`; the issuer is `https://care-login.95.111.247.118.sslip.io/realms/communitycare`. Public client: `communitycare-android`. Callback: `communitycare://oauth/callback`.

An administrator creates a worker account and assigns `dhis_org_unit` in Keycloak. The app uses the worker account, not the separate DHIS2 administrator credentials. Before committing the session, the app calls `GET /api/v1/me` to verify the signed identity and facility. No server password, DHIS2 service token or client secret belongs in the APK.

## Data and delivery

1. A worker signs in online once and selects a confirmed triage outcome.
2. The app creates a UUID and current timestamp and saves the report encrypted.
3. WorkManager submits when connectivity is available. Immediate work can be requested with Sync now; periodic work runs on Android's schedule, nominally every 15 minutes.
4. HTTP 202 means server acceptance, not DHIS2 delivery. The app polls the receipt until a qualifying result is returned.
5. Only `FORWARDED` is shown as delivered. Pending, accepted, rejected and conflicting records remain distinct.

Retries preserve the UUID, timestamp, category and original facility. `X-Organisation-Unit` binds requests to that facility after token refresh or reassignment. Another account cannot view or submit the original account's local reports. Signing out retains encrypted reports; clearing application storage or uninstalling removes them.

This is asynchronous reporting, not WebSocket streaming or continuous national analytics. No fixed end-to-end latency is guaranteed. The dashboard does not download reports saved by another phone.

## Build and checks

Requires JDK 17 or 21 and Android SDK platform 35. Minimum supported OS is Android 8.0 / API 26. Configure an untracked `local.properties` with the SDK location, or set `ANDROID_HOME`.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`. Use the same signing key for installable updates. The reporting-quality preview passed fourteen JVM tests, assembly and lint before the branding revision. Final branded build results and phone checks are recorded separately in the release evidence; automated checks do not establish all phone-level behaviour.

Before field use, install on a test phone and verify browser callback, encrypted persistence after offline restart, delayed delivery after connectivity returns, account isolation and OS background scheduling. Use labelled synthetic records for engineering checks. Patient-data pilots require their own authorisation and evaluation.

## Troubleshooting

Sign-in/facility errors require checking the worker's administrator-assigned organisation. Connectivity errors retain pending records. A storage-decryption error must not be resolved by silently clearing storage; preserve the device and involve the operator. Rejected/conflicting reports require review rather than repeated changed-data submission.

## AI boundary

The first implemented AI module is experimental reporting-quality review of the assigned facility’s live DHIS2 snapshot. The current six reports are engineering tests; operational validation remains pending. See MODEL_CARD.md and DATA_SOURCES.md. Clinical triage prediction, voice/image inference and signed model installation are not current features. The application must not label manually selected triage as an AI result.

## Live reporting review

The reporting-quality card queries the authenticated backend, refreshes every thirty seconds while the dashboard is open and supports manual refresh. It shows live facility-level counts separately from the device outbox, along with snapshot time, model status and dataset fingerprint. It clears results on logout and presents source failures explicitly.

## Restored nurse workspace

Version 1.2 restores the original dashboard and adds encrypted local patient registration, search, appointments and worker-entered notes. The Assess tab retains live DHIS2 reporting and reporting-quality AI. No sample patient rows or fixed counts are inserted. See RESTORED_APP.md for exact data boundaries, supported actions and the 18-test build evidence.

## Native form authentication

Native forms are the primary registration and sign-in experience in version 1.3. The AppAuth client and callback above are retained for optional browser-based account setup. See NATIVE_ACCOUNTS.md for the original fields, session storage, fixed engineering-pilot registration, role boundaries and current email-recovery status.
