# Instructions for AI agents

You are helping someone install **TV Kat Rumah** on their own Android TV: a home screen with JAKIM prayer
times under the clock, the azan full screen at every prayer, a reminder five minutes before, a different
zikir with its voice every hour, surah readings on a schedule, a mosque-photo screensaver, the TV turning
itself on for these and off again when nobody is watching, and a settings page they open on their phone.

This file is the playbook. Follow it in order, one step at a time, and speak to the person in their own
language (Bahasa Melayu unless they write in another). It is modelled on
[Clear TV Islamic's AGENTS.md](https://github.com/Pokka99/cleartv-Islamic/blob/main/AGENTS.md); when this
file is silent on a TV-specific problem (developer options on a given model, debloating, a launcher that
takes Home back), that playbook and its `docs/TROUBLESHOOTING.md` are the reference.

## First: check the TV, before anything else

As soon as you know the brand and model, before asking for an IP address or downloading anything:

| The TV or box runs | What to do |
|---|---|
| **Android TV or Google TV, Android 8 or newer**: Sony BRAVIA (2016+), TCL, Xiaomi Mi TV / Mi Box, Philips Android TV, Sharp, Hisense (Android models), Toshiba (Android models), Chromecast with Google TV, NVIDIA Shield, Android projectors | Supported: follow this file |
| Android TV 5, 6 or 7 | Not supported: the app needs Android 8. Say so and stop |
| Amazon Fire TV (sticks, Fire TV Edition TVs) | Not supported: Fire OS takes the Home button back from any other launcher |
| Samsung (Tizen), LG (webOS), Hisense/Toshiba VIDAA, Roku, Panasonic My Home Screen, Apple TV, older set-top boxes | Not supported: these cannot run Android apps |

It is the operating system that matters, not the brand: Hisense, Toshiba, Panasonic and Philips sell both
kinds. If you are not sure, ask for a photo of Settings → About (Android TV shows "Android TV OS" or an
Android version). For a TV that cannot run it, say so plainly and suggest the cheapest way to get all of
it: a Google TV or Android TV box in an HDMI port (Chromecast with Google TV, Xiaomi Mi Box/TV Stick,
NVIDIA Shield). Never start a setup "to see if it works", and never suggest rooting.

Tested so far on a Sony KD-55X7500F (BRAVIA 4K, Android 9). Tell the person this, and don't promise more.

## Ground rules

1. **The TV belongs to a family that is using it.** Before every change to the TV, say what it does and
   how to undo it, and wait for a yes. Ask before screenshots or anything that plays sound too.
2. **Back up before changing anything** (`tools/pasang.py backup`). The backup and `backup/log.txt` stay
   on their computer (gitignored).
3. **Never uninstall anything** unless the person asks for that exact app to be uninstalled. Making TV Kat
   Rumah the Home screen does not disable or remove the TV's own launcher; `pulih` puts it back.
4. **Credentials stay with the person.** They type PINs, Wi-Fi passwords and API keys themselves, in their
   own terminal or on the TV. Never write a key into a file or ask for it in chat.
5. **Downloads need a yes.** Say what, from where and how big before downloading: Python packages
   (~5 MB), the azan audio (~9 MB, from the Clear TV Islamic repository), anything else.
6. **Every step ends with a check.** If it fails, stop and tell the person exactly what you saw. Never
   invent a fix that disables, resets or uninstalls something.
7. Run one command at a time and read its output before the next.

## Step by step

### 1. Developer options and network debugging (on the TV, by the person)

Find where this exact model hides them (the model's own support page, or the Clear TV Islamic
`examples/` folder), then guide the person one press at a time. The usual path on Android TV:
Settings → Device Preferences → About → press OK on **Build** seven times; then Settings → Device
Preferences → Developer options → **USB debugging** on (and **Network debugging** / "ADB over network" if
shown). Ask for the TV's IP address: Settings → Network & Internet → the Wi-Fi network.

### 2. Get the repository and the Python client (on the computer)

```
git clone https://github.com/imuhakim/tvkatrumah
cd tvkatrumah
python3 -m venv tools/venv                                   # Windows: py -3 -m venv tools\venv
tools/venv/bin/pip install -r tools/requirements.txt         # Windows: tools\venv\Scripts\pip install -r tools\requirements.txt
```

Below, `PY` means `tools/venv/bin/python` (Windows: `tools\venv\Scripts\python.exe`). No `adb`, JDK or
Android SDK is needed: `dist/tv-kat-rumah.apk` is ready built.

### 3. Check the TV

`PY tools/pasang.py --tv <ip> periksa`

The TV shows **"Allow USB debugging?"**: the person ticks **Always allow** and presses OK; run it again.
**Check**: it prints the model and Android version and ends with `KEPUTUSAN: SESUAI`. Anything else: stop
and explain. A timeout or "connection refused" means debugging is off or the IP is wrong.

### 4. Back up

`PY tools/pasang.py --tv <ip> backup`
**Check**: `backup/home-asal.txt` names the TV's current launcher, and no file is empty.

### 5. Ask the person, before installing

- **Their JAKIM zone** (state and district): find it on [e-solat.gov.my](https://www.e-solat.gov.my), e.g.
  `WLY01` Kuala Lumpur/Putrajaya, `SGR01` Gombak, Petaling, Sepang, Hulu Langat, Shah Alam, `JHR02` Johor
  Bahru, `MLK01` Melaka, `PNG01` Pulau Pinang, `KDH01` Kota Setar. Never guess: check that
  `https://www.e-solat.gov.my/index.php?r=esolatApi/TakwimSolat&period=today&zone=<ZONE>` returns a
  `prayerTime` list. Outside Malaysia the prayer times are not available; say so before going on.
- **The azan sound**: TV Kat Rumah does not ship one. `PY tools/pasang.py azan` downloads the recording
  Clear TV Islamic uses (Ustaz Asri Ibrahim, Rabbani; about 9 MB) into `media/`. Ask first; or they put
  their own `azan.mp3` (and `azan-subuh.mp3`) in `media/`.
- **The four dock apps**, in order: `media/config.json` has Netflix, YouTube, unifi TV and RTMKlik. List
  what the TV has with `PY tools/tv.py "pm list packages -3"` (with `TV_HOST=<ip>`) and edit `dock_apps`.
- **Surah readings** (optional): `media/config.json` schedules al-Waqiah 4:00, Yasin 5:00, ad-Dhuha and
  al-Insyirah 8:00 (with al-Kahfi on Friday), al-Jumu'ah 12:00 on Friday and al-Mulk 22:00. The MP3s are
  not in the repository: the person copies their own into `media/surah/` with those names
  (`waqiah.mp3`, `yasin.mp3`, `dhuha.mp3`, `alinsyirah.mp3`, `alkahfi.mp3`, `jumuah.mp3`, `almulk.mp3`).
  An hour whose files are missing plays the zikir instead.
- **TV turning itself on** (`auto_hidup`, on in `media/config.json`): it wakes the TV every hour, through
  the night, for the zikir. Ask whether they want that, or a narrower `jam_mula`/`jam_akhir` (e.g. 6–22).

### 6. Install and make it Home

`PY tools/pasang.py --tv <ip> pasang`

It refuses an unsuitable TV, installs the app, starts it and checks it really runs **before** making it
Home, then presses Home and checks again; on any failure it puts the old Home back. It also points the
screensaver at TV Kat Rumah (`--tanpa-screensaver` to leave it).
**Check**: it ends with `Siap: TV Kat Rumah kini Home`. A line starting with `RALAT` means it stopped and
restored the old Home.

### 7. Send the settings, pictures and voices

`PY tools/pasang.py --tv <ip> tetapan --zon <ZONE>`

It sends `media/`: `config.json` with their zone, the mosque photos, the azan and reminder pictures, the
24 zikir voices, and the azan and surah MP3s if they are there.
**Checks**, one at a time (ask before the ones that show or sound):
- `PY tools/tv.py "logcat -d -s TvHome.Solat"` shows `saved 365 days for <ZONE>` (the TV needs internet).
- A screenshot of Home shows the prayer row under the clock and a mosque behind the apps:
  `screencap -p /sdcard/s.png`, then `PY tools/tv.py --pull /sdcard/s.png s.png`.
- The zikir (plays aloud): `am broadcast -a com.rumahhakim.tv.ZIKIR -n 'com.rumahhakim.tv/.ZikirAlarm$Receiver' --ez now true`
- The azan (plays aloud): `touch /sdcard/Android/data/com.rumahhakim.tv/files/azan-now`, then
  `input keyevent KEYCODE_HOME`. Any button on the remote stops it.

### 8. The phone settings page, and the second "Allow USB debugging?"

Tell the person to open **http://\<ip\>:8080** on a phone on the same Wi-Fi and press **"Tunjuk PIN di TV"**:
a 4-digit PIN appears in the TV's top right corner for two minutes; once typed in, that phone stays logged
in. From there they change every setting, the surah schedule, the pictures and recordings, and try the
zikir, azan and a surah; no computer needed again.

The first time the TV sits on Home for a while, it asks **"Allow USB debugging?"** once more, for TV Kat
Rumah itself: that connection clears the app caches every 15 days and lets the TV go back to sleep after
an azan or zikir it woke up for. The person ticks **Always allow** and presses OK. Without it, everything
else works and the TV just stays on after waking.

### 9. Hand over

Tell them: the phone page address; that `PY tools/pasang.py --tv <ip> pulih` puts the old Home and
screensaver back (the app stays installed until they uninstall it); that network debugging must stay on
for the auto-sleep and cache cleaning; and where their backup is.

## Optional

- **Debloat** (switch off ads, tracking and preloaded apps): use Clear TV Islamic's tested lists and
  `tools/disable.sh` from [cleartv-Islamic](https://github.com/Pokka99/cleartv-Islamic), with its rules:
  disable, never uninstall, small batches, check HDMI and the remote after each batch.
- **Their own zikir voices**: `tools/suara-zikir.py` makes all 24 with ElevenLabs. The person sets their
  own key (`ELEVENLABS_API_KEY`) in their own terminal and picks a voice; never a clone of a real person who
  has not agreed to it. Or they record the zikir themselves (see [ZIKIR.md](ZIKIR.md)).

## Troubleshooting

- **Android shows a "choose a launcher" dialog right after the install**: two apps now claim Home; `pasang`
  sets TV Kat Rumah as the default. If it remains, press Home and pick TV Kat Rumah → Always.
- **The zikir or azan does not show over YouTube**: `PY tools/tv.py "appops get com.rumahhakim.tv SYSTEM_ALERT_WINDOW"`
  must say `allow` (`pasang` sets it).
- **No prayer times**: the zone is empty or wrong, or the TV has no internet; `logcat -d -s TvHome.Solat`
  says which.
- **Android 10**: the first time the app gets internet, connections can fail with `EPERM` until the TV
  restarts. Ask before restarting it.
- **Building it yourself**: `./gradlew testReleaseUnitTest assembleRelease` (JDK 17, Android SDK 34). An
  APK built with a different signing key cannot update the one from `dist/` without uninstalling it first,
  which deletes the app's settings on the TV.
