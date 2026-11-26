# F-Droid publishes the APK signed with the release key, through a reproducible build

F-Droid by default builds an app from source and signs it with a key of its own. Android installs an update only over an install signed with the same key ([ADR 0001](0001-dedicated-release-signing-key.md)), so an F-Droid-signed build and the APK on GitHub Releases would not update each other. F-Droid therefore publishes the APK from GitHub Releases, signed with the release key: it rebuilds each tagged release from source and publishes the signed APK only when the rebuild matches it. A referee moves between F-Droid and GitHub Releases without an uninstall.

## Considered Options

- **F-Droid's own key**: nothing to keep reproducible, but a referee who moves between F-Droid and GitHub Releases has to uninstall first and loses the saved bout and the settings.

## Consequences

- A release whose rebuild does not match is not published on F-Droid. F-Droid stays on the release before it until a later release matches.
- GitHub Releases stays an install channel: F-Droid takes the signed APK from it.
- The rebuild has to carry the version code and version name of the published APK, and F-Droid reads both from the build file without running Gradle. Both are literals in the build file, not inputs of the release workflow.
- The choice is permanent once a referee has installed from F-Droid: a build signed with another key does not update that install.
