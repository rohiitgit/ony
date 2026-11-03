# Changelog

## [2026-11-03 10:00]
- MusicLink removed entirely: no account, no key, no monthly limit. A user's Apple Music
  link from MusicLink opened "not available in your region": it pointed at the US release
  ("Asi Gabhru Punjabi", 1585204443) while India's store sells "Asi Gabru Punjabi"
  (1426290349). Ony now searches the listener's own store.
- How: `Catalog` reads the song from the link (Spotify's public embed page, iTunes lookup for
  Apple links, YouTube oEmbed plus YouTube Music's row for the same video, which also gives
  the album), then searches the target's catalogue for the phone's country: iTunes Search
  for Apple Music, YouTube Music's web search for YouTube Music. Spotify has no free search,
  so it opens Spotify's search for "title artist" (one tap to play).
- `Match` picks from results: same base title (one letter of slack from 8 characters, for
  transliterations like Gabru/Gabhru), same version (remix, live, lo-fi, cover, parody...),
  overlapping artist; the same album wins ties, ignoring edition suffixes ("Original
  Motion Picture Soundtrack", "- Single", "Deluxe") and one-letter spellings (Judda/Judaa).
  Album never rules a song out. Apple song links only ever use the exact linked track.
- Country: SIM, then network, then region setting, then "us" (`Catalog.countryOf`, tested),
  passed to both Apple and YouTube Music searches. Checked live: the same song resolves to
  each store's own release in IN, GB, US and DE; when a store doesn't sell a song (the
  Punjabi track in GB and CA), the original link opens.
- Removed: `MusicLinkApi`, the key row and key panel, clipboard key detection, Custom Tabs
  dependency, BuildConfig key, the Lookups stat, the CI key check. Setup is three steps.
- Final build checked on the phone: the setup screen without the key row (two stats, two
  setup rows), and the user's link opening Asi Gabru Punjabi in Apple Music again.
- Earlier, before the album/tolerance refinements: the user's YouTube Music
  link opened Asi Gabru Punjabi in Apple Music (India copy), played it in YouTube Music, and
  showed it as the top song in Spotify's search. 66/66 tests, lint 0 errors.

## [2026-11-03 09:50]
- Appearance switch is now an icon bar fixed at the bottom of the setup screen, like the
  reference's bottom bar: Auto (brightness-auto), Light (sun), Dark (moon), with the words
  kept as screen-reader labels. The list scrolls above it with 88 dp of bottom padding so
  its end is never hidden.
- Checked on the phone: each icon applies its theme, the bar stays put while scrolling.

## [2026-11-03 09:45]
- Appearance switch: a segmented pill at the bottom of the setup screen, AUTO | LIGHT | DARK,
  in the reference's style (ink track, lavender selection). AUTO follows the phone; the
  choice is saved and applies instantly. The redirect card follows it too, and the setup
  screen's bar icons and window colour follow the app's theme, not the phone's.
- `ThemeMode` (tested: AUTO follows the phone, LIGHT/DARK ignore it, unknown saved values
  fall back to AUTO), `Prefs.themeMode` as a flow, `rememberDarkTheme()`, `Segmented`.
- Checked on the phone (in light mode): AUTO light, DARK dark with light bar icons, LIGHT,
  back to AUTO; redirect card dark under DARK. Left on AUTO. 52/52 tests, lint 0 errors.

## [2026-11-03 09:40]
- Dark mode, following the system setting. Colours are now defined by role (`OnyPalette`:
  page, text on page, outline, block, raised block, text on block, accent, …) with a light
  and a dark palette; `Ony.*` reads the current one. Dark: deep ink page `#0B1517`, blocks
  lifted to `#1A2E31` (1.30:1 against the page, so they still read as blocks), light text,
  soft `#4F676B` row outlines, same lavender. An opened row's border now animates to the
  block colour, so it never shows a light edge around a dark header.
- Status and navigation bar icons follow the theme (were pinned dark); `values-night`
  window colour avoids a white flash; the download page follows `prefers-color-scheme`.
- `ContrastTest` checks every text/background pair in both palettes against WCAG AA
  (4.5:1). It caught light mode's muted grey at 4.08:1; darkened `#6B7D80` -> `#5F7174`
  (4.85:1). Dark mode: 16.4:1 text, 5.6-6.5:1 muted.
- Files: `Theme.kt`, `Visuals.kt`, `SettingsActivity.kt`, `RouterActivity.kt`,
  `res/values-night/colors.xml`, `docs/download/index.html`, `ContrastTest.kt` (3 tests).
- Checked on the phone in dark mode (setup screen, opened key row, redirect card into Apple
  Music) and again in light mode for regressions; the phone's theme was put back to light.

## [2026-11-03 09:25]
- Fix: the download stalled at 11.38 / 11.38 MB. Reproduced on the phone: the APK fully
  arrived (as `.pending-…-Ony-0.1.2.apk`) but a Chrome Custom Tab, the in-app browser apps
  like WhatsApp and GitHub open, never committed it; the same URL in full Chrome finished in
  under 4 s. GitHub's response was correct (full length, APK content type, right filename).
- `docs/download/`: no automatic download any more. On Android the button is an `intent://`
  link that hands the APK URL to the browser app, with a plain-URL fallback; on desktop it
  stays a direct link. README notes what to do if a download stops at 100%.

## [2026-11-03 09:10]
- Open source under MIT (`LICENSE`), published as github.com/rohiitgit/ony.
- CI: `.github/workflows/build.yml` runs the unit tests, builds a release APK signed with the
  project's release key (from repository secrets), checks it contains nothing key-shaped,
  and publishes it as release `v0.1.<run>` with the file `Ony-0.1.<run>.apk`. Version code
  is the run number, so each build installs over the last.
- `app/build.gradle.kts`: version from `ONY_BUILD`; release signing from `ONY_KEYSTORE_*`
  env vars (falls back to the debug key locally); never reads a MusicLink key on CI.
- `docs/download/` (GitHub Pages): one-tap download of the newest `Ony-<version>.apk`, with
  a fallback link to the releases page. Linked at the top of the README.
- First build (run 1) verified: release v0.1.1, `Ony-0.1.1.apk` signed with the release key
  (SHA-256 2b4f…54d3), version code 1, no key-shaped strings, served as
  `filename=Ony-0.1.1.apk`; the download page resolves to it. Actions bumped to current
  majors (checkout v7, setup-java v6, setup-gradle v6) after Node 20 deprecation warnings.
- Privacy before publishing: replaced a test string that held part of the real MusicLink
  key with a fake one; `.idea/` and `tasks/` (local notes) kept out of the repo; signing
  keys ignored.

## [2026-11-03 08:58]
- App icon: the word "ony" in Manrope (400) on ink, matching the in-app wordmark, replacing
  the drawn cable mark. Built as an adaptive icon (ink background layer, off-white word as
  foreground, same shape as the themed monochrome icon), so it fills the launcher's own
  shape. Letters are real Manrope outlines (one vector path per letter), centred on their
  ink horizontally and on the "o" vertically, 44 dp wide: about 63% of the visible area on
  Samsung, where the first try at 54 dp measured 76% and looked cramped. 400 weight because
  300 thinned out at launcher size. A lavender dot variant was tried and dropped (it pulled
  the word off centre and meant nothing).
- `tools/make_icon.py` regenerates the paths from the font; `app/lint.xml` ignores
  ObsoleteSdkInt for `mipmap-anydpi-v26` only (adaptive icons need that folder).
- Files: `res/mipmap-anydpi-v26/ic_launcher.xml`, `res/drawable/ic_launcher_foreground.xml`
  (new), `res/drawable/ic_launcher.xml` (removed), `res/values/colors.xml`,
  `AndroidManifest.xml`, `tools/make_icon.py`, `app/lint.xml`, README.

## [2026-11-03 08:55]
- Measured layout pass on the phone (uiautomator bounds plus screenshot pixel scans, 420 dpi,
  2.625 px/dp), with fixes:
  - Spacing on an 8 dp rhythm: 12 dp page padding on all sides, 24 dp above and below the
    centred sentence, 8 dp between rows, 12 dp row to footer, 24 dp footer to button.
    Measured: 24.4 / 24.0 / 8.0 / 8.0 / 12.2 / 24.0 dp.
  - Chevrons line up with the 20 dp text inset (were 3 px inside it): 53 px both sides.
  - Buttons 48 dp (were 46).
  - Stats spread with space-between, like the reference: first column on the left inset,
    last ending on the right inset (53 / 52 px), equal gaps (296 / 295 px). Was three equal
    columns, which left a dead strip on the right.
  - Stats block space above and below the glyphs evened from 56 / 62 px to 56 / 57 px
    (bottom padding 16 dp, since digits have no descenders).
  - App choices in the unfolded panel line up with the header text (icons at 85 px).
  - Redirect card: equal 20 dp side insets (icon 57 px, Cancel 58 px from the edges; were
    18 / 14 dp).
  - Removed the READY / steps-left tag: the rows already show what's left. The wordmark is
    centred in the header: glyphs at x 540.0 vs block 539.5, and the "o" at y 214.5 vs 215.0
    after a measured 2.5 dp optical lift.
- Files: `SettingsActivity.kt`, `Visuals.kt` (InkBlock padding params, row end padding,
  48 dp pills), `RouterActivity.kt`.

## [2026-11-03 08:45]
- Design review against the reference frame, and fixes:
  - Joint: stacked blocks are now one shape with a pinched waist, a single wide neck with
    small rounded notches at the edges (was two narrow necks with a slit between, which the
    user flagged as the biggest gap). Joined corners are small (12 dp), outer corners 26 dp.
    The unfolding rows use the same joint, and the header's lower corners animate to match.
  - Stats: compact three-column block, label over value, values 26 sp in pale lavender:
    Songs, Avg. time, and Lookups (MusicLink calls this phone made this month).
  - Thin (300 weight) wordmark; a centered two-line sentence under the header, as in the
    reference; row text 15 sp.
- Logic: `Redirect.resolve` returns `Outcome(destination, lookedUp)` so the router can count
  lookups; `Setup.lookupsThisMonth` (tested) and a month-aware counter in `Prefs`.
- Files: `Visuals.kt`, `Theme.kt`, `SettingsActivity.kt`, `RouterActivity.kt`, `Redirect.kt`,
  `Prefs.kt`, `Setup.kt`, `SetupTest.kt` (+1). 46/46 tests, lint 0 errors, checked on the phone.

## [2026-11-03 08:40]
- Matching: MusicLink only. Removed the keyless iTunes guess for Apple Music and the song
  detail fetching behind it (YouTube oEmbed, iTunes lookup); in testing the top iTunes hit
  was often a remix or mashup. Kept the YouTube video to YouTube Music shortcut (same video
  id, exact, no network). `Resolver.kt` went from ~190 to ~70 lines.
- Design: new look after a reference shot the user chose: a pale page (`#F8F9F6`) with
  deep green-black ink blocks (`#10262A`) joined by small bridges, outlined pill rows, a
  lavender accent (`#D3CCF6`), big light numbers, small spaced caps on buttons, and Manrope
  (OFL, `res/font`, license in `third_party/fonts/`). Rows unfold in place into bridged ink
  panels (your app, MusicLink key). Real app icons come from the phone.
- Setup screen: header block with a READY / "N steps left" tag, a stats block (songs opened,
  average time, recorded by the router), and rows that read real state from Android,
  including whether other apps still open their own links (verified on the phone by
  switching YouTube's setting on and off).
- Redirect card: ink block with the app icon and a lavender Cancel pill; it appears only
  if the redirect takes over 350 ms, so instant redirects draw nothing.
- Briefly tried an Apple Music style grouped list and system font; replaced by this design
  at the user's request.
- Files: `Theme.kt`, `Visuals.kt`, `SettingsActivity.kt` rewritten; `RouterActivity.kt`
  (card, delay, stats), `Prefs.kt` (stats), `Setup.kt` (`averageTime`), `Launch.kt` (app
  icons, other apps' link state), `Resolver.kt`, `Redirect.kt`, `SetupTest.kt` (+3),
  resources (colors, theme, icon, font), README.
- Verified on the phone: setup screen, both unfolding rows, redirect card, four link types
  into YouTube Music, stats showing 4 opened / 0.8 s. 45/45 tests, lint 0 errors.

## [2026-11-03 08:15]
- Verified on the phone with Spotify, Apple Music and YouTube Music installed: all 12
  combinations (Spotify, Apple Music, YouTube and YouTube Music links, into each of the
  three apps) opened and played the exact song, checked via media_session and screen text.
  The one-time "which app?" card appeared on the first link and the choice stuck.
- Fix: text on the redirect card and app rows rendered muted. `OnyColors.Raised` is the
  scheme's surfaceVariant, so Material tinted content with onSurfaceVariant. Surfaces now
  set `contentColor` explicitly; rows on the card use a darker resting color to stand out.
- Found: installing Spotify and Apple Music after setup made Android revoke Ony's
  user-selected domains for them (their apps are verified owners). The setup screen
  already detects this (step 2 reopens); "You're set" copy and README now say to open Ony
  once after installing a music app.
- Measured: MusicLink takes ~3 s for a YouTube link (0.7 s for Apple Music), so YouTube to
  Spotify redirects take 6-10 s on the phone.
- Files: `Visuals.kt` (AppRow contentColor, restingColor), `RouterActivity.kt`,
  `SettingsActivity.kt` (banner copy), README.

## [2026-11-03 08:05]
- Design: Ony has its own look. Deep plum with a signal-yellow accent and mint for done,
  one typeface (Bricolage Grotesque, OFL, bundled in `res/font`, license in
  `third_party/fonts/`), and one animated moment: a hero where Spotify, Apple Music and
  YouTube Music cables run into a single yellow plug with pulses along them (static when
  Android animations are off). Setup is a vertical rail whose steps turn mint when done.
  New app icon in the same style (the old one was a white note on Spotify green).
- Key setup: one "Get my free key" button opens MusicLink's keys page in a Custom Tab (it
  shows sign-in with Google or email first when signed out). Coming back, Ony offers a key
  it finds on the clipboard (only after that trip, never otherwise), has a paste button,
  and checks every key with MusicLink before saving: a deliberately bad query answers 400
  for a good key and 401 for a wrong one, and failed requests don't use the allowance.
- Setup knows what's really done: link steps only turn mint once Android has handed Ony a
  link of that kind (recorded by the router), with Test buttons that fire a sample link.
  "You're set" banner with a test song when everything is done.
- Redirect card restyled: equalizer bars in the target app's color, "Opening in X",
  Cancel; transparent status and navigation bars. The one-time app question uses the same
  app rows as setup.
- Files: new `Setup.kt` (LinkKind, setup rules), `Theme.kt`, `Visuals.kt`, `SetupTest.kt`
  (8), `res/font/bricolage_grotesque.ttf`, `res/values/colors.xml`; rewritten
  `SettingsActivity.kt`; `RouterActivity.kt`, `MusicLinkApi.kt` (`checkKey`), `Prefs.kt`
  (caught link kinds), `Launch.kt` (Custom Tab, clipboard, test links), `themes.xml`,
  `ic_launcher.xml`, `app/build.gradle.kts` (+androidx.browser, -coil, which was unused).
- Verified on the emulator: setup screen, hero pulse, wrong key rejected with a fix-it
  message, real key "Key works. Saved.", Custom Tab opens MusicLink sign-in, redirect card
  over the home screen then YouTube Music. 42/42 tests, lint 0 errors.

## [2026-11-03 07:45]
- Change: Ony is now an invisible redirect. A tapped music link opens the exact song in the
  listener's own app (Spotify, Apple Music or YouTube Music) and Ony closes. The only thing
  drawn is a small "Opening in X…" card with Cancel, or a one-time "which app?" question.
  The per-link button screen and the "Always open in" menu are gone.
- New setup screen (launcher icon): pick your app, shows which link kinds Ony may open with a
  button to Android's "Open by default" page, buttons to stop other installed music apps
  grabbing their links, MusicLink key field, and a test link.
- Open source: each user pastes their own free MusicLink key (saved in DataStore). A key in
  `local.properties` still works for personal builds. No key in the repo or in shared APKs.
- MusicLink is only called when the link isn't already from the listener's app; YouTube
  links go to YouTube Music with no lookup. Not found: the original link opens in its own app
  or the browser, with a message.
- Files: new `Redirect.kt`, `Launch.kt`, `SettingsActivity.kt`, `RedirectTest.kt` (10);
  rewritten `RouterActivity.kt`, `Prefs.kt`, `ResolverTest.kt` (4); `Models.kt`
  (`MusicApp.provider` removed), `Resolver.kt` (`destinations()` removed, `original()`
  public), `AndroidManifest.xml` (`<queries>`, setup activity is the launcher, router is
  transparent and out of Recents), `themes.xml` (`Theme.Ony.Redirect`), README rewritten.
- Verified on the phone (Android 16): Spotify, Apple Music and YouTube links fired the way a
  tap does opened in YouTube Music and played the exact song (Blinding Lights, Never Gonna
  Give You Up, Gangnam Style); MusicLink 404 fell back to Chrome; setup screen states and its
  Android settings button checked. 31/31 tests, lint 0 errors.

## [2026-11-03 07:25]
- Change: every button now opens the exact song in its own app. All search buttons are
  gone (Spotify, YouTube Music, Apple Music and YouTube search links, plus the manual
  "song + artist" box that only fed them). A service with no exact match gets no button.
- Exact links come from MusicLink (musiclink.one, free key, 300 lookups a month), which
  returns Spotify, YouTube Music and Apple Music links plus title, artist and artwork.
  The key lives in git-ignored `local.properties` as `musiclink.apiKey=...` and reaches
  the app through `BuildConfig.MUSICLINK_API_KEY`.
- Free keyless paths stay: YouTube oEmbed and iTunes lookup for song details, the same
  video in YouTube Music for YouTube links. The iTunes Search guess for Apple Music is now
  only a fallback when MusicLink has no answer, because on the emulator it picked a
  Gangnam Style mashup instead of the song.
- Every intent is pinned to its app's package. Ony handles open.spotify.com, spotify:
  and youtube.com itself, so unpinned intents could loop back into Ony. A missing app
  shows "Couldn't open X. Is it installed?" instead of crashing.
- Supersedes the unlogged 2026-11-02 fix that sent Spotify search through
  `spotify:search:` with a clipboard fallback.
- Model: `Destination(app: MusicApp, url)` replaces label/exact/fallback fields;
  new `MusicApp` enum (label, package, button order) and `Lookup(song, matches)`.
- Files: `Models.kt`, `Resolver.kt`, `RouterActivity.kt`, new `MusicLinkApi.kt`,
  `app/build.gradle.kts` (buildConfig, key field, test-only `org.json`),
  `ResolverTest.kt` (rewritten, 8), new `MusicLinkApiTest.kt` (11).
- Verified: 25/25 unit tests, `assembleDebug`, lint 0 errors. On the emulator, YouTube,
  Spotify and Apple Music links each showed Spotify, YouTube Music and Apple Music
  buttons for the right song; YouTube Music opened its app; Spotify (not installed
  there) showed the message. On the phone, YouTube Music played the exact song from all
  three sources (checked via media_session). Spotify and Apple Music aren't installed in
  the phone's main profile, so they still need a phone check.

## [2026-11-02 16:37]
- Infra: added the Gradle wrapper (`gradlew`, `gradlew.bat`,
  `gradle/wrapper/gradle-wrapper.jar`), generated offline from the local Gradle 8.7.
  The jar matches Gradle's published SHA-256 and `gradle-wrapper.properties` is
  unchanged. CLI builds now use Gradle 8.7, the same as Studio, not system Gradle 9.8.2.
  This resolves the "wrapper generation still blocked" note below.
- Verified: `./gradlew assembleDebug testDebugUnitTest --rerun-tasks` OK, 10/10 tests.

## [2026-11-02 16:20]
- Fix (Studio sync): "Could not install Gradle distribution ... Connect timed out".
  Cause: IPv6 to services.gradle.org is broken on this network and Studio's wrapper
  downloader keeps trying it. Downloaded gradle-8.7-bin.zip over IPv4, checked it
  against Gradle's published SHA-256, and installed it into
  `~/.gradle/wrapper/dists/gradle-8.7-bin/bhs2wmbdwecv87pi65oeuq5iu/` (zip, unpacked
  folder, `.ok` marker). The wrapper now resolves locally with no download.
- Reverted the `gradleHome` line in `.idea/gradle.xml`. Studio ignored it (it needs
  `distributionType=LOCAL`), and the wrapper (Gradle 8.7, the pairing AGP 8.5.2
  expects) stays the one source of truth.
- Verified through the real wrapper code path: Gradle 8.7, no download,
  `assembleDebug` OK, `testDebugUnitTest --rerun` 10/10 passing.

## [2026-11-02]
- Verified: full `assembleDebug` builds (17MB APK) and all 10 unit tests pass
  (`LinkParserTest` 6/6, `ResolverTest` 4/4) under Gradle with `ANDROID_HOME` set.
- Fix: test expectation for YouTube sources — original URL and YT Music watch URL
  coincide, so dedupe yields 4 destinations, not 5.
- Infra: Gradle wrapper generation still blocked by flaky wrapper URL check, so the
  project builds with system Gradle (`brew install gradle`) for now.
- Fix: launcher icon now `@drawable/ic_launcher` (vector); manifest no longer
  references the missing `@mipmap/ic_launcher` which broke resource linking.
- Fix: auto-open fires for exact destinations only; search fallbacks stay manual.
- Fix: `exactApple` / `song` / `manualTitle` reset per incoming link; duplicate
  iTunes exact-match request guarded by `lastFetchedFor`.
- Feature: `spotify.link` short links expanded via redirect-following
  (`Resolver.expandShortLink`), then re-parsed.
- Refactor: `LinkParser` and `Resolver` are JVM-only (`java.net.URI`); unit tests
  run on plain JUnit, no Robolectric needed.
- Tests: added `ResolverTest` (destinations contract, 4 tests).
- Docs: honest Spotify-manual-entry copy in the UI.
