#!/usr/bin/env bash
# Extract changelog notes for a release version from CHANGELOG.md.
# Uses the matching ## [version] section only (not Unreleased).
# Fallback: git log between the previous release tag and the current tag.
set -euo pipefail

VERSION="${1:?version required (e.g. 1.0.0)}"
TAG="${2:?tag required (e.g. v1.0.0)}"
OUT_FILE="${3:?output file required}"
CHANGELOG_FILE="${4:-CHANGELOG.md}"
PREVIOUS_RELEASE_TAG="${5:-}"

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
  NOTES="$(extract_section "$CHANGELOG_FILE" "$VERSION" || true)"
  if [[ -z "${NOTES//[[:space:]]/}" ]]; then
    NOTES="$(extract_section "$CHANGELOG_FILE" "v$VERSION" || true)"
  fi
  if [[ -n "${NOTES//[[:space:]]/}" ]]; then
    SOURCE="CHANGELOG.md section [$VERSION]"
  fi
fi

if [[ -z "${NOTES//[[:space:]]/}" ]]; then
  echo "No CHANGELOG.md section found for [$VERSION]. Falling back to git history between tags." >&2
  echo "Tip: move ## [Unreleased] entries into ## [$VERSION] before publishing." >&2
  if git rev-parse -q --verify "${TAG}^{commit}" >/dev/null 2>&1; then
    if [[ -n "$PREVIOUS_RELEASE_TAG" ]]; then
      if ! git rev-parse -q --verify "${PREVIOUS_RELEASE_TAG}^{commit}" >/dev/null 2>&1; then
        echo "Previous published release tag '$PREVIOUS_RELEASE_TAG' is not available in this checkout." >&2
        exit 1
      fi
      RANGE="${PREVIOUS_RELEASE_TAG}..${TAG}"
    else
      RANGE="$TAG"
    fi
    NOTES="$(git log --pretty=format:'- %s' "$RANGE" 2>/dev/null || true)"
  fi
  if [[ -z "${NOTES//[[:space:]]/}" ]]; then
    NOTES="No changelog entries were found for this release. Add a ## [$VERSION] section to CHANGELOG.md."
  else
    NOTES=$'### Changed\n'"$NOTES"
  fi
  SOURCE="git history"
fi

{
  echo "# Everybuy Android $TAG"
  echo
  echo "$NOTES"
  echo
} > "$OUT_FILE"

echo "Wrote changelog notes to $OUT_FILE (source: $SOURCE)"
