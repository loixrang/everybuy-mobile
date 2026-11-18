#!/usr/bin/env python3
"""Collect release APKs from Android Gradle Plugin output metadata."""

import json
import os
import re
import shutil
import sys
from pathlib import Path


def fail(message: str) -> None:
    print(f"::error::{message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    if len(sys.argv) != 5:
        fail(
            "Usage: collect-release-apks.py "
            "<release-tag> <version-name> <version-code> <destination-directory>"
        )

    tag, expected_version_name, version_code_arg, destination_arg = sys.argv[1:]
    if not re.fullmatch(r"v?(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)", tag):
        fail(f"Unsupported release tag for APK filenames: {tag}")
    try:
        expected_version_code = int(version_code_arg)
    except ValueError:
        fail(f"Invalid Android versionCode: {version_code_arg}")

    workspace = Path.cwd()
    destination = Path(destination_arg)
    destination.mkdir(parents=True, exist_ok=True)

    metadata_files: list[Path] = []
    for current, directories, filenames in os.walk(workspace):
        directories[:] = [
            directory
            for directory in directories
            if directory not in {".git", ".gradle", "node_modules"}
        ]
        if "output-metadata.json" in filenames and "outputs" in Path(current).parts:
            metadata_files.append(Path(current) / "output-metadata.json")

    apk_count = 0
    has_universal = False
    used_names: set[str] = set()

    for metadata_path in metadata_files:
        try:
            metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            fail(f"Cannot read AGP output metadata at {metadata_path}: {error}")

        artifact_type = metadata.get("artifactType", {}).get("type")
        if metadata.get("variantName") != "release" or artifact_type != "APK":
            continue

        for element in metadata.get("elements", []):
            if (
                element.get("versionName") != expected_version_name
                or element.get("versionCode") != expected_version_code
            ):
                fail(
                    f"APK version metadata does not match release {tag}: "
                    f"{element.get('versionName')} / {element.get('versionCode')}"
                )
            output_file = element.get("outputFile")
            if not isinstance(output_file, str) or Path(output_file).name != output_file:
                fail(f"Invalid APK outputFile in {metadata_path}")

            source = metadata_path.parent / output_file
            if not source.is_file():
                fail(f"APK listed by AGP is missing: {source}")
            if source.stat().st_size == 0:
                fail(f"APK listed by AGP is empty: {source}")

            element_type = str(element.get("type", "")).upper()
            filters = [
                str(item.get("value", ""))
                for item in element.get("filters", [])
                if item.get("value")
            ]
            if element_type == "UNIVERSAL":
                suffix = "universal"
                has_universal = True
            elif filters:
                suffix = "-".join(filters)
            else:
                fail(f"Cannot identify APK variant in {metadata_path}: {element}")

            if not re.fullmatch(r"[A-Za-z0-9._-]+", suffix):
                fail(f"Unsupported APK variant name: {suffix}")

            asset_name = f"everybuy-{tag}-{suffix}.apk"
            if asset_name in used_names:
                fail(f"Duplicate APK asset name generated: {asset_name}")
            used_names.add(asset_name)
            target = destination / asset_name
            shutil.copy2(source, target)
            print(f"Collected {source} as {target} ({source.stat().st_size} bytes)")
            apk_count += 1

    if apk_count == 0:
        fail("No release APKs were listed in Android Gradle Plugin output metadata.")
    if not has_universal:
        fail("Gradle is configured to produce a universal APK, but AGP metadata has none.")


if __name__ == "__main__":
    main()
