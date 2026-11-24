Historical 1.4 release notes. The original dashboard is restored in [version 1.5 with staff roles](STAFF_ROLES.md).

# CommunityCare 1.4 preview

Updated 23 November 2026. This release supersedes the earlier UI and native-account previews.

## Screens

Home preserves the consultation hero, patient search, all four original quick actions, four activity statistics, recent-patient cards, care/reminder cards, notification/status button, profile access and sync controls. The presentation uses a coordinated teal gradient, decorative circles, a two-column action grid, consistent spacing and grouped cards. Patients, appointments and care notes use the encrypted local register. Patient details now show that patient's saved history. Invalid calendar dates are rejected rather than silently normalized. Welcome retains its original feature highlights and both account actions. Registration keeps every field and role choice. Sign-in keeps email/username, password, Remember me, recovery, browser setup and the original Google/Microsoft controls. The provider controls remain disabled with a short setup-pending label because provider credentials are not configured.

Assess shows live owned-DHIS2 statistics first: report total, late-report count, review candidates and category bars. These are real service responses, not fixed chart values. The count is a bounded latest snapshot, not an all-time total. Technical model details are collapsed. Device report totals are separately labelled. AI remains experimental and uses engineering pilot data; it does not diagnose patients.

Messages is a real shared facility-team conversation. Sender identity and facility come from verified server claims. Internet is required to send. The latest 100 messages refresh every 10 seconds while the screen is open. An acknowledged message appears with sender and server time. A failed send keeps its draft while that screen stays open; retry uses the same UUID unless the draft is edited. There are no push notifications, direct/private conversations, attachments, read receipts or background message alerts in this release. Unsent drafts are not a durable offline outbox. Shared pilot accounts must not send patient information.

Profile groups account settings, sign-out and data/service details. Longer explanations are available on demand rather than repeated across pages.

## Messaging API

Both routes require a valid worker bearer token. Android also sends X-Organisation-Unit; a mismatch returns 403. The client cannot choose the sender or target facility.

- GET /api/v1/messages returns orgUnit and the latest 100 messages, oldest first.
- PUT /api/v1/messages/{uuid} accepts only {"text":"Good morning, team. Please confirm the meeting time."} and returns the persisted message. Message fields are id, senderId, senderName, text and sentAt.
- Text is trimmed, must contain 1–2000 characters and cannot contain disallowed control characters. Unknown fields are rejected.
- A retry with identical sender, UUID and text returns the existing message. A changed text or sender returns 409. Per-sender sending is capped at 30 new messages per minute.
- PostgreSQL stores encrypted message bodies and displayed sender names using AES-GCM and message/facility/sender context. Transport uses HTTPS. Authorized facility users and the backend can read the chat; this is not end-to-end encryption. No retention/deletion interface is provided yet. Back up the encryption key alongside the existing database recovery procedures.

## Verification

54 Python tests passed under the deployed application account, including real PostgreSQL persistence, encrypted storage, facility isolation, two-worker delivery, duplicate retries, rejected overwrites, input validation and sending limits. Live hosted verification confirmed delivery and reply between two existing pilot accounts, rejected anonymous/mismatched-facility access and confirmed owned-DHIS2 analysis still works. Natural language was used for conversation checks.

The Android build runs JVM tests, assembly and lint. Current build and device acceptance results are recorded with the APK release. No claim is made that every phone model or every Android version has been physically tested.

## Remaining setup

Gmail password-reset delivery still requires SMTP_USERNAME and SMTP_APP_PASSWORD in the protected local environment. Patient identities, appointments and care notes are local device records, not a cross-phone DHIS2 patient register. Clinical diagnostic AI remains outside the implemented reporting-quality function.
