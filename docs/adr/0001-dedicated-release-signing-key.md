# Release builds are signed with one dedicated key held in GitHub secrets

Android installs an update over an existing install only when both are signed with the same key, so release builds made on GitHub are signed with a single dedicated keystore, stored as GitHub Actions secrets with a backup kept outside GitHub. Losing the key means every device has to uninstall the app before installing the next release.

## Considered Options

- **Debug-signed on CI**: no setup, but each runner generates a fresh key, so every release would need an uninstall first.
- **Keystore committed to the repo**: no secrets to manage, but the key leaks the day the repo goes public.
