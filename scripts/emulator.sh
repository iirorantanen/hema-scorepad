#!/bin/sh
# The commands of docs/agents/emulator.md behind one name, so a call carries no shell variables.
set -eu
cd "$(dirname "$0")/.."
SDK=$(sed -n 's/^sdk.dir=//p' local.properties)
[ -n "$SDK" ] || { echo "local.properties has no sdk.dir" >&2; exit 1; }
ADB=$SDK/platform-tools/adb
APP=io.github.iirorantanen.hemascoreboard
export ANDROID_SERIAL=emulator-5554 # keeps adb on the emulator while a phone is plugged in

case "${1:-}" in
boot)
    # setsid: the emulator outlives the shell that started it, until "stop".
    setsid "$SDK/emulator/emulator" -avd hema-scoreboard -no-window -no-audio -no-boot-anim -no-snapshot >/dev/null 2>&1 &
    timeout 180 "$ADB" wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 1; done'
    echo booted
    ;;
install)
    ./gradlew -q :app:installDebug
    "$ADB" shell am start -W -n "$APP/.MainActivity" | grep Status
    ;;
display)
    # One line per element: its bounds, its text and, after the |, its description for a screen reader.
    "$ADB" exec-out uiautomator dump /dev/tty | tr '>' '\n' |
        sed -nE 's/.* text="([^"]*)".* content-desc="([^"]*)".* bounds="([^"]*)".*/\3 \1|\2/p' | grep -v ' |$'
    ;;
shot)
    "$ADB" exec-out screencap -p >"$2"
    ;;
stop)
    "$ADB" emu kill
    ;;
adb)
    shift
    exec "$ADB" "$@"
    ;;
*)
    echo "usage: scripts/emulator.sh boot | install | display | shot <file.png> | stop | adb <arguments>" >&2
    exit 2
    ;;
esac
