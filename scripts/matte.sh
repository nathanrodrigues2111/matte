#!/usr/bin/env bash
# matte: solid (non-glass) ColorOS 17 notification cards from a computer, no root.
# Needs adb (Android SDK Platform-Tools) and USB debugging on the phone.
# Lost on every phone restart: run `matte.sh on` again after each restart.
set -euo pipefail
S="${ANDROID_SERIAL:-$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')}"
[ -n "$S" ] || { echo "matte: no phone found. Connect it by USB and allow USB debugging." >&2; exit 1; }
A=(adb -s "$S" shell)

restart() { "${A[@]}" am crash com.android.systemui >/dev/null 2>&1 || true; echo "Status bar restarting, the screen flickers for a few seconds."; }

case "${1:-status}" in
  on)     "${A[@]}" setprop debug.systemui.gaussblur_level 3; restart ;;
  off)    "${A[@]}" setprop debug.systemui.gaussblur_level -1; restart ;;
  status) v=$("${A[@]}" getprop debug.systemui.gaussblur_level | tr -d '\r')
          case "$v" in 3|4|5) true ;; *) false ;; esac && echo "Matte: on (solid cards)" || echo "Matte: off (glass cards)" ;;
  *)      echo "usage: matte.sh on|off|status"; exit 1 ;;
esac
