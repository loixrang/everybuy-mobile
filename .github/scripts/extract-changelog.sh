#!/usr/bin/env bash
# Extract release notes from CHANGELOG.md, with a Git-history fallback.
set -euo pipefail

VERSION="${1:?version required (e.g. 1.0.0)}"
TAG="${2:?tag required (e.g. v1.0.0)}"
OUT_FILE="${3:?output file required}"
CHANGELOG_FILE="${4:-CHANGELOG.md}"
PREVIOUS_RELEASE_TAG="${5:-}"
PUBLISHED_AT="${6:?published_at timestamp required}"
RELEASE_DATE="${PUBLISHED_AT%%T*}"

if [[ ! "$RELEASE_DATE" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "Invalid GitHub release date '$PUBLISHED_AT'." >&2
  exit 1
fi
if ! NORMALIZED_RELEASE_DATE="$(date -u -d "$RELEASE_DATE" +%F 2>/dev/null)" ||
    [[ "$NORMALIZED_RELEASE_DATE" != "$RELEASE_DATE" ]]; then
  echo "Invalid GitHub release date '$PUBLISHED_AT'." >&2
  exit 1
fi

extract_section() {
  local file="$1"
  local heading="$2"
  awk -v heading="$heading" '
    BEGIN { grab=0 }
    /^## \[/{
      if ($0 == "## [" heading "]") { grab=1; next }
      else if (grab) { exit }
    }
    grab { print }
  ' "$file" | sed 's/[[:space:]]*$//' | awk 'BEGIN{skip=1} NF{skip=0} !skip{print}'
}

NOTES=""
SOURCE="missing"

if [[ -f "$CHANGELOG_FILE" ]]; then
  NOTES="$(extract_section "$CHANGELOG_FILE" "$VERSION")"
  if [[ -z "${NOTES//[[:space:]]/}" ]]; then
    NOTES="$(extract_section "$CHANGELOG_FILE" "v$VERSION")"
  fi
  if grep -Eq '^[[:space:]]*[-*][[:space:]]+[^[:space:]]' <<< "$NOTES"; then
    SOURCE="CHANGELOG.md section [$VERSION]"
  else
    NOTES=""
  fi
fi

if [[ -z "${NOTES//[[:space:]]/}" ]]; then
  echo "No CHANGELOG.md section found for [$VERSION]. Falling back to Git history between published releases." >&2
  echo "Tip: move ## [Unreleased] entries into ## [$VERSION] before publishing." >&2
  if ! git rev-parse -q --verify "${TAG}^{commit}" >/dev/null 2>&1; then
    echo "Release tag '$TAG' is not available in this checkout; cannot build the changelog." >&2
    exit 1
  fi
  if [[ -n "$PREVIOUS_RELEASE_TAG" ]]; then
    if ! git rev-parse -q --verify "${PREVIOUS_RELEASE_TAG}^{commit}" >/dev/null 2>&1; then
      echo "Previous published release tag '$PREVIOUS_RELEASE_TAG' is not available in this checkout." >&2
      exit 1
    fi
    RANGE="${PREVIOUS_RELEASE_TAG}..${TAG}"
  else
    RANGE="$TAG"
  fi
  if ! NOTES="$(git log --pretty=format:'- %s' "$RANGE")"; then
    echo "Could not read Git history for release range '$RANGE'." >&2
    exit 1
  fi
  if [[ -z "${NOTES//[[:space:]]/}" ]]; then
    echo "No release changelog entries were found for '$TAG'. Add a ## [$VERSION] section to CHANGELOG.md." >&2
    exit 1
  fi
  NOTES=$'### Changes\n'"$NOTES"
  SOURCE="git history"
fi

{
  echo "# Everybuy Android $TAG"
  echo
  echo "**Release date:** $RELEASE_DATE"
  echo
  echo "$NOTES"
  echo
} > "$OUT_FILE"

echo "Wrote changelog notes to $OUT_FILE (source: $SOURCE)"
