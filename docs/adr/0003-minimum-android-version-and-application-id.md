# The app needs Android 8.0 and keeps the application ID `io.github.iirorantanen.hemascoreboard`

The app installs on Android 8.0 (API 26) and newer. The PRD in issue #1 leaves the minimum Android version to the builder and names the project template default. The version follows vibration instead of the template: API 26 is the first level with `VibrationEffect`, so a signal reaches vibration through one code path.

The application ID is `io.github.iirorantanen.hemascoreboard`: the repository owner's GitHub Pages domain reversed, followed by the app's name. Android treats a build with a different application ID as a different app, and every release since v0.1 carries this one, so the ID stays.

## Considered Options

Minimum Android version:

- **API 24 (Android 7.0)**: adds Android 7.x devices, but vibration needs a second, legacy code path next to `VibrationEffect`.

Application ID:

- **`com.example.*`, the project template default**: nothing to choose, but every template project shares the prefix and Google Play rejects it.
- **An ID built from the repository name**: `unnamed-hema-scoring-app` is a placeholder, and the ID has to outlive a rename of the repository.

## Consequences

- A phone on Android 7.1 or older cannot install the app. [ADR 0002](0002-shared-bout-rules-separate-browser-ui.md) decides a browser version for phones that cannot install the APK.
- A build with a different application ID does not update the released app. It installs next to it as a second app, without the bout and the settings the released app saved. The released app stays on the device until it is uninstalled.
