# Ony

Tap any song link and it opens in **your** music app.

**[Download the latest Ony for Android](https://rohiitgit.github.io/ony/download/)** (saves as `Ony-<version>.apk`)

Open the download on your phone and tap Install (allow your browser to install apps if
Android asks). If you opened the link inside another app (WhatsApp, GitHub) and the download
stops at 100%, open it in Chrome instead: in-app browsers don't finish APK downloads. New versions install over old ones and keep your settings. Every push to
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
3. Ony opens the same song in your app. No account, no API key.
   - If the link is already from your app, it opens right away.
   - A YouTube video opens as the same video in YouTube Music.
   - Otherwise Ony reads the song's title, artist and (where the link has it) album, then
     searches your app's catalogue for **your country** and opens the match. Apple Music
     uses Apple's public search, YouTube Music its own web search. Spotify has no free
     search, so Spotify users get Spotify's search for the song, one tap from playing.
4. If your app doesn't have the song in your country, Ony opens the original link in its
   own app or your browser and tells you.

### How a match is chosen

Search results often start with a remix, a live version or a cover, so Ony doesn't take the
top hit. A result must have the same title (one letter of slack for longer titles, since
stores transliterate Hindi and Punjabi differently), the same version (remix, live, lo-fi,
cover and so on must agree with the link), and an overlapping artist. Among those, one from
the same album wins. The country comes from the SIM, then the mobile network, then the
phone's region setting, so a phone anywhere searches its own store.

## Setup (once)

Open Ony from your home screen. It walks you through these steps and shows what's done:

1. **Pick your music app.** If only one of Spotify, Apple Music or YouTube Music is
   installed, Ony picks it for you.
2. **Let Ony open music links.** Android 12 and later sends each kind of web link to one
   app, with no "Open with" menu, and only you can hand another company's links (Spotify's,
   Apple's, YouTube's) to a different app. Tap the row, then switch on all 7 addresses:
   open.spotify.com, spotify.link, music.apple.com, music.youtube.com, youtube.com,
   www.youtube.com and youtu.be. Until they're all on, Ony opens with a short sheet listing
   them, ticking each one that's done, with a button straight to the right Settings page.
3. **Stop other apps grabbing their links.** Apps like YouTube (often preinstalled) open
   their own links before Ony sees them. For each one Ony lists, turn off **Open supported
   links**.

Prefer no setup at all? Use **Share → Ony** on any song link instead of tapping it: sharing
works straight after installing.

The setup screen reads every step's real state from Android, so a row only says "On" when
that's true. The bar at the bottom picks the look: Auto (follows your phone), Light or Dark.

## Build

Needs Android Studio (or the Android SDK) and JDK 17.

```
./gradlew :app:assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # 63 unit tests
```

Published APKs are built by GitHub Actions (`.github/workflows/build.yml`) on every push to
`main` and signed with the project's release key from repository secrets.

## Project layout

```
app/src/main/java/com/ony/app/
  RouterActivity.kt    invisible router: link in, song opens in your app, closes
  SettingsActivity.kt  the setup screen (launcher icon)
  Setup.kt             what setup still needs (pure logic, unit-tested)
  Redirect.kt          where a link should go
  Catalog.kt           reads a link's song, searches Apple Music / YouTube Music by country
  Match.kt             picks the same song out of search results (pure, unit-tested)
  Resolver.kt          short links, where a link opens, YouTube video to YouTube Music
  LinkParser.kt        URL parser for every supported service
  Launch.kt            Android side: installed apps, opening links, link settings
  Prefs.kt             your app and appearance (DataStore)
  Models.kt            data types
  Theme.kt, Visuals.kt ink-block palette and type, blocks, bridges, rows, pills, app icons
```

The typeface is [Manrope](https://github.com/googlefonts/manrope), under the SIL Open Font
License (`third_party/fonts/Manrope-OFL.txt`). The app icon is the word "ony" in Manrope,
converted to vector outlines by `tools/make_icon.py` (needs `pip install fonttools`).

## Limits

- Matching is by name, artist and album, so a song that's missing from your country's store,
  or titled very differently there, opens as the original link instead.
- Spotify has no free search, so Spotify users land on Spotify's search for the song.
- The YouTube Music search is unofficial (it's what its web player uses) and could change.
- Ony reads songs from Spotify, Apple Music, YouTube and YouTube Music links. Links from
  other services (shared to Ony) open as they are.
- Playlists belong to one service, so they can't be matched and open as the original link.
- Installing a music app after setup gives that app its own links back (Android does this
  silently). Open Ony once afterwards: the setup screen shows what to switch back on.

## License

[MIT](LICENSE)
