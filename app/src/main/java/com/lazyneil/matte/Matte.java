package com.lazyneil.matte;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.service.quicksettings.TileService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

// ColorOS 17 SystemUI reads debug.systemui.gaussblur_level once at startup (ScrimUtil.DEBUG_LEVEL).
// 4 = low-end "B anim" branch: no card blur, solid cards. -1 = use the device default (glass).
final class Matte {
    static final String PROP = "debug.systemui.gaussblur_level";
    static final String SHIZUKU = "moe.shizuku.privileged.api";
    static final int ON = 4, OFF = -1;
    // Experimental, seen on a OnePlus 15, ColorOS 17.0.0.102: 5, 4 and 3 all keep pop-ups solid and change
    // the notification bar background. -1 to 2 are stock glass; 6 breaks the shade, so it is not offered.
    static final int[] LEVELS = {5, 4, 3};
    static final String[] LOOKS = {"Light", "Default", "Dark"};

    static int level(Context c) {
        int l = prefs(c).getInt("barLevel", ON);
        for (int x : LEVELS) if (x == l) return l;
        return ON;
    }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("matte", 0); }

    static String getprop(String name) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"getprop", name});
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String s = r.readLine();
                return s == null ? "" : s.trim();
            }
        } catch (Exception e) { return ""; }
    }

    static boolean isOn() {
        String v = getprop(PROP);
        for (int x : LEVELS) if (v.equals(String.valueOf(x))) return true;
        return false;
    }

    static boolean installed(Context c) {
        try { c.getPackageManager().getPackageInfo(SHIZUKU, 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    static boolean running() { return Shizuku.pingBinder(); }

    static boolean ready() {
        try { return running() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; }
        catch (Throwable t) { return false; }
    }

    // The binder reaches a freshly started process asynchronously; wait for it off the main thread.
    static boolean waitReady(long ms) {
        for (long end = System.currentTimeMillis() + ms; !ready(); ) {
            if (System.currentTimeMillis() > end) return false;
            try { Thread.sleep(200); } catch (InterruptedException e) { return false; }
        }
        return true;
    }

    // ponytail: Shizuku.newProcess is private in api 13.x, called via reflection. Move to a UserService if a future api drops it.
    static int sh(String cmd) throws Exception {
        Method m = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
        m.setAccessible(true);
        Process p = (Process) m.invoke(null, new String[]{"sh", "-c", cmd}, null, null);
        return p.waitFor();
    }

    static long bootTime() { return System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime(); }

    // Blocking: call off the main thread. Returns true if the property now holds the wanted value.
    static synchronized boolean apply(Context c, boolean on) {
        try {
            sh("setprop " + PROP + " " + (on ? level(c) : OFF) + " && am crash com.android.systemui");
        } catch (Exception e) { return false; }
        boolean ok = isOn() == on;
        if (ok) prefs(c).edit().putBoolean("on", on).putLong("since", on ? System.currentTimeMillis() : 0)
                .putLong("seen", System.currentTimeMillis()).apply();
        TileService.requestListeningState(c, new ComponentName(c, Tile.class));
        return ok;
    }

    // "on" = the user wants solid cards. A property that is off while "on" was set before this boot means a restart reset it.
    static boolean resetByRestart(Context c) {
        SharedPreferences p = prefs(c);
        return p.getBoolean("on", false) && p.getLong("seen", 0) < bootTime() && !isOn();
    }

    // Called on every screen refresh. Remembers that it was on (also when set from a computer), and forgets a manual off.
    static void observe(Context c) {
        SharedPreferences p = prefs(c);
        if (isOn()) p.edit().putBoolean("on", true).putBoolean("ever", true).putLong("seen", System.currentTimeMillis()).apply();
        else if (!resetByRestart(c)) p.edit().putBoolean("on", false).putLong("since", 0).apply();
    }

    // Applied time, only if it happened during this boot.
    static long since(Context c) {
        long t = prefs(c).getLong("since", 0);
        return t > bootTime() ? t : 0;
    }

    // After a reboot the property is gone. Re-apply once Shizuku is usable, only if it is actually missing.
    static synchronized void autoApply(Context c) {
        if (prefs(c).getBoolean("auto", false) && resetByRestart(c) && ready()) apply(c, true);
    }
}
