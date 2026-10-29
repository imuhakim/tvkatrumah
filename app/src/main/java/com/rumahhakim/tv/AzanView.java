package com.rumahhakim.tv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import java.io.File;

/**
 * The azan screen: the family's own picture (azan.jpg or azan.png in the app's folder on the TV,
 * changed with tools\tetapan.ps1 and no rebuild) full screen, darkened towards the bottom, and over
 * it "TELAH MASUK WAKTU", the prayer's name, and its time with the Hijri date. Without a picture, a
 * plain night-teal background. Used by Home, the screensaver and, over other apps, AzanOverlay.
 *
 * <p>Sized from the screen's height, like the screensaver's clock. Adapted from Clear TV Islamic's
 * crescent screen (MIT).
 */
final class AzanView extends View {
    private static final String[] PICTURES = {"azan.jpg", "azan.jpeg", "azan.png"};

    private final String header, name, line, hintText;
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint info = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backgroundPaint = new Paint();
    private final Paint shade = new Paint();
    private final Paint picturePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Matrix fit = new Matrix();
    private final Bitmap picture;
    private int sizedFor;

    AzanView(Context ctx, String name, String time, String hijri, boolean reducedMotion) {
        this(ctx, "TELAH MASUK WAKTU", name, hijri.isEmpty() ? time : time + "   ·   " + hijri,
                "Tekan mana-mana butang untuk hentikan azan", PICTURES);
    }

    /** The same screen with other words and pictures: the five-minute reminder (Peringatan). */
    AzanView(Context ctx, String header, String name, String line, String hintLine, String[] pictures) {
        super(ctx);
        this.header = header;
        this.name = name;
        this.line = line;
        this.hintText = hintLine;
        android.util.DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        picture = picture(ctx, dm.widthPixels, dm.heightPixels, pictures);
        Typeface outfit = ctx.getResources().getFont(R.font.outfit);
        for (Paint p : new Paint[]{small, big, info, hint}) {
            p.setTypeface(outfit);
            p.setTextAlign(Paint.Align.CENTER);
        }
        // Outfit has no bold of its own; the stroke is thickened by hand so the line reads over light pictures.
        small.setColor(0xFFFFDE8A);
        small.setFakeBoldText(true);
        small.setLetterSpacing(0.25f);
        big.setColor(Color.WHITE);
        big.setLetterSpacing(0.02f);
        info.setColor(0xE6F4EEDC);
        info.setLetterSpacing(0.04f);
        hint.setColor(0x88F4EEDC);
        for (Paint p : new Paint[]{small, big, info}) p.setShadowLayer(10f, 0f, 2f, 0xCC000000);
        setAlpha(0f);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription("Telah masuk waktu " + name);
    }

    void start() {
        animate().alpha(1f).setDuration(1200).start();
        invalidate();
    }

    void stop() {
        // Nothing animates: the picture is still, so there is nothing to stop drawing.
    }

    /** The family's picture, decoded no larger than the screen needs; null when there is none. */
    private static Bitmap picture(Context ctx, int w, int h, String[] names) {
        File dir = Config.dir(ctx);
        for (String n : names) {
            File f = dir != null ? new File(dir, n) : null;
            if (f == null || !f.isFile() || f.length() == 0) continue;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), o);
            int sample = 1;
            while (o.outWidth / (sample * 2) >= w && o.outHeight / (sample * 2) >= h) sample *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            Bitmap b = BitmapFactory.decodeFile(f.getPath(), o);
            if (b != null) return b;
        }
        return null;
    }

    private void size(int w, int h) {
        sizedFor = w * 31 + h;
        float u = h / 1080f;
        backgroundPaint.setShader(new RadialGradient(w / 2f, h * 0.36f, Math.max(w, h) * 0.75f,
                new int[]{0xFF154A3E, 0xFF0A2620, 0xFF040E0C}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        // Dark from the lower middle down, so the words read over any picture.
        shade.setShader(new LinearGradient(0, h * 0.38f, 0, h, new int[]{0x00061813, 0xD9061813, 0xF7040E0C},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        if (picture != null) {
            float s = Math.max(w / (float) picture.getWidth(), h / (float) picture.getHeight());
            fit.setScale(s, s);
            fit.postTranslate((w - picture.getWidth() * s) / 2f, (h - picture.getHeight() * s) / 2f);
        }
        small.setTextSize(42f * u);
        big.setTextSize(150f * u);
        info.setTextSize(40f * u);
        hint.setTextSize(20f * u);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        if (sizedFor != w * 31 + h) size(w, h);
        if (picture != null) canvas.drawBitmap(picture, fit, picturePaint);
        else canvas.drawRect(0, 0, w, h, backgroundPaint);
        canvas.drawRect(0, h * 0.38f, w, h, shade);

        float u = h / 1080f, cx = w / 2f;
        canvas.drawText(header, cx, h - 380f * u, small);
        canvas.drawText(name, cx, h - 210f * u, big);
        canvas.drawText(line, cx, h - 120f * u, info);
        canvas.drawText(hintText, cx, h - 64f * u, hint);
    }
}
