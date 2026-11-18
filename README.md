# Everybuy Android

Native Everybuy client built with Kotlin, Jetpack Compose Material 3, Retrofit/OkHttp, Coroutines/Flow, Hilt, DataStore and Navigation Compose. The app is online-only and uses the existing NestJS + Better Auth backend.

## API configuration

Every build variant uses the deployed API base URL `https://api.everybuy.loixrang.com/api/v1/`. It is defined once in `app/build.gradle.kts` and inherited by both debug and release; it is public configuration, not a secret. Retrofit endpoint paths are relative to that `/api/v1/` root, including authentication and session requests. The Android app does not support local backend overrides and does not read a backend URL from `local.properties`.

The shared network security configuration denies cleartext traffic for all variants. The deployed domain uses HTTPS with Android system trust anchors.

## Local development

Open the `everybuy-mobile` directory in Android Studio, select the **debug** build variant, choose an emulator or device, and run the app. Debug and release connect to the same deployed backend.

CLI:

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

## Auth / Better Auth

The native client sends `Origin: everybuy://app` for Better Auth CSRF validation. Add `everybuy://app` to the deployment’s `BETTER_AUTH_TRUSTED_ORIGINS`. This is environment configuration only; the Android client does not use a bearer token or store the account password.

## Build and test

From this directory:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Publishing releases

The GitHub Actions workflow runs only for the `published` release event. Pushes, pull requests, tag pushes, draft releases, and release edits do not start it. It reads the repository’s configured default branch from the release event and verifies that the tag commit is reachable from that branch before tests or builds.

### Configure signing with the existing keystore

Use the existing production keystore. Do not generate a replacement: users can install updates only when each release uses the same signing identity. Keep the keystore and its passwords in secure, backed-up storage; never commit them.

To list the aliases and certificate information in the keystore, run this locally and enter its password when prompted:

```bash
keytool -list -v -keystore /secure/path/to/existing-keystore.jks
```

The output includes the key alias. Keystore and key passwords cannot be recovered from the keystore; obtain the existing values from secure records or the keystore maintainer. If the key password is the same as the keystore password, use that same value for both secrets.

Convert the existing keystore to a single Base64 line on a trusted machine:

```bash
base64 < /secure/path/to/existing-keystore.jks | tr -d '\n'
```

In the GitHub repository, open **Settings → Secrets and variables → Actions** and add these repository secrets. Do not paste their values into source files, commits, or workflow logs.

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The single-line Base64 output for the keystore |
| `ANDROID_KEYSTORE_PASSWORD` | The existing keystore password |
| `ANDROID_KEY_ALIAS` | The existing key alias shown by `keytool -list` |
| `ANDROID_KEY_PASSWORD` | The existing key password, or the same value as `ANDROID_KEYSTORE_PASSWORD` when they match |

The workflow validates all four secrets, decodes the keystore to a permission-restricted temporary file, and removes it even when a later step fails. It derives the expected certificate fingerprint from the configured alias and refuses to publish APKs signed by a different identity. CI uses the same fixed production API configuration from `app/build.gradle.kts`; the backend URL is not a GitHub secret or workflow variable. CI obtains the Android SDK from the setup action and does not create or depend on `local.properties`.

### Prepare and publish a release

1. Keep `CHANGELOG.md` current during development. Before releasing, move the included user-facing changes from `## [Unreleased]` into a version section such as `## [1.2.3]`, then start a fresh `## [Unreleased]` section. Use concise entries under `Added`, `Changed`, `Fixed`, or `Improved`.
2. Merge the release commit into the repository’s configured default branch (shown in **Settings → Branches**).
3. From that branch, create and push a stable semantic-version tag:

   ```bash
   DEFAULT_BRANCH="$(git remote show origin | sed -n 's/^  HEAD branch: //p')"
   test -n "$DEFAULT_BRANCH"
   git switch "$DEFAULT_BRANCH"
   git pull --ff-only origin "$DEFAULT_BRANCH"
   git tag -a v1.2.3 -m "Everybuy v1.2.3"
   git push origin v1.2.3
   ```

   Supported tags are `MAJOR.MINOR.PATCH` (optionally prefixed with `v`), without prerelease/build suffixes. The workflow maps the tag deterministically to `versionName` and `versionCode`, and checks that the code exceeds every previously published stable release.
4. On GitHub, create a Release using that existing tag and publish it. Drafts and edits do not run the pipeline.

The workflow runs `testDebugUnitTest`, assembles the configured release variant, collects APKs from Android Gradle Plugin output metadata, and validates every APK with `apksigner` against the existing keystore’s certificate fingerprint. Current outputs are universal, ARM64, ARMv7, x86_64, and x86 APKs; filenames use the actual variants recorded by Gradle. It then uploads the versioned APKs and `CHANGELOG-v1.2.3.md` to that same release and updates its description with the generated notes. The changelog uses the matching version section when present; otherwise, the documented fallback lists Git commit subjects since the preceding published release (or all history for the first release), and fails if there are no changes to report. Its date comes from the GitHub release publication timestamp. Users can download the assets from the release’s **Assets** section.

## Current scope

Email/password + TOTP authentication, encrypted session cookies, staff-profile authorization, theme preference selection, product browsing/search/filtering/details and Owner-only product create/edit forms, daily/monthly/profit overview, stock movements/receipts/adjustments, stocktakes, sales, customers, expenses, audit, and settings (profile, shop, team, catalog, suppliers) are implemented. See the parent `MIGRATION_PLAN.md` for remaining verification and release work.
