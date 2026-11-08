package com.rumahhakim.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WebUtilTest {
    @Test
    public void fileNamesStayInsideTheirFolder() {
        assertEquals("waqiah.mp3", WebUtil.safeName(WebUtil.Kind.SURAH, "Waqiah.MP3"));
        assertEquals("surah-al-kahfi.mp3", WebUtil.safeName(WebUtil.Kind.SURAH, "Surah Al Kahfi.mp3"));
        assertEquals("etc-passwd.mp3", WebUtil.safeName(WebUtil.Kind.SURAH, "../../etc/passwd.mp3"));
        assertNull(WebUtil.safeName(WebUtil.Kind.SURAH, "virus.apk"));
        assertNull(WebUtil.safeName(WebUtil.Kind.SURAH, ".mp3"));
        assertNull(WebUtil.safeName(WebUtil.Kind.SURAH, "../..mp3"));
        assertEquals("raya-2026.jpg", WebUtil.safeName(WebUtil.Kind.MASJID, "Raya 2026.jpeg"));
        assertNull(WebUtil.safeName(WebUtil.Kind.MASJID, "gambar.png"));
        // the picture names, and only that JSON
        assertEquals("captions.json", WebUtil.safeName(WebUtil.Kind.MASJID, "captions.json"));
        assertNull(WebUtil.safeName(WebUtil.Kind.MASJID, "config.json"));
        // a video reading, as the default schedule's Asmaul Husna
        assertEquals("asmaulhusna.mp4", WebUtil.safeName(WebUtil.Kind.SURAH, "asmaulhusna.mp4"));
        assertNull(WebUtil.safeName(WebUtil.Kind.ZIKIR, "05.mp4"));
        // the screensaver's videos, and nothing else
        assertEquals(WebUtil.Kind.SCREENSAVER, WebUtil.Kind.of("screensaver"));
        assertEquals("laut-malam.mp4", WebUtil.safeName(WebUtil.Kind.SCREENSAVER, "Laut Malam.MP4"));
        assertNull(WebUtil.safeName(WebUtil.Kind.SCREENSAVER, "gambar.jpg"));
    }

    @Test
    public void fixedNamesForTheAzanAndTheZikir() {
        assertEquals("azan.jpg", WebUtil.safeName(WebUtil.Kind.AZAN_GAMBAR, "IMG_2031.jpg"));
        assertEquals("peringatan.jpg", WebUtil.safeName(WebUtil.Kind.PERINGATAN_GAMBAR, "foto.jpeg"));
        assertEquals("azan-subuh.mp3", WebUtil.safeName(WebUtil.Kind.AZAN_BUNYI, "azan-subuh.mp3"));
        assertEquals("azan.mp3", WebUtil.safeName(WebUtil.Kind.AZAN_BUNYI, "rabbani.mp3"));
        assertEquals("05.m4a", WebUtil.safeName(WebUtil.Kind.ZIKIR, "5.m4a"));
        assertEquals("24.mp3", WebUtil.safeName(WebUtil.Kind.ZIKIR, "24.mp3"));
        assertNull(WebUtil.safeName(WebUtil.Kind.ZIKIR, "25.mp3"));
        assertNull(WebUtil.safeName(WebUtil.Kind.ZIKIR, "00.mp3"));
        assertNull(WebUtil.safeName(WebUtil.Kind.ZIKIR, "subhanallah.mp3"));
        assertNull(WebUtil.Kind.of("lain"));
    }

    @Test
    public void pinMustMatchAndBeFresh() {
        assertTrue(WebUtil.pinOk("1234", "1234", 2000, 1000));
        assertTrue(WebUtil.pinOk(" 1234 ", "1234", 2000, 1000));
        assertFalse(WebUtil.pinOk("1235", "1234", 2000, 1000));
        assertFalse(WebUtil.pinOk("123", "1234", 2000, 1000));
        assertFalse(WebUtil.pinOk("1234", "1234", 2000, 3000));
        assertFalse(WebUtil.pinOk("1234", "", 2000, 1000));
        assertFalse(WebUtil.pinOk(null, "1234", 2000, 1000));
    }

    @Test
    public void queryAndCookieRead() {
        assertEquals("surah", WebUtil.query("jenis=surah&nama=al%20mulk.mp3").get("jenis"));
        assertEquals("al mulk.mp3", WebUtil.query("jenis=surah&nama=al%20mulk.mp3").get("nama"));
        assertEquals("abc", WebUtil.cookie("x=1; rh=abc; y=2", "rh"));
        assertNull(WebUtil.cookie("x=1", "rh"));
    }
}
