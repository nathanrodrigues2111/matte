<div align="center">

# Matte

**Solid, readable notifications for ColorOS 17. No root.**

Turns the frosted-glass notification cards on OnePlus, OPPO and realme phones into solid cards you can actually read.

![Platform](https://img.shields.io/badge/platform-Android-3ddc84)
![ColorOS](https://img.shields.io/badge/ColorOS-17-1f6feb)
![Root](https://img.shields.io/badge/root-not%20required-2ea043)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

[Download](https://github.com/nathanrodrigues2111/matte/releases/latest) · [Website](https://nathanrodrigues2111.github.io/matte/) · [How it works](#how-it-works) · [FAQ](#faq)

</div>

| Before: ColorOS 17 glass | After: Matte |
| :---: | :---: |
| ![ColorOS 17 glass notification card, white text on light grey, hard to read](docs/img/glass.png) | ![Matte on: solid dark notification card with clear white text](docs/img/matte.png) |

## Why

ColorOS 17 draws heads-up banners and notification shade cards as light grey frosted glass with white text. Over any light app the text all but disappears. No setting fixes it: Visual Blur, Allow window-level blurs, high contrast text and light mode were all tested and none change the cards.

Matte flips a SystemUI switch so the cards are drawn solid: `#404040` in dark mode, `#fafafa` in light mode.

## Quick start

### Option A: Shizuku (on the phone)

1. Install and start [Shizuku](https://shizuku.rikka.app/).
2. Install Matte from [Releases](https://github.com/nathanrodrigues2111/matte/releases/latest) and grant it Shizuku access.
3. Switch Matte on. Or add the **Matte** tile to Quick Settings.

Matte re-applies itself after a reboot as soon as Shizuku is running again.

### Option B: Computer (adb)

Enable USB debugging, connect the phone, then:

```sh
adb shell setprop debug.systemui.gaussblur_level 4 && adb shell am crash com.android.systemui
```

To turn it off:

```sh
adb shell setprop debug.systemui.gaussblur_level -1 && adb shell am crash com.android.systemui
```

Or use the scripts in [`scripts/`](scripts):

```sh
./scripts/matte.sh on | off | status      # macOS, Linux
scripts\matte.bat on | off | status       # Windows
```

## How it works

ColorOS SystemUI has a debug override, `debug.systemui.gaussblur_level`. Level `4` selects the branch ColorOS uses on low-end phones that cannot afford blur, where cards are painted solid. Android lets the shell user (adb, or Shizuku) set `debug.*` properties, so no root is needed. SystemUI only reads the value at startup, which is why it is restarted right after.

Nothing is installed into the system and no files are modified. The value lives in memory until the phone restarts.

Found by testing on a OnePlus 15 running ColorOS 17.0.0.102.

<details>
<summary><b>Why not something else?</b></summary>

- **Overlays (fabricated RRO):** blocked for the shell on Android 16: `Non-root shell cannot fabricate overlays`.
- **Custom themes:** the theme store does not load SystemUI colors for these cards.
- **Root + LSPosed module** ([SANWU5/c17-statusbar](https://github.com/SANWU5/c17-statusbar)): works, but needs root.

</details>

## FAQ

**Does it survive a restart?**
No. Sleep, lock and SystemUI restarts are fine, a full phone restart resets it. With Shizuku, Matte re-applies it once Shizuku is running. With adb, run the command again.

**Is it safe?**
It sets one debug property that SystemUI already supports and restarts the status bar. No root, no system files touched, fully reversible.

**How do I undo it?**
Switch it off in Matte, run the `-1` command above, or restart the phone.

**Any side effects?**
Other SystemUI blur goes flat too: the shade background, Quick Settings and the volume panel. The status bar flickers for a few seconds when switching.

**Which phones?**
ColorOS 17 on OnePlus, OPPO and realme. Other versions are untested.

## Disclaimer

Matte is not affiliated with, endorsed by, or sponsored by OPPO, OnePlus or realme. Use at your own risk.

## License

MIT. See [LICENSE](LICENSE).
