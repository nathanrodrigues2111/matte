package com.lazyneil.matte;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;

import rikka.shizuku.Shizuku;

public class App extends Application {
    // Theme choices, index = stored value. AMOLED is dark plus a pure black overlay.
    static final String[] THEMES = {"System", "Light", "Dark", "AMOLED"};

    @Override public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
        nightMode(Matte.prefs(this).getInt("theme", 0));
        // Shizuku pushes its binder to permitted apps when it starts (also after boot), which wakes this process.
        Shizuku.addBinderReceivedListenerSticky(() -> new Thread(() -> Matte.autoApply(this)).start());
    }

    static void nightMode(int theme) {
        AppCompatDelegate.setDefaultNightMode(theme == 0 ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                : theme == 1 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
    }
}
