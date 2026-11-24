# CommunityCare AI — Android and hosted backend integration

Native Kotlin / Jetpack Compose app connected to the owned CommunityCare services
on Contabo. Minimum Android version: 8.0 (API 26).

Current release: **1.5 preview**. [Download Android app](https://github.com/VincentBanda010/communitycareai/releases/tag/v1.5.0-preview). See [screen organization and messaging contract](docs/ORGANIZED_APP.md).

## Connected services

| Setting | Value |
| --- | --- |
| Backend | `https://care-api.95.111.247.118.sslip.io/` |
| OIDC issuer | `https://care-login.95.111.247.118.sslip.io/realms/communitycare` |
| Public Android client | `communitycare-android` |
| Browser callback | `communitycare://oauth/callback` |

These public addresses are in `app/build.gradle.kts`. No DHIS2 token, database
password, administrator password or client secret belongs in the Android app.
DHIS2 is contacted by the backend only.

## Behavior

- Native account creation and sign-in use the hosted account endpoints. Browser setup uses AppAuth, OpenID Connect Authorization Code + PKCE S256.
  The backend confirms the authenticated worker's identity and assigned organisation
  using `GET /api/v1/me` before the app opens the dashboard.
- New accounts can register in the app. The server assigns the configured engineering pilot facility; selected application roles are immediately available in the explicitly configured engineering pilot. Operational facility assignment and role approval remain controlled.
- Tokens and report records are encrypted using AES-256-GCM and an Android Keystore
  key. App backup is disabled. Unreadable encrypted data is preserved and reported,
  rather than silently reset.
- After first successful sign-in, workers can save assessment time and HSA-confirmed
  triage outcomes offline. UUIDs and payloads are retained across retries.
- WorkManager queues delivery when connectivity returns, polls server receipts,
  and retries temporary failures. A 202 response means accepted by the server;
  only a FORWARDED receipt means delivered to DHIS2.
- Reports are scoped to the authenticated subject **and** organisation. Signing out
  leaves records encrypted on the device; another account cannot view or submit them.
- The dashboard shows actual local records, pending/delivered counts and delivery
  errors. Counts describe this device, not all facilities or all backend records.
- Changed-data conflicts and rejected reports remain visible for review. They are
  not silently discarded or continually resubmitted.

The existing repository was a UI prototype: sign-in navigated without verification,
the dashboard displayed hard-coded patients and counts, and sync buttons were empty.
This branch connects account access and the supported assessment-reporting pipeline.
Patient registers, appointments and care notes are encrypted local modules. Facility messaging is connected to PostgreSQL through the Python API. Clinical diagnostic AI, image/voice processing and generated referral documents remain future modules. The reporting form records the outcome
already confirmed by a health worker using approved clinical guidance.

## API contract

Protected requests require `Authorization: Bearer <worker-access-token>`. Native registration, login, refresh and recovery have the public account-route exception documented in docs/NATIVE_ACCOUNTS.md. Submission and receipt polling also send `X-Organisation-Unit` with the original facility for that report. The backend rejects a mismatch after token refresh or staff reassignment; reports never move automatically between facilities.

| Method | Route | Use |
| --- | --- | --- |
| GET | `/api/v1/me` | Confirm `subject`, `name`, `username`, `orgUnit`. |
| PUT | `/api/v1/encounters/{uuid}` | Submit one immutable report; 202 while queued, 200 for terminal receipts. |
| GET | `/api/v1/encounters/{uuid}` | Poll QUEUED, FORWARDED or REJECTED. |

```json
{"occurredAt":"2026-11-22T10:00:00Z","triage":"URGENT_REFERRAL"}
```

Triage values are `ROUTINE`, `REFERRAL`, `URGENT_REFERRAL`. Patient names, notes,
images and audio are outside this reporting contract. Reusing an encounter UUID
with changed data returns 409. HTTP 401/403 requires signing in or an administrator's
help; temporary outages keep reports pending. A missing queued receipt is safely
resubmitted with the same UUID and unchanged payload.

## Build

Requires JDK 17 or 21, Android SDK platform 35 and the Gradle wrapper.

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Windows: use `gradlew.bat`. Set `sdk.dir` in an untracked `local.properties` or
configure `ANDROID_HOME`. The debug APK is at
`app/build/outputs/apk/debug/app-debug.apk`. Keep the same signing key for updates
to an installed app. Uninstalling the app removes its encrypted local reports.

Tests cover actual HTTP paths, bearer headers, payload fields, receipt validation,
duplicate-safe retries, account/facility isolation, queued-versus-delivered status,
network interruption and review-required errors. The hosted backend also has its
own authentication, queue, encryption and PostgreSQL tests.

## Device acceptance checks

1. Install the APK on a test device with a browser and sign in using the assigned
   **CommunityCare app account**. DHIS2's separate administrator login is not the
   mobile app account. A temporary password is changed on the hosted sign-in page.
2. Check the callback returns to the app and the dashboard shows the actual worker.
3. Disable connectivity, save a synthetic triage outcome, and restart the app.
   Confirm the report remains pending.
4. Restore connectivity and tap Sync now. Confirm QUEUED becomes delivered after
   a FORWARDED receipt, then find the event in the owned DHIS2 instance.
5. Sign out and sign in as a different test worker. Confirm previous-account
   reports are hidden and never sent under the new account.

Browser handoff, Keystore persistence and background scheduling require these real
device checks in addition to JVM tests and compilation.

## Implementation guides

- [Frontend guide](docs/FRONTEND_GUIDE.md)
- [Backend guide](docs/BACKEND_GUIDE.md)
- [API contract](docs/API_CONTRACT.md)
- [Current submission status and limitations](docs/SUBMISSION_STATUS.md)


## Reporting-quality preview — 23 November 2026

Live nonclinical reporting-quality AI is implemented. See [release status](docs/REPORTING_QUALITY_RELEASE.md), [model card](docs/MODEL_CARD.md) and [data provenance](docs/DATA_SOURCES.md). The current owned DHIS2 pilot includes six engineering reports; the model is experimental and has no clinical validation.


## Restored app experience

Version 1.2 restores the original CommunityCare dashboard, quick actions, patient cards, five-tab navigation and sign-in styling while retaining hosted login, DHIS2 reporting and experimental reporting-quality AI. Patient registration, search, appointments and worker-entered notes use encrypted local storage. Patient identities and notes are not sent through the aggregate reporting contract. Messaging and clinical prediction remain unimplemented. See [feature and data boundaries](docs/RESTORED_APP.md).


## Original on phone account forms

Version 1.3 restores the original Create Account and Sign In fields directly on the phone, connected to the owned identity service through the Python backend. Live registration, login, refresh, pilot-only assignment and reporting analysis are verified. Checks: 49 backend tests and 22 Android tests, assembly and lint. [Account guide](docs/NATIVE_ACCOUNTS.md). Gmail recovery email still requires the owner’s app password.

## Four staff roles and original dashboard

See [STAFF_ROLES.md](docs/STAFF_ROLES.md) for the HSA, Nurse, Clinical Officer and Supervisor task matrix, test-pilot registration policy, operational approval, audit history and API contract.
