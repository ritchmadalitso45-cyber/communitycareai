# Restored CommunityCare nurse workspace

Version 1.2 preview. Updated 23 November 2026.

The original GitHub dashboard layout is restored: teal hero, quick actions, patient cards, reminders, synchronisation banner and five bottom tabs. Sign-in uses the original arc and card styling around the hosted secure login. The modern icon and launch screen remain.

## Features and data sources

| Feature | Current behaviour |
| --- | --- |
| Home | Real signed-in worker name, saved patient register and calculated counts. No inserted sample patients or fixed totals. |
| New patient and Patients | Add and search patients saved in encrypted device storage. Age is explicitly the age at registration. |
| Appointments | Save a local appointment purpose and local date/time against a patient. This does not book an external service or notify anyone. |
| Treatments | Save worker-entered consultation or treatment notes against a patient. No treatment is generated or prescribed by AI. |
| Emergency and Assess | Open confirmed-outcome reporting, actual delivery receipts and live reporting-quality analysis. Emergency does not call or dispatch an ambulance. |
| Messages | Displays that no messaging service is connected. No fake conversations or delivery claims. |
| Profile | Shows verified worker and assigned facility, account-management link, reporting status and sign-out. |

## Data boundaries

Patients, appointments and notes are local to this phone. The Android Keystore protects the encrypted storage; saves are atomic. Data is scoped to the signed-in worker and administrator-assigned facility. Another worker or a changed facility cannot add entries to the original patient's record. Account changes recreate the workspace and clear transient selections.

These patient identities and notes are not sent to the Python reporting endpoint or DHIS2. The existing reporting contract continues to send only a timestamp and manually confirmed category. There is no backend patient-register API or cross-phone patient synchronisation. Signing out retains encrypted local records; uninstalling or clearing application storage removes them.

The Assess tab preserves the owned DHIS2 connection and the experimental reporting-quality model. The current verified pilot contains engineering test reports. Scores do not diagnose patients or change triage. Clinical diagnosis AI, image/voice inference, automated prescribing and message delivery are not implemented.

## Verification

18 JVM tests, Android assembly and lint passed for versionCode 3. New tests verify local persistence without seeded rows, account/facility isolation, validation before writes and preservation of the visible state when saving fails. Existing login, reporting and AI contract tests remain in the suite. Physical-phone checks are recorded separately; passing these tests is not clinical validation or evidence of universal device compatibility.

Use engineering test details with shared reviewer accounts. Named staff accounts and approved operational governance are required before real patient use. Patient notes remain separate from aggregate reporting outcomes; this release does not imply a patient-linked DHIS2 record.
