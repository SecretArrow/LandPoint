# Security Policy

## Supported versions

Only the latest tagged release receives security fixes. Older releases should
be replaced, not patched: the app is offline-first and sideloaded, so there is
no server-side component that could keep an old install safe.

## Reporting a vulnerability

Open a private security advisory on this repository (Security → Report a
vulnerability) or contact the repository owner directly. Please do not open a
public issue for anything that might be a vulnerability. Include:

- the component and the version tag it affects,
- a minimal description of how the issue can be triggered,
- what an attacker would gain.

Reports are acknowledged within a week where possible.

## Secret handling rules

The project treats the following as secrets, none of which may ever be
committed to this repository, printed in CI logs, or baked into a release:

| Secret | Where it belongs |
|---|---|
| Release keystore (`*.jks`, `*.keystore`) | Offline with the owner; in CI only as the GitHub Secret `KEYSTORE_ZIP_BASE64` (base64 of a zip locked with `KEYSTORE_PASSWORD`) |
| Keystore & key passwords | GitHub Secrets `KEYSTORE_PASSWORD`, `KEY_PASSWORD` |
| Key alias | GitHub Secret `KEY_ALIAS` |
| GitHub personal access token | GitHub Secret `GH_PAT`; the built-in `GITHUB_TOKEN` is used wherever it is enough |

The `.gitignore` blocks the common filenames (`.jks`, `.keystore`,
`key-sign-apk.zip`, `.env`, `*.pem`, `keystore.properties`). CI jobs are
granted the minimum permission they need (`contents: read`; `contents: write`
only in the release and auto-fix jobs), and every workflow step that touches a
secret passes it through an environment variable so GitHub's automatic log
masking covers it.

## Rotation procedure

Any secret that has been exposed — posted in a chat, shown in a screenshot,
leaked in a log — is burned: **rotate it the same day**. Exposure is not
disgrace; reuse is the mistake.

### GitHub personal access token

1. Revoke the exposed token: GitHub → Settings → Developer settings →
   Personal access tokens → delete it.
2. Create a new token with the minimum scopes it needs.
3. Update the `GH_PAT` secret: Settings → Secrets and variables → Actions →
   update `GH_PAT`.
4. Check the repository's audit log for actions taken with the old token.

### Release signing key

Rotating the signing key means Android sees the next release as a *different
app*: existing installs keep the old signature and must stay on it, or be
re-installed (losing local data unless a backup was made). Rotate only for a
real compromise, and say so in the release notes.

1. Generate a new keystore **on an offline machine**:

   ```sh
   keytool -genkeypair -v -keystore landpoint-release.jks \
     -storetype PKCS12 -alias landpoint -keyalg RSA -keysize 2048 \
     -validity 10950 -storepass '<password>' -keypass '<password>' \
     -dname "CN=LandPoint, OU=LandPoint, O=SecretArrow, L=Jakarta, C=ID"
   ```

2. Zip it behind a password:

   ```sh
   zip -P '<zip password>' key-sign-apk.zip landpoint-release.jks
   base64 -w 0 key-sign-apk.zip
   ```

3. Update the four secrets `KEYSTORE_ZIP_BASE64`, `KEYSTORE_PASSWORD`,
   `KEY_ALIAS`, `KEY_PASSWORD` in Settings → Secrets and variables → Actions.
4. Delete the local zip and keystore, keeping the offline original only.
5. Tag the next release; verify the APK in the release installs beside (not
   over) the previous one.

### CI/CD workflows

If a workflow file is found modifying itself, exfiltrating secrets, or
downloading from an unpinned URL:

1. Disable Actions: Settings → Actions → General → disable.
2. Audit the workflow runs for what the malicious version executed.
3. Revert the workflow from the last known-good commit; re-pin every action
   to a full commit SHA (already the policy here).
4. Rotate every secret above regardless — workflow context has read access
   to all of them.
5. Re-enable Actions.

## Signing key custody

The signing key exists in exactly two places: the owner's offline storage, and
the GitHub Secrets of this repository. It is never committed, never attached to
a release, and never present on a build machine outside a CI run. If GitHub
account access itself is compromised, rotate both the account credentials and
the signing key.
