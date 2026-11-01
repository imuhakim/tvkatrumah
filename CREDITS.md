# Kredit

## Kod

TV Kat Rumah ialah launcher [Clear TV Islamic](https://github.com/Pokka99/cleartv-Islamic), berasaskan
[Clean Your TV](https://github.com/Gathin23/cleanyourtv) oleh Gathin (MIT), dengan zikir, surah, peringatan,
TV hidup sendiri dan halaman tetapan telefon ditambah. `tools/tv.py`, `tools/b64push.py`,
`tools/masjid-photos.py` dan `tools/masjid.json` diambil dari repo yang sama (MIT).

Waktu solat dari [e-Solat JAKIM](https://www.e-solat.gov.my).

## Fon

Outfit (`app/src/main/res/font/outfit.ttf`), The Outfit Project Authors, SIL Open Font License 1.1
([FONT-LICENSE-OFL.txt](FONT-LICENSE-OFL.txt)).

## Gambar masjid (`media/masjid/`, `media/azan.jpg`, `media/peringatan.jpg`)

Dari Wikimedia Commons, dimuat turun dengan `tools/masjid-photos.py` pada lebar 1920 piksel. Nama tempat,
jurugambar dan lesen setiap gambar ada dalam [media/masjid/captions.json](media/masjid/captions.json) dan
dipaparkan di skrin bersama gambar itu. `media/azan.jpg` ialah `00.jpg` (Masjidil Haram, Saudipics.com,
CC BY-SA 4.0) dan `media/peringatan.jpg` ialah `02.jpg` (Masjid Nabawi, Muhammad Mahdi Karim, GFDL 1.2).

## Suara zikir (`media/zikir/`)

24 rakaman dijana dengan [ElevenLabs](https://elevenlabs.io) (`tools/suara-zikir.py`, model
eleven_multilingual_v2) daripada teks dalam
[Zikir.java](app/src/main/java/com/rumahhakim/tv/Zikir.java). Ini **suara pemilik repo ini sendiri** (imuhakim),
diklon dengan ElevenLabs.

**Syarat penggunaan suara.** Rakaman ini tidak termasuk dalam lesen MIT. Hak atas suara ini kekal pada
pemiliknya. Anda boleh memuat turun dan memainkannya bersama TV Kat Rumah, termasuk versi TV Kat Rumah
yang anda ubah suai, untuk kegunaan bukan komersial. Anda tidak boleh:

- mengklon suara ini, atau menggunakannya untuk melatih mana-mana model suara atau AI
- menggunakan rakaman ini di luar TV Kat Rumah, atau mengubahnya untuk menghasilkan kata-kata lain
- menjual rakaman ini, atau menggunakannya dalam produk atau iklan

Untuk suara lain, jana semula dengan `tools/suara-zikir.py` menggunakan akaun anda sendiri, atau rakam sendiri
(lihat [ZIKIR.md](ZIKIR.md)).

## Rakaman dalam app (`app/src/main/assets/zikir/`)

Dari Wikimedia Commons; dipakai hanya bila tiada rakaman dalam `media/zikir/` untuk zikir itu. CC BY-SA:
nyatakan kredit, dan kongsi dengan lesen yang sama jika diubah.

| Fail | Zikir | Perakam | Lesen | Sumber |
|---|---|---|---|---|
| `02-alhamdulillah.ogg` | Alhamdulillah | ArabicAudios | CC BY-SA 3.0 | [Ar-الحمد لله.ogg](https://commons.wikimedia.org/wiki/File:Ar-%D8%A7%D9%84%D8%AD%D9%85%D8%AF_%D9%84%D9%84%D9%87.ogg) |
| `03-allahuakbar.ogg` | Allahu Akbar | Assem khidhr | CC BY-SA 4.0 | [Ar-eg-الله أكبر.oga](https://commons.wikimedia.org/wiki/File:Ar-eg-%D8%A7%D9%84%D9%84%D9%87_%D8%A3%D9%83%D8%A8%D8%B1.oga) |
| `04-syahadah.ogg` | Syahadah | iSurrender | CC BY-SA 3.0 | [Shahadah.ogg](https://commons.wikimedia.org/wiki/File:Shahadah.ogg) |
| `09-selawat.mp3` | Selawat Ibrahimiyah | أحمد ناجي | CC BY-SA 4.0 | [الصلاة الإبراهيمية.mp3](https://commons.wikimedia.org/wiki/File:%D8%A7%D9%84%D8%B5%D9%84%D8%A7%D8%A9_%D8%A7%D9%84%D8%A5%D8%A8%D8%B1%D8%A7%D9%87%D9%8A%D9%85%D9%8A%D8%A9.mp3) |
| `10-bismillah.wav` | Bismillah | باسم | Domain awam | [Bismillah Al-Rahman Al-Rahim-ar.wav](https://commons.wikimedia.org/wiki/File:Bismillah_Al-Rahman_Al-Rahim-ar.wav) |

## Doa Pagi dan Asmaul Husna (`media/surah/`)

`doapagi.mp3` (Doa Pagi) dan `asmaulhusna.mp4` (video Asmaul Husna) tidak termasuk dalam lesen MIT. Asal,
pemilik dan lesen kedua-dua rakaman ini belum disahkan, dan fail itu sendiri tidak menyatakan kredit. Jangan
anggap ia bebas digunakan semula. Jika anda pemilik haknya atau tahu sumbernya, sila buka issue supaya kredit
ditambah atau fail itu dibuang.

## Azan dan surah lain

Tidak disertakan dalam repo ini. `tools/pasang.py azan` memuat turun `azan.mp3` (Ustaz Asri Ibrahim,
Rabbani) dan `azan-subuh.mp3` dari
[Clear TV Islamic, themes/islamik](https://github.com/Pokka99/cleartv-Islamic/tree/main/themes/islamik),
di mana ia diterbitkan. MP3 surah ialah milik pengguna sendiri.
