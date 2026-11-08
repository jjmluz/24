#!/usr/bin/env bash
# Creates the release signing key and stores it in the GitHub repo's Actions secrets,
# so CI signs every release with the same key and installed copies can update in place.
#
# Run once, from the repo root, with the GitHub CLI logged in:
#   tools/setup_release_signing.sh
#
# The key and its password are kept in ~/.android/24-release/. Back that folder up:
# if the key is lost, published APKs can never be updated; everyone would have to uninstall first.
set -euo pipefail

REPO="${REPO:-jjmluz/24}"
DIR="$HOME/.android/24-release"
KEYSTORE="$DIR/24-release.keystore"
ALIAS="24"
KEYTOOL="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}/bin/keytool"

if [[ -e "$KEYSTORE" ]]; then
  echo "$KEYSTORE already exists; not overwriting it." >&2
  exit 1
fi

mkdir -p "$DIR"
chmod 700 "$DIR"
PASSWORD="$(openssl rand -base64 48 | tr -d '/+=\n' | cut -c1-32)"

"$KEYTOOL" -genkeypair -keystore "$KEYSTORE" -storetype PKCS12 -alias "$ALIAS" \
  -keyalg RSA -keysize 4096 -validity 18250 \
  -storepass "$PASSWORD" -keypass "$PASSWORD" -dname "CN=24"

cat > "$DIR/README.txt" <<EOF
Release signing key for the "24" Android TV game (github.com/$REPO).
Back this folder up, e.g. in a password manager. If it's lost, published APKs can
never be updated in place: everyone would have to uninstall and reinstall.

Keystore: 24-release.keystore (PKCS12)
Alias:    $ALIAS
Store and key password: $PASSWORD
EOF
chmod 600 "$KEYSTORE" "$DIR/README.txt"

base64 < "$KEYSTORE" | tr -d '\n' | gh secret set SIGNING_KEYSTORE_BASE64 --repo "$REPO"
printf %s "$PASSWORD" | gh secret set SIGNING_STORE_PASSWORD --repo "$REPO"
printf %s "$PASSWORD" | gh secret set SIGNING_KEY_PASSWORD --repo "$REPO"
printf %s "$ALIAS" | gh secret set SIGNING_KEY_ALIAS --repo "$REPO"

echo "Done. Key and password are in $DIR; back that folder up."
gh secret list --repo "$REPO"
