# Changelog

All notable changes to the Everybuy Android application are documented in this file.

When preparing a GitHub Release, move completed items from **Unreleased** into a new
version section that matches the release tag (for example `## [1.0.0]` for tag `v1.0.0`
or `1.0.0`). The workflow uses only that section when present; otherwise, it uses the
documented Git-history fallback and never includes unrelated **Unreleased** entries.

## [Unreleased]

### Added
- Native Everybuy Android client covering authentication (email/password, TOTP, session restore, password reset and invite deep links), inventory, stock movements and receiving, stocktakes, sales, customers, reports, expenses, audit and settings.
- Local NestJS development support for the Android emulator (`10.0.2.2`) and physical devices (LAN IP override), with cleartext HTTP limited to debug builds.
- GitHub Actions release pipeline that tests, signs and publishes APKs plus release notes when a GitHub Release is published for a commit on the repository default branch.

### Changed

### Fixed

### Improved
- Release builds enable R8 code shrinking, resource shrinking and ABI splits with a universal APK for broad device support.
