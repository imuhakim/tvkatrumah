package com.rumahhakim.tv;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The phone settings page's rules, with no Android in them so the tests can run them: which file
 * names may be written where, how a query string reads, and when a PIN is accepted.
 */
final class WebUtil {
    /** What each kind of upload may be called and how large it may be. */
    enum Kind {
        AZAN_GAMBAR("", new String[]{"jpg"}, 15 << 20),
        PERINGATAN_GAMBAR("", new String[]{"jpg"}, 15 << 20),
        AZAN_BUNYI("", new String[]{"mp3"}, 30 << 20),
        // captions.json names the pictures; no other JSON may go there
        MASJID("masjid", new String[]{"jpg", "json"}, 15 << 20),
        ZIKIR("zikir", new String[]{"mp3", "m4a", "ogg", "wav"}, 5 << 20),
        // mp4: a full-screen video reading (SurahPlayer), such as the Asmaul Husna
        SURAH("surah", new String[]{"mp3", "m4a", "mp4"}, 120 << 20),
        // videos played without sound behind the screensaver's clock (IslamicDream)
        SCREENSAVER("screensaver", new String[]{"mp4"}, 200 << 20);

        final String dir;
        final String[] types;
        final long maxBytes;

        Kind(String dir, String[] types, long maxBytes) {
            this.dir = dir;
            this.types = types;
            this.maxBytes = maxBytes;
        }

        static Kind of(String name) {
            switch (name == null ? "" : name) {
                case "azan-gambar": return AZAN_GAMBAR;
                case "peringatan-gambar": return PERINGATAN_GAMBAR;
                case "azan-bunyi": return AZAN_BUNYI;
                case "masjid": return MASJID;
                case "zikir": return ZIKIR;
                case "surah": return SURAH;
                case "screensaver": return SCREENSAVER;
                default: return null;
            }
        }
    }

    private WebUtil() {
    }

    /**
     * The name a file of this kind is stored under, or null when the name is not allowed. Only
     * letters, digits, '-' and '_' survive, so nothing can reach outside the app's own folder.
     */
    static String safeName(Kind kind, String name) {
        if (kind == null || name == null) return null;
        String n = name.trim().toLowerCase(Locale.ROOT);
        int dot = n.lastIndexOf('.');
        if (dot <= 0) return null;
        String ext = n.substring(dot + 1);
        if (ext.equals("jpeg")) ext = "jpg";
        boolean typeOk = false;
        for (String t : kind.types) typeOk |= t.equals(ext);
        if (!typeOk) return null;
        String base = n.substring(0, dot).replaceAll("[^a-z0-9_-]", "-").replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (base.isEmpty() || base.length() > 60) return null;
        switch (kind) {
            case AZAN_GAMBAR:
                return "azan.jpg";
            case PERINGATAN_GAMBAR:
                return "peringatan.jpg";
            case AZAN_BUNYI:
                return base.contains("subuh") ? "azan-subuh.mp3" : "azan.mp3";
            case MASJID:
                return ext.equals("json") ? (base.equals("captions") ? "captions.json" : null) : base + "." + ext;
            case ZIKIR: {
                // 01 to 24, the zikir's number
                if (!base.matches("\\d{1,2}")) return null;
                int z = Integer.parseInt(base);
                return z >= 1 && z <= 24 ? String.format(Locale.ROOT, "%02d.%s", z, ext) : null;
            }
            default:
                return base + "." + ext;
        }
    }

    /** "a=1&b=x%20y" to {a: 1, b: x y}; a broken escape leaves that pair out. */
    static Map<String, String> query(String q) {
        Map<String, String> out = new HashMap<>();
        if (q == null || q.isEmpty()) return out;
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq), v = eq < 0 ? "" : pair.substring(eq + 1);
            try {
                out.put(URLDecoder.decode(k, "UTF-8"), URLDecoder.decode(v, "UTF-8"));
            } catch (Exception ignored) {
                // left out
            }
        }
        return out;
    }

    /** True when {@code given} is the PIN on screen and that PIN has not yet run out. */
    static boolean pinOk(String given, String pin, long expiresAt, long now) {
        if (given == null || pin == null || pin.isEmpty() || now > expiresAt) return false;
        // Compared in full every time, so the time taken says nothing about which digits were right.
        byte[] a = given.trim().getBytes(StandardCharsets.US_ASCII), b = pin.getBytes(StandardCharsets.US_ASCII);
        int diff = a.length ^ b.length;
        for (int i = 0; i < Math.min(a.length, b.length); i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }

    /** The value of one cookie in a Cookie header, or null. */
    static String cookie(String header, String name) {
        if (header == null) return null;
        for (String part : header.split(";")) {
            String p = part.trim();
            if (p.startsWith(name + "=")) return p.substring(name.length() + 1);
        }
        return null;
    }
}
