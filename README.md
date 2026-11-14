# Everybuy Android

Native Everybuy client built with Kotlin, Jetpack Compose Material 3, Retrofit/OkHttp, Coroutines/Flow, Hilt, DataStore and Navigation Compose. The app is online-only and uses the existing NestJS + Better Auth backend.

## Configure the API

Set `everybuy.apiBaseUrl` in `local.properties` or as a Gradle property. Include the trailing slash. The default debug URL is `http://10.0.2.2:3000/` for an Android emulator reaching a backend on the host machine. Physical devices need a reachable host URL. Release uses `everybuy.apiBaseUrlRelease` and defaults to `https://api.everybuy.app/`.

The native client sends `Origin: everybuy://app` for Better Auth CSRF validation. Add `everybuy://app` to the deployment's `BETTER_AUTH_TRUSTED_ORIGINS` setting. This is an environment configuration change; the Android client does not use a bearer token or store the account password.

## Build and test

From this directory:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Build and test status is tracked in the parent `MIGRATION_PLAN.md`; the artifact currently present there may be stale until a successful build completes.

## Current scope

Email/password + TOTP authentication, encrypted session cookies, staff-profile authorization, theme preference selection, product browsing/search/filtering/details and Owner-only product create/edit forms, and the daily overview are implemented in source. The remaining frontend routes and workflows are listed in the parent `MIGRATION_PLAN.md` and are still being migrated.
