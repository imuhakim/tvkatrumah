package com.rumahhakim.tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

/**
 * The app's own screen: when the next zikir comes and which one, whether everything it needs is in
 * place, a button to try it now, and the twelve zikir with the credits for the bundled recordings.
 * Opening it once after install also lets Android deliver the alarms (a never-opened app gets none).
 */
public class TetapanActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        float u = getResources().getDisplayMetrics().heightPixels / 1080f;

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Math.round(120 * u), Math.round(70 * u), Math.round(120 * u), Math.round(70 * u));
        col.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF0A2A22, 0xFF040E0C}));

        col.addView(text(getString(R.string.app_name), 56 * u, 0xFFE9CB7E));
        status = text("", 26 * u, 0xFFF4EEDC);
        status.setPadding(0, Math.round(16 * u), 0, Math.round(24 * u));
        col.addView(status);

        Button test = new Button(this);
        test.setText("Cuba zikir sekarang");
        test.setTextSize(TypedValue.COMPLEX_UNIT_PX, 26 * u);
        test.setOnClickListener(v -> sendBroadcast(new Intent(ZikirAlarm.ACTION).setClass(this, ZikirAlarm.Receiver.class)
                .putExtra(ZikirAlarm.EXTRA_NOW, true)));
        col.addView(test, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        Button azan = new Button(this);
        azan.setText("Cuba skrin azan sekarang");
        azan.setTextSize(TypedValue.COMPLEX_UNIT_PX, 26 * u);
        // azan-now asks Azan.takeDue for the next prayer's azan at once (the launcher's own test hook).
        azan.setOnClickListener(v -> {
            java.io.File dir = Config.dir(this);
            try {
                if (dir != null) new java.io.File(dir, "azan-now").createNewFile();
            } catch (java.io.IOException ignored) {
                // the broadcast then finds nothing due, and nothing shows
            }
            sendBroadcast(new Intent(AzanAlarm.ACTION).setClass(this, AzanAlarm.Receiver.class));
        });
        col.addView(azan, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        StringBuilder list = new StringBuilder();
        for (Zikir z : Zikir.ALL) {
            list.append(String.format(Locale.ROOT, "Jam %02d  ·  %s  —  %s\n", z.number % 24, z.rumi, z.maksud));
        }
        TextView zikir = text(list.toString(), 20 * u, 0xCCF4EEDC);
        zikir.setPadding(0, Math.round(30 * u), 0, Math.round(20 * u));
        col.addView(zikir);
        col.addView(text("Rakaman audio: Wikimedia Commons — ArabicAudios (CC BY-SA 3.0), Assem khidhr (CC BY-SA 4.0), "
                + "iSurrender (CC BY-SA 3.0), أحمد ناجي (CC BY-SA 4.0), باسم (domain awam).", 16 * u, 0x88F4EEDC));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
        test.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        long at = ZikirAlarm.scheduleNext(this);
        AzanAlarm.scheduleNext(this);
        Config cfg = Config.load(this);
        if (!cfg.prayerZone.isEmpty() && PrayerTimes.due(this, cfg.prayerZone, PrayerTimes.key(Calendar.getInstance()))) {
            new Thread(() -> {
                PrayerTimes.refresh(getApplicationContext(), cfg.prayerZone);
                runOnUiThread(() -> {
                    AzanAlarm.scheduleNext(this);
                    showStatus();
                });
            }, "solat").start();
        }
        showStatus(at);
    }

    private void showStatus() {
        showStatus(ZikirAlarm.scheduleNext(this));
    }

    private void showStatus(long at) {
        Config cfg = Config.load(this);
        StringBuilder s = new StringBuilder();
        if (at > 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(at);
            Zikir z = Zikir.forHour(c.get(Calendar.HOUR_OF_DAY));
            s.append(String.format(Locale.ROOT, "Zikir seterusnya: jam %d:00  —  %s\n", c.get(Calendar.HOUR_OF_DAY), z.rumi));
            String why = ZikirAlarm.skipReason(this, cfg, c);
            if (why != null && !"screen off".equals(why)) s.append("(jam itu dilangkau: ").append(malay(why)).append(")\n");
        } else {
            s.append("Zikir dimatikan dalam tetapan.\n");
        }
        s.append("Waktu: jam ").append(cfg.firstHour).append(" hingga jam ").append(cfg.lastHour);
        Calendar now = Calendar.getInstance();
        PrayerTimes.Day today = cfg.prayerZone.isEmpty() ? null : PrayerTimes.day(this, cfg.prayerZone, PrayerTimes.key(now));
        if (!cfg.prayerZone.isEmpty()) {
            s.append("  ·  zon ").append(cfg.prayerZone).append(today != null ? " (waktu solat ada)" : " (waktu solat belum dimuat turun)");
        }
        int next = today != null ? PrayerTimes.next(today.minutes, now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)) : -1;
        if (!cfg.azan) {
            s.append("\nAzan dimatikan dalam tetapan (\"azan\": false).");
        } else if (next >= 0) {
            s.append("\nAzan seterusnya: ").append(PrayerTimes.NAMES[next]).append(", jam ")
                    .append(PrayerTimes.format(today.minutes[next], true));
        } else {
            s.append(today != null ? "\nAzan seterusnya: Subuh esok." : "\nAzan hidup, tetapi waktu solat belum ada.");
        }
        java.io.File dir = Config.dir(this);
        boolean picture = dir != null && (new java.io.File(dir, "azan.jpg").isFile() || new java.io.File(dir, "azan.png").isFile());
        boolean sound = dir != null && new java.io.File(dir, "azan.mp3").isFile();
        s.append(picture ? "  ·  gambar azan ada" : "  ·  tiada gambar azan").append(sound ? "  ·  bunyi azan ada" : "  ·  tiada bunyi azan");
        s.append("\n\nTetapan dari telefon (Wi-Fi rumah): http://").append(WebTetapan.address()).append(':').append(WebTetapan.PORT);
        if (!Settings.canDrawOverlays(this)) s.append("\nBelum dibenarkan papar di atas app lain (jalankan tools\\pasang.ps1).");
        status.setText(s);
    }

    private static String malay(String why) {
        if (why.startsWith("outside")) return "di luar waktu zikir";
        if (why.equals("near a prayer time")) return "dekat waktu solat";
        return why;
    }

    private TextView text(String s, float px, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTextColor(color == 0 ? Color.WHITE : color);
        t.setGravity(Gravity.START);
        return t;
    }
}
