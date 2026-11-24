# CommunityCare staff roles and restored dashboard — 1.5 preview

24 November 2026. This release restores the original mobile dashboard layout: teal consultation hero, patient search, four quick actions in a row, horizontally scrolling activity statistics, recent patient cards, care reminders, sync banner and original five-tab bottom navigation. It retains native registration/login, encrypted saved records, live DHIS2 statistics and working facility messaging. No sample patients or fixed clinical totals are added.

## Role tasks

| Assigned role | Available work |
| --- | --- |
| HSA | Register local patients, record observations, schedule local follow-up appointments, submit confirmed aggregate reports, create community referrals, add observations to own cases, and complete community follow-up assigned to them. |
| Nurse | Local nursing records and appointments, confirmed reports, claim nursing referrals, add review notes, escalate to Clinical Officer, or assign an active HSA for follow-up. May start a new nursing review. |
| Clinical Officer | Local care records and appointments, confirmed reports, claim clinical referrals, record review notes, plan HSA follow-up, and close their reviewed cases. May start a new clinical review. |
| Supervisor | Review facility workload and history, assign staff appropriate to the current task, return cases to nursing review, reopen closed cases, and approve/change staff application roles. Does not create local care records, submit clinical reports or close a clinical review. |

All approved roles can use facility chat and view live reporting-quality statistics. Local patient registers are still bound to the original account and facility. HSA server case access is limited to cases they created or follow-up assigned to them; other approved staff roles see facility cases. All worker decisions and notes are human entered. These workflows do not generate medical advice, send emergency dispatches or validate a person's professional qualifications.

Open the centre Assess tab and choose **My role tasks**. **Reports & AI** keeps the DHIS2 reporting and statistics view available. Profile also has Open my role tasks and Refresh role and account. The original Home presentation is preserved.

## Registration and approval

Registration still collects the original full name, email, phone, role, password and confirmation. As explicitly approved by the owner, NATIVE_REGISTRATION_AUTO_ROLES=true plus a fixed NATIVE_REGISTRATION_PILOT_ORG gives a newly registered engineering-test account its selected application role immediately, including Supervisor. This is a shared engineering pilot with test-only information. This configuration must not be used for real operational enrolment.

For operational deployment, set NATIVE_REGISTRATION_AUTO_ROLES=false (or remove it) and remove the public fixed pilot assignment. Registration stores the selected requested_role and a PENDING assigned role. An administrator must assign the proper facility; an existing approved Supervisor in that facility then approves the application role. A pending account can sign in to view its approval status when it has a facility, but protected tasks are denied. Without a facility, the existing facility-assignment message is returned. The initial operational Supervisor is bootstrapped by the system operator, not by unrestricted public registration.

Assigned roles come from the identity service's administrator-editable-only communitycare_role attribute, included in signed access tokens for both native and browser sign-in. The server does not accept a client profile field or requested_role as proof of permission. Application Supervisor is separate from Keycloak/identity-server administration; registration and Supervisor role changes do not grant realm administrator permissions. The owner account is assigned application Supervisor; the existing test.user reviewer remains HSA. Existing pilot users were migrated using their selected staff role where present. Passwords were not changed.

Role approval/change requires an already approved Supervisor, fresh confirmation of that actor's current role through the identity service, the same facility for the target, a reason and a request UUID. Supervisors cannot change their own role. The change is recorded in an encrypted audit entry. Refresh role and account obtains a fresh token after approval. Other permissions carried by an existing access token can remain effective until that short-lived token expires; ordinary role refresh is not an immediate global revocation mechanism.

## Shared workflow and persistence

HSA referrals begin in AWAITING_NURSE. A Nurse claims NURSE_REVIEW and either escalates to AWAITING_CLINICAL_OFFICER or plans FOLLOW_UP for an active HSA. A Clinical Officer claims CLINICAL_REVIEW, records notes, then plans FOLLOW_UP or closes the reviewed case. The assigned HSA completes follow-up to CLOSED. A Supervisor can reopen to the nursing queue, return active cases to that queue and assign a worker of the role required by the current state.

Supervisors cannot use coordination actions to record a clinical closure. Nurses and Clinical Officers must hold the relevant assigned review before making their clinical workflow decisions. Wrong roles return 403; missing/out-of-scope records return 404; stale versions or conflicting reuse return 409. The client renders only applicable actions, and the backend independently enforces the transition.

Case creation uses a persistent UUID and immutable creation digest. Each action carries a command UUID and expected version. Database locks, optimistic versions and action receipts prevent duplicate actions and conflicting concurrent edits. PostgreSQL stores encrypted case summaries and review notes with contextual AES-GCM protection. Each history entry records the actor, role, action, time, resulting state and version. The API returns the latest 100 accessible cases, actual status counts and at most the latest 50 history entries for a case. The screen refreshes while open; internet is required for shared task changes. It is not a durable offline case-command outbox.

Use a visit reference instead of a patient name in shared cases. Local patient chart notes and appointments remain on the phone. Shared workflow summaries and review/follow-up notes are a separate server-backed record visible to authorized facility staff; these are not sent through the aggregate timestamp/category DHIS2 reporting endpoint. This release does not introduce DHIS2 Tracker patient registration or cross-phone local-chart synchronization.

## API additions

- GET /api/v1/me now includes role and permissions. Missing assigned role means PENDING; unknown role is denied. Protected resources require an active assigned role.
- GET /api/v1/workflows lists accessible cases and actual counts.
- PUT /api/v1/workflows/{uuid} creates a case with patientReference, summary and priority (ROUTINE or URGENT).
- GET /api/v1/workflows/{uuid} returns case and recent audit history.
- POST /api/v1/workflows/{uuid}/actions accepts commandId, version, action, note and optional assigneeId. Actions are CLAIM, ESCALATE, FOLLOW_UP, COMPLETE_FOLLOW_UP, CLOSE, ADD_NOTE, ASSIGN, RETURN_TO_NURSE and REOPEN.
- GET /api/v1/staff lists same-facility active staff; Supervisors also see pending staff. The bounded directory reports whether it is limited.
- PUT /api/v1/staff/{subject}/role accepts role, reason and requestId, and requires a same-facility Supervisor. Role changes never assign the target a different facility.

All workflow/staff routes require a bearer token. Android sends X-Organisation-Unit and the server rejects a mismatch before mutation. Extra input fields are rejected and form-size limits apply. These routes use the existing verified issuer, audience, signature and token expiry checks.

## Validation

71 backend checks passed, including real PostgreSQL role handoffs, encrypted case persistence, wrong-role rejection, cross-facility isolation, version conflicts, duplicate retry handling, clinical closure and operational pending status. 34 Android JVM checks, assembly and lint passed, including role policy, legacy-profile migration and local write restrictions. Live hosted registration/login was verified for all four roles, including operational pending/approval, the complete role handoff and preserved existing account access. Build evidence and live hosted verification are recorded with the release. A few Android devices and engineering test data do not constitute clinical validation or testing on every phone.

Gmail reset-email setup and external Google/Microsoft login configuration remain separate pending integrations. The reporting-quality model stays experimental and clinically unvalidated.
