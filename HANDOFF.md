# Everybuy Mobile — coding handoff

## Project overview

Everybuy Mobile is the new native Android client for the existing Everybuy business application. `../everybuy-frontend/` is the React reference client; `../everybuy-backend/` is the authoritative NestJS + Better Auth API. Do not change either project for routine mobile work. Business rules, validations, permissions, pricing, stock and balances remain backend-owned. This is an online-only app; do not add offline sales, Room business storage, queues or WorkManager sync.

The broader route/API inventory and migration checklist are in `../MIGRATION_PLAN.md`; the original instructions are in `../Migration.md`; the detailed auth investigation is in `../AUTH_MIGRATION_NOTES.md`. Reuse them instead of repeating repository exploration.

## Project layout and stack

- `app/src/main/java/com/everybuy/mobile/data/`: auth/network DTOs, Retrofit interfaces and repositories. `AuthData.kt` contains auth DTOs, Hilt network providers and auth recovery calls; feature repositories are split per domain (`StockRepository.kt`, `StockTakesRepository.kt`, `SalesRepository.kt`, `CustomersRepository.kt`, `ExpensesRepository.kt`, `AuditRepository.kt`, `AdminRepository.kt`).
- `ui/auth/`: sign-in/TOTP, auth ViewModel, DataStore theme preference, forgot/reset password and invite acceptance, and `AuthDeepLinks.kt`.
- `ui/theme/EverybuyTheme.kt`: dynamic Material theme and Poppins typography.
- `ui/inventory/`: inventory list, product detail (prices, stock, recent movements), product creation/editing and validation.
- `ui/reports/`: Today/Monthly/Profit reports and `ReportDates.kt`.
- `ui/stock/`: movement history/detail, receiving, adjustments and validation.
- `ui/stocktakes/`, `ui/sales/`, `ui/customers/`, `ui/expenses/`, `ui/audit/`: completed workflows listed below.
- `ui/settings/`: settings hub, profile/shop, team, catalog and supplier management.
- `navigation/AuthenticatedApp.kt`: authenticated Navigation Compose shell with six bottom destinations (Stock, Overview, History, Counts, Sales, Customers); detail/form and settings routes hide the tab bar.
- `core/util/Formatters.kt`: Naira and Africa/Lagos date formatting. `core/util/ApiErrors.kt`: safe Nest/Zod error messages.
- `app/src/test/`: auth/recovery, product, stock, stocktake, sales, customer, report-date, expense, audit and admin/settings tests.
- `app/build.gradle.kts`, `gradle/libs.versions.toml`, `settings.gradle.kts`: Gradle Kotlin DSL and version catalog.
- `README.md`: local API/build instructions. `../MIGRATION_PLAN.md`: feature status and remaining migration work.

Required architecture is Kotlin + Jetpack Compose/Material 3 Expressive, MVVM, repository pattern, Retrofit/OkHttp, Coroutines/Flow/StateFlow, Hilt, DataStore and Navigation Compose. Keep API operations out of composables and expose UI state from ViewModels.

Current versions in the catalog: AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Navigation 2.10.2, Hilt 2.60.1, Retrofit 3.0.0, OkHttp 5.5.1, Coroutines 1.10.2. Wrapper is Gradle 9.9.1; Java source/target is 17; `compileSdk`/`targetSdk` are 37 and `minSdk` is 26. These versions build successfully in this environment using the offline Gradle command in "Git state and verification".

## Design and theme decisions

`AppTheme` has `SYSTEM`, `LIGHT`, `DARK`, `AMOLED`; DataStore defaults to System and persists changes. System follows device appearance. System/Light/Dark use Android dynamic Material You colors on API 31+, with Material fallbacks. AMOLED selects dark mode and explicitly sets black background/surface with raised contrasting containers. Theme is collected by `MainActivity`; changes apply in composition. Poppins font files and OFL license are under `app/src/main/res/font/` and `app/src/main/assets/licenses/`.

## Authentication and API findings

See `../AUTH_MIGRATION_NOTES.md` (particularly its endpoint, cookie, Origin and remaining-issues sections). Confirmed backend contract:

- Better Auth v1.7.10 uses signed HttpOnly cookie sessions; there is no Android bearer-token flow. Never use the login `token` as an Authorization header or store passwords.
- Login: `POST /api/v1/auth/sign-in/email`; TOTP: `POST /api/v1/auth/two-factor/verify-totp`; session: `GET /api/v1/auth/get-session`; logout: `POST /api/v1/auth/sign-out`.
- `get-session` returns HTTP 200 with JSON `null` when signed out. A valid session returns `{session,user}`. `/api/v1/me` returns an array; empty/403 means signed in without active staff access.
- Better Auth checks Origin on cookie-bearing POSTs. OkHttp sends `Origin: everybuy://app`. The deployment environment must trust this origin in `BETTER_AUTH_TRUSTED_ORIGINS`; this is configuration only, and backend source has not been modified.
- Session cookies are persisted by `EncryptedCookieJar` in `AuthData.kt`, scoped by scheme/host/port and encrypted with Android Keystore AES-GCM before storage in Preferences DataStore. Logout clears the jar even when the request fails. IO failure during restore preserves cookies for retry; a protected-request 401 returns to auth. Debug OkHttp logging is headers-only with sensitive headers redacted.
- Better Auth session lifetime is 7 days with a 1-day update age. Call `get-session` to capture rotations. App startup restores the session; foreground refresh remains outstanding.
- Current implementation supports email/password, session restore, logout, staff/role resolution, TOTP, forgot/reset password and invite acceptance. Backup codes and passkeys/native WebAuthn remain unimplemented (web-only).
- Password reset requests pass `redirectTo=everybuy://reset-password`; invite acceptance uses `everybuy://invite/accept?token=…`. The manifest registers both deep links. The reset redirect must be trusted by Better Auth, and invite emails only open the app if `STAFF_INVITE_PREVIEW_URL` points at an app link (otherwise they open the web client).
- Auth recovery source is compiled and unit-tested but not yet verified against a running backend.

## API configuration

All Android variants use the deployed production API over HTTPS. The base URL is centralized in `app/build.gradle.kts`; Retrofit endpoint paths are relative to its `/api/v1/` prefix, including authentication and session APIs. No local backend URL or `local.properties` value is used for backend configuration. The shared network security configuration denies cleartext traffic.

## Current implementation status

**Implemented and build/test verified in this environment:**

- Auth shell with sign-in/TOTP, secure cookie session restore/logout, no-staff handling, protected-request expiry handling, forgot/reset password and invite acceptance deep links.
- Four persisted theme modes described above.
- Product list with search/category/brand/pack filters, detail (prices, stock, recent movements), Owner-only create/edit forms, and a per-product movement history link.
- Stock movement list with search/type/product filters/pagination, movement detail, owner-only adjustment, and receiving with supplier selection, backend-aligned cost validation and stable UUID retry keys.
- Stocktake list/create/detail/count/recount/finalize/cancel.
- Sales list/summary/filters, record-sale form with server-verified prices/stock and idempotent request UUID, sale detail with items/payments.
- Customers list/search/status, add/edit, statements with outstanding sales, payment history and idempotent repayments.
- Overview tabs for Today (daily report), Monthly (sales, money received, month-end debt, products sold, closing stock with month navigation) and Profit (revenue/cogs/gross/net plus missing-cost warnings), with Naira/Lagos display.
- Expenses list/search/summaries and record-expense dialog with stable request UUID.
- Audit log with search and detail summaries.
- Settings hub: profile name edit, shop details read for staff/edit for owners, owner team management (role/access/invitations/revoke), catalog categories/brands and supplier management.

**Remaining/partial:**

- All features are verified by unit tests and `assembleDebug`, not yet against a running backend, emulator or physical device. Auth Origin/redirect trust, cookie rotation, TOTP, deep links and every write flow still need runtime verification.
- Browser-only security settings (passkeys, backup codes) are intentionally not exposed; document them as web-only rather than adding unsupported native flows.
- Theme splash/startup flash, dialogs and system-bar behavior need device verification.
- Release signing/CI workflow is not configured (see below).

## CI/CD and release signing

There is currently **no `.github/` directory or GitHub Actions workflow** in this mobile repository, no changelog file was found at its root, and `app/build.gradle.kts` has no release `signingConfig`. The release build enables minification/resource shrinking but is not configured here for signing. A workspace-level `../everybuy-release.jks` file exists, but it is not referenced by Gradle; do not copy it into the repo, print its contents, or commit it. Its validity, ownership and GitHub secret setup are not confirmed.

Recommended workflow requirements (not yet implemented):

1. Run on pushes to `master` only for published releases; a manual trigger must also refuse publishing unless the ref is `master`. Run unit tests before assembling.
2. Configure Java/Android SDK and API URL, build a signed release APK, generate release notes from a maintained changelog, and publish a GitHub Release with the APK and changelog/release notes as assets. Avoid publishing from feature branches or pull requests.
3. Keep keystore bytes/passwords outside source control. A practical Actions secret set is `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Decode the keystore only into the ephemeral runner; delete it after build. Wire Gradle release signing to environment/Gradle properties, not literal credentials.
4. These secret names are recommendations only. No workflow or GitHub secret store was inspected/confirmed, so configuration is **pending**, not complete. Do not put actual values in this handoff. Confirm the existing parent JKS is the intended production signing key before using it; replacing a published app's signing key can break upgrades.

## Git state and verification

At handoff, `git status --short --branch` in `Everybuy-mobile` showed `master...origin/master` and these pending paths:

- Modified: `.idea/misc.xml`, `README.md`, `AuthData.kt`, `StockRepository.kt`, `AuthenticatedApp.kt`, `ProductDetailScreen.kt`, `ReceiveStockViewModel.kt`, `StockMovementsScreen.kt`.
- Untracked: `ReceiveStockScreen.kt`, `StockAdjustmentViewModel.kt`, stock receipt/adjustment test files under `app/src/test/...`.

Preserve all of them. `.idea/misc.xml` is a pre-existing IDE metadata change unrelated to app work; do not revert it without the user's direction. `git diff --check` completed successfully with no whitespace errors.

Commands attempted in this environment:

```text
GRADLE_USER_HOME=/tmp/everybuy-gradle GRADLE_RO_DEP_CACHE=/home/loixrang/.gradle/caches gradle --offline --no-daemon :app:testDebugUnitTest :app:assembleDebug
Same with JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true and -Dorg.gradle.vfs.watch=false
Same Gradle setup with --stacktrace help
```

All Gradle attempts failed **before project configuration**: Gradle's `FileLockContentionHandler` could not determine a wildcard IP because the sandbox denied `NetworkInterface.getNetworkInterfaces()` with `SocketException: Operation not permitted`. Therefore no tests ran and no build/compile succeeded. The existing `app/build/outputs/apk/debug/app-debug.apk` timestamp is 2026-11-14 14:06 (+0100), predates the current source changes, and is stale. Do not deliver it as a verified build.

## Recommended next steps

1. Preserve the pending Git changes. In an environment that permits network-interface enumeration, run `./gradlew testDebugUnitTest assembleDebug`; first fix compile/test failures, including the current handoff changes and cookie-jar type correction. Install against the existing backend and validate auth before feature expansion.
2. Confirm the deployment trusts `everybuy://app`; test email/password, TOTP, cold-start restoration, session cookie rotation, logout and expired-session handling. Do not modify backend source for this.
3. Finish and test stock flows, then implement stocktake list/create/detail/count/finalize/cancel using existing routes and Staff/Owner guards.
4. Migrate sales and customer/payment workflows. Preserve server-computed prices/balances and UUID idempotency; use Africa/Lagos dates and NGN formatting.
5. Migrate monthly/profit reports, expenses, audit, settings/team/catalog/supplier management and remaining product actions. Add tests as each workflow lands; do not add offline behavior.
6. Verify all role-specific paths, network errors/loading/empty states, theme persistence and screen behavior on emulator/device; refresh the migration plan.
7. Configure and verify master-only GitHub release workflow and signing secrets, maintain a changelog, build a fresh signed/release APK, then separately build and report the debug APK requested by the migration deliverables.
