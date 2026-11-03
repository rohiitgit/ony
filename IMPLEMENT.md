# IMPLEMENT.md — Ony link router

## Decided
- 2026-11-03 (v2): Ony is an invisible redirect, not a picker. Tap a music link anywhere,
  the exact song opens in the listener's own app, nobody visits Ony after setup.
- Target apps: Spotify, Apple Music, YouTube Music, Meld (2026-11-03: Meld is an open-source
  YouTube Music client, `com.meld.app`; it's searched in YouTube Music's catalogue and opens
  the same `music.youtube.com/watch` URL). Listener's app = their choice if
  installed, else the only one installed, else ask once on the redirect card.
- Android 12+ gives web links to one app with no chooser, so setup is required once: allow
  Ony's domains (Open by default) and turn off "Open supported links" for apps that already
  own theirs (YouTube is preinstalled and wins otherwise). Proven on Android 16 with
  `pm set-app-links-user-selection` and `pm set-app-links-allowed`.
- Matching: MusicLink (free key, 300/month, 1/s). Only called when the link isn't already
  from the listener's app. song.link is dead without a key, SongPort is invite-only.
- Open source: each user brings their own key (setup screen, DataStore). `musiclink.apiKey`
  in git-ignored `local.properties` for personal builds. Never ship an APK with a key.

- 2026-11-03 (v2.1): guided key setup and a visual identity. Plum `#1A1033`, signal
  yellow `#FFD447`, mint `#5BE3B0`, Bricolage Grotesque; the cable hero is the one
  animated element. Key flow: Custom Tab to MusicLink keys page, clipboard offer on
  return, free key check (400 = accepted, 401 = rejected) before saving. Setup steps
  are proven by links Android actually delivered, not assumed.

- 2026-11-03 (v2.2): MusicLink is the only matcher (no search guesses). Visual design
  follows the user's reference: ink blocks with bridges on a pale page, lavender accent,
  Manrope. Testing happens on the user's phone only.

## Implemented
- `RouterActivity.kt`: transparent router (`Theme.Ony.Redirect`, excluded from Recents).
- `SettingsActivity.kt`: launcher setup screen, re-reads system state on resume.
- `Redirect.kt`: `targetApp`, `instant`, `pick` (pure) and `resolve` (instant, short-link
  expansion, then MusicLink; nothing else).
- `Launch.kt`: installed apps, package-pinned opens, original-link fallback (own app, then
  browser), Open by default settings, allowed domains (Android 12+).
- `Setup.kt`: LinkKind (hosts, owner app, sample link) and pure setup rules.
- `Theme.kt`, `Visuals.kt`: palette and type, InkBlock, Bridge, OutlineRow, UnfoldingRow,
  Tag, PillButton, InkChoice, AppIcon; Joint (single neck, edge notches).
- `MusicLinkApi.kt` (incl. `checkKey`), `Resolver.kt`, `LinkParser.kt`, `Prefs.kt`, `Models.kt`.
- `AndroidManifest.xml`: `<queries>` for music apps and browsers.
- Tests: `LinkParserTest` (6), `MusicLinkApiTest` (14), `RedirectTest` (10), `ResolverTest` (4), `SetupTest` (12).

## Status
- Complete and verified on a Galaxy A35 (Android 16) with YouTube Music as the listener's
  app: Spotify, Apple Music and YouTube links play the exact song; not-found falls back to
  the browser; setup screen correct. 31/31 tests, lint 0 errors.
- v2.1 verified on the emulator (setup screen, key check both ways, Custom Tab, redirect
  card) and on the phone with all three apps installed: 12/12 link-to-app combinations
  play the exact song; "which app?" card works.
- Not yet seen on a real device: the clipboard key offer (logic unit-tested). the one-time "which app?" card (needs two music apps
  installed; logic unit-tested), and Spotify or Apple Music as the listener's app.
- Open: license choice and publishing (user's call).
