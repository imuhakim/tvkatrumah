package com.rumahhakim.tv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;

/**
 * The hour's zikir over whatever is on screen: a band across the lower part of the screen with the
 * Arabic, how it is read and what it means, and its sound when it has one. With a sound the app
 * underneath is asked to pause (audio focus) and carries on afterwards; without one nothing is
 * interrupted. Any button on the remote closes it, and it closes by itself after the sound, or after
 * {@link #SILENT_MS} without one.
 */
public class ZikirOverlay extends Service {
    private static final String CHANNEL = "zikir";
    private static final long SILENT_MS = 15_000L, AFTER_SOUND_MS = 3_000L, LONGEST_MS = 60_000L;
    /** A recording longer than this is faded out here, so every zikir stays within 10 to 15 seconds. */
    private static final long SOUND_CAP_MS = 15_000L;
    private static final String[] SOUND_TYPES = {".mp3", ".ogg", ".wav", ".m4a"};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable finish = this::finish;
    private WindowManager wm;
    private View band;
    private MediaPlayer player;
    private AudioManager audio;
    private AudioFocusRequest focus;
    /** The hour's surah follows this zikir (ZikirAlarm.EXTRA_THEN_SURAH). */
    private boolean thenSurah;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        goForeground();
        if (band != null) return START_NOT_STICKY;
        thenSurah = intent != null && intent.getBooleanExtra(ZikirAlarm.EXTRA_THEN_SURAH, false);
        if (!Settings.canDrawOverlays(this)) {
            Log.w(ZikirAlarm.TAG, "no permission to show over other apps; run appops set "
                    + getPackageName() + " SYSTEM_ALERT_WINDOW allow");
            stopSelf();
            return START_NOT_STICKY;
        }
        Calendar now = Calendar.getInstance();
        Zikir z = Zikir.forHour(now.get(Calendar.HOUR_OF_DAY));
        show(z, String.format(Locale.ROOT, "%d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)));
        play(z);
        handler.postDelayed(finish, LONGEST_MS);
        Log.i(ZikirAlarm.TAG, "zikir " + z.number + " " + z.rumi);
        return START_NOT_STICKY;
    }

    private void goForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Zikir", NotificationManager.IMPORTANCE_LOW));
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        Notification n = b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Zikir").build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, n);
        }
    }

    private void show(Zikir z, String time) {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int h = getResources().getDisplayMetrics().heightPixels;
        float u = h / 1080f;

        LinearLayout box = new LinearLayout(this) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                // Any button closes the zikir; that press does nothing else.
                if (event.getAction() == KeyEvent.ACTION_DOWN) finish();
                return true;
            }
        };
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        box.setPadding(Math.round(120 * u), Math.round(70 * u), Math.round(120 * u), Math.round(40 * u));
        box.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                // Dark from the top edge down, so the words stay readable over bright app tiles.
                new int[]{0xC0061813, 0xF0061813, 0xFA040E0C}));
        box.setFocusable(true);
        box.setFocusableInTouchMode(true);

        box.addView(text("ZIKIR  ·  " + time, 22 * u, 0xFFE9CB7E, 0.25f));
        TextView arabic = text(z.arabic, (z.arabic.length() > 60 ? 46 : 64) * u, Color.WHITE, 0f);
        arabic.setPadding(0, Math.round(10 * u), 0, Math.round(6 * u));
        box.addView(arabic);
        box.addView(text(z.rumi, 30 * u, 0xFFF4EEDC, 0.02f));
        TextView maksud = text(z.maksud, 26 * u, 0xCCF4EEDC, 0f);
        maksud.setPadding(0, Math.round(6 * u), 0, Math.round(14 * u));
        box.addView(maksud);
        box.addView(text("Tekan mana-mana butang untuk tutup", 18 * u, 0x88F4EEDC, 0f));
        band = box;

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                // Focusable, so the remote's next press comes here and closes it.
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM;
        box.setAlpha(0f);
        box.setTranslationY(60 * u);
        wm.addView(box, lp);
        box.animate().alpha(1f).translationY(0f).setDuration(600).start();
        box.post(box::requestFocus);
    }

    private TextView text(String s, float px, int color, float spacing) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTextColor(color);
        t.setLetterSpacing(spacing);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setShadowLayer(6f, 0f, 2f, 0x99000000);
        return t;
    }

    private void play(Zikir z) {
        String prefix = String.format(Locale.ROOT, "%02d", z.number);
        AssetFileDescriptor asset = null;
        File own = null;
        File dir = Config.dir(this);
        // The family's own recording (files/zikir/NN.mp3 ...) comes before the bundled one.
        for (String type : SOUND_TYPES) {
            File f = dir != null ? new File(dir, "zikir/" + prefix + type) : null;
            if (f != null && f.isFile() && f.length() > 0) {
                own = f;
                break;
            }
        }
        if (own == null) {
            try {
                String[] names = getAssets().list("zikir");
                for (String n : names != null ? names : new String[0]) {
                    if (n.startsWith(prefix + "-")) {
                        asset = getAssets().openFd("zikir/" + n);
                        break;
                    }
                }
            } catch (Exception e) {
                Log.w(ZikirAlarm.TAG, "bundled sound: " + e.getMessage());
            }
        }
        if (own == null && asset == null) {
            handler.postDelayed(finish, SILENT_MS);
            return;
        }
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs).build();
            audio.requestAudioFocus(focus);
        }
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(attrs);
            if (own != null) {
                player.setDataSource(own.getPath());
            } else {
                player.setDataSource(asset.getFileDescriptor(), asset.getStartOffset(), asset.getLength());
                asset.close();
            }
            // Short clips still leave the words on screen long enough to read.
            long shownAt = System.currentTimeMillis();
            player.setOnCompletionListener(mp -> handler.postDelayed(finish,
                    Math.max(AFTER_SOUND_MS, SILENT_MS - (System.currentTimeMillis() - shownAt))));
            player.setOnErrorListener((mp, what, extra) -> {
                Log.w(ZikirAlarm.TAG, "sound failed: " + what + "/" + extra);
                handler.postDelayed(finish, SILENT_MS);
                return true;
            });
            player.setOnPreparedListener(mp -> {
                mp.start();
                handler.postDelayed(this::fadeOut, SOUND_CAP_MS);
            });
            player.prepareAsync();
        } catch (Exception e) {
            Log.w(ZikirAlarm.TAG, "cannot play zikir " + z.number + ": " + e.getMessage());
            handler.postDelayed(finish, SILENT_MS);
        }
    }

    /** Lowers the sound over a second, then ends the zikir: for recordings longer than the cap. */
    private void fadeOut() {
        if (player == null) return;
        for (int i = 1; i <= 10; i++) {
            final float v = 1f - i / 10f;
            handler.postDelayed(() -> {
                if (player != null) player.setVolume(v, v);
            }, i * 100L);
        }
        handler.postDelayed(finish, 1_100L);
    }

    /** Stops the sound, gives the app underneath its sound back and slides the band away. */
    private void finish() {
        handler.removeCallbacksAndMessages(null);
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
                // not prepared yet
            }
            player.release();
            player = null;
        }
        if (focus != null && audio != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audio.abandonAudioFocusRequest(focus);
            focus = null;
        }
        if (band == null) {
            stopSelf();
            return;
        }
        final View b = band;
        band = null;
        b.animate().alpha(0f).translationY(40f).setDuration(500).withEndAction(() -> {
            try {
                wm.removeView(b);
            } catch (IllegalArgumentException ignored) {
                // already gone
            }
            stopSelf();
            if (thenSurah) ZikirAlarm.start(this, SurahPlayer.class);
            else PowerSession.done(this);
        }).start();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (band != null) {
            try {
                wm.removeView(band);
            } catch (IllegalArgumentException ignored) {
                // already gone
            }
            band = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
