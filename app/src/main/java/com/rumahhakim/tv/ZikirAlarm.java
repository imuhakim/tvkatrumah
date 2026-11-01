package com.rumahhakim.tv;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Wakes the app on every whole hour. A surah set for that hour (Surah) plays instead of the zikir;
 * otherwise {@link ZikirOverlay} shows the hour's zikir, unless the zikir is off, the hour is outside
 * jam_mula..jam_akhir or a prayer time is close (the azan has the screen then). A TV in standby is
 * woken for it with "auto_hidup" (PowerSession), and left asleep without. Set again after every
 * alarm, when the TV boots and when Home or the app is opened or updated.
 *
 * <p>For a test: {@code am broadcast -a com.rumahhakim.tv.ZIKIR -n com.rumahhakim.tv/.ZikirAlarm\$Receiver --ez now true}
 */
final class ZikirAlarm {
    static final String TAG = "RumahHakim.Zikir";
    static final String ACTION = "com.rumahhakim.tv.ZIKIR";
    static final String EXTRA_NOW = "now";
    /** On ZikirOverlay: start the hour's surah (SurahPlayer) once the zikir has ended. */
    static final String EXTRA_THEN_SURAH = "then_surah";
    /** The alarm is for a surah at this minute past the hour (its "minit"), not the hour's zikir. */
    static final String EXTRA_MINUTE = "minit";

    private ZikirAlarm() {
    }

    static long scheduleNext(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return -1;
        List<Surah> surahs = Surah.load(ctx);
        if (!Config.load(ctx).zikir && surahs.isEmpty()) {
            am.cancel(PendingIntent.getBroadcast(ctx, 0, new Intent(ACTION).setClass(ctx, Receiver.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            Log.i(TAG, "zikir is off and no surah is set");
            return -1;
        }
        // A second past the hour (or a surah's minute), so the clock on screen already says it.
        long now = System.currentTimeMillis();
        long at = Schedule.nextHour(now, TimeZone.getDefault()) + 1000;
        int minute = 0;
        for (Surah s : surahs) {
            if (s.minute == 0) continue;
            long t = Schedule.nextAt(now, TimeZone.getDefault(), s.hour, s.minute) + 1000;
            if (t < at) {
                at = t;
                minute = s.minute;
            }
        }
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0,
                new Intent(ACTION).setClass(ctx, Receiver.class).putExtra(EXTRA_MINUTE, minute),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setWindow(AlarmManager.RTC_WAKEUP, at, 60_000L, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
        Log.i(TAG, (minute == 0 ? "next zikir alarm at " : "next surah alarm at ") + new java.util.Date(at));
        return at;
    }

    /** Why this hour gets no zikir, or null when it does. */
    static String skipReason(Context ctx, Config cfg, Calendar now) {
        int hour = now.get(Calendar.HOUR_OF_DAY);
        if (!cfg.zikir) return "zikir off";
        if (!Schedule.inSpan(hour, cfg.firstHour, cfg.lastHour)) return "outside " + cfg.firstHour + ".." + cfg.lastHour;
        if (!cfg.prayerZone.isEmpty()) {
            PrayerTimes.Day today = PrayerTimes.day(ctx, cfg.prayerZone, PrayerTimes.key(now));
            if (today != null && Schedule.nearPrayer(hour * 60 + now.get(Calendar.MINUTE), today.minutes, cfg.prayerGap)) {
                return "near a prayer time";
            }
        }
        return null;
    }

    static void start(Context ctx, Class<?> service) {
        Intent svc = new Intent(ctx, service);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(svc);
        else ctx.startService(svc);
    }

    public static final class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            Context app = ctx.getApplicationContext();
            // After a boot or an update the phone's page comes back before Home is first shown.
            WebTetapan.start(app);
            if (ACTION.equals(intent.getAction())) {
                Config config = Config.load(app);
                Calendar now = Calendar.getInstance();
                boolean test = intent.getBooleanExtra(EXTRA_NOW, false);
                Surah surah = test ? null : Surah.due(app, now);
                // An alarm between the hours is only for its surah: no zikir then.
                int minute = intent.getIntExtra(EXTRA_MINUTE, 0);
                String why = surah != null || test ? null
                        : minute != 0 ? "no surah due at :" + minute
                        : skipReason(app, config, now);
                if (why != null) {
                    Log.i(TAG, "no zikir this hour: " + why);
                } else if (!PowerSession.wakeIfAllowed(app, config)) {
                    Log.i(TAG, "screen off and auto_hidup off: nothing this hour");
                } else if (surah != null && surah.zikirFirst && skipReason(app, config, now) == null) {
                    // The zikir first; ZikirOverlay starts the surah when it ends.
                    Intent svc = new Intent(app, ZikirOverlay.class).putExtra(EXTRA_THEN_SURAH, true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) app.startForegroundService(svc);
                    else app.startService(svc);
                } else {
                    start(app, surah != null ? SurahPlayer.class : ZikirOverlay.class);
                }
            }
            scheduleNext(app);
            // The five-minute reminder follows the prayer times; set again every hour in case a day changed.
            Peringatan.scheduleNext(app);
            // Keep next hours' prayer times at hand; the network is used once a year, or after a failure.
            Config cfg = Config.load(app);
            if (!cfg.prayerZone.isEmpty() && PrayerTimes.due(app, cfg.prayerZone, PrayerTimes.key(Calendar.getInstance()))) {
                PendingResult done = goAsync();
                new Thread(() -> {
                    try {
                        PrayerTimes.refresh(app, cfg.prayerZone);
                    } finally {
                        done.finish();
                    }
                }, "solat").start();
            }
        }
    }
}
