# Beautiful Lyrics — native presentation checkpoint

## 2026-10-24 · chunk start / timing foundation

Scope: native Compose presentation, existing lyrics retrieval/navigation retained. Main branch; no push. Pre-existing LibraryScreen/RootLibrary work and untracked briefs are not ours and are left untouched.

### Evidence / contracts
- Existing `ui/screens/LyricsScreen.kt`: full-screen overlay, inline card, tap-to-seek, translation/share and playback controls. `PlayerScreen.kt` already opens it from inline expand + a deliberate artwork upward flick (horizontal track gestures preserved), and Back closes lyrics first.
- `LyricsViewModel.state: StateFlow<State>` owns loading/loaded/not-found, cached prefetch and stale-track protection. `LyricsApi.fromSpotify` adapts `:api` lyrics, then LRCLIB/optional sources fall back. No new data requests or providers.
- `:api LyricsWord` provides **start only**, not a word end. App `LyricWord.endMs` supports explicit end; missing ends are inferred from next word, source line end, then next line/track duration. Preserve `:api LyricsLine.endMs` in the app adapter (previously discarded). This is metadata preservation, not a new pipeline.
- `SongPlayer` collects `api.player.state`; Spotify events are irregular, plus a **3,000 ms** remote refresh loop. `applyState` anchors position to `SystemClock.elapsedRealtime`, retains its running anchor for corrections under 1,000 ms; `getCurrentPosition()` interpolates continuously, freezes for pause/buffering and handles local playback. Use this clock each draw frame, not each provider emission.
- Existing `rememberFrameClock` is lifecycle/energy-policy aware. Lyrics will request 60 fps rather than the old 30; power policy can cap it. No new independent playback clock.
- App font is **Figtree / AppSans**, not Satoshi. Retain it; no fonts bundled.

### Reference evidence
Read Beautiful Lyrics `surfbryce/beautiful-lyrics` through agent-reach's GitHub CLI route: `Extension/Source/Modules/LyricsRenderer/Components/{SyllableVocals,Interlude}.ts`. It uses syllable timing, separate spring scale/offset/glow, three interlude dots, and a 2.25-second breathing interval. Native adaptation will use solid clipped text (no shader/gradient), smoothstep timing and restrained line springs. Lyrics Plus source lookup is still pending; do not claim pixel/parameter parity.

### Step 1
Added immutable `LyricsTiming` with precomputed word/syllable offsets, inferred timing ends, exact active-line binary search, explicit-gap/intro/silent-line detection and distance opacity. No parsing on frames. Added JUnit4 regressions in the app's existing test home (timing boundaries, continuous sweep, inferred/explicit ends, syllables/spaces, gaps, backward seek and unsynced fallback).

Verification: tests authored, **not executed** in this chunk. Per session instruction CI is the Kotlin/Android build gate; no local Java/Gradle invoked, no push. `.github/workflows/android.yml` already runs app unit tests + API tests + debug assembly. It cannot validate these local-only commits until the parent pushes them.

## Device-unverified (must remain pending)
- Actual word/syllable sweep, wrapped words, punctuation, RTL/CJK layout and font clipping.
- Frame pacing/allocations/recomposition on a real Android device (normal + saver modes); provider correction/Connect drift, buffering, pause/resume, local tracks and rapid track changes.
- Centered follow / large forward and backward seek; manual scrolling and resume-follow; interlude placement at intro/explicit gaps.
- Blurred artwork, bright-cover contrast, missing-art AMOLED black, API <31 blur fallback, insets/font scaling/landscape.
- Full-screen expand + deliberate swipe-up + Back/close, accessibility and line seeking.
- Loading/no-lyrics/unsynced/line-sync/word-sync/translation states, APK compilation and screenshots.

Reachability: play a lyrics-bearing track → open Now Playing → tap inline lyrics expand or flick up on artwork. No new navigation stack, test login or hidden gesture is required. `:host-cdp` can drive Spotify playback/position but **cannot capture native Compose**; an installed emulator/device must drive the existing player route for screenshots.
