package com.lazyneil.matte;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.color.MaterialColors;

// Hero preview: a mock notification banner over a colorful backdrop. Glass = blurred backdrop through a thin tint,
// matte = the same solid colors ColorOS uses on its no-blur path (#404040 dark, #fafafa light).
final class Preview extends FrameLayout {
    final Blobs back, copy;
    final View tint;
    final FrameLayout banner;
    final boolean dark;
    final int solid;
    float t = -1;
    ValueAnimator anim;

    Preview(Context c, boolean dark) {
        super(c);
        this.dark = dark;
        solid = dark ? 0xff404040 : 0xfffafafa;
        int[] colors = {MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary),
                MaterialColors.getColor(this, com.google.android.material.R.attr.colorTertiary),
                MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondary)};
        int base = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHighest);
        back = new Blobs(c, colors, base);
        addView(back, new LayoutParams(-1, -1));
        roundCorners(this, dp(20));

        banner = new FrameLayout(c);
        roundCorners(banner, dp(20));
        copy = new Blobs(c, colors, base);
        banner.addView(copy, new LayoutParams(-1, -1));
        LinearLayout row = new LinearLayout(c);
        tint = row; // the row's background is the tint over the blurred copy
        row.setBackgroundColor(solid);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        int ink = dark ? Color.WHITE : 0xff1f1f1f;
        ImageView icon = new ImageView(c);
        icon.setImageResource(R.drawable.ic_notifications);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(ink));
        row.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        LinearLayout txt = new LinearLayout(c);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPadding(dp(14), 0, 0, 0);
        txt.addView(text(c, "lazyneil", com.google.android.material.R.style.TextAppearance_Material3_LabelLarge, ink));
        txt.addView(text(c, "Dinner at 8? Can you read this?", com.google.android.material.R.style.TextAppearance_Material3_BodyMedium, ink));
        row.addView(txt);
        banner.addView(row, new LayoutParams(-1, -2));

        LayoutParams lp = new LayoutParams(-1, -2, Gravity.CENTER_VERTICAL);
        lp.setMargins(dp(16), 0, dp(16), 0);
        addView(banner, lp);
    }

    // The blurred copy draws the backdrop as if seen through the banner.
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        copy.offset(banner.getLeft(), banner.getTop(), getWidth(), getHeight());
    }

    void set(boolean matte, boolean animate) {
        float to = matte ? 1 : 0;
        if (t == to) return;
        if (anim != null) anim.cancel();
        if (!animate || t < 0) { frame(to); return; }
        anim = ValueAnimator.ofFloat(t, to);
        anim.setDuration(300);
        anim.setInterpolator(new PathInterpolator(0.05f, 0.7f, 0.1f, 1f)); // M3 emphasized decelerate
        anim.addUpdateListener(a -> frame((float) a.getAnimatedValue()));
        anim.start();
    }

    void frame(float v) {
        t = v;
        tint.getBackground().setAlpha(Math.round(255 * (0.28f + 0.72f * v)));
        if (Build.VERSION.SDK_INT >= 31) {
            float r = dp(28) * (1 - v);
            copy.setRenderEffect(r < 0.5f ? null : RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP));
        }
    }

    static void roundCorners(View v, int r) {
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline o) { o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r); }
        });
        v.setClipToOutline(true);
    }

    static TextView text(Context c, String s, int style, int color) {
        TextView t = new TextView(c);
        t.setTextAppearance(style);
        t.setTextColor(color);
        t.setText(s);
        return t;
    }

    int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    // Soft color shapes standing in for a wallpaper.
    static final class Blobs extends View {
        final int[] colors;
        final int base;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float ox, oy, w, h;

        Blobs(Context c, int[] colors, int base) { super(c); this.colors = colors; this.base = base; }

        // No size of its own: fills whatever the parent gives it, never makes the banner taller.
        @Override protected void onMeasure(int w, int h) {
            setMeasuredDimension(MeasureSpec.getMode(w) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(w) : 0,
                    MeasureSpec.getMode(h) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(h) : 0);
        }

        void offset(float x, float y, float fullW, float fullH) { ox = x; oy = y; w = fullW; h = fullH; invalidate(); }

        @Override protected void onDraw(Canvas c) {
            float W = w > 0 ? w : getWidth(), H = h > 0 ? h : getHeight();
            c.translate(-ox, -oy);
            c.drawColor(base);
            float[][] spots = {{0.18f, 0.30f, 0.34f}, {0.62f, 0.78f, 0.30f}, {0.88f, 0.18f, 0.26f}};
            for (int i = 0; i < spots.length; i++) {
                p.setColor(colors[i]);
                c.drawCircle(W * spots[i][0], H * spots[i][1], W * spots[i][2] * 0.6f, p);
            }
        }
    }
}
