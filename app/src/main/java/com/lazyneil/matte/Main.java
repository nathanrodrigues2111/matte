package com.lazyneil.matte;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.transition.TransitionManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialSharedAxis;

import java.util.ArrayList;
import java.util.List;

import rikka.shizuku.Shizuku;

// Two screens in one activity: method chooser, then the main screen for that method. Views are built once per
// screen; update() re-reads live state (property, Shizuku, settings) into them.
public class Main extends AppCompatActivity {
    static final String CH = "test";
    static final String CMD_ON = "adb shell setprop " + Matte.PROP + " 4 && adb shell am crash com.android.systemui";
    static final String CMD_OFF = "adb shell setprop " + Matte.PROP + " -1 && adb shell am crash com.android.systemui";
    static final int TITLE_L = com.google.android.material.R.style.TextAppearance_Material3_TitleLarge;
    static final int TITLE_M = com.google.android.material.R.style.TextAppearance_Material3_TitleMedium;
    static final int BODY = com.google.android.material.R.style.TextAppearance_Material3_BodyMedium;
    static final int LABEL = com.google.android.material.R.style.TextAppearance_Material3_LabelLarge;
    static final int DISPLAY = com.google.android.material.R.style.TextAppearance_Material3_DisplaySmall;

    final List<Runnable> updaters = new ArrayList<>();
    LinearLayout content;
    View coord;
    boolean busy, onChooser;
    OnBackPressedCallback backToMain;

    final Shizuku.OnBinderReceivedListener binderUp = () -> runOnUiThread(this::update);
    final Shizuku.OnBinderDeadListener binderDown = () -> runOnUiThread(this::update);
    final Shizuku.OnRequestPermissionResultListener permResult = (c, r) -> runOnUiThread(this::update);

    @Override protected void onCreate(Bundle b) {
        if (Matte.prefs(this).getInt("theme", 0) == 3) getTheme().applyStyle(R.style.ThemeOverlay_Matte_Amoled, true);
        EdgeToEdge.enable(this);
        super.onCreate(b);
        setContentView(R.layout.main);
        coord = findViewById(R.id.coord);
        content = findViewById(R.id.content);
        NestedScrollView scroll = findViewById(R.id.scroll);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, in) -> {
            Insets i = in.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(i.left, 0, i.right, i.bottom);
            coord.setPadding(0, i.top, 0, 0); // solid status bar, no header
            return in;
        });
        backToMain = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { showMain(false); }
        };
        getOnBackPressedDispatcher().addCallback(this, backToMain);
        channel(this);
        if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0)
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        else if (b == null && !popups(this)) askPopups();
        if (method().isEmpty()) showChooser(null); else showMain(null);
        Shizuku.addBinderReceivedListener(binderUp);
        Shizuku.addBinderDeadListener(binderDown);
        Shizuku.addRequestPermissionResultListener(permResult);
    }

    @Override protected void onDestroy() {
        Shizuku.removeBinderReceivedListener(binderUp);
        Shizuku.removeBinderDeadListener(binderDown);
        Shizuku.removeRequestPermissionResultListener(permResult);
        super.onDestroy();
    }

    @Override public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        super.onRequestPermissionsResult(code, p, r);
        if (code == 1 && r.length > 0 && r[0] == 0 && !popups(this)) askPopups();
    }

    // ColorOS creates new channels with banners off, whatever importance the app asks for.
    static void channel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CH) == null)
            nm.createNotificationChannel(new NotificationChannel(CH, "Test notifications", NotificationManager.IMPORTANCE_HIGH));
    }

    static boolean allowed(Context c) {
        return c.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == 0
                && c.getSystemService(NotificationManager.class).areNotificationsEnabled();
    }

    static boolean popups(Context c) {
        NotificationChannel ch = c.getSystemService(NotificationManager.class).getNotificationChannel(CH);
        return allowed(c) && ch != null && ch.getImportance() >= NotificationManager.IMPORTANCE_HIGH;
    }

    void openChannel() {
        // App page, not the channel page: ColorOS keeps the Banner toggle there.
        startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
    }

    void askPopups() {
        new MaterialAlertDialogBuilder(this).setTitle("Turn on pop-ups")
                .setMessage("The test notification needs to pop up at the top of the screen. Your phone turns this off for new apps.\n\nOn the next screen, turn on Banner notifications.")
                .setPositiveButton("Open settings", (d, w) -> openChannel())
                .setNegativeButton("Not now", null).show();
    }

    @Override protected void onResume() {
        super.onResume();
        update();
    }

    String method() { return Matte.prefs(this).getString("method", ""); }

    boolean shizuku() { return method().equals("shizuku"); }

    void update() {
        if (!busy && !onChooser) Matte.observe(this);
        for (Runnable r : updaters) r.run();
    }

    // forward: null = no animation, true/false = shared axis X forward/back.
    void screen(Boolean forward, Runnable build) {
        if (forward != null) TransitionManager.beginDelayedTransition(content, new MaterialSharedAxis(MaterialSharedAxis.X, forward));
        content.removeAllViews();
        updaters.clear();
        header();
        build.run();
        update();
        ((NestedScrollView) findViewById(R.id.scroll)).scrollTo(0, 0);
    }

    void header() {
        TextView t = text(getString(R.string.app_name), com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge);
        t.setPadding(dp(16), dp(32), dp(16), 0);
        content.addView(t);
        TextView s = text(getString(R.string.subtitle), TITLE_M);
        s.setTextColor(color(androidx.appcompat.R.attr.colorPrimary));
        s.setPadding(dp(16), dp(2), dp(16), dp(16));
        content.addView(s);
    }

    // ---------- Chooser ----------

    void showChooser(Boolean forward) {
        onChooser = true;
        backToMain.setEnabled(!method().isEmpty());
        screen(forward, () -> {
            compatCard(true);
            section("How should Matte switch?");
            choice(R.drawable.ic_extension, "Use Shizuku", "One tap on the phone. Needs the free Shizuku app.", "shizuku");
            choice(R.drawable.ic_terminal, "Use a computer", "No extra app. Run one command over USB.", "computer");
            content.addView(caption("You can change this any time."));
        });
    }

    void choice(int icon, String title, String body, String m) {
        MaterialCardView c = card();
        c.setCheckable(true);
        c.setChecked(m.equals(method()));
        Row r = new Row((LinearLayout) c.getChildAt(0), icon, title);
        r.v.setBackground(null);
        r.v.setClickable(false);
        r.title.setTextAppearance(TITLE_L);
        r.sub(body);
        r.trailIcon(R.drawable.ic_chevron_right, com.google.android.material.R.attr.colorOnSurfaceVariant);
        c.setOnClickListener(v -> {
            Matte.prefs(this).edit().putString("method", m).apply();
            showMain(true);
        });
    }

    // ---------- Main ----------

    void showMain(Boolean forward) {
        onChooser = false;
        backToMain.setEnabled(false);
        screen(forward, () -> {
            boolean co17 = colorOs17() != null;
            if (!co17) compatCard(false);
            hero();
            if (shizuku()) shizukuSetup(); else computerSetup();
            if (shizuku()) afterRestart();
            tryIt();
            settings();
        });
    }

    void hero() {
        MaterialCardView c = new MaterialCardView(this, null, com.google.android.material.R.attr.materialCardViewFilledStyle);
        c.setRadius(dp(28));
        c.setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceContainerHigh));
        if (Matte.prefs(this).getInt("theme", 0) == 3) { c.setStrokeWidth(dp(1)); c.setStrokeColor(0x14ffffff); }
        LinearLayout col = column();
        col.setPadding(dp(12), dp(12), dp(12), dp(12));
        c.addView(col);
        content.addView(c, margins(-1, 8, 8));

        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        Preview p = new Preview(this, dark);
        col.addView(p, new LinearLayout.LayoutParams(-1, dp(168)));

        LinearLayout txt = column();
        txt.setPadding(dp(12), dp(20), dp(12), dp(8));
        TextView head = text("", DISPLAY);
        TextView state = text("", BODY);
        state.setPadding(0, dp(8), 0, 0);
        TextView lasts = text("Lasts until your phone restarts. Survives sleep, lock and app switching.", BODY);
        lasts.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        txt.addView(head);
        txt.addView(state);
        txt.addView(lasts);
        col.addView(txt);

        MaterialSwitch sw = shizuku() ? switchBar(col) : null;
        updaters.add(() -> {
            boolean on = Matte.isOn(), reset = Matte.resetByRestart(this);
            p.set(on, true);
            String h, s;
            if (busy) { h = "Applying"; s = "The screen will flicker for a few seconds."; }
            else if (on) {
                h = "Matte is on";
                long t = Matte.since(this);
                s = t > 0 ? "Active since " + DateFormat.getTimeFormat(this).format(t) : "Active";
            } else if (reset) {
                h = "Reset by restart";
                s = shizuku() ? "Tap the switch to re-apply." : "Run the command again to re-apply.";
            } else {
                h = Matte.prefs(this).getBoolean("ever", false) ? "Glass is back" : "Glass is on";
                s = shizuku() ? (Matte.ready() ? "Flip the switch for solid cards." : "Finish setup below to switch.")
                        : "Run the command below on a computer.";
            }
            head.setText(h);
            state.setText(s);
            state.setTextColor(reset && !busy ? color(androidx.appcompat.R.attr.colorError) : color(com.google.android.material.R.attr.colorOnSurface));
            if (sw != null) {
                sw.setTag(true);
                sw.setChecked(on);
                sw.setTag(null);
                sw.setEnabled(Matte.ready() && !busy);
            }
        });
    }

    // Settings-style main switch bar: a pill that fills with the accent when on.
    MaterialSwitch switchBar(LinearLayout parent) {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(24), 0, dp(16), 0);
        bar.setMinimumHeight(dp(72));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(36));
        bar.setBackground(bg);
        TextView label = text("Matte notifications", TITLE_L);
        bar.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        MaterialSwitch sw = new MaterialSwitch(this);
        bar.addView(sw);
        parent.addView(bar, margins(-1, 8, 0));
        int offBg = color(com.google.android.material.R.attr.colorSurfaceContainerHighest);
        int onBg = color(com.google.android.material.R.attr.colorPrimaryContainer);
        int offInk = color(com.google.android.material.R.attr.colorOnSurface);
        int onInk = color(com.google.android.material.R.attr.colorOnPrimaryContainer);
        bg.setColor(offBg);
        final int[] cur = {offBg};
        sw.setOnCheckedChangeListener((v, on) -> {
            int to = on ? onBg : offBg;
            if (to != cur[0]) {
                ValueAnimator a = ValueAnimator.ofArgb(cur[0], to);
                a.setDuration(300);
                a.addUpdateListener(x -> bg.setColor((int) x.getAnimatedValue()));
                a.start();
                cur[0] = to;
            }
            label.setTextColor(on ? onInk : offInk);
            if (v.getTag() == null) {
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                toggle(on);
            }
        });
        bar.setOnClickListener(v -> { if (sw.isEnabled()) sw.toggle(); });
        return sw;
    }

    void shizukuSetup() {
        section("Setup");
        LinearLayout c = (LinearLayout) card().getChildAt(0);
        Row inst = new Row(c, R.drawable.ic_counter_1_fill, "Install Shizuku");
        Row run = new Row(c, R.drawable.ic_counter_2_fill, "Start Shizuku");
        Row perm = new Row(c, R.drawable.ic_counter_3_fill, "Allow Matte");
        Row notif = new Row(c, R.drawable.ic_notifications, "Notifications");
        updaters.add(() -> {
            boolean i = Matte.installed(this), r = i && Matte.running(), p = r && Matte.ready();
            inst.step(1, i, i ? "Installed" : "Free app from Google Play.", "Get", this::playShizuku);
            run.step(2, r, r ? "Running" : "Open Shizuku and tap Start.", "Open", i ? this::openShizuku : null);
            perm.step(3, p, p ? "Allowed" : "Lets Matte change the setting.", "Allow", r ? () -> Shizuku.requestPermission(1) : null);
            notifRow(notif);
        });
    }

    void computerSetup() {
        section("On your computer");
        LinearLayout c = (LinearLayout) card().getChildAt(0);
        Row dev = new Row(c, R.drawable.ic_counter_1_fill, "Developer options");
        Row usb = new Row(c, R.drawable.ic_counter_2_fill, "USB debugging");
        Row tools = new Row(c, R.drawable.ic_counter_3_fill, "Platform-tools");
        Row cmd = new Row(c, R.drawable.ic_counter_4_fill, "Run the command");
        LinearLayout cmds = column();
        cmds.setPadding(dp(16), 0, dp(16), dp(12));
        command(cmds, "Turn on", CMD_ON);
        command(cmds, "Back to glass", CMD_OFF);
        c.addView(cmds);
        expandable(c, "Prefer a script?", "The Matte project includes matte.sh (Mac, Linux) and matte.bat (Windows). "
                + "Run \"matte on\", \"matte off\" or \"matte status\".");
        expandable(c, "\"More than one device\" error?", "The same phone can show up twice over wireless debugging. "
                + "Run \"adb devices\", copy one serial, and add \"-s <serial>\" after \"adb\" in both commands. "
                + "The scripts pick the first phone by themselves.");
        tools.step(3, false, "Google's free adb tools for Windows, Mac and Linux.", "Get",
                () -> open("https://developer.android.com/tools/releases/platform-tools"));
        updaters.add(() -> {
            boolean d = Settings.Global.getInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1;
            boolean u = Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
            dev.step(1, d, d ? "On" : "About device > Version, tap Build number 7 times.", "Open",
                    () -> startActivity(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)));
            usb.step(2, u, u ? "On. Connect by USB and tap Allow." : "Developer options > USB debugging.", "Open",
                    () -> startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)));
            boolean on = Matte.isOn();
            cmd.step(4, on, on ? "Done. Run it again after a restart." : "Copy it, paste it in a terminal.", null, null);
        });
    }

    void afterRestart() {
        section("After a restart");
        LinearLayout c = (LinearLayout) card().getChildAt(0);
        Row auto = new Row(c, R.drawable.ic_restart_alt, "Re-apply automatically");
        auto.sub("When Shizuku starts again after a restart.");
        MaterialSwitch sw = auto.trailSwitch();
        sw.setChecked(Matte.prefs(this).getBoolean("auto", false));
        sw.setOnCheckedChangeListener((v, on) -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            Matte.prefs(this).edit().putBoolean("auto", on).apply();
        });
        auto.v.setOnClickListener(v -> sw.toggle());
        expandable(c, "Starting Shizuku after a restart", "In Shizuku, turn on \"Start on boot\" (Android 13 and newer, "
                + "uses Wireless debugging on Wi-Fi). Otherwise open Shizuku and tap Start after each restart, then Matte "
                + "re-applies by itself.");
        if (Build.VERSION.SDK_INT >= 33) {
            Row tile = new Row(c, R.drawable.ic_toggle_on, "Quick Settings tile");
            tile.sub("Switch Matte from anywhere.");
            tile.button("Add", this::addTile);
        }
    }

    void tryIt() {
        section("Try it");
        LinearLayout c = (LinearLayout) card().getChildAt(0);
        Row send = new Row(c, R.drawable.ic_play_arrow, "Test notification");
        send.sub("See how a pop-up looks right now.");
        Row white = new Row(c, R.drawable.ic_contrast, "Over a white screen");
        white.sub("The hardest background for glass.");
        MaterialSwitch ws = white.trailSwitch();
        ws.setChecked(Matte.prefs(this).getBoolean("white", true));
        ws.setOnCheckedChangeListener((v, on) -> Matte.prefs(this).edit().putBoolean("white", on).apply());
        white.v.setOnClickListener(v -> ws.toggle());
        send.button("Send", () -> {
            if (!popups(this)) askPopups();
            else if (ws.isChecked()) startActivity(new Intent(this, White.class));
            else postTest(this);
        });
        if (!shizuku()) {
            Row notif = new Row(c, R.drawable.ic_notifications, "Notifications");
            updaters.add(() -> notifRow(notif));
        }
    }

    void settings() {
        section("Settings");
        LinearLayout c = (LinearLayout) card().getChildAt(0);
        Row m = new Row(c, R.drawable.ic_swap_horiz, "Method");
        m.sub(shizuku() ? "Shizuku" : "Computer");
        m.trailIcon(R.drawable.ic_chevron_right, com.google.android.material.R.attr.colorOnSurfaceVariant);
        m.v.setOnClickListener(v -> showChooser(false));
        int theme = Matte.prefs(this).getInt("theme", 0);
        Row t = new Row(c, R.drawable.ic_palette, "Theme");
        t.sub(App.THEMES[theme]);
        t.trailIcon(R.drawable.ic_chevron_right, com.google.android.material.R.attr.colorOnSurfaceVariant);
        t.v.setOnClickListener(v -> new MaterialAlertDialogBuilder(this).setTitle("Theme")
                .setSingleChoiceItems(App.THEMES, theme, (d, w) -> {
                    d.dismiss();
                    Matte.prefs(this).edit().putInt("theme", w).apply();
                    App.nightMode(w);
                    recreate(); // AppCompat skips it when the look changes but the night flag does not (Dark to AMOLED).
                }).show());
        Row y = new Row(c, R.drawable.ic_play_arrow, "Made by lazyneil");
        y.sub("YouTube channel");
        y.trailIcon(R.drawable.ic_chevron_right, com.google.android.material.R.attr.colorOnSurfaceVariant);
        y.v.setOnClickListener(v -> open("https://www.youtube.com/channel/UCW9BqnLNdhOjRBsHnhXZjjA"));
    }

    void compatCard(boolean always) {
        String ver = colorOs17();
        LinearLayout c = (LinearLayout) card().getChildAt(0);
        Row r = new Row(c, R.drawable.ic_mobile, ver != null ? "ColorOS 17 detected" : "Made for ColorOS 17");
        if (ver != null) {
            r.sub(Matte.getprop("ro.build.display.id"));
            r.trailIcon(R.drawable.ic_check_circle_fill, androidx.appcompat.R.attr.colorPrimary);
        } else {
            String v = Matte.getprop("ro.build.version.oplusrom.display");
            if (v.isEmpty()) v = Matte.getprop("ro.build.version.oplusrom");
            r.sub("This phone runs " + (v.isEmpty() ? "Android " + Build.VERSION.RELEASE : "ColorOS " + v) + ". It may not work.");
            r.trailIcon(R.drawable.ic_warning_fill, androidx.appcompat.R.attr.colorError);
        }
    }

    // ColorOS version if it is 17, else null.
    static String colorOs17() {
        String v = Matte.getprop("ro.build.version.oplusrom.display");
        if (v.isEmpty()) v = Matte.getprop("ro.build.version.oplusrom");
        return v.matches("V?17(\\..*)?") ? v : null;
    }

    void notifRow(Row r) {
        boolean ok = allowed(this), pop = popups(this);
        r.sub(pop ? "Allowed, with pop-ups" : ok ? "Pop-ups are off. Needed for the test." : "Needed for the test notification.");
        if (pop) r.trailIcon(R.drawable.ic_check_circle_fill, androidx.appcompat.R.attr.colorPrimary);
        else r.button(ok ? "Turn on" : "Allow", this::openChannel);
    }

    void toggle(boolean on) {
        busy = true;
        update();
        new Thread(() -> {
            boolean ok = Matte.apply(this, on);
            try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
            runOnUiThread(() -> {
                busy = false;
                update();
                snack(ok ? (on ? "Matte is on" : "Back to glass") : "Could not apply. Is Shizuku running?");
            });
        }).start();
    }

    void addTile() {
        if (Build.VERSION.SDK_INT < 33) return;
        getSystemService(StatusBarManager.class).requestAddTileService(new ComponentName(this, Tile.class),
                getString(R.string.tile_label), Icon.createWithResource(this, R.drawable.ic_solid), getMainExecutor(),
                r -> snack(r == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED || r == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
                        ? "Tile added to Quick Settings" : "Tile not added"));
    }

    static void postTest(Context c) {
        channel(c);
        c.getSystemService(NotificationManager.class).notify(1, new Notification.Builder(c, CH)
                .setSmallIcon(R.drawable.ic_solid)
                .setContentTitle("lazyneil")
                .setContentText("This is how your notifications look now. Pull down the shade to compare.")
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .build());
    }

    void playShizuku() {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Matte.SHIZUKU))); }
        catch (Exception e) { open("https://play.google.com/store/apps/details?id=" + Matte.SHIZUKU); }
    }

    void openShizuku() {
        Intent i = getPackageManager().getLaunchIntentForPackage(Matte.SHIZUKU);
        if (i != null) startActivity(i); else playShizuku();
    }

    void open(String url) { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }

    void snack(String s) { Snackbar.make(coord, s, Snackbar.LENGTH_SHORT).show(); }

    // ---------- Building blocks ----------

    void section(String s) {
        TextView t = text(s, LABEL);
        t.setTextColor(color(androidx.appcompat.R.attr.colorPrimary));
        t.setPadding(dp(16), dp(24), dp(16), dp(8));
        content.addView(t);
    }

    TextView caption(String s) {
        TextView t = text(s, BODY);
        t.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        t.setPadding(dp(16), dp(12), dp(16), 0);
        return t;
    }

    // Card with an inner vertical column, added to the content.
    MaterialCardView card() {
        MaterialCardView c = new MaterialCardView(this);
        c.setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceContainer));
        LinearLayout col = column();
        col.setPadding(0, dp(4), 0, dp(4));
        c.addView(col);
        content.addView(c, margins(-1, 0, 8));
        return c;
    }

    void command(LinearLayout parent, String label, String cmd) {
        TextView l = text(label, LABEL);
        l.setPadding(dp(40), dp(8), 0, dp(4));
        parent.addView(l);
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(16), dp(8), dp(4), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(color(com.google.android.material.R.attr.colorSurfaceContainerHighest));
        box.setBackground(bg);
        TextView code = text(cmd, BODY);
        code.setTypeface(Typeface.MONOSPACE);
        code.setTextIsSelectable(true);
        box.addView(code, new LinearLayout.LayoutParams(0, -2, 1));
        MaterialButton copy = new MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle);
        copy.setIconResource(R.drawable.ic_content_copy);
        copy.setContentDescription("Copy " + label.toLowerCase() + " command");
        copy.setOnClickListener(v -> {
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("command", cmd));
            if (Build.VERSION.SDK_INT < 33) snack("Copied");
        });
        box.addView(copy);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMarginStart(dp(40));
        parent.addView(box, lp);
    }

    // Row whose details stay hidden until tapped, so cards stay short.
    void expandable(LinearLayout parent, String title, String details) {
        Row r = new Row(parent, R.drawable.ic_keyboard_arrow_down, title);
        r.lead.setImageTintList(ColorStateList.valueOf(color(com.google.android.material.R.attr.colorOnSurfaceVariant)));
        TextView d = text(details, BODY);
        d.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        d.setPadding(dp(56), 0, dp(16), dp(12));
        d.setVisibility(View.GONE);
        parent.addView(d);
        r.v.setOnClickListener(v -> {
            boolean open = d.getVisibility() != View.VISIBLE;
            TransitionManager.beginDelayedTransition((ViewGroup) content.getRootView());
            d.setVisibility(open ? View.VISIBLE : View.GONE);
            r.lead.animate().rotation(open ? 180 : 0).setDuration(200).start();
        });
    }

    // M3 list item: leading icon, title, supporting text, trailing button / icon / switch.
    final class Row {
        final LinearLayout v = new LinearLayout(Main.this);
        final ImageView lead = new ImageView(Main.this);
        final TextView title, sub;
        final LinearLayout trail = new LinearLayout(Main.this);
        int leadRes;

        Row(LinearLayout parent, int icon, String t) {
            v.setGravity(Gravity.CENTER_VERTICAL);
            v.setMinimumHeight(dp(72));
            v.setPadding(dp(16), dp(12), dp(16), dp(12));
            android.util.TypedValue tv = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
            v.setBackgroundResource(tv.resourceId);
            setLead(icon, com.google.android.material.R.attr.colorOnSurfaceVariant, false);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(24), dp(24));
            ip.setMarginEnd(dp(16));
            v.addView(lead, ip);
            LinearLayout txt = column();
            title = text(t, TITLE_M);
            sub = text("", BODY);
            sub.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
            sub.setVisibility(View.GONE);
            txt.addView(title);
            txt.addView(sub);
            v.addView(txt, new LinearLayout.LayoutParams(0, -2, 1));
            trail.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
            tp.setMarginStart(dp(12));
            v.addView(trail, tp);
            if (parent != null) parent.addView(v);
        }

        void sub(String s) {
            sub.setText(s);
            sub.setVisibility(s == null || s.isEmpty() ? View.GONE : View.VISIBLE);
        }

        // Icon swaps pop in so a step turning done is noticeable.
        void setLead(int res, int tintAttr, boolean animate) {
            lead.setImageTintList(ColorStateList.valueOf(color(tintAttr)));
            if (res == leadRes) return;
            leadRes = res;
            lead.setImageResource(res);
            if (animate) {
                lead.setScaleX(0.4f);
                lead.setScaleY(0.4f);
                lead.animate().scaleX(1).scaleY(1).setDuration(300)
                        .setInterpolator(new android.view.animation.OvershootInterpolator()).start();
            }
        }

        // Numbered step: counter icon while pending, check when done, tonal action button while pending.
        void step(int n, boolean done, String s, String action, Runnable r) {
            int[] counters = {R.drawable.ic_counter_1_fill, R.drawable.ic_counter_2_fill, R.drawable.ic_counter_3_fill, R.drawable.ic_counter_4_fill};
            setLead(done ? R.drawable.ic_check_circle_fill : counters[n - 1],
                    done ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOnSurfaceVariant, true);
            sub(s);
            if (done || action == null) trail.removeAllViews();
            else button(action, r);
            if (r == null && action != null) trail.removeAllViews();
        }

        void button(String label, Runnable r) {
            trail.removeAllViews();
            MaterialButton b = new MaterialButton(Main.this, null, com.google.android.material.R.attr.materialButtonTonalStyle);
            b.setText(label);
            b.setOnClickListener(x -> r.run());
            trail.addView(b);
        }

        void trailIcon(int res, int tintAttr) {
            trail.removeAllViews();
            ImageView i = new ImageView(Main.this);
            i.setImageResource(res);
            i.setImageTintList(ColorStateList.valueOf(color(tintAttr)));
            trail.addView(i, new LinearLayout.LayoutParams(dp(24), dp(24)));
        }

        MaterialSwitch trailSwitch() {
            trail.removeAllViews();
            MaterialSwitch s = new MaterialSwitch(Main.this);
            trail.addView(s);
            return s;
        }
    }

    LinearLayout.LayoutParams margins(int w, int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, -2);
        lp.topMargin = dp(top);
        lp.bottomMargin = dp(bottom);
        return lp;
    }

    LinearLayout column() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        return c;
    }

    TextView text(String s, int style) {
        TextView t = new TextView(this);
        t.setTextAppearance(style);
        t.setText(s);
        return t;
    }

    int color(int attr) { return MaterialColors.getColor(this, attr, 0xff888888); }

    int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
