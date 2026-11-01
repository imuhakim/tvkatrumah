package com.rumahhakim.tv;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wakes the TV from standby for the azan, the zikir or a surah ("auto_hidup": true), and puts it
 * back to sleep afterwards when nobody touched the remote meanwhile. Only a TV this app woke is ever
 * put to sleep: one someone turned on, or pressed a button on since, stays on.
 *
 * <p>"Touched the remote" is read from the TV's own power service (mLastUserActivityTime, which every
 * button press moves on, and the wake itself sets), through the debugging connection CacheCleaner
 * already has. Without that connection the TV simply stays on.
 */
final class PowerSession {
    private static final String TAG = "RumahHakim.Kuasa";
    static final String ACTION_CHECK = "com.rumahhakim.tv.TIDUR";
    /** Slack after the wake within which activity still counts as the wake itself. */
    private static final long WAKE_SLACK_MS = 2_000;
    /**
     * In case nothing reports the end (a crash, a stuck sound): look again this long after the wake.
     * Longer than the longest surah (SurahPlayer stops at 60 minutes), so it never cuts one short.
     */
    private static final long SAFETY_MS = 70 * 60_000L;
    private static final Pattern LAST_ACTIVITY = Pattern.compile("mLastUserActivityTime=(\\d+)");
    /** Between looks at whether anything is still on screen or playing. */
    private static final long SETTLE_MS = 3_000;
    /** The longest a check waits for the sound to end: past the longest surah. */
    private static final long LONGEST_WAIT_MS = 75 * 60_000L;
    private static final AtomicInteger BUSY = new AtomicInteger();
    private static final AtomicBoolean CHECKING = new AtomicBoolean();

    private PowerSession() {
    }

    /** Wakes the TV if it is asleep and the settings allow it; true when the screen is on now. */
    static boolean wakeIfAllowed(Context ctx, Config cfg) {
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (pm == null || pm.isInteractive()) return true;
        if (!cfg.autoOn) return false;
        @SuppressWarnings("deprecation")
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "rumahhakim:hidup");
        wl.acquire(5_000);
        // The TV wakes into whatever app was in front when it was turned off (Netflix's profile page,
        // say): Home comes to the front instead, with the zikir, surah or azan over it. Not the
        // screensaver: its window sits above the overlays and would hide them.
        try {
            ctx.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .setPackage(ctx.getPackageName()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            Log.w(TAG, "cannot bring Home to the front: " + e.getMessage());
        }
        prefs(ctx).edit().putLong("woke_at", SystemClock.uptimeMillis()).apply();
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + SAFETY_MS, check(ctx));
        }
        Log.i(TAG, "TV dihidupkan");
        return true;
    }

    /**
     * Turns the screen on for someone who is there and asked for it (the phone's PIN): no sleep is
     * planned afterwards, the TV's own screen timeout applies.
     */
    static void wakeForSomeone(Context ctx) {
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (pm == null || pm.isInteractive()) return;
        @SuppressWarnings("deprecation")
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "rumahhakim:pin");
        wl.acquire(5_000);
        Log.i(TAG, "TV dihidupkan untuk PIN telefon");
    }

    /** The azan, zikir, surah or reminder now on screen: the TV never sleeps while any is. */
    static void begin() {
        BUSY.incrementAndGet();
    }

    /** One of them has ended: when it was the last, see whether the TV may sleep. */
    static void end(Context ctx) {
        if (BUSY.decrementAndGet() < 0) BUSY.set(0);
        done(ctx);
    }

    /**
     * Called when the azan, zikir or surah has ended, or had nothing to show: if this app woke the TV,
     * it waits until nothing of this app is on screen or playing (the azan on Home can still be going
     * when the azan over other apps finds nothing to do), then puts the TV back to sleep if nobody has
     * pressed a button since the wake. Runs off the main thread; one check at a time.
     */
    static void done(Context ctx) {
        final Context app = ctx.getApplicationContext();
        if (prefs(app).getLong("woke_at", 0) == 0 || !CHECKING.compareAndSet(false, true)) return;
        new Thread(() -> {
            try {
                AudioManager audio = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
                long until = SystemClock.uptimeMillis() + LONGEST_WAIT_MS;
                // A moment first, so the next thing (the surah after a zikir) can begin.
                do {
                    SystemClock.sleep(SETTLE_MS);
                } while ((BUSY.get() > 0 || (audio != null && audio.isMusicActive()))
                        && SystemClock.uptimeMillis() < until);
                long wokeAt = prefs(app).getLong("woke_at", 0);
                if (wokeAt == 0) return;
                String dump = CacheCleaner.run(app, CacheCleaner.POWER_STATE);
                long last = dump != null ? lastUserActivity(dump) : -1;
                if (BUSY.get() > 0) return;   // something began during the check; it will call again
                prefs(app).edit().remove("woke_at").apply();
                AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
                if (am != null) am.cancel(check(app));
                if (last < 0) {
                    Log.i(TAG, "tidak dapat membaca butang remote terakhir; TV dibiarkan hidup");
                } else if (nobodyThere(last, wokeAt)) {
                    CacheCleaner.run(app, CacheCleaner.SLEEP);
                    Log.i(TAG, "tiada sesiapa tekan remote; TV ditidurkan semula");
                } else {
                    Log.i(TAG, "remote ditekan; TV kekal hidup");
                }
            } finally {
                CHECKING.set(false);
            }
        }, "tidur").start();
    }

    /** The uptime of the last button press (or wake) in a dumpsys power report; -1 when absent. */
    static long lastUserActivity(String dumpsysPower) {
        Matcher m = LAST_ACTIVITY.matcher(dumpsysPower);
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    /** True when nothing happened after the wake beyond the wake itself. */
    static boolean nobodyThere(long lastActivity, long wokeAt) {
        return lastActivity <= wokeAt + WAKE_SLACK_MS;
    }

    private static PendingIntent check(Context ctx) {
        return PendingIntent.getBroadcast(ctx, 3, new Intent(ACTION_CHECK).setClass(ctx, Receiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("kuasa", Context.MODE_PRIVATE);
    }

    /** The safety check, long after a wake whose end was never reported. */
    public static final class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (ACTION_CHECK.equals(intent.getAction())) done(ctx);
        }
    }
}
