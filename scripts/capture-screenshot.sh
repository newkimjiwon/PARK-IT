#!/bin/zsh
# Saves the current emulator screen as a README image in docs/images/.
#
#   scripts/capture-screenshot.sh <adb-serial> <name> <car|car-full|phone>
#
#   car       Pleos head unit, cropped to the app window (right 2/3 of a 2560x1440 screen)
#   car-full  Pleos head unit, whole screen
#   phone     Phone, whole screen
#
# macOS only (uses sips). Example:
#   scripts/capture-screenshot.sh emulator-5554 car-map car
set -e

if [ $# -ne 3 ]; then
  sed -n '2,13p' "$0"
  exit 1
fi

serial=$1 name=$2 kind=$3
out="$(cd "$(dirname "$0")/.." && pwd)/docs/images/$name.jpg"
raw=$(mktemp -t screenshot).png
trap 'rm -f "$raw"' EXIT

adb -s "$serial" exec-out screencap -p > "$raw"
case "$kind" in
  # The Pleos app window starts right of the cluster panel and sits between the status bar and the dock.
  car)      sips -c 1207 1680 --cropOffset 91 854 "$raw" >/dev/null; width=1280 ;;
  car-full) width=1600 ;;
  phone)    width=540 ;;
  *) echo "unknown kind: $kind" >&2; exit 1 ;;
esac
sips --resampleWidth "$width" -s format jpeg -s formatOptions 82 "$raw" --out "$out" >/dev/null
echo "$out"
