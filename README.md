![HEMA Scorepad: the head of Fiore the Boar, the mascot, between a Red score and a Blue score drawn as on the display](docs/brand/readme-header.svg)

# HEMA Scorepad

HEMA Scorepad is an Android scoreboard for one bout of Historical European Martial Arts (HEMA) sparring, operated by a referee on the sideline. It is not a tournament system.

## What it does

- The display is split into a Red half and a Blue half, each showing that fencer's score.
- Tapping a half awards one point to that fencer.
- The scoreboard gives a signal for each point, so the referee can keep watching the fencers: one vibration for Red, two for Blue. Signals given close together play one after another. At most three wait behind the one playing; a further signal takes the place of the oldest one waiting.
- Signals play with the device on silent and with Android's touch feedback switched off. They do not play while vibration, or alarm vibration, is switched off in the Android settings.
- A bout timer counts down from the bout length, 3:00 by default. Tapping it starts, pauses and resumes it. It stops at 0:00 without a signal, and points can still be awarded.
- When ten seconds remain, the scoreboard gives one long vibration and shows "Last exchange" for the rest of the bout. The signal is given once per bout, however often the bout timer is paused and resumed.
- The point cap is 10 by default. When a fencer reaches it, the scoreboard gives three medium vibrations after the point's own signal and the bout timer stops. Taps for that fencer are then ignored. The other fencer can still be awarded points up to the point cap, so 10–10 is possible, without a second end-of-bout signal. The scoreboard never declares a winner.
- Undo takes back the most recent point. It can be repeated back to 0–0, gives no signal and does not change the bout timer. Undoing the point that reached the point cap lets that fencer be awarded again; the bout timer stays paused until the referee resumes it.
- A point log lists each point that still stands, with the fencer, the bout timer reading when it was awarded and the score after it. With the bout timer off, the entries have no reading.
- Swap sides exchanges which side of the display the Red half and the Blue half are on, so the display matches where the fencers stand. Scores, the point log and the bout timer are untouched, so it is safe mid-bout.
- The final score stays on the display until the referee starts a new bout. New bout asks for confirmation, then clears the score, the point log and "Last exchange", and returns the bout timer to a paused full bout length. The side arrangement and the settings are kept.
- The settings control (⚙) opens the bout length and the point cap. The bout length changes in steps of 30 seconds, down to 0:30, and the point cap in steps of one, down to 1. Each can be switched off on its own and keeps its value while off. With the bout timer off there is no countdown, no "Last exchange" and no last-exchange signal. With the point cap off, scores are not limited and there is no end-of-bout signal. With both off the scoreboard is a plain counter.
- Settings are remembered between bouts and between app launches. A change made before the bout has begun, with no points standing and the bout timer not yet run, applies to that bout. A change made during a bout applies from the next new bout, so the bout in progress keeps the bout length and point cap it started with. If undo then takes back every point of a bout whose bout timer has not run, the change applies to that bout.
- An interruption does not lose the bout. When the app leaves the foreground, for a phone call for example, the bout timer pauses, so the time away is not counted. Reopening the app brings the bout back paused, with its score, point log, bout timer reading, "Last exchange" and side arrangement, also after Android has closed the app in the background. A bout brought back gives no signal again: not the last-exchange signal, and not the end-of-bout signal.
- The screen stays awake while the app is in the foreground, so the device does not lock while the referee watches the fencers.
- The bout is saved on the device only. Nothing leaves the device. The app has no internet permission.

## Install

1. On an Android phone running Android 8.0 or newer, open the [latest release](https://github.com/iirorantanen/hema-scorepad/releases/latest).
2. Download `hema-scorepad-<version>.apk` and open it.
3. Allow the browser to install apps when Android asks.

Android lists the app as "Scorepad". The full name does not fit under a launcher icon. The icon is the head of Fiore the Boar, the mascot, on black.

## Development

Enable the pre-commit hook once per clone. It runs `./gradlew check` (the bout tests, on the JVM and in JavaScript, the measure of which bout rules those tests run, the Kotlin format check, Android lint and the comparison of the artwork with its master) and refuses the commit when that fails. The hook runs on the development machine only. GitHub runs the same check on every push to `main`, on every pull request and before a release build.

```bash
git config core.hooksPath .githooks
```

Format the Kotlin code. The check names the file and line of anything not in this format, import order included:

```bash
./gradlew ktlintFormat
```

Run the bout tests on the JVM and in JavaScript. Gradle downloads the Node.js that runs the JavaScript tests:

```bash
./gradlew :bout:allTests
```

Measure which bout rules the tests run. The check fails when a line or a branch of the bout module is run by no bout test, and names the file and line. The measure comes from the JVM run of the bout tests, and the app module is not measured:

```bash
./gradlew :bout:boutCoverage
```

A line named as run in part has code no test reaches, most often one side of a branch. The HTML report shows which:

```bash
./gradlew :bout:koverHtmlReport
```

Build the app and install it on a connected device or emulator:

```bash
./gradlew :app:installDebug
```

Serve the browser version of the scoreboard on the development machine. The command prints the address, `http://localhost:8080/` while that port is free, and a second address that a phone on the same network can open. It keeps running and builds the page again after a change to the code:

```bash
./gradlew :browser:jsBrowserDevelopmentRun --continuous
```

Pushing a `v*` tag publishes a signed APK as a GitHub Release. See [ADR 0001](docs/adr/0001-dedicated-release-signing-key.md) for how releases are signed.

The artwork is in [`docs/brand/`](docs/brand/):

- `fiore.svg` is the master of Fiore the Boar: the head in colour and, under the id `fiore-mono`, its one-colour form.
- `icon.svg` is the icon, the head on black.
- `readme-header.svg` is the picture at the top of this file.
- `social-preview.png` is the picture GitHub shows with a link to the repository. It is uploaded by hand in the repository settings and is rendered from `social-preview.svg`.

The icon, the two pictures and the two layers of the Android launcher icon in `app/src/main/res/drawable/` each hold a copy of the master's shapes. After a change to the master, write them again. The command needs Python 3 and the JDK:

```bash
python3 scripts/brand/derive.py
```

The check fails when one of those files differs from what the command writes, and names the file. The one-colour form is computed by `scripts/brand/Mono.java`, where the shapes it is made of are typed a second time: a change to the shape of the head is repeated there.

Render the social preview again after a change to its SVG. The digits and the name take the default font of the machine that renders it:

```bash
google-chrome --headless --hide-scrollbars --window-size=1280,640 --screenshot=docs/brand/social-preview.png docs/brand/social-preview.svg
```

Write a page that shows the icon and the themed icon at 192 px and 48 px, in a round and a rounded-square mask:

```bash
python3 scripts/brand/sheet.py build/icon-sheet.html
```

The project's vocabulary (bout, fencer, referee, signal and so on) is defined in [GLOSSARY.md](GLOSSARY.md).
