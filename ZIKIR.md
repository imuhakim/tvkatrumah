# Senarai zikir

Satu zikir untuk setiap jam (zikir 24 pada tengah malam). Rakam setiap satu 10 hingga 15 saat, contohnya dengan
telefon, dan simpan dengan nombor zikir sebagai nama fail: `01.mp3` hingga `24.mp3` (`.m4a`, `.ogg` dan `.wav`
juga boleh). Muat naik dari halaman tetapan telefon, atau letak dalam `media/zikir/` dan
jalankan `tools/pasang.py tetapan`. Rakaman yang lebih panjang
dari 15 saat diperlahankan dan dihentikan oleh app pada saat ke-15. Repo ini sudah ada 24 suara (ElevenLabs);
rakaman sendiri menggantikannya.

Jam yang ada surah (lihat `"surah"` dalam tetapan) memainkan surah, bukan zikir, kecuali surah dengan
`"zikir_dulu": true`. Jam yang terlalu dekat dengan waktu solat juga dilangkau, kerana azan didahulukan.

| Fail | Jam | Zikir |
|---|---|---|
| `01.mp3` | 01:00 | Subhanallah |
| `02.mp3` | 02:00 | Alhamdulillah |
| `03.mp3` | 03:00 | Allahu Akbar |
| `04.mp3` | 04:00 | Asyhadu an la ilaha illallah, wa asyhadu anna Muhammadar rasulullah |
| `05.mp3` | 05:00 | Astaghfirullahal 'azim |
| `06.mp3` | 06:00 | Subhanallahi wa bihamdihi, subhanallahil 'azim |
| `07.mp3` | 07:00 | La hawla wa la quwwata illa billah |
| `08.mp3` | 08:00 | Hasbunallah wa ni'mal wakil |
| `09.mp3` | 09:00 | Allahumma salli 'ala Muhammad wa 'ala ali Muhammad, kama sallaita 'ala Ibrahim wa 'ala ali Ibrahim, innaka hamidum majid |
| `10.mp3` | 10:00 | Bismillahir rahmanir rahim |
| `11.mp3` | 11:00 | Subhanallah, walhamdulillah, wa la ilaha illallah, wallahu akbar |
| `12.mp3` | 12:00 | Rabbana atina fid dunya hasanah, wa fil akhirati hasanah, wa qina 'azaban nar |
| `13.mp3` | 13:00 | La ilaha illa anta subhanaka inni kuntu minaz zalimin |
| `14.mp3` | 14:00 | Hasbiyallahu la ilaha illa huwa, 'alaihi tawakkaltu wa huwa rabbul 'arsyil 'azim |
| `15.mp3` | 15:00 | La ilaha illallahu wahdahu la syarika lah, lahul mulku wa lahul hamdu wa huwa 'ala kulli syai'in qadir |
| `16.mp3` | 16:00 | Rabbighfirli wa tub 'alayya innaka antat tawwabur rahim |
| `17.mp3` | 17:00 | Ya Hayyu ya Qayyum, bi rahmatika astaghith |
| `18.mp3` | 18:00 | Allahumma innaka 'afuwwun tuhibbul 'afwa fa'fu 'anni |
| `19.mp3` | 19:00 | Rabbana la tuzigh qulubana ba'da idz hadaitana |
| `20.mp3` | 20:00 | Rabbi zidni 'ilma |
| `21.mp3` | 21:00 | Allahumma salli wa sallim 'ala nabiyyina Muhammad |
| `22.mp3` | 22:00 | Rabbana hab lana min azwajina wa dzurriyyatina qurrata a'yun, waj'alna lil muttaqina imama |
| `23.mp3` | 23:00 | Allahumma a'inni 'ala dzikrika wa syukrika wa husni 'ibadatik |
| `24.mp3` | 00:00 | Bismillahil ladzi la yadhurru ma'asmihi syai'un fil ardhi wa la fis sama'i wa huwas sami'ul 'alim |

Teks Arab dan maksud penuh: [Zikir.java](app/src/main/java/com/rumahhakim/tv/Zikir.java).
