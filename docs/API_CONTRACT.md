# Android and Python API contract

Base URL: `https://care-api.95.111.247.118.sslip.io/`. Protected `/api/v1` requests require `Authorization: Bearer <worker-token>`. Encounter requests from Android also send `X-Organisation-Unit: <original-facility-uid>`.

## Identity

`GET /api/v1/me` returns `subject`, `name`, `username` and `orgUnit` from verified claims. Missing/invalid facility assignments return 403. Never use this endpoint to let a worker choose their own facility.

## Save and poll

`PUT /api/v1/encounters/{uuid}` accepts only:

```json
{"occurredAt":"2026-11-20T09:30:00Z","triage":"REFERRAL"}
```

This is a documented example, not the runtime data source. Timestamps require an explicit timezone, permit up to nine fractional digits, and cannot be in the future. Allowed categories are `ROUTINE`, `REFERRAL` and `URGENT_REFERRAL`. Unknown body fields are rejected. The worker must confirm the category; no AI result is implied.

`GET /api/v1/encounters/{uuid}` polls the receipt. A successful body is:

```json
{"encounterId":"a810227c-273f-486c-90c3-810fe71a2c33","state":"QUEUED"}
```

| Response | Meaning and client action |
| --- | --- |
| 202 with QUEUED | Accepted by server; keep record and poll |
| 200 with FORWARDED | Confirmed DHIS2 delivery |
| 200 with REJECTED | Terminal delivery rejection; show review state |
| 401 | Sign-in/token problem |
| 403 | Missing facility or original-facility mismatch; administrator/sign-in action |
| 404 on receipt | Not found in this organisation; retry original immutable submission |
| 409 | UUID/payload conflict; preserve record for review |
| 422 | Payload validation failure; preserve record for review |
| Network failure or 5xx | Retain unchanged UUID/payload and retry |

Receipt ID must match the requested record and state must be recognised. Do not treat an arbitrary 2xx or malformed receipt as delivery confirmation.

## Other routes

`GET /actuator/health` is public and reports UP/200 or DOWN/503 based on database availability. It does not verify model availability or constitute a DHIS2 round trip.

Authenticated update routes are `/api/v1/updates/manifest.jws` and `/api/v1/updates/{sha256}.zip`. Missing files return 404. These are file-serving routes; mobile verification/activation and published model assets are absent in the current release.

There is no current API for patient registers, clinical AI inference, all-facility dashboards or streaming data. Reports submitted while offline cannot appear in DHIS2 until connectivity and delivery succeed.

## Reporting quality

GET /api/v1/reporting/quality requires the worker bearer token. Android sends X-Organisation-Unit with the original assigned facility. A mismatch returns 403 before source access. The response contains orgUnit, generatedAt, source, dataContext, reportCount, validCount, invalidCount, categoryCounts, delayedOver24Hours, snapshotLimit, truncated, cacheSeconds, modelVersion, datasetFingerprint, modelStatus, trainingSamples, minimumSamples, fittedOn, candidates and notice. Candidate fields are eventId, occurredAt, category, delayHours, anomalyScore and reviewSuggested. Scores are not probabilities. Fewer than five valid rows return INSUFFICIENT_DATA with no candidates. Source or configuration failures are explicit errors.


## Native account forms

POST /api/v1/auth/register, /api/v1/auth/login, /api/v1/auth/refresh and /api/v1/auth/password-reset accept their corresponding form bodies without an existing bearer header. See NATIVE_ACCOUNTS.md for exact fields and data handling. Registration returns 201 with ENGINEERING_PILOT or AWAITING_ASSIGNMENT. Login and refresh return accessToken, refreshToken and expiresIn only after signed identity and organisation validation. Reset returns a generic 202 when recovery delivery is configured; missing SMTP configuration returns 503 without claiming delivery.

Account form validation responses are redacted. Unknown fields are rejected, client-supplied facility or administrator permissions are not accepted, declared oversized forms return 413, and rate limits return 429. A requested role remains profile metadata. Ordinary workers do not request the backend's account-management scope. Protected reporting and identity routes continue to require a verified bearer token.

## Facility team messaging

See [the 1.4 messaging contract](ORGANIZED_APP.md#messaging-api) for authenticated facility chat, limits and retry semantics.

## Staff roles and shared task workflows — version 1.5

See [the staff-role contract](STAFF_ROLES.md#api-additions) for verified role claims, approval, case transitions and staff endpoints. Protected routes now require an active assigned staff role.
