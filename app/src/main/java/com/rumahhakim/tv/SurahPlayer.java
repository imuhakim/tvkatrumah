package com.rumahhakim.tv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Plays the hour's surah (Surah) from the family's MP3 files, one after another, with a band across
 * the bottom of the screen naming it. The app underneath gives up its sound until the surah ends.
 * Any button on the remote stops it, and so does the azan (it takes the sound away).
 */
public class SurahPlayer extends Service {
    private static final String TAG = "RumahHakim.Surah";
    private static final String CHANNEL = "surah";
    /** Plays this one file from files/surah/ instead of the hour's surah: a try-out from the phone. */
    static final String EXTRA_FILE = "fail";
    /** A surah never holds the TV longer than this, whatever the files say. */
    private static final long LONGEST_MS = 60 * 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable finish = this::finish;
    private final List<File> queue = new ArrayList<>();
    private WindowManager wm;
    private View band;
    private MediaPlayer player;
    private AudioManager audio;
    private AudioFocusRequest focus;
    private AudioAttributes attrs;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        goForeground();
        if (band != null) return START_NOT_STICKY;
        // One file to try out from the phone's settings page, or the surah set for this hour.
        String file = intent != null ? intent.getStringExtra(EXTRA_FILE) : null;
        Surah surah = file != null
                ? new Surah(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), -1, file.replaceAll("\\.[a-z0-9]+$", ""), new String[]{file})
                : Surah.due(this, Calendar.getInstance());
        if (surah != null && surah.sounds(this).isEmpty()) surah = null;
        if (surah == null || !Settings.canDrawOverlays(this)) {
            Log.w(TAG, surah == null ? "no surah due" : "no permission to show over other apps");
            stopSelf();
            PowerSession.done(this);
            return START_NOT_STICKY;
        }
        queue.addAll(surah.sounds(this));
        show(surah);
        attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The azan taking the sound ends the surah; it does not resume over it.
            focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(change -> {
                        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            handler.post(finish);
                        }
                    }).build();
            audio.requestAudioFocus(focus);
        }
        Log.i(TAG, "surah " + surah.name + ": " + queue.size() + " fail");
        playNext();
        handler.postDelayed(finish, LONGEST_MS);
        return START_NOT_STICKY;
    }

    private void goForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Surah", NotificationManager.IMPORTANCE_LOW));
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        Notification n = b.setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Surah").build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(4, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(4, n);
        }
    }

    private void show(Surah surah) {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        float u = getResources().getDisplayMetrics().heightPixels / 1080f;
        Typeface outfit = getResources().getFont(R.font.outfit);
        LinearLayout box = new LinearLayout(this) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                // Any button stops the surah; that press does nothing else.
                if (event.getAction() == KeyEvent.ACTION_DOWN) finish();
                return true;
            }
        };
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        box.setPadding(Math.round(120 * u), Math.round(60 * u), Math.round(120 * u), Math.round(36 * u));
        box.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xC0061813, 0xF0061813, 0xFA040E0C}));
        box.setFocusable(true);
        box.setFocusableInTouchMode(true);
        Calendar now = Calendar.getInstance();
        box.addView(text(String.format(Locale.ROOT, "BACAAN  ·  %d:%02d", now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE)), 26 * u, 0xFFFFDE8A, 0.25f, outfit));
        String name = surah.name.isEmpty() ? "Surah" : surah.name.startsWith("Surah") ? surah.name : "Surah " + surah.name;
        box.addView(text(name, 64 * u, 0xFFFFFFFF, 0.02f, outfit));
        box.addView(text("Tekan mana-mana butang untuk hentikan", 18 * u, 0x88F4EEDC, 0f, outfit));
        band = box;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM;
        box.setAlpha(0f);
        wm.addView(box, lp);
        box.animate().alpha(1f).setDuration(600).start();
        box.post(box::requestFocus);
    }

    private TextView text(String s, float px, int color, float spacing, Typeface font) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTextColor(color);
        t.setLetterSpacing(spacing);
        t.setTypeface(font);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setShadowLayer(8f, 0f, 2f, 0xAA000000);
        return t;
    }

    /** The next file in the queue; when there is none left, the surah ends. */
    private void playNext() {
        releasePlayer();
        if (queue.isEmpty()) {
            handler.postDelayed(finish, 2_000);
            return;
        }
        File f = queue.remove(0);
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(attrs);
            player.setDataSource(f.getPath());
            player.setOnCompletionListener(mp -> playNext());
            player.setOnErrorListener((mp, what, extra) -> {
                Log.w(TAG, "sound failed: " + f + " " + what + "/" + extra);
                handler.post(this::playNext);
                return true;
            });
            player.setOnPreparedListener(MediaPlayer::start);
            player.prepareAsync();
        } catch (Exception e) {
            Log.w(TAG, "cannot play " + f + ": " + e.getMessage());
            handler.post(this::playNext);
        }
    }

    private void releasePlayer() {
        if (player == null) return;
        try {
            player.stop();
        } catch (IllegalStateException ignored) {
            // not prepared yet
        }
        player.release();
        player = null;
    }

    /** Stops the sound, gives the app underneath its sound back, removes the band. */
    private void finish() {
        handler.removeCallbacksAndMessages(null);
        queue.clear();
        releasePlayer();
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
        b.animate().alpha(0f).setDuration(500).withEndAction(() -> {
            try {
                wm.removeView(b);
            } catch (IllegalArgumentException ignored) {
                // already gone
            }
            stopSelf();
            PowerSession.done(this);
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
        releasePlayer();
        super.onDestroy();
    }
}
