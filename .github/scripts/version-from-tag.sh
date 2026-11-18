#!/usr/bin/env bash
# Validate a GitHub release tag and emit VERSION_NAME / VERSION_CODE for Gradle.
set -euo pipefail

TAG="${1:?tag required}"
RAW="${TAG#v}"

if [[ ! "$RAW" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
  echo "Unsupported release tag '$TAG'." >&2
  echo "Use a stable SemVer tag such as v1.0.0 or 1.2.3 (no prerelease/build suffix)." >&2
  exit 1
fi

MAJOR="${BASH_REMATCH[1]}"
MINOR="${BASH_REMATCH[2]}"
PATCH="${BASH_REMATCH[3]}"

if (( ${#MAJOR} > 4 || ${#MINOR} > 3 || ${#PATCH} > 3 )); then
  echo "Version components exceed the supported versionCode range." >&2
  exit 1
fi

# Component bounds preserve SemVer ordering in the encoded Android versionCode.
if (( MINOR > 999 || PATCH > 999 )); then
  echo "Minor and patch versions must be between 0 and 999." >&2
  exit 1
fi

VERSION_CODE=$(( MAJOR * 1000000 + MINOR * 1000 + PATCH ))
VERSION_NAME="$RAW"

if (( VERSION_CODE < 1 || VERSION_CODE > 2100000000 )); then
  echo "Version '$RAW' produces an Android versionCode outside 1..2100000000." >&2
  exit 1
fi

echo "VERSION_NAME=$VERSION_NAME"
echo "VERSION_CODE=$VERSION_CODE"
