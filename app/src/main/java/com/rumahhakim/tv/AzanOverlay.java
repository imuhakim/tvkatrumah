package com.rumahhakim.tv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
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
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.File;
import java.util.Calendar;

/**
 * The azan over whatever app is in front (YouTube, Netflix, Live TV): the same full-screen azan
 * screen as on Home ({@link AzanView}, the family's picture), and the azan playing.
 * The app underneath is asked to pause (audio focus) and carries on afterwards. Any button on the
 * remote stops it; it also ends by itself once the azan has finished.
 *
 * <p>Started by AzanAlarm at the prayer time. Needs "display over other apps", which the install
 * scripts grant ({@code appops set com.rumahhakim.tv SYSTEM_ALERT_WINDOW allow}). Runs only for the
 * length of the azan.
 */
public class AzanOverlay extends Service {
    private static final String TAG = "TvHome.Azan";
    private static final String CHANNEL = "azan";
    private static final long SILENT_MS = 60_000L, AFTER_SOUND_MS = 4_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable finish = this::finish;
    private WindowManager wm;
    private Band band;
    private MediaPlayer player;
    private AudioManager audio;
    private AudioFocusRequest focus;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        goForeground();
        if (band != null) return START_NOT_STICKY;
        Config cfg = Config.load(this);
        int index = Azan.takeDue(this, cfg.prayerZone);
        if (index < 0 || !Settings.canDrawOverlays(this)) {
            if (index >= 0) Log.w(TAG, "no permission to show over other apps; run appops set "
                    + getPackageName() + " SYSTEM_ALERT_WINDOW allow");
            stopSelf();
            PowerSession.done(this);
            return START_NOT_STICKY;
        }
        show(cfg.prayerZone, index);
        play(index);
        Log.i(TAG, "azan " + PrayerTimes.NAMES[index] + " over another app");
        return START_NOT_STICKY;
    }

    private void goForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Azan", NotificationManager.IMPORTANCE_LOW));
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        Notification n = b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Azan").build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, n);
        }
    }

    private void show(String zone, int index) {
        PrayerTimes.Day today = PrayerTimes.day(this, zone, PrayerTimes.key(Calendar.getInstance()));
        boolean is24 = android.text.format.DateFormat.is24HourFormat(this);
        String time = today != null ? PrayerTimes.format(today.minutes[index], is24)
                + (is24 ? "" : today.minutes[index] < 12 * 60 ? " AM" : " PM") : "";
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        band = new Band(this, PrayerTimes.NAMES[index], time, today != null ? today.hijri : "");
        // Full screen, like on Home: the family's picture over whatever app was in front.
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                // Focusable, so the remote's next press comes here and stops the azan.
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.OPAQUE);
        wm.addView(band, lp);
        band.enter();
    }

    private void play(int index) {
        File sound = Azan.sound(this, index);
        if (sound == null) {
            handler.postDelayed(finish, SILENT_MS);
            return;
        }
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        // The video underneath pauses for the azan, and may carry on after it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs).build();
            audio.requestAudioFocus(focus);
        }
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(attrs);
            player.setDataSource(sound.getPath());
            player.setOnCompletionListener(mp -> handler.postDelayed(finish, AFTER_SOUND_MS));
            player.setOnErrorListener((mp, what, extra) -> {
                Log.w(TAG, "sound failed: " + what + "/" + extra);
                handler.postDelayed(finish, SILENT_MS);
                return true;
            });
            player.setOnPreparedListener(MediaPlayer::start);
            player.prepareAsync();
        } catch (Exception e) {
            Log.w(TAG, "cannot play " + sound + ": " + e.getMessage());
            handler.postDelayed(finish, SILENT_MS);
        }
    }

    /** Stops the sound, lets the app underneath have its sound back and slides the band away. */
    private void finish() {
        handler.removeCallbacks(finish);
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
        final Band b = band;
        band = null;
        b.leave(() -> {
            try {
                wm.removeView(b);
            } catch (IllegalArgumentException ignored) {
                // already gone
            }
            stopSelf();
            PowerSession.done(this);
        });
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(finish);
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

    /**
     * The azan screen over another app: AzanView full screen, fading in and out, and any button on
     * the remote stops the azan.
     */
    private final class Band extends FrameLayout {
        private final AzanView view;

        Band(Context ctx, String name, String time, String hijri) {
            super(ctx);
            view = new AzanView(ctx, name, time, hijri, false);
            addView(view, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            setFocusable(true);
            setFocusableInTouchMode(true);
            setAlpha(0f);
            setContentDescription("Telah masuk waktu " + name);
        }

        void enter() {
            view.start();
            animate().alpha(1f).setDuration(700).start();
            post(this::requestFocus);
        }

        void leave(Runnable done) {
            animate().alpha(0f).setDuration(600).withEndAction(done).start();
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            // Any button stops the azan; that press does nothing else.
            if (event.getAction() == KeyEvent.ACTION_DOWN) finish();
            return true;
        }
    }
}
