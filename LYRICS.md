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

## 2026-10-24 · chunk 3 / renderer connected

Recovered the draft `BeautifulLyrics.kt` left by the preceding chunk and wired it into the full-screen route. Word/syllable geometry is cached from `TextLayoutResult` (including multi-row wraps); solid white text is clipped to each eased span width on draw frames. The list consumes derived active/interlude indices, not raw frame state, and uses stable timestamp/index keys. Unsynced text stays static/full white; line-sync switches the whole current line. Past lines settle to distance-dimmed white. Removed the full-screen unsynced banner/provider footer; retained translation/share and existing player controls.

Motion: word sweep **smoothstep** `p*p*(3-2*p)` over each source/inferred start–end; line scale **1.035**, spring damping **0.82**, stiffness **240**; distance alpha spring damping **1.0**, stiffness **260**; centered scroll spring damping **0.9**, stiffness **180**, up to four geometry corrections with no distant-item teleport; manual drag suspends follow **3 s**. Distance alpha **1 / .64 / .44 / .28**. Instrumental gaps require an explicit source end and at least **4 s**, or an explicit silent row/intro. Three dots breathe on playback time over **2.25 s**, phase stagger **0.16 cycles**, radius **4 dp** and spacing **20 dp**.

Checks so far: `git diff --check`; no local JVM/build/test or device run. Artwork and Solar controls are the next scoped step.

## 2026-10-24 · chunk 4 / stranded presentation recovered

Reviewed the preceding session's in-flight `LyricsScreen`, `PlayerScreen`, artwork draft and eight Solar Bold vectors; they are now a scoped commit rather than stranded work. Player passes the current cover URI, full-screen foreground is solid white, the backing surface is AMOLED black and the draft image treatment uses 48 dp platform blur plus a flat 68% black overlay (no gradients). The tiny decode alone on API <31 is only a provisional softening, **not a verified heavy-blur fallback**; a real off-main bitmap blur is the next step. Figtree/AppSans unchanged.

Solar close/menu/expand/share/translation/play/pause/check use preserved 480 Design paths and attribution in the vector headers + `docs/assets/lyrics-solar-icons.json`; reproducible conversion is `tools/generate_lyrics_icons.py` and preserves each path's fill rule. Added `lyrics-page` and `lyrics-lines` test tags for native automation. Existing navigation, translation/share and playback actions retained. Unrelated library files/briefs are not included.

Checks: generator executed, vectors parsed as XML, `git diff --check`. No Android build/device result claimed for this recovery commit.

## 2026-10-24 · chunk 4 / bounded artwork blur

Finished the fallback rather than relying on low-resolution pixelation: Glide decodes at most 64 × 64, disables hardware bitmaps and applies a cached `BitmapTransformation` on its worker thread. Three separable box-blur passes, **radius 8 source pixels**, approximate a heavy Gaussian on every supported API; API 31+ adds Compose's static **48 dp** platform blur. Border sampling clamps so the bitmap itself has no dark/transparent halo. No image transition, no gradient, no frame-clock work. Pixel buffers are bounded to 4,096 pixels and allocated only per artwork transform. The transformation's versioned disk key includes size/radius/pass settings.

The cover crop remains full-screen beneath a **flat black alpha 0.68**. Missing/blank/failed covers reveal the existing pure black backing surface; an artwork-URI composition key prevents the preceding track's image from being retained on a new-track failure. Added five JUnit4 regressions for uniform colours/opaque edges, two-axis spreading/source immutability, rectangular/one-pixel covers, and input bounds. Solar attribution now also appears in README Credits. Cleaned the recovered full-screen nesting/indentation; no timing parameters changed.

Build assumption correction: `LESSONS.md`/workspace tooling show JDK 21 + SDK 37.0 are now available, despite the inherited brief's “no local Java” note. Started the real repository Gradle gate serially (`:app:testDebugUnitTest :app:assembleDebug :api:test :api-spotify:test :host-cdp:test`, exact Gradle 9.7.1, offline caches). Result is **pending** at this commit; receipts will be checkpointed before finishing. CI still has not run for local-only commits; no push.

## 2026-10-24 · chunk 4 / gate complete and handoff

Commits: **47cc3ff** (recovered route/artwork draft + Solar controls), **0115731** (real bounded artwork blur + regressions). Both on main; no push.

Repository Gradle gate **PASSED**, exit 0, **3m 51s**: app unit tests + debug APK assembly, `:api:test` (no source), `:api-spotify:test` (up-to-date prior passing result), `:host-cdp:test`. App XML: **202 tests, zero failures/errors/skips**, including **4 LyricsTiming + 5 LyricsArtworkPixels tests** newly executed. API Spotify XML: 85 passing tests (cached task); host CDP: 31 account-only tests skipped, not a live playback proof. Gate ran against this working tree, including the pre-existing uncommitted library files; those files remain untouched/uncommitted by this session.

Evidence: `docs/port-evidence/lyrics-chunk4-gate.{log,exit}` and `lyrics-chunk4-summary.json` (source commit, exact command, tooling and counts). APK produced at `base/app/build/outputs/apk/debug/app-debug.apk`, not committed. Generator/VectorDrawable XML checks and `git diff --check` also passed; stash list empty. No CI dispatch, install, native screenshot or account playback run.

Next scoped step: native emulator/device verification of the existing player → lyrics route, prioritizing the API <31 bitmap fallback versus API 31+ treatment, bright/missing/failed covers and rapid track switches; then sweep/wrap/scroll/frame pacing. Do not redo timing/artwork plumbing merely because screenshots remain pending. Lyrics Plus reference lookup remains pending; current motion is an adaptation, not a pixel-parity claim.

## Device-unverified (must remain pending)
- Actual word/syllable sweep, wrapped words, punctuation, RTL/CJK layout and font clipping.
- Frame pacing/allocations/recomposition on a real Android device (normal + saver modes); provider correction/Connect drift, buffering, pause/resume, local tracks and rapid track changes.
- Centered follow / large forward and backward seek; manual scrolling and resume-follow; interlude placement at intro/explicit gaps.
- Blurred artwork, bright-cover contrast, missing-art AMOLED black, API <31 blur fallback, insets/font scaling/landscape.
- Full-screen expand + deliberate swipe-up + Back/close, accessibility and line seeking.
- Loading/no-lyrics/unsynced/line-sync/word-sync/translation states and native screenshots (APK compilation now verified above).

Reachability: play a lyrics-bearing track → open Now Playing → tap inline lyrics expand or flick up on artwork. No new navigation stack, test login or hidden gesture is required. `:host-cdp` can drive Spotify playback/position but **cannot capture native Compose**; an installed emulator/device must drive the existing player route for screenshots.
