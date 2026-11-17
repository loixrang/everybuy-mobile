#!/usr/bin/env bash
# Validate a GitHub release tag and emit VERSION_NAME / VERSION_CODE for Gradle.
set -euo pipefail

TAG="${1:?tag required}"
RAW="${TAG#v}"

if [[ ! "$RAW" =~ ^([0-9]+)\.([0-9]+)(\.([0-9]+))?([-+][A-Za-z0-9.-]+)?$ ]]; then
  echo "Unsupported release tag '$TAG'." >&2
  echo "Use a SemVer-compatible tag such as v1.0.0 or 1.2.3." >&2
  exit 1
fi

MAJOR="${BASH_REMATCH[1]}"
MINOR="${BASH_REMATCH[2]}"
PATCH="${BASH_REMATCH[4]:-0}"

if (( MAJOR > 2146 )); then
  echo "Major version $MAJOR is too large for Android versionCode encoding." >&2
  exit 1
fi

# Monotonic for normal SemVer progressions: major*1_000_000 + minor*1_000 + patch
VERSION_CODE=$(( MAJOR * 1000000 + MINOR * 1000 + PATCH ))
VERSION_NAME="$RAW"

# Android versionName must be a non-empty string; keep it concise.
if (( ${#VERSION_NAME} > 100 )); then
  echo "versionName '$VERSION_NAME' exceeds 100 characters." >&2
  exit 1
fi

echo "VERSION_NAME=$VERSION_NAME"
echo "VERSION_CODE=$VERSION_CODE"
