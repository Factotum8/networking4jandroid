#!/usr/bin/env bash
# Builds the upload-key-signed release bundle (.aab) for Google Play.
# The keystore password is resolved from 1Password by `op run` and passed to
# the build container by name only (never on the command line or on disk).
# The host has no JDK, so Gradle runs in the n4j-android-build image.
set -euo pipefail

cd "$(dirname "$0")/.."

KEYS_DIR="${ANDROID_KEYS_DIR:-$HOME/.android-keys}"
KEYSTORE_NAME="networking4jandroid-upload.jks"

# release.env is gitignored, so a fresh clone has to recreate it.
if [[ ! -f release.env ]]; then
    echo "release.env not found in $PWD (it is gitignored, not part of the repo)." >&2
    echo "Create it with the 1Password reference to the keystore password (a reference, not the secret):" >&2
    echo "" >&2
    echo "  echo 'ANDROID_UPLOAD_KEYSTORE_PASSWORD=\"op://networking4j/networking4jandroid upload keystore/password\"' > release.env" >&2
    exit 1
fi

if [[ ! -f "$KEYS_DIR/$KEYSTORE_NAME" ]]; then
    echo "Upload keystore not found: $KEYS_DIR/$KEYSTORE_NAME" >&2
    echo "Restore it from 1Password (vault networking4j, document \"networking4jandroid upload keystore (.jks)\")." >&2
    exit 1
fi

op run --env-file=release.env -- \
    docker run --rm --platform linux/amd64 \
        -e ANDROID_UPLOAD_KEYSTORE="/keys/$KEYSTORE_NAME" \
        -e ANDROID_UPLOAD_KEYSTORE_PASSWORD \
        -v "$KEYS_DIR":/keys:ro \
        -v "$PWD":/project \
        -v n4j-gradle-cache-amd64:/root/.gradle \
        -w /project \
        n4j-android-build:amd64 \
        ./gradlew --no-daemon bundleRelease

echo "Signed bundle: app/build/outputs/bundle/release/app-release.aab"
