package com.rumahhakim.tv;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.util.Calendar;

/**
 * "Lagi 5 minit nak azan": a few minutes before each prayer time ("peringatan_minit", 5 unless set;
 * 0 turns it off), {@link PeringatanOverlay} shows the family's reminder picture full screen and
 * beeps three times. Needs the azan on and a prayer_zone. A TV in standby is woken for it with
 * "auto_hidup", like the azan. Set again by Home, by the hourly alarm and after every reminder.
 *
 * <p>For a test: {@code am broadcast -a com.rumahhakim.tv.PERINGATAN -n com.rumahhakim.tv/.Peringatan\$Receiver --ez now true}
 */
final class Peringatan {
    static final String TAG = "RumahHakim.Peringatan";
    static final String ACTION = "com.rumahhakim.tv.PERINGATAN";
    static final String EXTRA_INDEX = "index";
    static final int DEFAULT_MINUTES = 5;

    private Peringatan() {
    }

    /** Minutes before the prayer time, from config.json; 0 when off. */
    static int minutes(Context ctx) {
        File dir = Config.dir(ctx);
        JSONObject json = Config.read(dir != null ? new File(dir, Config.FILE_NAME) : null);
        int m = json != null ? json.optInt("peringatan_minit", DEFAULT_MINUTES) : DEFAULT_MINUTES;
        return m < 0 || m > 60 ? DEFAULT_MINUTES : m;
    }

    /** The first prayer whose reminder ({@code lead} minutes before it) is still to come; -1 if none today. */
    static int nextIndex(int[] prayerMinutes, int nowMinute, int lead) {
        for (int i = 0; i < prayerMinutes.length; i++) if (prayerMinutes[i] - lead > nowMinute) return i;
        return -1;
    }

    static long scheduleNext(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return -1;
        Config cfg = Config.load(ctx);
        int lead = minutes(ctx);
        long at = -1;
        int index = -1;
        if (cfg.azan && lead > 0 && !cfg.prayerZone.isEmpty()) {
            Calendar now = Calendar.getInstance();
            for (int day = 0; day < 2 && at < 0; day++) {
                Calendar d = (Calendar) now.clone();
                d.add(Calendar.DAY_OF_MONTH, day);
                PrayerTimes.Day times = PrayerTimes.day(ctx, cfg.prayerZone, PrayerTimes.key(d));
                if (times == null) continue;
                int nowMin = day == 0 ? now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) : -1;
                index = nextIndex(times.minutes, nowMin, lead);
                if (index < 0) continue;
                int m = times.minutes[index] - lead;
                d.set(Calendar.HOUR_OF_DAY, m / 60);
                d.set(Calendar.MINUTE, m % 60);
                d.set(Calendar.SECOND, 0);
                d.set(Calendar.MILLISECOND, 0);
                at = d.getTimeInMillis();
            }
        }
        Intent intent = new Intent(ACTION).setClass(ctx, Receiver.class);
        if (at > 0) intent.putExtra(EXTRA_INDEX, index);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 5, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (at < 0) {
            am.cancel(pi);
            return -1;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setWindow(AlarmManager.RTC_WAKEUP, at, 60_000L, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
        Log.i(TAG, "next reminder " + PrayerTimes.NAMES[index] + " at " + new java.util.Date(at));
        return at;
    }

    public static final class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            Context app = ctx.getApplicationContext();
            if (ACTION.equals(intent.getAction())) {
                boolean test = intent.getBooleanExtra(ZikirAlarm.EXTRA_NOW, false);
                int index = intent.getIntExtra(EXTRA_INDEX, test ? 1 : -1);
                if (index >= 0 && PowerSession.wakeIfAllowed(app, Config.load(app))) {
                    Intent svc = new Intent(app, PeringatanOverlay.class).putExtra(EXTRA_INDEX, index);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) app.startForegroundService(svc);
                    else app.startService(svc);
                } else {
                    Log.i(TAG, "no reminder: screen off and auto_hidup off");
                }
            }
            scheduleNext(app);
        }
    }
}
