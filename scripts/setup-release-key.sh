#!/usr/bin/env bash
# One-time setup, run by the maintainer: generates the release signing key and
# uploads it as the repository secrets .github/workflows/release.yml reads (ADR 0001).
set -euo pipefail
cd "$(dirname "$0")/.."

keystore=release.jks # covered by .gitignore
# Not a plain word: GitHub masks every secret's value wherever it appears in a workflow log.
key_alias=hema-scoreboard-release-key

if [ -e "$keystore" ]; then
  echo "$keystore already exists. Refusing to overwrite what may be the only copy of the release key." >&2
  echo "If it has never signed a published release, delete it and run this script again." >&2
  exit 1
fi

# Fail before generating anything if gh is missing, signed out, or lacks access to secrets.
gh secret list >/dev/null

# One password for both store and key: keytool's PKCS12 keystores do not support a separate key password.
read -rsp "Choose a keystore password (at least 6 characters): " password; echo
read -rsp "Repeat it: " repeated; echo
if [ "$password" != "$repeated" ]; then
  echo "The passwords do not match." >&2
  exit 1
fi
if [ "${#password}" -lt 6 ]; then
  echo "The password is shorter than 6 characters." >&2
  exit 1
fi

# The password goes to keytool through the environment and to gh through stdin, never as an argument.
RELEASE_PASSWORD="$password" keytool -genkeypair \
  -keystore "$keystore" -storepass:env RELEASE_PASSWORD \
  -alias "$key_alias" -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=HEMA Scoreboard"

base64 < "$keystore" | gh secret set RELEASE_KEYSTORE_BASE64
printf %s "$password" | gh secret set RELEASE_KEYSTORE_PASSWORD
printf %s "$key_alias" | gh secret set RELEASE_KEY_ALIAS
printf %s "$password" | gh secret set RELEASE_KEY_PASSWORD

cat <<EOF

Done. Now store $keystore and its password in your password manager.
Repository secrets cannot be read back, and without the key every device has to
uninstall the app before it can install the next release.
EOF
