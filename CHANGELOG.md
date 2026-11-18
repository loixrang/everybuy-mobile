# Changelog

All notable changes to the Everybuy Android application are documented in this file.

When preparing a GitHub Release, move completed items from **Unreleased** into a new
version section that matches the release tag (for example `## [1.0.0]` for tag `v1.0.0`
or `1.0.0`). The release workflow publishes only that version’s section.

## [Unreleased]

### Added
- Native Everybuy Android client covering authentication (email/password, TOTP, session restore, password reset and invite deep links), inventory, stock movements and receiving, stocktakes, sales, customers, reports, expenses, audit and settings.
- Local NestJS development support for the Android emulator (`10.0.2.2`) and physical devices (LAN IP override), with cleartext HTTP limited to debug builds.
- GitHub Actions release pipeline that tests, builds, signs and publishes release APKs plus a release changelog when a GitHub Release targeting `master` is published.

### Changed

### Fixed

### Improved
- Release builds enable R8 code shrinking, resource shrinking and ABI splits with a universal APK for broad device support.
