package com.rumahhakim.tv;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * A surah the family plays at a set hour, from MP3 files they push to the TV (files/surah/), listed
 * in config.json. The hour's zikir gives way to it.
 *
 * <pre>
 * "surah": [
 *   { "jam": 4, "nama": "al-Waqiah", "fail": ["waqiah.mp3"] },
 *   { "jam": 8, "hari": "jumaat", "nama": "ad-Dhuha, al-Insyirah, al-Kahfi", "zikir_dulu": true,
 *     "fail": ["dhuha.mp3", "alinsyirah.mp3", "alkahfi.mp3"] },
 *   { "jam": 12, "hari": "jumaat", "nama": "al-Jumu'ah", "fail": ["jumuah.mp3"] }
 * ]
 * </pre>
 *
 * The first entry that fits the hour and day wins, so a Friday entry goes before an every-day one
 * at the same hour.
 */
final class Surah {
    static final String DIR = "surah";

    final int hour, day;
    final String name;
    final String[] files;
    /** The hour's zikir comes first, and the surah follows it ("zikir_dulu"). */
    final boolean zikirFirst;

    /** {@code day} is a Calendar day of the week, or -1 for every day. */
    Surah(int hour, int day, String name, String[] files) {
        this(hour, day, name, files, false);
    }

    Surah(int hour, int day, String name, String[] files, boolean zikirFirst) {
        this.hour = hour;
        this.day = day;
        this.name = name;
        this.files = files;
        this.zikirFirst = zikirFirst;
    }

    boolean at(int hourOfDay, int dayOfWeek) {
        return hour == hourOfDay && (day < 0 || day == dayOfWeek);
    }

    /** "jumaat" (or "friday") to Calendar.FRIDAY, and so on; -1 for "semua", empty or unknown. */
    static int day(String name) {
        switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "ahad": case "sunday": return Calendar.SUNDAY;
            case "isnin": case "monday": return Calendar.MONDAY;
            case "selasa": case "tuesday": return Calendar.TUESDAY;
            case "rabu": case "wednesday": return Calendar.WEDNESDAY;
            case "khamis": case "thursday": return Calendar.THURSDAY;
            case "jumaat": case "jumat": case "friday": return Calendar.FRIDAY;
            case "sabtu": case "saturday": return Calendar.SATURDAY;
            default: return -1;
        }
    }

    /** The files of this surah that are on the TV, in order. */
    List<File> sounds(Context ctx) {
        List<File> out = new ArrayList<>();
        File dir = Config.dir(ctx);
        for (String f : files) {
            File s = dir != null ? new File(new File(dir, DIR), f) : null;
            if (s != null && s.isFile() && s.length() > 0) out.add(s);
        }
        return out;
    }

    /** Every surah in config.json; an entry without an hour or files is left out. */
    static List<Surah> load(Context ctx) {
        List<Surah> out = new ArrayList<>();
        File dir = Config.dir(ctx);
        JSONObject json = Config.read(dir != null ? new File(dir, Config.FILE_NAME) : null);
        JSONArray list = json != null ? json.optJSONArray("surah") : null;
        for (int i = 0; list != null && i < list.length(); i++) {
            JSONObject o = list.optJSONObject(i);
            if (o == null) continue;
            int hour = o.optInt("jam", -1);
            JSONArray f = o.optJSONArray("fail");
            if (hour < 0 || hour > 23 || f == null || f.length() == 0) continue;
            String[] files = new String[f.length()];
            for (int k = 0; k < files.length; k++) files[k] = new File(f.optString(k)).getName();
            out.add(new Surah(hour, day(o.optString("hari", "")), o.optString("nama", "").trim(), files,
                    o.optBoolean("zikir_dulu", false)));
        }
        return out;
    }

    /** The surah set for this hour and day whose files are on the TV, or null. */
    static Surah due(Context ctx, Calendar now) {
        for (Surah s : load(ctx)) {
            if (s.at(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.DAY_OF_WEEK)) && !s.sounds(ctx).isEmpty()) return s;
        }
        return null;
    }
}
