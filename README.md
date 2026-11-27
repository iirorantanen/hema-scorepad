![HEMA Scorepad: the head of Fiore the Boar, the mascot, between a Red score and a Blue score drawn as on the display](docs/brand/readme-header.svg)

# HEMA Scorepad

HEMA Scorepad is a scoreboard for one bout of Historical European Martial Arts (HEMA) sparring, operated by a referee on the sideline. It comes as an Android app and as a browser version, which opens at a web address on an iPhone or an Android phone. It is not a tournament system.

## What it does

This list describes the Android app. The browser version follows the same bout rules and has the same display and the same controls. [Browser version](#browser-version) lists where it differs.

- The display is split into a Red half and a Blue half, each showing that fencer's score.
- Tapping a half awards one point to that fencer. Touching and holding a half deducts one point from that fencer instead, down to 0; a hold awards nothing, and a hold at 0 does nothing.
- The scoreboard gives a signal for each point, so the referee can keep watching the fencers: one vibration for Red, two for Blue, three for a deduction from either fencer. Signals given close together play one after another. At most three wait behind the one playing; a further signal takes the place of the oldest one waiting.
- Signals play with the device on silent and with Android's touch feedback switched off. They do not play while vibration, or alarm vibration, is switched off in the Android settings.
- A bout timer counts down from the bout length, 3:00 by default. Tapping it starts, pauses and resumes it. It stops at 0:00 without a signal, and points can still be awarded.
- When ten seconds remain, the scoreboard gives one long vibration and shows "Last exchange" for the rest of the bout. The signal is given once per bout, however often the bout timer is paused and resumed.
- The point cap is 10 by default. When a fencer reaches it, the scoreboard gives three medium vibrations after the point's own signal and the bout timer stops. Taps for that fencer are then ignored. The other fencer can still be awarded points up to the point cap, so 10–10 is possible, without a second end-of-bout signal. The scoreboard never declares a winner.
- Deducting from a fencer at the point cap leaves the bout as undo does: that fencer can be awarded again, and the bout timer stays paused until the referee resumes it.
- Undo takes back the most recent entry in the point log, a point or a deduction. It can be repeated until the point log is empty, gives no signal and does not change the bout timer. Undoing the point that reached the point cap lets that fencer be awarded again; the bout timer stays paused until the referee resumes it.
- A point log lists each point and each deduction that still stands, with the fencer, the bout timer reading at that moment and the score after it. A deduction is marked "−1". With the bout timer off, the entries have no reading.
- Swap sides exchanges which side of the display the Red half and the Blue half are on, so the display matches where the fencers stand. Scores, the point log and the bout timer are untouched, so it is safe mid-bout.
- The final score stays on the display until the referee starts a new bout. New bout asks for confirmation, then clears the score, the point log and "Last exchange", and returns the bout timer to a paused full bout length. The side arrangement and the settings are kept.
- The settings control (⚙) opens the bout length and the point cap. The bout length changes in steps of 30 seconds, down to 0:30, and the point cap in steps of one, down to 1. Each can be switched off on its own and keeps its value while off. With the bout timer off there is no countdown, no "Last exchange" and no last-exchange signal. With the point cap off, scores are not limited and there is no end-of-bout signal. With both off the scoreboard is a plain counter.
- Settings are remembered between bouts and between app launches. A change made before the bout has begun, with the point log empty and the bout timer not yet run, applies to that bout. A point and its deduction leave the score at 0–0, but the bout has begun. A change made during a bout applies from the next new bout, so the bout in progress keeps the bout length and point cap it started with. If undo then takes back every entry of a bout whose bout timer has not run, the change applies to that bout.
- An interruption does not lose the bout. When the app leaves the foreground, for a phone call for example, the bout timer pauses, so the time away is not counted. Reopening the app brings the bout back paused, with its score, point log, bout timer reading, "Last exchange" and side arrangement, also after Android has closed the app in the background. A bout brought back gives no signal again: not the last-exchange signal, and not the end-of-bout signal.
- The screen stays awake while the app is in the foreground, so the device does not lock while the referee watches the fencers.
- The bout is saved on the device only. Nothing leaves the device. The app has no internet permission.

## Install

The Android app is installed from an APK. A phone that cannot install it, an iPhone for example, opens the [browser version](#browser-version).

1. On an Android phone running Android 8.0 or newer, open the [latest release](https://github.com/iirorantanen/hema-scorepad/releases/latest).
2. Download `hema-scorepad-<version>.apk` and open it.
3. Allow the browser to install apps when Android asks.

Android lists the app as "Scorepad". The full name does not fit under a launcher icon. The icon is the head of Fiore the Boar, the mascot, on black.

## Browser version

The browser version is the scoreboard at a web address:

<https://iirorantanen.github.io/hema-scorepad/>

It needs no install and no account. It is made for current Safari on an iPhone and current Chrome on an Android phone.
Open the address once with a network connection. The browser then keeps the files of the scoreboard, and the scoreboard opens with no network connection, in a sports hall for example.

Add the scoreboard to the home screen. Opened from there it fills the screen, with no address bar.

On an iPhone, in Safari:

1. Open the address.
2. Tap the Share button. Where Safari shows none, tap the page menu button and then "Share".
3. Tap "Add to Home Screen". It is further down the list of actions.
4. Switch on "Open as Web App", where iOS shows that switch, and tap "Add".

On an Android phone, in Chrome:

1. Open the address.
2. Tap More (⋮) at the right of the address bar.
3. Tap "Install and create shortcut", then "Install". Earlier versions of Chrome name the menu item "Add to Home screen".

Open the scoreboard once from the home screen while the phone still has a network connection. A phone can keep the files for the home screen apart from those of the browser.

Where the browser version falls short of the Android app:

- **No signal on an iPhone.** Safari on an iPhone has no vibration interface, and nothing takes the place of a signal: no sound and nothing on the display. The rest of the scoreboard works without it.
- **Signals on an Android phone depend on the phone's sound mode.** In Chrome the signals are the same vibrations as in the Android app. A phone on silent or in Do Not Disturb may not give them.
- **Full screen only from the home screen.** Opened in a browser tab, the scoreboard shares the screen with the browser's address bar.
- **The screen may lock.** The browser version keeps the screen awake through the browser's wake lock. Chrome has one from version 84 and Safari on an iPhone from iOS 16.4; opened from the home screen, an iPhone needs iOS 18.4. In a browser without a wake lock the phone locks after its usual time. A browser that has one can refuse it, in power-saving mode for example.
- **Safari clears a site that is not on the home screen.** After seven days of Safari use without a visit to the scoreboard, Safari on an iPhone deletes what the scoreboard stored: the files, the bout and the settings. The scoreboard then needs a network connection again. The scoreboard on the home screen is not counted as Safari and keeps them.

Where the list under [What it does](#what-it-does) names the app or Android, the browser version does this:

- The bout timer pauses when the page is hidden: another tab or another app in front, or the phone locked. The bout comes back paused after the page is reloaded and after the browser is closed.
- The bout and the settings are saved in the browser, on the phone. Clearing the browser's data for the site clears them. Nothing about a bout leaves the phone: the scoreboard sends nothing, and every file it loads comes from its own address.
- A release never reloads an open scoreboard. The first opening after a release still shows the version before it, while the browser fetches the new one. The new version shows at the next opening after every tab with the scoreboard, and the scoreboard opened from the home screen, has been closed.

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

Check that the browser version works with no network connection. The page from the command above has no service worker, the script that keeps the files of the site in the browser: the build adds it to the site only. Build the site and serve it from the development machine. The port is not the one above, because a service worker keeps answering at its address after the server has stopped:

```bash
./gradlew :browser:jsBrowserDistribution
python3 -m http.server 8081 --bind 127.0.0.1 --directory browser/build/dist/js/productionExecutable
```

Open `http://localhost:8081/` in a browser on the development machine, stop the server and open the address again: the scoreboard opens and awards points. A browser runs a service worker only at `localhost` or over HTTPS, so this check does not work from a phone on the same network.

To check an update, start the server again, change a file of the browser version and build the site again. A page that is open keeps its version, also after a reload. Once every tab that shows the scoreboard is closed, the next one opened shows the new version.

Pushing a `v*` tag runs [`.github/workflows/release.yml`](.github/workflows/release.yml). It publishes the Android app and the browser version from the commit the tag names, so both are the same version:

1. The first job runs the check, `./gradlew check`.
2. It builds the site with `./gradlew :browser:jsBrowserDistribution`, and then the signed APK. See [ADR 0001](docs/adr/0001-dedicated-release-signing-key.md) for how releases are signed.
3. It publishes the APK as a GitHub Release.
4. A second job publishes the site to GitHub Pages, at <https://iirorantanen.github.io/hema-scorepad/>. That job is handed no signing secret.

A step that fails stops the steps after it, so nothing is published before the check passes, and the site is not published without the APK.

The site is the directory `browser/build/dist/js/productionExecutable` as the build writes it. The source map `browser.js.map` is published with it. A browser's developer tools fetch it; the scoreboard does not, and the service worker does not keep it.

Publishing the site depends on repository settings. The repository owner sets them once, before the first tag that publishes the site:

- Under Settings, Pages, "Build and deployment", the source is "GitHub Actions".
- Under Settings, Environments, `github-pages`, "Deployment branches and tags" is "Selected branches and tags", with one rule of ref type "Tag" and name pattern `v*`. The workflow runs on a tag, and GitHub's default for that environment lets only the default branch deploy. If the environment is not listed, create it under that name.
- After the first release that publishes the site, the repository's website link, under "About" on the repository page, is set to the address.

When the second job fails on a setting, the GitHub Release is already published. Correct the setting and re-run the failed job within one day: GitHub keeps the built site for that long.

The artwork is in [`docs/brand/`](docs/brand/):

- `fiore.svg` is the master of Fiore the Boar: the head in colour and, under the id `fiore-mono`, its one-colour form.
- `icon.svg` is the icon, the head on black, in the square an Android launcher masks.
- `readme-header.svg` is the picture at the top of this file.
- `social-preview.png` is the picture GitHub shows with a link to the repository. It is uploaded by hand in the repository settings and is rendered from `social-preview.svg`.

The icon, the two pictures, the two layers of the Android launcher icon in `app/src/main/res/drawable/` and the three PNG files of the browser version's icon in `browser/src/jsMain/resources/` each hold a copy of the master's shapes. After a change to the master, write them again. The command needs Python 3 and the JDK:

```bash
python3 scripts/brand/derive.py
```

The check fails when one of those files differs from what the command writes, and names the file. It also fails when the head without its feather no longer fills the circle of 33 dp that no launcher mask cuts: `CX`, `CY` and `VIEWPORT` in the script place the head in the launcher icon. A PNG file is drawn by `scripts/brand/Png.java` and compared by its pixels. `icon-maskable-512.png` shows the head smaller than the other two: a home screen may cut that file down to a circle of 0.4 of its width, and the head stays inside it. The one-colour form is computed by `scripts/brand/Mono.java`, where the shapes it is made of are typed a second time: a change to the shape of the head is repeated there.

Render the social preview again after a change to its SVG. The digits and the name take the default font of the machine that renders it:

```bash
google-chrome --headless --hide-scrollbars --window-size=1280,640 --screenshot=docs/brand/social-preview.png docs/brand/social-preview.svg
```

Write a page that shows the icon and the themed icon at 192 px and 48 px, in a round and a rounded-square mask:

```bash
python3 scripts/brand/sheet.py build/icon-sheet.html
```

The project's vocabulary (bout, fencer, referee, signal and so on) is defined in [GLOSSARY.md](GLOSSARY.md).

## Licence

HEMA Scorepad is under the [MIT licence](LICENSE). The licence covers the code and the artwork, Fiore the Boar included.
