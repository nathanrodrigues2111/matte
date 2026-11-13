package com.lazyneil.matte;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

// Plain white backdrop so the test banner shows on the hardest background for glass. Tap to close.
public class White extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        View v = new View(this);
        v.setBackgroundColor(Color.WHITE);
        v.setOnClickListener(x -> finish());
        setContentView(v);
        v.postDelayed(() -> Main.postTest(this), 700);
    }
}
