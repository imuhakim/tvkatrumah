package com.rumahhakim.tv;

/**
 * When the zikir comes, with no Android in it so the tests can run it: on every hour from
 * {@code firstHour} to {@code lastHour}, except an hour too close to a prayer time, where the azan
 * has the screen.
 */
final class Schedule {
    private Schedule() {
    }

    /** True when the hour (0 to 23) is inside the day's span; a span past midnight (22 to 6) wraps. */
    static boolean inSpan(int hour, int firstHour, int lastHour) {
        if (firstHour <= lastHour) return hour >= firstHour && hour <= lastHour;
        return hour >= firstHour || hour <= lastHour;
    }

    /** True when a prayer time is within {@code gap} minutes of {@code minuteOfDay}, either side. */
    static boolean nearPrayer(int minuteOfDay, int[] prayerMinutes, int gap) {
        if (prayerMinutes == null) return false;
        for (int p : prayerMinutes) {
            int d = Math.abs(p - minuteOfDay);
            if (Math.min(d, 24 * 60 - d) <= gap) return true;
        }
        return false;
    }

    /** The first whole hour strictly after {@code nowMillis}, in the TV's time zone. */
    static long nextHour(long nowMillis, java.util.TimeZone tz) {
        java.util.Calendar c = java.util.Calendar.getInstance(tz);
        c.setTimeInMillis(nowMillis);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        c.add(java.util.Calendar.HOUR_OF_DAY, 1);
        return c.getTimeInMillis();
    }
}
