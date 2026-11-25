# Running the app on an emulator

The sequence for checking on a device what the app looks like and what it does. Commands run from the repo root.

From step 3 on, every command is a verb of `scripts/emulator.sh`. The script reads the SDK path from `local.properties` and keeps adb on the emulator while a phone is plugged in. `scripts/emulator.sh adb <arguments>` runs any other adb command the same way. `local.properties has no sdk.dir` from the script means stop and report it, as the Gradle build needs the same line.

## 1. Emulator and system image, once per machine

```bash
SDK=$(sed -n 's/^sdk.dir=//p' local.properties); ls $SDK/emulator/emulator $SDK/system-images/android-36/default/x86_64/system.img
```

If either file is missing, **ask the maintainer before downloading**: the two packages are roughly 1 GB to download and 3 GB on disk, and the command accepts their SDK licences. After a yes:

```bash
SDK=$(sed -n 's/^sdk.dir=//p' local.properties); yes | $SDK/cmdline-tools/latest/bin/sdkmanager emulator "system-images;android-36;default;x86_64"
```

If `$SDK/emulator/emulator -list-avds` does not print `hema-scoreboard`, create the virtual device:

```bash
SDK=$(sed -n 's/^sdk.dir=//p' local.properties); echo no | $SDK/cmdline-tools/latest/bin/avdmanager create avd --name hema-scoreboard --package "system-images;android-36;default;x86_64" --device pixel_8
```

Done when `$SDK/emulator/emulator -list-avds` prints `hema-scoreboard`.

## 2. KVM

```bash
SDK=$(sed -n 's/^sdk.dir=//p' local.properties); $SDK/emulator/emulator -accel-check
```

Done when the output contains `is installed and usable`.

If it contains `This user doesn't have permissions to use KVM (/dev/kvm)`, stop work and report this command to the maintainer, who runs it and logs in again:

```bash
sudo gpasswd -a "$USER" kvm
```

On any other output, stop work and report the output.

## 3. Start the headless emulator

```bash
scripts/emulator.sh boot
```

The emulator keeps running after the command returns, until step 10. The boot takes about 20 seconds. Done when it prints `booted`.

## 4. Install and launch

```bash
scripts/emulator.sh install
```

Done when it prints `Installed on 1 device.` and `Status: ok`.

The scoreboard keeps the bout and the settings between app launches, and they survive an emulator restart. A check that starts from 0–0 and the default settings begins with:

```bash
scripts/emulator.sh fresh
```

It erases everything the app stored, launches the app as after a first install and reads the display. Done when it prints `Success` and `Status: ok` and exits 0.

- `the display is not at 0-0`: something else is on the display, a system dialog for example. Step 5 reads it.
- `Failed`: the erase did not happen. Seen once, on 2026-11-25, with a build an earlier session had installed; after `scripts/emulator.sh install` the erase succeeded. The cause is unknown.

## 5. Read the display

As text, one line per element: its bounds in pixels, its text, and after the `|` its description for a screen reader.

```bash
scripts/emulator.sh display
```

Each score carries its fencer in the description (`[42,401][495,1631] 1|Red 1`), and the bout timer its reading (`3:00|Bout timer 3:00`). Done when the lines for Red, Blue and the bout timer are printed.

As a screenshot, read as an image. A relative path is taken from the repo root, and `build/` is ignored by git:

```bash
scripts/emulator.sh shot build/display.png
```

A ticket that asks for the display to be unchanged gets two screenshots in one boot: the first of `HEAD` (`git stash -u`, step 4, the screenshot, `git stash pop`), the second of the change after step 4 again. The two are compared below the status bar, from y = 150 down, because the clock above it changes:

```bash
scripts/emulator.sh compare build/before.png build/after.png
```

Done when it prints `identical`. `differs in [x1,y1][x2,y2]` gives the box that holds every differing pixel, in the form of the bounds above.

## 6. Tap

```bash
scripts/emulator.sh adb shell input tap <x> <y>
```

The point comes from the bounds in step 5: a tap inside the `[x1,y1][x2,y2]` of a score lands on that fencer's half. Bounds change with rotation, so they are read again after step 7. Done when step 5 shows what the tap changed.

## 7. Rotate

```bash
scripts/emulator.sh adb shell wm user-rotation lock 1   # landscape
scripts/emulator.sh adb shell wm user-rotation lock 0   # portrait
```

The activity is recreated as on a real device. Done when `scripts/emulator.sh adb shell dumpsys window displays | grep -o 'mCurrentRotation=ROTATION_[0-9]*'` prints `ROTATION_90` for landscape and `ROTATION_0` for portrait.

## 8. Read the most recent vibration

A vibration is recorded when it ends, and signals given close together play one after another. The read comes after the last signal has finished: one second after a single point. `DeviceEffects.kt` holds the lengths of the longer signals and the pause between two signals.

```bash
scripts/emulator.sh adb shell dumpsys vibrator_manager | grep io.github.iirorantanen.hemascoreboard | sort | tail -1
```

The dump groups vibrations by usage, and `sort` puts them back in time order. In the line:

- The first field is the time the vibration was requested. A time before the tap means the read came too early.
- The third field is the outcome. `finished` means the vibration played. `ignored_for_settings` means a device setting suppressed it and nothing played.
- `played:` lists the steps. A step with `amplitude=1.00` is a pulse and one with `amplitude=0.00` is a pause, each with its length in milliseconds.

A Red point is one pulse, recorded as `[Step=0ms(amplitude=0.00),Step=120ms(amplitude=1.00)]` at the lengths `DeviceEffects.kt` held when this was written. A Blue point is two pulses with a pause between them.

## 9. Read the launcher

The launcher is AOSP's, `com.android.launcher3`. It shows the app's label and its icon. Step 5 reads it like the display.

The app list, which shows the icon in colour:

```bash
scripts/emulator.sh adb shell input keyevent KEYCODE_HOME
scripts/emulator.sh adb shell input swipe 540 1900 540 600 200
```

Done when step 5 prints a line for `Scorepad|Scorepad`.

The themed icon, in one colour. The launcher themes icons on the home screen only, so the app gets a shortcut there first. The drag starts at the centre of the app's bounds in the app list:

```bash
scripts/emulator.sh adb shell input draganddrop <x> <y> 540 1300 3000
scripts/emulator.sh adb root
scripts/emulator.sh adb shell content update --uri content://com.android.launcher3.grid_control/icon_themed --bind boolean_value:b:true
scripts/emulator.sh adb shell input keyevent KEYCODE_HOME
```

Done when a screenshot of the home screen shows the icon in one colour on a plain square.

Restore the launcher afterwards. This removes the shortcut and switches themed icons off:

```bash
scripts/emulator.sh adb shell pm clear com.android.launcher3
```

Done when, after `KEYCODE_HOME`, step 5 prints no line for `Scorepad`.

## 10. Stop

```bash
scripts/emulator.sh stop
```

Done when `scripts/emulator.sh adb devices` lists no `emulator-5554`.

## What the emulator cannot show

The emulator has no motor, no hand and no hall. It shows which vibration was requested and where each element sits on the display, in pixels. How a signal feels, how the device sits in the hand, how the display reads from the floor and how a manufacturer's Android build behaves are checked on a real phone and tablet in issue #10.
