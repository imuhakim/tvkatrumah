package com.rumahhakim.tv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The settings page for a phone on the home Wi-Fi: http://&lt;TV&gt;:8080. The phone asks for a PIN,
 * the TV shows it on screen for two minutes, and once typed in the phone keeps a login cookie. From
 * there it changes config.json, uploads and removes pictures and recordings, and tries the zikir,
 * azan and a surah. Nothing leaves the house: no account, no cloud.
 *
 * <p>Off with "tetapan_telefon": false. Only the app's own folder can be written to (WebUtil).
 */
final class WebTetapan {
    private static final String TAG = "RumahHakim.Web";
    static final int PORT = 8080;
    private static final long PIN_MS = 2 * 60_000L, LOCK_MS = 5 * 60_000L, PIN_GAP_MS = 10_000L;
    private static final int MAX_FAILS = 5, MAX_TOKENS = 6, MAX_SMALL_BODY = 256 * 1024;
    /** The keys of config.json the page may change, and nothing else. */
    private static final Set<String> KEYS = new HashSet<>(Arrays.asList("zikir", "azan", "auto_hidup",
            "jam_mula", "jam_akhir", "jarak_solat_minit", "prayer_zone", "surah", "wallpaper_minutes",
            "ambient_glow", "dock_apps", "peringatan_minit"));

    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile ServerSocket server;
    private static View pinView;

    private WebTetapan() {
    }

    /** Starts the page once per process; a no-op when it already runs or is switched off. */
    static synchronized void start(Context ctx) {
        if (server != null) return;
        final Context app = ctx.getApplicationContext();
        File dir = Config.dir(app);
        JSONObject cfg = Config.read(dir != null ? new File(dir, Config.FILE_NAME) : null);
        if (cfg != null && !cfg.optBoolean("tetapan_telefon", true)) return;
        try {
            server = new ServerSocket(PORT);
        } catch (IOException e) {
            Log.w(TAG, "cannot listen on " + PORT + ": " + e);
            return;
        }
        ExecutorService pool = Executors.newFixedThreadPool(3);
        new Thread(() -> {
            while (true) {
                try {
                    Socket s = server.accept();
                    pool.execute(() -> handle(app, s));
                } catch (IOException e) {
                    Log.w(TAG, "accept: " + e);
                    return;
                }
            }
        }, "web-tetapan").start();
        Log.i(TAG, "settings page at http://" + address() + ":" + PORT);
    }

    /** The TV's address on the home network ("192.168.0.10"), or "?" when it has none. */
    static String address() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {
            // no network
        }
        return "?";
    }

    // ---- one request -------------------------------------------------------------------------

    private static final class Request {
        String method, path;
        Map<String, String> query;
        String cookie;
        long length;
        InputStream body;
    }

    private static void handle(Context ctx, Socket s) {
        try (Socket socket = s) {
            socket.setSoTimeout(60_000);
            InputStream in = new BufferedInputStream(socket.getInputStream());
            OutputStream out = socket.getOutputStream();
            Request r = read(in);
            if (r == null) return;
            route(ctx, r, out);
        } catch (Exception e) {
            Log.w(TAG, "request failed: " + e);
        }
    }

    private static Request read(InputStream in) throws IOException {
        String first = line(in);
        if (first == null) return null;
        String[] p = first.split(" ");
        if (p.length < 2) return null;
        Request r = new Request();
        r.method = p[0];
        int q = p[1].indexOf('?');
        r.path = q < 0 ? p[1] : p[1].substring(0, q);
        r.query = WebUtil.query(q < 0 ? "" : p[1].substring(q + 1));
        for (int i = 0; i < 64; i++) {
            String h = line(in);
            if (h == null || h.isEmpty()) break;
            int c = h.indexOf(':');
            if (c < 0) continue;
            String name = h.substring(0, c).trim().toLowerCase(Locale.ROOT), value = h.substring(c + 1).trim();
            if (name.equals("content-length")) r.length = Long.parseLong(value);
            else if (name.equals("cookie")) r.cookie = WebUtil.cookie(value, "rh");
        }
        r.body = in;
        return r;
    }

    /** One header line, at most 8 KB; null at the end of the stream. */
    private static String line(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) >= 0) {
            if (c == '\n') break;
            if (c != '\r') b.write(c);
            if (b.size() > 8192) throw new IOException("header too long");
        }
        if (c < 0 && b.size() == 0) return null;
        return b.toString("ISO-8859-1");
    }

    private static String smallBody(Request r) throws IOException {
        if (r.length > MAX_SMALL_BODY) throw new IOException("body too large");
        byte[] buf = new byte[(int) r.length];
        int n = 0, k;
        while (n < buf.length && (k = r.body.read(buf, n, buf.length - n)) > 0) n += k;
        return new String(buf, 0, n, StandardCharsets.UTF_8);
    }

    private static void route(Context ctx, Request r, OutputStream out) throws Exception {
        if (r.path.equals("/") && r.method.equals("GET")) {
            try (InputStream page = ctx.getAssets().open("web/index.html")) {
                send(out, 200, "text/html; charset=utf-8", readAll(page), null);
            }
            return;
        }
        if (r.path.equals("/api/pin") && r.method.equals("POST")) {
            newPin(ctx);
            json(out, 200, new JSONObject().put("ok", true));
            return;
        }
        if (r.path.equals("/api/masuk") && r.method.equals("POST")) {
            String token = login(ctx, WebUtil.query(smallBody(r)).get("pin"));
            if (token == null) {
                json(out, 403, new JSONObject().put("ok", false).put("ralat", lockedFor(ctx) > 0
                        ? "Terlalu banyak cubaan. Cuba lagi dalam 5 minit." : "PIN salah atau sudah tamat."));
            } else {
                send(out, 200, "application/json", "{\"ok\":true}".getBytes(StandardCharsets.UTF_8),
                        "rh=" + token + "; Max-Age=31536000; Path=/; HttpOnly; SameSite=Strict");
            }
            return;
        }
        if (!loggedIn(ctx, r.cookie)) {
            json(out, 401, new JSONObject().put("ok", false).put("ralat", "Perlu PIN"));
            return;
        }
        switch (r.path) {
            case "/api/status":
                json(out, 200, status(ctx));
                return;
            case "/api/tetapan":
                if (!r.method.equals("POST")) break;
                json(out, 200, saveConfig(ctx, new JSONObject(smallBody(r))));
                return;
            case "/api/naik":
                if (!r.method.equals("POST")) break;
                json(out, 200, upload(ctx, r));
                return;
            case "/api/padam":
                if (!r.method.equals("POST")) break;
                json(out, 200, remove(ctx, r.query.get("jenis"), r.query.get("nama")));
                return;
            case "/api/fail":
                serveFile(ctx, r.query.get("jenis"), r.query.get("nama"), out);
                return;
            case "/api/uji":
                if (!r.method.equals("POST")) break;
                json(out, 200, tryOut(ctx, r.query.get("apa"), r.query.get("nama")));
                return;
            default:
                break;
        }
        json(out, 404, new JSONObject().put("ok", false).put("ralat", "Tiada"));
    }

    // ---- PIN and login ------------------------------------------------------------------------

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("web_tetapan", Context.MODE_PRIVATE);
    }

    private static long lockedFor(Context ctx) {
        return Math.max(0, prefs(ctx).getLong("lock_until", 0) - System.currentTimeMillis());
    }

    /** A fresh PIN, shown on the TV for two minutes. At most one every ten seconds. */
    private static synchronized void newPin(Context ctx) {
        SharedPreferences p = prefs(ctx);
        long now = System.currentTimeMillis();
        if (now - p.getLong("pin_at", 0) < PIN_GAP_MS || lockedFor(ctx) > 0) return;
        String pin = String.format(Locale.ROOT, "%04d", RANDOM.nextInt(10_000));
        p.edit().putString("pin", pin).putLong("pin_at", now).putLong("pin_until", now + PIN_MS).apply();
        // A TV in standby turns on, or nobody could read the PIN.
        PowerSession.wakeForSomeone(ctx);
        new Handler(Looper.getMainLooper()).postDelayed(() -> showPin(ctx, pin), 1_500);
        Log.i(TAG, "PIN shown on the TV");
    }

    private static synchronized String login(Context ctx, String given) {
        SharedPreferences p = prefs(ctx);
        long now = System.currentTimeMillis();
        if (lockedFor(ctx) > 0) return null;
        if (!WebUtil.pinOk(given, p.getString("pin", ""), p.getLong("pin_until", 0), now)) {
            int fails = p.getInt("fails", 0) + 1;
            SharedPreferences.Editor e = p.edit().putInt("fails", fails);
            // Too many wrong guesses: the PIN is spent and nobody may try for a while.
            if (fails >= MAX_FAILS) e.putInt("fails", 0).putLong("lock_until", now + LOCK_MS).remove("pin");
            e.apply();
            return null;
        }
        byte[] b = new byte[16];
        RANDOM.nextBytes(b);
        StringBuilder token = new StringBuilder();
        for (byte x : b) token.append(String.format(Locale.ROOT, "%02x", x));
        // The newest few phones stay logged in; an older one asks for a PIN again.
        String tokens = p.getString("tokens", "");
        String all = tokens.isEmpty() ? token.toString() : token + "," + tokens;
        String[] keep = all.split(",");
        all = String.join(",", Arrays.copyOf(keep, Math.min(keep.length, MAX_TOKENS)));
        p.edit().putString("tokens", all).remove("pin").putInt("fails", 0).apply();
        new Handler(Looper.getMainLooper()).post(WebTetapan::hidePin);
        return token.toString();
    }

    private static boolean loggedIn(Context ctx, String token) {
        if (token == null || !token.matches("[0-9a-f]{32}")) return false;
        for (String t : prefs(ctx).getString("tokens", "").split(",")) if (t.equals(token)) return true;
        return false;
    }

    /** The PIN in the TV's top right corner, over whatever is on screen; it takes no button presses. */
    private static void showPin(Context ctx, String pin) {
        hidePin();
        if (!Settings.canDrawOverlays(ctx)) return;
        float u = ctx.getResources().getDisplayMetrics().heightPixels / 1080f;
        TextView t = new TextView(ctx);
        t.setText("PIN telefon\n" + pin);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, 40 * u);
        t.setTextColor(0xFFFFDE8A);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(ctx.getResources().getFont(R.font.outfit), Typeface.BOLD);
        t.setLetterSpacing(0.12f);
        int pad = Math.round(28 * u);
        t.setPadding(pad * 2, pad, pad * 2, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF0061813);
        bg.setCornerRadius(24 * u);
        bg.setStroke(Math.max(1, Math.round(2 * u)), 0x99E9CB7E);
        t.setBackground(bg);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.x = Math.round(60 * u);
        lp.y = Math.round(60 * u);
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(t, lp);
        pinView = t;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (pinView == t) hidePin();
        }, PIN_MS);
    }

    private static void hidePin() {
        if (pinView == null) return;
        try {
            ((WindowManager) pinView.getContext().getSystemService(Context.WINDOW_SERVICE)).removeView(pinView);
        } catch (Exception ignored) {
            // already gone
        }
        pinView = null;
    }

    // ---- what the page reads and changes -------------------------------------------------------

    private static File configFile(Context ctx) {
        File dir = Config.dir(ctx);
        return dir != null ? new File(dir, Config.FILE_NAME) : null;
    }

    private static JSONObject status(Context ctx) throws Exception {
        File dir = Config.dir(ctx);
        JSONObject cfg = Config.read(configFile(ctx));
        if (cfg == null) cfg = new JSONObject();
        JSONObject out = new JSONObject().put("ok", true).put("tetapan", cfg);
        Config c = Config.load(ctx);
        Calendar now = Calendar.getInstance();
        PrayerTimes.Day today = c.prayerZone.isEmpty() ? null : PrayerTimes.day(ctx, c.prayerZone, PrayerTimes.key(now));
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int next = today != null ? PrayerTimes.next(today.minutes, nowMin) : -1;
        if (today != null) {
            JSONObject solat = new JSONObject();
            for (int i = 0; i < today.minutes.length; i++) solat.put(PrayerTimes.NAMES[i], PrayerTimes.format(today.minutes[i], true));
            out.put("solat", solat).put("hijri", today.hijri);
            if (next >= 0) out.put("azan_seterusnya", PrayerTimes.NAMES[next] + " " + PrayerTimes.format(today.minutes[next], true));
        }
        int nextHour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24;
        Zikir z = Zikir.forHour(nextHour);
        out.put("zikir_seterusnya", String.format(Locale.ROOT, "%02d:00 %s", nextHour, z.rumi));
        JSONArray zikir = new JSONArray();
        for (Zikir x : Zikir.ALL) {
            zikir.put(new JSONObject().put("no", x.number).put("rumi", x.rumi).put("maksud", x.maksud));
        }
        out.put("zikir", zikir);
        JSONObject files = new JSONObject();
        for (String d : new String[]{"masjid", "zikir", "surah"}) {
            JSONArray list = new JSONArray();
            File[] fs = dir != null ? new File(dir, d).listFiles() : null;
            if (fs != null) {
                Arrays.sort(fs);
                for (File f : fs) if (f.isFile() && !f.getName().endsWith(".json")) list.put(f.getName());
            }
            files.put(d, list);
        }
        files.put("azan_gambar", dir != null && new File(dir, "azan.jpg").isFile());
        files.put("peringatan_gambar", dir != null && new File(dir, "peringatan.jpg").isFile());
        files.put("azan_bunyi", dir != null && new File(dir, "azan.mp3").isFile());
        files.put("azan_subuh", dir != null && new File(dir, "azan-subuh.mp3").isFile());
        out.put("fail", files).put("alamat", "http://" + address() + ":" + PORT);
        return out;
    }

    /** Merges the allowed keys into config.json, then lets the alarms and Home pick them up. */
    private static synchronized JSONObject saveConfig(Context ctx, JSONObject changes) throws Exception {
        File f = configFile(ctx);
        if (f == null) return new JSONObject().put("ok", false).put("ralat", "Storan TV tidak tersedia");
        JSONObject cfg = Config.read(f);
        if (cfg == null) cfg = new JSONObject();
        for (java.util.Iterator<String> it = changes.keys(); it.hasNext(); ) {
            String k = it.next();
            if (KEYS.contains(k)) cfg.put(k, changes.get(k));
        }
        File tmp = new File(f.getPath() + ".new");
        try (OutputStream os = new FileOutputStream(tmp)) {
            os.write(cfg.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        if (!tmp.renameTo(f)) return new JSONObject().put("ok", false).put("ralat", "Tidak dapat menyimpan");
        applied(ctx);
        return new JSONObject().put("ok", true);
    }

    /** After a change: the alarms are set again and Home reloads with it. */
    private static void applied(Context ctx) {
        new Handler(Looper.getMainLooper()).post(() -> {
            ZikirAlarm.scheduleNext(ctx);
            AzanAlarm.scheduleNext(ctx);
            MainActivity.reload();
        });
    }

    private static JSONObject upload(Context ctx, Request r) throws Exception {
        WebUtil.Kind kind = WebUtil.Kind.of(r.query.get("jenis"));
        String name = WebUtil.safeName(kind, r.query.get("nama"));
        if (name == null) return new JSONObject().put("ok", false).put("ralat", "Jenis atau nama fail tidak dibenarkan");
        if (r.length <= 0 || r.length > kind.maxBytes) {
            return new JSONObject().put("ok", false).put("ralat", "Fail terlalu besar (had " + (kind.maxBytes >> 20) + " MB)");
        }
        File base = Config.dir(ctx);
        File dir = kind.dir.isEmpty() ? base : new File(base, kind.dir);
        if (base == null || (!dir.isDirectory() && !dir.mkdirs())) return new JSONObject().put("ok", false).put("ralat", "Storan TV tidak tersedia");
        File tmp = new File(dir, "." + name + ".part"), out = new File(dir, name);
        long left = r.length;
        try (OutputStream os = new FileOutputStream(tmp)) {
            byte[] buf = new byte[64 * 1024];
            int k;
            while (left > 0 && (k = r.body.read(buf, 0, (int) Math.min(buf.length, left))) > 0) {
                os.write(buf, 0, k);
                left -= k;
            }
        }
        if (left > 0 || !tmp.renameTo(out)) {
            tmp.delete();
            return new JSONObject().put("ok", false).put("ralat", "Muat naik terputus; cuba lagi");
        }
        // One recording per zikir: another type of the same number would win the race.
        if (kind == WebUtil.Kind.ZIKIR) {
            String num = name.substring(0, 2);
            for (String t : kind.types) if (!name.endsWith("." + t)) new File(dir, num + "." + t).delete();
        }
        Log.i(TAG, "uploaded " + kind + " " + name + " (" + r.length + " bytes)");
        applied(ctx);
        return new JSONObject().put("ok", true).put("nama", name);
    }

    private static JSONObject remove(Context ctx, String jenis, String nama) throws Exception {
        WebUtil.Kind kind = WebUtil.Kind.of(jenis);
        String name = WebUtil.safeName(kind, nama);
        File base = Config.dir(ctx);
        if (name == null || base == null) return new JSONObject().put("ok", false).put("ralat", "Tidak dibenarkan");
        File f = new File(kind.dir.isEmpty() ? base : new File(base, kind.dir), name);
        boolean gone = f.delete();
        if (gone) applied(ctx);
        return new JSONObject().put("ok", gone);
    }

    private static void serveFile(Context ctx, String jenis, String nama, OutputStream out) throws Exception {
        WebUtil.Kind kind = WebUtil.Kind.of(jenis);
        String name = WebUtil.safeName(kind, nama);
        File base = Config.dir(ctx);
        File f = name == null || base == null ? null : new File(kind.dir.isEmpty() ? base : new File(base, kind.dir), name);
        if (f == null || !f.isFile()) {
            json(out, 404, new JSONObject().put("ok", false));
            return;
        }
        String type = name.endsWith(".jpg") ? "image/jpeg" : name.endsWith(".m4a") ? "audio/mp4"
                : name.endsWith(".ogg") ? "audio/ogg" : name.endsWith(".wav") ? "audio/wav" : "audio/mpeg";
        String head = "HTTP/1.1 200 OK\r\nContent-Type: " + type + "\r\nContent-Length: " + f.length()
                + "\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.US_ASCII));
        try (InputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[64 * 1024];
            int k;
            while ((k = in.read(buf)) > 0) out.write(buf, 0, k);
        }
        out.flush();
    }

    /** Plays the zikir, the azan or one surah file now, on the TV. */
    private static JSONObject tryOut(Context ctx, String what, String nama) throws Exception {
        switch (what == null ? "" : what) {
            case "zikir":
                ctx.sendBroadcast(new Intent(ZikirAlarm.ACTION).setClass(ctx, ZikirAlarm.Receiver.class)
                        .putExtra(ZikirAlarm.EXTRA_NOW, true));
                return new JSONObject().put("ok", true);
            case "azan": {
                File dir = Config.dir(ctx);
                if (dir != null) new File(dir, "azan-now").createNewFile();
                ctx.sendBroadcast(new Intent(AzanAlarm.ACTION).setClass(ctx, AzanAlarm.Receiver.class));
                return new JSONObject().put("ok", true);
            }
            case "surah": {
                String name = WebUtil.safeName(WebUtil.Kind.SURAH, nama);
                if (name == null) return new JSONObject().put("ok", false).put("ralat", "Pilih fail surah");
                Intent svc = new Intent(ctx, SurahPlayer.class).putExtra(SurahPlayer.EXTRA_FILE, name);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(svc);
                else ctx.startService(svc);
                return new JSONObject().put("ok", true);
            }
            default:
                return new JSONObject().put("ok", false).put("ralat", "Tiada");
        }
    }

    // ---- replies -----------------------------------------------------------------------------

    private static void json(OutputStream out, int code, JSONObject body) throws IOException {
        send(out, code, "application/json; charset=utf-8", body.toString().getBytes(StandardCharsets.UTF_8), null);
    }

    private static void send(OutputStream out, int code, String type, byte[] body, String cookie) throws IOException {
        String reason = code == 200 ? "OK" : code == 401 ? "Unauthorized" : code == 403 ? "Forbidden" : "Not Found";
        StringBuilder h = new StringBuilder("HTTP/1.1 ").append(code).append(' ').append(reason).append("\r\n")
                .append("Content-Type: ").append(type).append("\r\n")
                .append("Content-Length: ").append(body.length).append("\r\n")
                .append("Cache-Control: no-store\r\nConnection: close\r\n");
        if (cookie != null) h.append("Set-Cookie: ").append(cookie).append("\r\n");
        h.append("\r\n");
        out.write(h.toString().getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[16 * 1024];
        int k;
        while ((k = in.read(buf)) > 0) b.write(buf, 0, k);
        return b.toByteArray();
    }
}
