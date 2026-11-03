# Ony

Tap any song link and it opens in **your** music app.

**[Download the latest Ony for Android](https://rohiitgit.github.io/ony/download/)** (saves as `Ony-<version>.apk`)

Open the download on your phone and tap Install (allow your browser to install apps if
Android asks). New versions install over old ones and keep your settings. Every push to
`main` builds a new one; past versions are on the
[Releases page](https://github.com/rohiitgit/ony/releases).

A friend sends you a Spotify link, but you use Apple Music. Normally that means copying the
title and searching for it. With Ony, you tap the link and the same song opens in Apple Music.
It works the same way for YouTube Music, Spotify and Apple Music, whichever one you use and
whatever kind of link you get.

You set Ony up once. After that you never open it: a tapped link shows a small "Opening in
Apple Music…" card for a second or two, then the song plays.

Android only, for now.

## How it works

1. You tap a Spotify, Apple Music, YouTube Music or YouTube link (in WhatsApp, a browser,
   anywhere).
2. Android hands the link to Ony instead of the app it came from.
3. Ony finds the exact same song in your app and opens it there. No search pages.
   - If the link is already from your app, it opens right away.
   - A YouTube video opens as the same video in YouTube Music, with no lookup.
   - Anything else is looked up with [MusicLink](https://musiclink.one), which matches songs
     across services. Ony never guesses with a search: no exact match means the original
     link opens instead.
4. If your app doesn't have the song, Ony opens the original link in its own app or your
   browser and tells you.

## Setup (once)

Open Ony from your home screen. It walks you through these steps and shows what's done:

1. **Pick your music app.** If only one of Spotify, Apple Music or YouTube Music is
   installed, Ony picks it for you.
2. **Let Ony open music links.** Android 12 and later sends each kind of web link to one
   app, with no "Open with" menu. Tap **Open Android settings**, then **Add link**, and tick
   every link.
3. **Stop other apps grabbing their links.** Apps like YouTube (often preinstalled) open
   their own links before Ony sees them. For each one Ony lists, turn off **Open supported
   links**.
4. **Add your free MusicLink key.** Tap **Get my free key**: MusicLink opens in a browser
   tab, you sign up (Google or email), create a key and copy it. Back in Ony, it offers the
   key you just copied, checks it with MusicLink, and saves it. Free for 300 lookups a month,
   and they only count when the link is from a different app than yours. Without a key, only
   YouTube to YouTube Music works.

The setup screen reads every step's real state from Android, so a row only says "On" or
"Added" when that's true. When everything is set, **Play a test song** checks the whole
thing, and the stats block counts songs opened, the average time, and MusicLink lookups this
phone made this month (the free plan allows 300).

## Build

Needs Android Studio (or the Android SDK) and JDK 17.

```
./gradlew :app:assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # 46 unit tests
```

Published APKs are built by GitHub Actions (`.github/workflows/build.yml`), signed with the
project's release key from repository secrets, and never contain a MusicLink key: the build
ignores `local.properties` on CI and fails if anything key-shaped ends up in the APK.

For your own builds you can bake in a MusicLink key instead of pasting it in the app. Add
this to `local.properties` (git ignores it, so it's never committed):

```
musiclink.apiKey=YOUR_KEY
```

Never publish an APK built with your key in it. Anyone can pull the key out of an APK.

## Project layout

```
app/src/main/java/com/ony/app/
  RouterActivity.kt    invisible router: link in, song opens in your app, closes
  SettingsActivity.kt  the setup screen (launcher icon)
  Setup.kt             what setup still needs (pure logic, unit-tested)
  Redirect.kt          where a link should go (pure logic, unit-tested)
  MusicLinkApi.kt      MusicLink client
  Resolver.kt          short links, where a link opens, YouTube video to YouTube Music
  LinkParser.kt        URL parser for every supported service
  Launch.kt            Android side: installed apps, opening links, link settings
  Prefs.kt             your app and key (DataStore)
  Models.kt            data types
  Theme.kt, Visuals.kt ink-block palette and type, blocks, bridges, rows, pills, app icons
```

The typeface is [Manrope](https://github.com/googlefonts/manrope), under the SIL Open Font
License (`third_party/fonts/Manrope-OFL.txt`). The app icon is the word "ony" in Manrope,
converted to vector outlines by `tools/make_icon.py` (needs `pip install fonttools`).

## Limits

- Matching depends on MusicLink. Rare songs may not be found, and then you get the
  original link.
- The first lookup of a song MusicLink hasn't seen takes 10 to 15 seconds. After that it's
  cached and quick.
- Playlists belong to one service, so they can't be matched and open as the original link.
- Installing a music app after setup gives that app its own links back (Android does this
  silently). Open Ony once afterwards: the setup screen shows what to switch back on.
- Looking up a YouTube link takes MusicLink about 3 seconds, so those redirects take a few
  seconds longer than Spotify or Apple Music links.

## License

[MIT](LICENSE)
