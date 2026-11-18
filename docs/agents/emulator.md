# Running the app on an emulator

The sequence for checking on a device what the app looks like and what it does. Commands run from the repo root. Shell variables do not survive between tool calls, so each call starts with this line:

```bash
SDK=$(sed -n 's/^sdk.dir=//p' local.properties); ADB=$SDK/platform-tools/adb; APP=io.github.iirorantanen.hemascoreboard; export ANDROID_SERIAL=emulator-5554
```

`ANDROID_SERIAL` keeps adb on the emulator while a phone is plugged in. An empty `$SDK` means `local.properties` has no `sdk.dir`: stop and report it, as the Gradle build needs the same line.

## 1. Emulator and system image, once per machine

```bash
ls $SDK/emulator/emulator $SDK/system-images/android-36/default/x86_64/system.img
```

If either file is missing, **ask the maintainer before downloading**: the two packages are roughly 1 GB to download and 3 GB on disk, and the command accepts their SDK licences. After a yes:

```bash
yes | $SDK/cmdline-tools/latest/bin/sdkmanager emulator "system-images;android-36;default;x86_64"
```

If `$SDK/emulator/emulator -list-avds` does not print `hema-scoreboard`, create the virtual device:

```bash
echo no | $SDK/cmdline-tools/latest/bin/avdmanager create avd --name hema-scoreboard --package "system-images;android-36;default;x86_64" --device pixel_8
```

Done when `$SDK/emulator/emulator -list-avds` prints `hema-scoreboard`.

## 2. KVM

```bash
$SDK/emulator/emulator -accel-check
```

Done when the output contains `is installed and usable`.

If it contains `This user doesn't have permissions to use KVM (/dev/kvm)`, stop work and report this command to the maintainer, who runs it and logs in again:

```bash
sudo gpasswd -a "$USER" kvm
```

On any other output, stop work and report the output.

## 3. Start the headless emulator

Run as a background command; it keeps running until step 9.

```bash
$SDK/emulator/emulator -avd hema-scoreboard -no-window -no-audio -no-boot-anim -no-snapshot
```

Then wait for the boot, which takes about 20 seconds:

```bash
timeout 180 $ADB wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 1; done' && echo booted
```

Done when it prints `booted`.

## 4. Install and launch

```bash
./gradlew :app:installDebug
$ADB shell am start -W -n $APP/.MainActivity
```

Done when the install prints `Installed on 1 device.` and the launch prints `Status: ok`.

The scoreboard keeps the bout and the settings between app launches. `$ADB shell pm clear $APP` before the launch erases everything it stored, so the app launches as after a first install.

## 5. Read the display

As text, one line per element: its bounds in pixels, its text, and after the `|` its description for a screen reader.

```bash
$ADB exec-out uiautomator dump /dev/tty | tr '>' '\n' | sed -nE 's/.* text="([^"]*)".* content-desc="([^"]*)".* bounds="([^"]*)".*/\3 \1|\2/p' | grep -v ' |$'
```

Each score carries its fencer in the description (`[42,401][495,1631] 1|Red 1`), and the bout timer its reading (`3:00|Bout timer 3:00`). Done when the lines for Red, Blue and the bout timer are printed.

As a screenshot, written to the scratchpad and read as an image:

```bash
$ADB exec-out screencap -p > <scratchpad>/display.png
```

## 6. Tap

```bash
$ADB shell input tap <x> <y>
```

The point comes from the bounds in step 5: a tap inside the `[x1,y1][x2,y2]` of a score lands on that fencer's half. Bounds change with rotation, so they are read again after step 7. Done when step 5 shows what the tap changed.

## 7. Rotate

```bash
$ADB shell wm user-rotation lock 1   # landscape
$ADB shell wm user-rotation lock 0   # portrait
```

The activity is recreated as on a real device. Done when `$ADB shell dumpsys window displays | grep -o 'mCurrentRotation=ROTATION_[0-9]*'` prints `ROTATION_90` for landscape and `ROTATION_0` for portrait.

## 8. Read the most recent vibration

A vibration is recorded when it ends, and signals given close together play one after another. The read comes after the last signal has finished: one second after a single point. `DeviceEffects.kt` holds the lengths of the longer signals and the pause between two signals.

```bash
$ADB shell dumpsys vibrator_manager | grep $APP | sort | tail -1
```

The dump groups vibrations by usage, and `sort` puts them back in time order. In the line:

- The first field is the time the vibration was requested. A time before the tap means the read came too early.
- The third field is the outcome. `finished` means the vibration played. `ignored_for_settings` means a device setting suppressed it and nothing played.
- `played:` lists the steps. A step with `amplitude=1.00` is a pulse and one with `amplitude=0.00` is a pause, each with its length in milliseconds.

A Red point is one pulse, recorded as `[Step=0ms(amplitude=0.00),Step=120ms(amplitude=1.00)]` at the lengths `DeviceEffects.kt` held when this was written. A Blue point is two pulses with a pause between them.

## 9. Stop

```bash
$ADB emu kill
```

Done when `$ADB devices` lists no `emulator-5554`.

## What the emulator cannot show

The emulator has no motor, no hand and no hall. It shows which vibration was requested and where each element sits on the display, in pixels. How a signal feels, how the device sits in the hand, how the display reads from the floor and how a manufacturer's Android build behaves are checked on a real phone and tablet in issue #10.
