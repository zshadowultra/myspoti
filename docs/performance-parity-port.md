# Native performance parity audit — Steps 3–4 (2026-10-24)

Sources reviewed: extraction `findings/PERF.md`, all 20 rows of the closing
`SPOTIFY-SPEED.md` table, production hunks of `5b7aeb4`, wiring hunks of
`736227e`, `DATA_WIRING.md`, visual changes `ad7e581` / `6842c73`, and
`FEATURE-GAP.md`. This is a source/mechanism audit, not a new internet research
pass, device latency measurement or promise to implement the feature roadmap.

## PERF.md / 5b7aeb4 mechanisms

| Extracted action | Native disposition and concrete evidence |
|---|---|
| Cache scrub geometry and duration; one frame paint, one release seek, no cancel seek | Ported missing cancel/duration behavior: `SeekPreview.kt`, `SeekPreviewTest.kt`, the seek-control portion of `PlayerScreen.kt`; video/podcast callers discard previews on cancel. `SpotifySeekBar` already gets width from `onSizeChanged` and draws locally in Canvas, not from DOM geometry or bridge calls on moves. Updated callback holders prevent pointer handlers retaining an old track's preview. Three regression cases cover 120 moves/one commit, cancellation/fresh duration, invalid input/bounds. |
| Sample player once rather than repeated getters | Already present: `SongPlayer.nowPlaying`, `CurrentSongState.snapshot`, `player-bridge.js::mapState/pushState`; typed state, no independent title/art DOM getters. Retain prior playback integrity tests; do not redo their implementation. |
| Async bounded native HTTP for Home/Library/Search | Already present: typed HTTP reads in `Gql.kt` run under `withContext(Dispatchers.IO)`; native screen ViewModels launch/cancel jobs. Page network seam `ControlRelay.kt::dispatchMessage/send` queues and uses OkHttp callbacks. N/A to copy the old `nFetch` String compatibility API: this client has no CustomUI/ClassicBridge HTTP seam. |
| Dirty-cache Home DOM and cached connected player nodes | N/A to DOM cache: Home is typed `Home.feed` → `Api.getHomeFeed` / `HomeViewModel` → Compose; `HomeCache`/`ScreenCache` already provide caching. Page service discovery already disconnects its lookup observer once ready (`player-bridge.js`). No Home subtree scans at every progress tick. |
| Fixed-window/scoped metadata notifications; retain fallback | Already present native equivalent: page player `update`/`queue_update` listeners and equal-state suppression; native player health watchdog retained. N/A to 50ms observer debounce: no DOM-derived UI metadata pipeline to coalesce. Do not introduce a second Dealer socket or new observer. |
| Bound search row stagger without discarding results | N/A to CSS delay: native `SearchScreen` is a LazyColumn with no index×55ms invisible-row gate. `SearchViewModel` uses a 150ms cancellable debounce; Step 0 supplies search cache. No changed debounce, truncated result set or network-latency claim. |
| AndroidTracker body-observer starvation/reconnect | N/A: no AndroidTracker injection in the native client. Typed page events plus bounded health recovery replace it. |
| ToastFix skip custom-shell subtrees | N/A: no injected custom-shell DOM and no ToastFix scanner here; native errors/snackbars remain native. |
| Preserve legacy bridge callers; bind originating view/document | Already present native pending-call lifecycle in `Bridge.kt::pageGone/call` and relay replies. N/A to adding legacy native `nFetch` and custom-shell document nonce API. |
| Backup HTML/assets and fixture-only performance harness | N/A: not runtime code for this native architecture. Do not bundle demo songs, fake URIs, duplicate HTML shell or Satoshi assets. Native Gradle tests replace browser-shell fixture evidence, without claiming device speed. |

## Earlier real-data wiring 736227e

| Wiring fix | Disposition / native evidence |
|---|---|
| Play/pause desired state instead of current-state toggle bug | Already present: native play/pause actions delegate to `SongPlayer` and real page `PlayerAPI` services; native transports do not call `actPlayPause(isPlaying())`. |
| Correct title, artist, artwork, progress/duration | Already present: typed mapped `PlaybackState`/`SongPlayer.nowPlaying` atomic metadata, not context-item-link DOM selectors. Prior `PlaybackSnapshotTest` proves no mixed late metadata. |
| Real Home shelves | Already present: `Api.getHomeFeed`, `HomeViewModel`, `HomeScreen` with typed HomeItem and real URI routes; `prefetchHomeItem` starts real context lookups. No demo fallback port. |
| Real library, ID/cover/type adaptation; auth/error handling | Already present: `Api.getLibraryRoot/getLibraryFolder`, `SpotifyLibrary`, `LibraryScreeen.kt`, Response.Loading/Error/Success. Root memoization retained as `bfd7005`, not fake cadence IDs. |
| Real search; debounce, stale rejection, errors vs empty | Already present: `SearchViewModel` cancels prior job, repository typed search and Response error states; `SearchScreen` routes tracks/artists/albums/shows by actual ID. Step 0 cache retained. |
| URI playback from Home/Library/Search | Already present: actual native entity routes and context-track actions delegate to `SongPlayer.playContext/playSong`; native page-service URI validation. No unrelated-track toggle fallback copied. |
| Retain old AndroidAuto caller semantics / DOM sidebar fallback | N/A to those injection-specific compatibility adapters: native host/media session has its own typed contracts. Native Android Auto is not replaced by shell callbacks. |

## Visual ad7e581 / 6842c73 (scope: static house rules only)

Already present: `ui/theme/Color.kt::AppBackground=Color.Black`, black Material
background/surface tokens, `Type.kt` uses bundled **Figtree**, native Solar Bold
dock/player resources. `tools/check_fork_changes.py` checks all resource XML and
runtime gradient declarations. No stylesheet, font swap, blur/color redesign,
extra copy or screenshot acceptance added. `6842c73` is reference/findings work,
not new native runtime code; its dock was accounted for in completed Step 1.
Existing lyrics are expressly out of scope and untouched.

## Step 4 / 44d46ff

The old literal checker was inherited untracked, reviewed and corrected before
adoption. Its naive quote scanner desynchronized on nested `${...}` strings and
raw strings ending in four quotes. Six Python regressions now cover comments,
raw quoted delimiters, nested templates, NUL, surrogate-pair modified UTF-8,
constant-pool lengths and two-slot constants. Compiled mode reads actual
CONSTANT_Utf8 entries from project `.class` files and built project jars after
the Gradle gate, catching
constant-folded strings that literal scanning cannot. The source budget is
60,000 bytes (JVM hard limit 65,535); compiled mode fails if build outputs are
absent. Source parsing is conservative, not a Kotlin evaluator. CI runs both
modes and the checker tests. Do not run compiled mode while Gradle rewrites files.

No oversized native payload needed splitting. `BridgeScript.kt` loads the page
bridge as a resource; `ControlRelay.kt` has a much smaller embedded script.
The extracted 68,422-byte SpotilolPlayer `buildString` split is therefore N/A as
a production patch; the analogous preventive guard is ported.

## Limits / exclusions

`FEATURE-GAP.md` is an inventory/roadmap, not authority to implement new social,
commerce, DSP, entitlement or private-setting writes in Steps 3–4. All such
feature expansion is N/A to this chunk; existing native APIs/routes are retained,
not promoted to live-account verification. The final speed-table dispositions
are in `PORT.md`. In particular: paged APIs and incremental playlist/liked UI
are present, but album assembly remains eager; full local sort/queue views need
the complete loaded list. A new on-demand paging contract is not copied from a
shell proposal. No complete native delta-sync engine, protobuf decoder,
service-worker activity, first-audio latency, screenshot parity or device
continuity claim follows from this audit. Device acceptance from completed
Steps 1–2 remains a separate handoff, not an unresolved code-ledger row.
