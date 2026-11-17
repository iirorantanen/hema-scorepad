# HEMA Scoreboard

An Android scoreboard for one bout of Historical European Martial Arts (HEMA) sparring, operated by a referee on the sideline. It is not a tournament system.

## What it does

- The display is split into a Red half and a Blue half, each showing that fencer's score.
- Tapping a half awards one point to that fencer.
- The scoreboard gives a signal for each point, so the referee can keep watching the fencers: one vibration for Red, two for Blue.
- A bout timer counts down from 3:00. Tapping it starts, pauses and resumes it. It stops at 0:00 without a signal, and points can still be awarded.
- When ten seconds remain, the scoreboard gives one long vibration and shows "Last exchange" for the rest of the bout. The signal is given once per bout, however often the bout timer is paused and resumed.
- Undo takes back the most recent point. It can be repeated back to 0–0, gives no signal and does not change the bout timer.
- A point log lists each point that still stands, with the fencer, the bout timer reading when it was awarded and the score after it.
- Nothing leaves the device. The app has no internet permission.

## Install

1. On an Android phone running Android 8.0 or newer, open the [latest release](https://github.com/iirorantanen/unnamed-hema-scoring-app/releases/latest).
2. Download `hema-scoreboard-<version>.apk` and open it.
3. Allow the browser to install apps when Android asks.

## Development

Enable the pre-commit hook once per clone. It runs `./gradlew check` (the unit tests and Android lint) and refuses the commit when that fails. The hook runs on the development machine only and triggers nothing on GitHub.

```bash
git config core.hooksPath .githooks
```

Run the unit tests:

```bash
./gradlew test
```

Build the app and install it on a connected device or emulator:

```bash
./gradlew :app:installDebug
```

Pushing a `v*` tag publishes a signed APK as a GitHub Release. See [ADR 0001](docs/adr/0001-dedicated-release-signing-key.md) for how releases are signed.

The project's vocabulary (bout, fencer, referee, signal and so on) is defined in [GLOSSARY.md](GLOSSARY.md).
