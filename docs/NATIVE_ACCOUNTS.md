Current registration and staff-role authorization behavior is documented in [STAFF_ROLES.md](STAFF_ROLES.md). The 1.3 implementation notes below are historical.

# Original native account forms

Version 1.3 preview, 23 November 2026.

The original on-phone forms are restored. Create Account collects full name, email, phone, requested role, password and confirmation. Sign In collects email or assigned username, password and Remember me. The original rounded cards, teal arc, password visibility controls and role choices are retained. Get Started opens registration; the existing-account link opens sign in. These forms perform actual server requests and do not bypass authentication to reach the dashboard.

## Account endpoints

POST /api/v1/auth/register accepts fullName, email, phone, role and password. Valid role requests are HSA, Nurse, Clinical Officer and Supervisor. The server uses its protected identity-service client to create the account. Role requests are profile metadata, not authorisation. The phone cannot choose an organisation or assign administrator roles. The current explicitly configured engineering pilot assigns its fixed test facility. Without NATIVE_REGISTRATION_PILOT_ORG, registration returns AWAITING_ASSIGNMENT and an administrator must assign the facility before API access.

POST /api/v1/auth/login accepts username and password. POST /api/v1/auth/refresh accepts refreshToken. Responses contain accessToken, refreshToken and expiresIn. The backend verifies the returned token signature, issuer, audience, expiry, subject and assigned organisation before returning a session. Passwords are not stored by the Android app. Remember me stores the session encrypted using the Android Keystore; when unchecked the native session remains in memory only. Report and patient-record data remain encrypted separately after sign-out.

The owned Keycloak service stores credentials. The backend-only confidential client uses native password authentication; no client secret is shipped in the APK. The existing browser PKCE flow remains available for account setup that needs browser interaction. MFA-required accounts are not silently bypassed by the native flow. This engineering preview does not claim clinical production certification.

The native client's default scopes include basic, profile, email and roles. The basic scope supplies the mandatory subject claim. Its optional kc-admin-api scope is requested only by backend service-account management calls; normal worker login does not request it. The service account holds manage-users in the owned realm. Ordinary newly registered users do not receive that role. Do not disable the identity server's admin audience checks.

## Password recovery and external providers

Forgot Password opens an on-phone request form using /api/v1/auth/password-reset. The current Gmail sender is awaiting the owner's app password in the protected local .env file. Until configured, the app clearly reports that recovery email is unavailable; it does not claim an email was sent. Google and Microsoft sign-in buttons remain visibly unconnected because those identity-provider integrations have not been supplied.

For Gmail setup, save SMTP_USERNAME and SMTP_APP_PASSWORD locally; never commit them. The configuration helper authenticates through STARTTLS without sending a test email, stores the sender configuration in the owned identity service and enables NATIVE_AUTH_SMTP_ENABLED. The API must reload that flag. Existing users can be recovered by email or username once delivery is configured.

## Verification

49 backend tests passed under the same application user used by the deployed service. 22 Android JVM tests, assembly and lint passed. Live verification created a disposable identity account, authenticated it, refreshed its session, rejected an incorrect password, verified pilot assignment and absence of administrator permissions, authenticated test.user and read live owned-DHIS2 reporting-quality analysis. The disposable account was removed; no new reporting event was created by that check.

The current owned DHIS2 pilot contains engineering test reports. Patient registers, appointments and notes remain encrypted on the device and are not sent through the timestamp/category reporting endpoint. See RESTORED_APP.md and MODEL_CARD.md for the feature and model boundaries.
