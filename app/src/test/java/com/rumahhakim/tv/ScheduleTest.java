package com.rumahhakim.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class ScheduleTest {
    private static final TimeZone KL = TimeZone.getTimeZone("Asia/Kuala_Lumpur");

    @Test
    public void everyHourOfTheDayIsInTheDefaultSpan() {
        for (int h = 0; h < 24; h++) assertTrue(Schedule.inSpan(h, 0, 23));
    }

    @Test
    public void spanPastMidnightWraps() {
        assertTrue(Schedule.inSpan(23, 22, 6));
        assertTrue(Schedule.inSpan(3, 22, 6));
        assertFalse(Schedule.inSpan(12, 22, 6));
    }

    @Test
    public void hourNextToZohorIsSkipped() {
        int[] mlk01 = {5 * 60 + 45, 12 * 60 + 57, 16 * 60 + 17, 18 * 60 + 57, 20 * 60 + 8};
        assertTrue(Schedule.nearPrayer(13 * 60, mlk01, 10));   // 13:00, Zohor 12:57
        assertTrue(Schedule.nearPrayer(19 * 60, mlk01, 10));   // 19:00, Maghrib 18:57
        assertFalse(Schedule.nearPrayer(10 * 60, mlk01, 10));
        assertFalse(Schedule.nearPrayer(10 * 60, null, 10));
    }

    @Test
    public void nextHourIsTheComingWholeHour() {
        Calendar c = Calendar.getInstance(KL);
        c.set(2026, Calendar.OCTOBER, 29, 9, 59, 30);
        Calendar next = Calendar.getInstance(KL);
        next.setTimeInMillis(Schedule.nextHour(c.getTimeInMillis(), KL));
        assertEquals(10, next.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, next.get(Calendar.MINUTE));

        c.set(2026, Calendar.OCTOBER, 29, 23, 0, 0);
        next.setTimeInMillis(Schedule.nextHour(c.getTimeInMillis(), KL));
        assertEquals(0, next.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, next.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void hijriDateReadsLikeTheAzanScreen() {
        assertEquals("18 Jamadilawal 1448", PrayerTimes.hijri("1448-05-18"));
        assertEquals("1 Ramadan 1448", PrayerTimes.hijri("1448-09-01"));
        assertEquals("", PrayerTimes.hijri("not a date"));
    }

    @Test
    public void eachHourOfTheDayHasItsOwnZikir() {
        assertEquals(1, Zikir.forHour(1).number);
        assertEquals(13, Zikir.forHour(13).number);
        assertEquals(24, Zikir.forHour(0).number);
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int h = 0; h < 24; h++) {
            assertEquals(h == 0 ? 24 : h, Zikir.forHour(h).number);
            seen.add(Zikir.forHour(h).arabic);
        }
        assertEquals(24, seen.size());
    }

    @Test
    public void reminderComesFiveMinutesBeforeTheNextPrayer() {
        int[] mlk01 = {5 * 60 + 45, 12 * 60 + 57, 16 * 60 + 17, 18 * 60 + 57, 20 * 60 + 8};
        assertEquals(1, Peringatan.nextIndex(mlk01, 12 * 60 + 51, 5));   // 12:51: Zohor's reminder at 12:52
        assertEquals(2, Peringatan.nextIndex(mlk01, 12 * 60 + 52, 5));   // at 12:52 it is due now; next is Asar
        assertEquals(0, Peringatan.nextIndex(mlk01, -1, 5));             // tomorrow: Subuh first
        assertEquals(-1, Peringatan.nextIndex(mlk01, 20 * 60 + 3, 5));   // after Isyak's reminder
    }

    @Test
    public void surahTakesItsHourAndDay() {
        Surah waqiah = new Surah(4, -1, "al-Waqiah", new String[]{"waqiah.mp3"});
        Surah jumuah = new Surah(12, java.util.Calendar.FRIDAY, "al-Jumu'ah", new String[]{"jumuah.mp3"});
        assertTrue(waqiah.at(4, java.util.Calendar.MONDAY));
        assertFalse(waqiah.at(5, java.util.Calendar.MONDAY));
        assertTrue(jumuah.at(12, java.util.Calendar.FRIDAY));
        assertFalse(jumuah.at(12, java.util.Calendar.THURSDAY));
        assertEquals(java.util.Calendar.FRIDAY, Surah.day("Jumaat"));
        assertEquals(-1, Surah.day("semua"));
        assertEquals(-1, Surah.day(""));
    }

    @Test
    public void lastRemotePressComesFromDumpsysPower() {
        String dump = "Power Manager State:\n  mWakefulness=Awake\n  mLastWakeTime=5000 (12 ms ago)\n"
                + "  mLastUserActivityTime=4200 (812 ms ago)\n  mLastUserActivityTimeNoChangeLights=0 (... ms ago)\n";
        assertEquals(4200L, PowerSession.lastUserActivity(dump));
        assertEquals(-1L, PowerSession.lastUserActivity("nothing here"));
        // Someone pressed the remote after the TV was woken: it stays on.
        assertFalse(PowerSession.nobodyThere(9000L, 4000L));
        // Nothing since the wake (the wake itself counts as activity): back to sleep.
        assertTrue(PowerSession.nobodyThere(4100L, 4000L));
    }
}
