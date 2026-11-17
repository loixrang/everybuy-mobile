# Everybuy Android

Native Everybuy client built with Kotlin, Jetpack Compose Material 3, Retrofit/OkHttp, Coroutines/Flow, Hilt, DataStore and Navigation Compose. The app is online-only and uses the existing NestJS + Better Auth backend.

## Local development

### 1. Start the NestJS backend

From the repository root:

```bash
cd everybuy-backend
bun install   # or npm install / yarn, matching the backend docs
bun run start:dev
```

The API listens on port `3000` with a global `/api/v1` prefix (auth under `/api/v1/auth`). Keep Better Auth cookie sessions unchanged; the Android client already preserves them via OkHttp’s encrypted cookie jar.

### 2. Run an Android emulator

Start an AVD from Android Studio (**Device Manager → Play**) or:

```bash
emulator -avd <your_avd_name>
```

The emulator reaches the host machine at `10.0.2.2`, so the default debug API origin is `http://10.0.2.2:3000/` (effective API root: `http://10.0.2.2:3000/api/v1`).

### 3. Run the app from Android Studio

1. Open the `everybuy-mobile` directory in Android Studio.
2. Select the **debug** build variant (**Build → Select Build Variant → debug**).
3. Choose the running emulator (or a device) and press **Run**.

From the CLI:

```bash
./gradlew :app:installDebug
```

### 4. Physical device: override the API URL

`10.0.2.2` only works on the emulator. On a physical device, point debug builds at your development machine’s LAN IP (do not commit that IP).

In `local.properties` (gitignored):

```properties
everybuy.apiBaseUrl=http://192.168.x.x:3000/
```

Or as a Gradle property:

```bash
./gradlew :app:installDebug -Peverybuy.apiBaseUrl=http://192.168.x.x:3000/
```

Use your machine’s real LAN address, keep the trailing slash, and ensure the phone and computer are on the same network. Debug builds allow cleartext HTTP so LAN testing works; release builds do not.

### 5. Debug vs release

| Variant | Default `API_BASE_URL` (server origin) | Cleartext HTTP | How to override |
| --- | --- | --- | --- |
| **debug** | `http://10.0.2.2:3000/` | Allowed (debug `network_security_config` only) | `everybuy.apiBaseUrl` in `local.properties` or `-P` |
| **release** | `https://api.everybuy.app/` | Denied | `everybuy.apiBaseUrlRelease` |

Retrofit reads `BuildConfig.API_BASE_URL` once in `NetworkModule`; endpoint paths already include `api/v1/...`, so the effective API root is `{API_BASE_URL}api/v1`. The local debug URL never ships in release builds because each build type sets its own `buildConfigField`.

In Android Studio, switch variants under **Build → Select Build Variant**. CLI:

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

## Current scope

Email/password + TOTP authentication, encrypted session cookies, staff-profile authorization, theme preference selection, product browsing/search/filtering/details and Owner-only product create/edit forms, daily/monthly/profit overview, stock movements/receipts/adjustments, stocktakes, sales, customers, expenses, audit, and settings (profile, shop, team, catalog, suppliers) are implemented. See the parent `MIGRATION_PLAN.md` for remaining verification and release work.
