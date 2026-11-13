package com.lazyneil.matte;

import android.app.PendingIntent;
import android.content.Intent;
import android.service.quicksettings.TileService;

public class Tile extends TileService {
    @Override public void onStartListening() { show(Matte.isOn()); }

    void show(boolean on) {
        android.service.quicksettings.Tile t = getQsTile();
        if (t == null) return;
        t.setState(on ? android.service.quicksettings.Tile.STATE_ACTIVE : android.service.quicksettings.Tile.STATE_INACTIVE);
        t.setSubtitle(on ? "Matte" : "Glass");
        t.updateTile();
    }

    @Override public void onClick() {
        boolean target = !Matte.isOn();
        new Thread(() -> {
            if (!Matte.waitReady(2000)) {
                // Shizuku missing, stopped or not permitted: the app explains what to do.
                getMainExecutor().execute(() -> startActivityAndCollapse(PendingIntent.getActivity(this, 0,
                        new Intent(this, Main.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)));
                return;
            }
            getMainExecutor().execute(() -> show(target));
            Matte.apply(this, target);
        }).start();
    }
}
