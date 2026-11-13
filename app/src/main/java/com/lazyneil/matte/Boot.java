package com.lazyneil.matte;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// Starting the process is the point: App registers the Shizuku listener. If Shizuku is already up, apply now.
public class Boot extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        PendingResult r = goAsync();
        new Thread(() -> {
            if (Matte.waitReady(8000)) Matte.autoApply(c.getApplicationContext());
            r.finish();
        }).start();
    }
}
