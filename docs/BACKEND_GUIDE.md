# CommunityCare Python backend

## Architecture and deployment

The hosted FastAPI API, PostgreSQL, Keycloak and owned DHIS2 instance run on Contabo. Source is under `/opt/communitycare`; deployment configuration is under `/opt/communitycare/deploy`. The laptop holds source and tools, not the hosted runtime.

DHIS2 URL: `https://care-dhis.95.111.247.118.sslip.io/`. This is an owned persistent DHIS2 2.43.2.1 installation, not the public resettable demo. The configured organisation is `CommunityCare Pilot`. It is not a connection to Malawi's national DHIS2 and is not evidence of a real-patient pilot.

## Source map

| Source | Responsibility |
| --- | --- |
| `app/main.py` | Lifespan, identity, report/receipt routes, downloads and health |
| `app/auth.py` | OIDC discovery, RS256 token verification and facility claim |
| `app/models.py` | Strict timestamp and triage payload validation |
| `app/queue.py` and `app/schema.sql` | PostgreSQL persistence, encryption, HMAC deduplication and leases |
| `app/delivery.py` | DHIS2 Tracker import, response interpretation and retries |
| `app/quality.py` | Read-only scoped DHIS2 snapshot and experimental IsolationForest |
| `app/config.py` | Required configuration and validation |
| Server deployment configuration | Protected hosting configuration retained on the server |

## Queue lifecycle

Authenticated requests require a valid subject, issuer, audience, signature and expiration. Encounter routes additionally require a valid signed `dhis_org_unit`. When supplied, the original-facility header must match before any queue read or write.

The queue encrypts `timestamp|triage` using AES-GCM, binds it to organisation/encounter context, and stores a keyed digest for duplicate checking. Reusing a UUID and identical payload is safe. Reusing the UUID with changed data in the same facility returns 409. Organisation-scoped receipts are shared by users assigned to that organisation; they are not individually owner-scoped on the backend.

Workers claim rows using leases and `FOR UPDATE SKIP LOCKED`. The same deterministic DHIS2 event UID is retained across retries. Delivery uses synchronous Tracker import with `CREATE_AND_UPDATE`. A qualifying import report confirming one created/updated event is required for `FORWARDED`; structured validation errors can produce `REJECTED`. Outages, authentication failures and unrecognised responses retry with backoff. Terminal receipts retain the digest while clearing the encrypted queue payload.

## Configuration and secrets

Use `requirements.lock` for the recorded runtime dependency versions. Required settings include database URL/user/password, OIDC issuer/audience, a persistent base64 32-byte encryption key, DHIS2 URL/service token and configured program/stage/data-element IDs. Consult `.env.example` for names, not production credentials.

The local historical public-demo `.env` is not used by the hosted deployment. Never put `.env`, private login files, SSH keys, database dumps or service tokens in an application submission. Never regenerate the encryption key against an existing queue without a migration plan.

HTTPS terminates at the shared Caddy proxy. PostgreSQL is private and application connections verify its TLS certificate. The VPS also hosts other applications; CommunityCare changes must not restart or reconfigure them inadvertently.

## Operations and verification

```sh
cd /opt/communitycare/deploy
docker compose -f compose.json --profile api ps
docker compose -f compose.json logs --tail 50 communitycare-api
```

Logs must exclude tokens, report payloads and remote clinical response bodies. Health checks establish API/database availability, not complete DHIS2 delivery readiness or AI capability.

The full 42-test suite passed against a separate test database on 23 November 2026. To repeat in that isolated environment:

```sh
docker compose -f compose.json run --rm --no-deps --user 0 \
  -v /opt/communitycare/deploy:/server-deploy \
  communitycare-api python /server-deploy/run_tests.py
```

`verify.py` and `verify_android_pkce.py` are active integration checks that create labelled synthetic reporting data. They are not patient-data studies. `readiness_audit.py` performs read-only health, metadata and aggregate checks without creating reports or printing patient records.

## Backups and limitations

Daily server-local backups and a previous isolated restore check exist. Offsite backup storage remains to be configured. Preserve the queue key with protected backups. See the deployment guide for commands and ownership.

The update download routes serve files; they do not create, train or validate models. No model/protocol packages were published at the last audit, and the mobile app has no signed update installer. No clinical AI endpoint is implemented. DHIS2 dashboards and cross-device mobile analytics are also not implemented.

## Reporting quality endpoint

GET /api/v1/reporting/quality verifies the signed organisation claim and original-facility header before reading the configured DHIS2 Tracker program and stage. It uses a ten-second bounded cache and rejects cross-scope source rows. Source errors do not return fabricated or stale successful results. The model card documents the exact experimental method and limitations.
