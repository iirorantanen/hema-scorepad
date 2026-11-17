# HEMA Scoreboard

An Android scoreboard for one bout of Historical European Martial Arts (HEMA) sparring, operated by a referee on the sideline. It is not a tournament system.

## What it does

- The display is split into a Red half and a Blue half, each showing that fencer's score.
- Tapping a half awards one point to that fencer.
- The scoreboard gives a signal for each point, so the referee can keep watching the fencers: one vibration for Red, two for Blue. Signals given close together play one after another.
- A bout timer counts down from the bout length, 3:00 by default. Tapping it starts, pauses and resumes it. It stops at 0:00 without a signal, and points can still be awarded.
- When ten seconds remain, the scoreboard gives one long vibration and shows "Last exchange" for the rest of the bout. The signal is given once per bout, however often the bout timer is paused and resumed.
- The point cap is 10 by default. When a fencer reaches it, the scoreboard gives three medium vibrations after the point's own signal and the bout timer stops. Taps for that fencer are then ignored. The other fencer can still be awarded points up to the point cap, so 10–10 is possible, without a second end-of-bout signal. The scoreboard never declares a winner.
- Undo takes back the most recent point. It can be repeated back to 0–0, gives no signal and does not change the bout timer. Undoing the point that reached the point cap lets that fencer be awarded again; the bout timer stays paused until the referee resumes it.
- A point log lists each point that still stands, with the fencer, the bout timer reading when it was awarded and the score after it. With the bout timer off, the entries have no reading.
- Swap sides exchanges which half shows Red and which shows Blue, so the display matches where the fencers stand. Scores, the point log and the bout timer are untouched, so it is safe mid-bout.
- The final score stays on the display until the referee starts a new bout. New bout asks for confirmation, then clears the score, the point log and "Last exchange", and returns the bout timer to a paused full bout length. The side arrangement and the settings are kept.
- The settings control (⚙) opens the bout length and the point cap. The bout length changes in steps of 30 seconds, down to 0:30, and the point cap in steps of one, down to 1. Each can be switched off on its own and keeps its value while off. With the bout timer off there is no countdown, no "Last exchange" and no last-exchange signal. With the point cap off, scores are not limited and there is no end-of-bout signal. With both off the scoreboard is a plain counter.
- Settings are remembered between bouts and between app launches. A change made before the bout has begun, with no points standing and the bout timer not yet run, applies to that bout. A change made during a bout applies from the next new bout, so the bout in progress keeps the bout length and point cap it started with.
- Nothing leaves the device. The app has no internet permission.

## Install

1. On an Android phone running Android 8.0 or newer, open the [latest release](https://github.com/iirorantanen/unnamed-hema-scoring-app/releases/latest).
2. Download `hema-scoreboard-<version>.apk` and open it.
3. Allow the browser to install apps when Android asks.

## Development

Enable the pre-commit hook once per clone. It runs `./gradlew check` (the unit tests and Android lint) and refuses the commit when that fails. The hook runs on the development machine only. GitHub runs the same check on every push to `main`, on every pull request and before a release build.

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
