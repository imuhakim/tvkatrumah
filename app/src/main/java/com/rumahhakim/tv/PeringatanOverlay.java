package com.rumahhakim.tv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.Calendar;

/**
 * The five-minute reminder, full screen over whatever is in front: the family's reminder picture
 * (peringatan.jpg, .png or .webp in the app's folder on the TV), "LAGI 5 MINIT NAK AZAN", the
 * prayer's name and time, and three beeps of three seconds each. Any button on the remote closes it;
 * it closes by itself after {@link #SHOWN_MS}.
 */
public class PeringatanOverlay extends Service {
    private static final String CHANNEL = "peringatan";
    private static final String[] PICTURES = {"peringatan.jpg", "peringatan.jpeg", "peringatan.png", "peringatan.webp"};
    static final int BEEPS = 3;
    static final int BEEP_MS = 3_000, GAP_MS = 1_000;
    private static final long SHOWN_MS = BEEPS * (BEEP_MS + GAP_MS) + 5_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable finish = this::finish;
    private WindowManager wm;
    private FrameLayout screen;
    private ToneGenerator tone;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        goForeground();
        int index = intent != null ? intent.getIntExtra(Peringatan.EXTRA_INDEX, -1) : -1;
        if (screen != null || index < 0 || index >= PrayerTimes.NAMES.length || !Settings.canDrawOverlays(this)) {
            if (screen == null) {
                Log.i(Peringatan.TAG, "no reminder shown: index " + index + ", overlay "
                        + Settings.canDrawOverlays(this));
                stopSelf();
                PowerSession.done(this);
            }
            return START_NOT_STICKY;
        }
        show(index);
        beep();
        handler.postDelayed(finish, SHOWN_MS);
        Log.i(Peringatan.TAG, "peringatan " + PrayerTimes.NAMES[index]);
        return START_NOT_STICKY;
    }

    private void goForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Peringatan", NotificationManager.IMPORTANCE_LOW));
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        Notification n = b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Peringatan").build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(5, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(5, n);
        }
    }

    private void show(int index) {
        Config cfg = Config.load(this);
        PrayerTimes.Day today = cfg.prayerZone.isEmpty() ? null
                : PrayerTimes.day(this, cfg.prayerZone, PrayerTimes.key(Calendar.getInstance()));
        String time = today != null ? "Azan jam " + PrayerTimes.format(today.minutes[index], true) : "";
        int lead = Peringatan.minutes(this);
        AzanView view = new AzanView(this, "LAGI " + lead + " MINIT NAK AZAN", PrayerTimes.NAMES[index], time,
                "Cepat siap nak solat  ·  tekan mana-mana butang untuk tutup", PICTURES);
        screen = new FrameLayout(this) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) finish();
                return true;
            }
        };
        screen.addView(view, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        screen.setFocusable(true);
        screen.setFocusableInTouchMode(true);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.OPAQUE);
        screen.setAlpha(0f);
        wm.addView(screen, lp);
        view.start();
        screen.animate().alpha(1f).setDuration(500).start();
        screen.post(screen::requestFocus);
        PowerSession.begin();
    }

    /** Three beeps of three seconds, a second apart. */
    private void beep() {
        try {
            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
        } catch (RuntimeException e) {
            Log.w(Peringatan.TAG, "no tone generator: " + e.getMessage());
            return;
        }
        for (int i = 0; i < BEEPS; i++) {
            handler.postDelayed(() -> {
                if (tone != null) tone.startTone(ToneGenerator.TONE_SUP_DIAL, BEEP_MS);
            }, i * (long) (BEEP_MS + GAP_MS));
        }
    }

    private void finish() {
        handler.removeCallbacksAndMessages(null);
        if (tone != null) {
            tone.stopTone();
            tone.release();
            tone = null;
        }
        if (screen == null) {
            stopSelf();
            return;
        }
        final FrameLayout s = screen;
        screen = null;
        s.animate().alpha(0f).setDuration(400).withEndAction(() -> {
            try {
                wm.removeView(s);
            } catch (IllegalArgumentException ignored) {
                // already gone
            }
            stopSelf();
            PowerSession.end(this);
        }).start();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (tone != null) {
            tone.release();
            tone = null;
        }
        if (screen != null) {
            PowerSession.end(this);
            try {
                wm.removeView(screen);
            } catch (IllegalArgumentException ignored) {
                // already gone
            }
            screen = null;
        }
        super.onDestroy();
    }
}
