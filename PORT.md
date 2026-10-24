# Cadence / Spotilol parity — work checkpoint

2026-10-24, supervised port chunks. **Steps 0–2 and lyrics are completed and retained; Steps 3–4 source dispositions are recorded below.** This is code/repo-test parity, not measured device performance or screenshot/audio acceptance. Step 3 retains the verified library memoization and native performance mechanisms, and fixes scrub cancellation/duration capture. Step 4 ports source + compiled UTF8 guards. See `docs/performance-parity-port.md` and the current gate receipt below. No lyrics edits, dependency downgrades or push. Historical chunk reports below describe the state at their own date and are superseded by the final ledger.

## Historical gate / continuation (superseded by Chunk 5 below)

Step 0's JVM verification is committed and is not being repeated. The user explicitly permits continuing to Step 1 with an accurately recorded Android environment blocker; an implemented slice still does not count as verified without the repo gate. The first gate (`/tmp/cadence-port/verified-build.log`) **failed** before tasks: Android SDK Platform 37.0 was missing and its license unaccepted. The second chunk installed the exact declared `platforms;android-37.0` (revision 2) using SDK Manager; no compileSdk/dependency downgrade. SDK Manager needed the refreshed egress truststore passed explicitly via `_JAVA_OPTIONS` (Gradle's properties do not apply to SDK Manager).

The first fresh JVM gate (`chunk2-jvm.log`) ran **68 tests: 67 passed / 1 failed**; CDP separately skipped 31 account-gated tests. Its failure was an invalid `assertSame` exception assertion (coroutine stack-trace recovery copies exceptions); corrected to assert propagated class/message while retaining owner-survival/retry checks. The first Android run timed out on Gradle's shared journal lock, not source compilation.

Current repo evidence: corrected JVM `:api:test :api-spotify:test :host-cdp:test` **BUILD SUCCESSFUL in 47s**, exit **0**, `/tmp/cadence-port/chunk2-jvm-retry.log` + `.exit`. XML totals: **68 passed / 31 account-gated skipped / 0 failures / 0 errors**. This verifies all cache regressions and the earlier JVM player/order/refusal/volume checks under the actual Gradle test runtime.

Android `testDebugUnitTest assembleDebug` in `/tmp/cadence-port/chunk2-android-final.log` **failed**, exit **1**, during dependency resolution: the local mirror timed out serving a HEAD request for Glide Compose `1.0.0-beta01.aar`, disabling its repository and producing a secondary configuration-cache serialization error. No Android source compilation/test success is claimed. Retrying with local HTTP connection/socket timeouts of 300000 ms (no project/version changes): `/tmp/cadence-port/chunk2-android-timeout-retry.log` + `.exit`. Its earlier retry was deliberately interrupted during first-build dependency resolution to prioritize verifying the corrected JVM regression within the chunk; that daemon-disappeared result is **not** a crash claim or a passing build. **Android gate still pending; Step 0 is not complete; Steps 1–4 remain untouched.** Exact wrapper 9.7.1 is provisioned. No supplemental compiler result is substituted for the Android gate.

## Chunk 3 — Maven diagnosis / next native slice

The timeout retry also **failed**, exit **1**: `/tmp/cadence-port/chunk2-android-timeout-retry.log` could not resolve the exact Kotlin Compose/serialization compiler-plugin jars (`2.4.0`). The mirror's downloader stops on a non-404 from its first upstream and reports interrupted successful bodies as 404. Direct Maven Central probes reproduced `IncompleteRead` for all three artifacts (`chunk3-mirror-probe.log`); increasing Gradle's timeout alone cannot repair truncated responses.

Local-environment repair only (no dependency, SDK or wrapper version change): resumed explicit byte ranges and checked the artifacts against Maven Central's SHA-1 receipts before seeding the local mirror cache. Glide Compose AAR: **128586 bytes**, `986b041df72fd9daacc9122a13282ed053dff96b`; serialization plugin: **570247 bytes**, `9c09aa2727d9ade95c1b0a00458e74feced83908` (`chunk3-mirror-range.log`); Compose plugin: **949087 bytes**, `77913398cd4a9cb385000d56d51501954d1da6db` (`chunk3-compose-repair.log`). Local HEAD now returns **200** and those exact Content-Length values for all three. `~/bin/maven-mirror.py` now resumes incomplete downloads with checked range offsets/lengths and tries another upstream after a failed one; `python3 -m py_compile` passes. This is a sandbox repair, not a repo code change.

`chunk3-android.log` was deliberately stopped during refreshed dependency resolution to restart the repaired mirror (not a spontaneous daemon crash). The native slice's first serial gate (`chunk3-nav-android.log`, exit **1**) still hit Gradle's cached missing-jar entries, **before source compilation**. A serial retry with `--refresh-dependencies` is running at `/tmp/cadence-port/chunk3-nav-refresh.log` + `.exit`; do not launch another concurrent Gradle gate. Original Glide timeout remains exactly evidenced by `chunk2-android-final.log`, exit **1**. Android success is **not yet claimed**.

Both nav design documents were read completely, and both reference files were opened with Read: the current model cannot receive image attachments (they are JPEGs under `.png` names). No visual acceptance is claimed. Native slice uses pixel-sampled `#313131` / selected `#646464`, the existing Figtree typeface, real Home/Library/Settings routes plus detached Search, immutable `CurrentSongState.snapshot`, existing `SongPlayer` transports and `PlayerSheet`. Expanded-only work is being gated; collapsed scroll/hysteresis/reduced-motion parity is still pending, so Step 1 is not complete. Existing page clearance is already `160.dp + navigation inset` (`BottomBarsSpace.kt`). `python3 tools/check_fork_changes.py` passes (exit **0**, `chunk3-fork-check.log`): product naming, all resource XML, merge policy and no runtime gradient declarations. This static check does **not** substitute for Android compilation/tests.

## Chunk 5 — reboot recovery and native dock gate

On continuation the checkpoint's `/tmp/cadence-port/` logs and Gradle process were absent (sandbox reboot). Step 0's committed JVM pass is retained and **not rerun**. The original Android failure remains recorded above with its exact supplied evidence: `/tmp/cadence-port/chunk2-android-final.log`, exit 1, Glide Compose AAR HEAD timeout; that historical file is no longer locally readable. The inherited untracked `docs/port-evidence/chunk4-android.log` / `.exit` contains only daemon startup and exit **143**, not a test result.

The prior environment repair is genuinely effective: after `~/bin/android-build-env.sh`, local HEAD for `com/github/bumptech/glide/compose/1.0.0-beta01/compose-1.0.0-beta01.aar` returns **200**, Content-Length **128586**. No project dependency/SDK/wrapper version change. A **single serial** repo gate is now running at `/tmp/cadence-port/chunk5-android.log` + `.exit`: from `base/`, `./gradlew -I /tmp/cadence-early-mirror.gradle :app:testDebugUnitTest :app:assembleDebug --refresh-dependencies --no-configuration-cache`, JDK 21, SDK 37r2, explicit `GRADLE_USER_HOME=$HOME/.gradle`, IPv4 and refreshed egress truststore. The temporary init script adds the local mirror in `beforeSettings` for Foojay; the existing global `settingsEvaluated` hook handles normal dependencies. The gate has reached `:app:compileDebugKotlin` / dexing, past the historical dependency blocker. The first chunk-5 gate **failed**, exit **1**, on the temporarily inconsistent `DockMiniPlayer(enabled=...)` signature while source edits were in flight. It is not a verification pass and not attributed to Maven. The final source was frozen before the serial retry: `:app:testDebugUnitTest :app:assembleDebug` **BUILD SUCCESSFUL in 3m 36s**, exit **0**, `docs/port-evidence/chunk5-dock-final.log` / `.exit`. The exact broader requested entry point, **`testDebugUnitTest assembleDebug`**, then **BUILD SUCCESSFUL in 6s**, exit **0**, `docs/port-evidence/chunk5-android-root.log` / `.exit` (84 tasks up-to-date from valid repo outputs). XML totals: app **189 passed**, host-android **8 passed**, **0 failures / 0 errors / 0 skipped**; includes all ten new dock regressions. `docs/port-evidence/chunk5-test-summary.json` and the two dock XML reports preserve the counts/tests. APK: `base/app/build/outputs/apk/debug/app-debug.apk`. Static fork/style checker also passes, exit 0 (`chunk5-fork-check.log` / `.exit`). Step 0's historical Android environment blocker is **resolved**, with no version downgrade; its JVM gate was not repeated.

Step 1 is implemented natively: expanded and collapsed reference geometries, real navigation/player transports, atomic mini-player snapshot, Solar Bold vectors, AMOLED black, fixed clearance, one-frame scroll decisions, 8dp hysteresis / 4dp top threshold / 120ms settle, transform/opacity-only 280ms expressive morph using the system motion-scale-aware Compose animation runtime. `docs/native-dock.md` records mechanisms and limits. Both reference files were opened with Read again, but this model cannot receive image attachments; no screenshot perfection, TalkBack/focus transfer, on-device reduced-motion or background-continuity acceptance is claimed. Those remain device validation, not an Android compile blocker. No new large step was started late in the chunk.

## Chunk 6 — playback retry / snapshot slice

Started with the requested `git log --oneline -8` and this checkpoint, then read
`port-brief.md` and extraction `INDEX.md` in full. The recorded Step 0 JVM and
Step 1 dock work was retained, not rebuilt from scratch. `~/bin/android-build-env.sh`
restored sandbox services; the exact Glide Compose AAR HEAD returns **200**,
Content-Length **128586**. The historical Maven blocker stays resolved.

The inherited tree had uncommitted `SpotifyPlayer.kt`, `StallTest.kt`,
`SongPlayer.kt` and `TransportIntentTest.kt` edits. Reviewed them, added real
bridge interleaving tests, and closed further stale recovery/fallback paths.
Explicit play/pause/resume/skip revisions supersede suspended retries; automatic
play/resume keeps its initiating revision. Pause discards held skips/reload
intent; cancelled previous must **not** enter the seek/play fallback. App-side
transports cancel superseded jobs, episode repair stays in the queue, pending
skip bursts/cold-start resume are cleared on pause, and pause logs name user /
system-media / autoplay-off / sleep sources without new UI copy.

`docs/playback-integrity-port.md` accounts for all four integrity symptoms and
all five uncommitted extraction files. Atomic PlayerAPI → Bridge → mapped state
→ Compose snapshot already exists; new regression sends A(100), B(200), late
A(150) through the real bridge and keeps all of B's metadata. Native media
session publishes one metadata object and rejects stale cover downloads.

First serial repo gate (`:api-spotify:test testDebugUnitTest assembleDebug`)
**failed**, exit **1**, `/tmp/cadence-port/chunk6-playback.log`: **78 JVM tests,
77 passed / 1 failed**, `TransportIntentTest` previous→pause proved cancellation
was swallowed into the previous fallback. Fixed by rethrowing cancellation and
retaining the initiating revision in fallback commands. This failure is not a
Maven/environment claim. Source was frozen before serial retry at
`/tmp/cadence-port/chunk6-playback-retry.log` + `.exit`: **BUILD SUCCESSFUL in
2m 10s**, exit **0**. Durable copies of both runs and the style check are in
`docs/port-evidence/`. XML totals: **api-spotify 78 passed, app 189 passed,
host-android 8 passed; 0 failures / 0 errors / 0 skipped**. All ten new regressions
pass via Gradle. `chunk6-test-summary.json` and the three playback regression
XML files preserve the counts and cases. APK rebuilt at
`base/app/build/outputs/apk/debug/app-debug.apk`. Static fork/style check also
passes, exit **0** (`chunk6-fork-check.log` / `.exit`). No Step 0-specific JVM gate
or account-gated CDP test suite was rerun.

**Step 2 remains partial.** Next slice: remaining automatic watchdog / reload
handoffs and Connect ownership (no repair permission from unknown/remote
ownership), plus local-source handoff pauses that still bypass the transport
queue. `MediaProxy.ENABLED=false` makes the old full-body CDN type-probe fix N/A;
no global Spotify interval throttling exists. Existing native Canvas/clip
lifecycle, renderer-priority policy, foreground media-service grace and Media3
local audio-focus/noisy policy were audited; not presented as measured audible
screen-off continuity. Task-dismissal/detached WebView visibility still needs
real device evidence. Steps 3–4 and final complete speed/visual/data ledger were
not started out of order. No push, no dependency/version downgrade.

## Chunk 7 — watchdog / Connect ownership / local handoff

Retained Step 0 and the committed dock/playback verification. Rechecked the
specific historical dependency only: exact Glide Compose AAR HEAD **200**,
Content-Length **128586**, local mirror `127.0.0.1:8899`; no repeat of the old
Step 0 verification slice. The original `chunk2-android-final.log`, exit 1,
remains historical evidence, not a current environment blocker.

Closed the remaining automatic transport handoffs: capture revisions before
suspended media reads/pings and ready callbacks; carry them through reload and
onward/seek commands, with positive local Connect ownership before recovery and
each retry. Kotlin null ownership no longer inherits a prior local answer; JS
clears expired ownership on a failed refresh. Page loss clears native ownership.
A frozen/unanswering page has **unknown ownership**: consecutive-miss diagnostics
remain, but automatic reload/claim is deliberately withheld rather than seizing a
possibly remote session. Explicit user transport/transfer is retained. Preload
and fades require known-local ownership. Both local-source handoff pauses now
use the latest-generation queue, clear cold-start resume/delayed skips, and log
`local-source-handoff` / `local-source-retry`; app pull-back preserves cancellation
and rechecks generation before issuing the final command.

First serial gate **failed**, exit **1**, `docs/port-evidence/chunk7-playback.log`:
83 JVM tests / 2 assertion failures. Both incorrectly counted `player.preloadNext`
as a transport command; XML showed the expected pause/resume/next and only that
extra preload. Corrected the test's explicit transport-method filter and added
automatic-retry transfer plus stale-null-ownership regressions. No production
failure is attributed to Maven.

Frozen-source serial retry from `base/`, exact repo entry points
`./gradlew -I /tmp/cadence-early-mirror.gradle :api-spotify:test testDebugUnitTest assembleDebug --no-configuration-cache`,
**BUILD SUCCESSFUL in 1m 5s**, exit **0**:
`docs/port-evidence/chunk7-playback-retry.log` / `.exit`. XML totals **api-spotify
85 passed, app 189 passed, host-android 8 passed; 0 failures/errors/skips**,
including seven real-bridge/watchdog regressions (`WatchdogRecoveryTest`). Summary
and regression XML are durable in `docs/port-evidence/`. APK rebuilt. Static
fork/style gate exit **0**, `chunk7-fork-check.log` / `.exit`. No Step 0-specific
JVM/account-gated CDP gate repeated. `adb devices` reports **no devices attached**;
no physical local-handoff, screen-off, task-dismissal, TalkBack or audible playback
proof is claimed. `docs/playback-integrity-port.md` now accounts for all four
symptoms and extraction leftovers at the source/repo-test boundary. **Step 2's
code/source audit is complete; device acceptance remains explicitly open.**

## Final extraction ledger (source/repo-test scope)

| Source from extraction INDEX | Disposition | Evidence / remaining work |
|---|---|---|
| Stranded lessons / catalog-search caching | Ported; repo JVM and Android gates verified | `b6e25e1` implementation; `87d1dd1` recovery-compatible regression. JVM: 68 passed / 31 account-gated skipped (`chunk2-jvm-retry.log`, exit 0). Android: `chunk5-android-root.log`, exit 0. `SpotifyApi.kt`, `gql/CachedGql.kt`, cache lifecycle/live tests; bounded LRU, TTL/SWR, single-flight, failure backoff, session invalidation. Existing home/library caches retained. |
| Nav a163288 / b0908b9 / c197801 (including reduced motion) | Ported natively; Android gate verified; device visual/accessibility acceptance separate | `MainBottomNavigation.kt`, `DockMiniPlayer.kt`, `DockScrollController.kt`, policy + ten regressions; `docs/native-dock.md`. Both source docs and reference files read. Actual repo Android gates exit 0, 197 native tests passed. Native system animator-scale support replaces CSS media query; Figtree replaces absent native Satoshi. No HTML/CSS copied or screenshot acceptance asserted. |
| Playback dcfb50e / 5a2dd1c | Ported native transport/watchdog protection; atomic metadata and lifecycle policy already present; device acceptance open | `docs/playback-integrity-port.md`; immutable PlayerAPI/Compose metadata, real-bridge interleaving and generation/ownership regressions. Repo gates exit **0** (`chunk6-playback-retry.log`, `chunk7-playback-retry.log`); latest 282 tests passed. No permission to repair from unknown/remote ownership; local-source pauses serialized. Audible screen-off continuity is not claimed. |
| Uncommitted integrity leftovers (app.js, CustomUI.kt, AndroidAuto.kt, AndroidTracker.kt, PowerSave.kt + working-tree.diff) | Reviewed; per-file native/already-present/N/A dispositions recorded | `docs/playback-integrity-port.md`: native context transport, atomic metadata/media session, no global timer throttling; DOM bridge/overlay N/A. Arbitrary search album-context fallback intentionally not adopted. No extra UI copy; device audio proof not claimed. |
| Earlier UI/data 736227e | Already present native equivalents; injection compatibility N/A | `docs/performance-parity-port.md` wiring table: actual typed title/art/range, Home feed, root/folder library, cancellable real search, URI/context transports, auth/errors. No demo data or DOM sidebar copied. |
| Perf 5b7aeb4 / PERF.md | Ported missing native scrub behavior and library memoization; remaining mechanisms already present / injection-only N/A | `SeekPreview.kt` + three regressions; `bfd7005` root assembly + four retained regressions. Full per-action reasons in `docs/performance-parity-port.md`; typed async HTTP, native Canvas, cached Home, service events replace shell seams. Step 0 caches untouched. |
| Visual ad7e581 / 6842c73 | Already present native static rules; HTML/Satoshi assets N/A | Black `AppBackground` and Material surfaces, bundled Figtree, Solar Bold native controls; fork/style checker. Reference/findings commit has no additional native runtime code. No gradients, type swap, lyrics changes or new UI text. Visual/device acceptance is not claimed. |
| FEATURE-GAP.md / SPOTIFY-SPEED.md | Resolved: native mechanisms already present / justified N/A; see all 20 actions below | Feature expansion roadmap N/A to Steps 3–4: no new social/commerce/DSP/entitlement writes. Preserve existing native APIs without treating interfaces as live proof. `docs/performance-parity-port.md` gives scope and exclusions. |
| UTF-8 44d46ff + check-kotlin-string-size.py | Guard ported; oversized injection split N/A | `tools/check_kotlin_string_size.py`, six Python regressions, CI source/compiled modes; actual project CONSTANT_Utf8 inspection after Gradle. Native bridge is a resource, not Spotilol's 68,422-byte literal. Gate/size receipts below. |

## Step 3 — all 20 SPOTIFY-SPEED closing actions

“Already present” means the concrete native mechanism, not proven latency parity.
N/A reasons apply to the proposed adaptation, not a claim that upstream behavior
is absent. No speculative network feature or new measured speed promise.

| # / action | Disposition | Concrete reason / source evidence |
|---|---|---|
| 1 Typed reads / page-service commands | Already present | `SpotifyApi.kt` / `gql/Gql.kt` shared typed HTTP; `player-bridge.js::player.play` uses page services. No UI DOM navigation/scraping adapter needed. |
| 2 Single-flight / tokens / bounded freshness | Already present | `SongPlayer.libraryContains` shared reads + 60s saved freshness; token mutex in `Gql.kt`; completed Step 0 bounded catalog/search cache. Mutations are not deduplicated. |
| 3 Idempotent boot / ready | Already present | `Bridge.kt::inject/pageGone`, `player-bridge.js` existing bootstrap/reannouncement guard, `MyspotiRuntime` retained token. Duplicate page-finished events do not reinstall collectors. |
| 4 Dealer/service pushes vs polling | Already present; second Dealer N/A | `player-bridge.js` listens to `update`/`queue_update` and suppresses equal state; `SpotifyPlayer` keeps health watchdog. Official page owns Dealer, so a competing native socket is not justified. |
| 5 Changed media/notification publication | Already present | `player-bridge.js::pushState` equality; `PlaybackService`/`SongPlayer` metadata keys; shared health reads. Progress remains local, not notification work every frame. |
| 6 Local progress / finger-owned scrub | Ported missing portion; clock already present | Native Canvas + local `MusicVideoController.position`; `SeekPreview` captures duration, release commits once, cancel never seeks. Video/podcast cancel previews too. Three new native regressions; no per-move page seek. |
| 7 Persisted operation hashes | Already present | `Gql.kt` persisted queries and `GqlOpsUpdater` build/hash table. No hash-only server-cache claim or mutation replay. |
| 8 Paged catalog/library reads | Already present APIs and incremental playlist/liked UI; new on-demand contract N/A | `SpotifyLibrary` / `SpotifyContent` offsets/limits; `Api.getPlaylistPage/getLikedPage` emit growing snapshots before completion. Current album/full-library sort/queue contract deliberately consumes complete data; shell-only scroll-demand callbacks cannot be copied without changing that contract. Album assembly still eager, not advertised as lazy parity. |
| 9 Batched protobuf metadata / TTLs | N/A to new transport/decoder; typed metadata already present | Current GQL returns text/structured JSON and already includes row fields; no missing-entity batch workload or verified native protobuf decoder in extraction. Binary endpoint/TTL engine would be new protocol work, not a demonstrated native perf fix. Step 0 explicit read TTLs retained, not relabeled endpoint TTLs. |
| 10 Artwork variants / colors | Already present native size selection; gradients N/A | `Mappers.kt` preserves dimensions; `Api.cover` selects best(300), `big` best(640); Glide/lazy rows decode at target bounds, not universally largest art. Existing `DockMiniPlayer` palette/Canvas owner retained. Do not turn colors into gradients or add a second prefetcher. No claimed measured image-transfer saving. |
| 11 Revision-aware sync / write reconciliation | Already present confirmed local writes; new delta engine N/A | `SpotifyLibrary.changes`, `SongPlayer.setSaved` failure/read-back and `likedChanges`, `ContextViewModel` refresh/rollback. Full cross-device native revision/delta engine is not implemented or claimed; extraction only proposes it and supplies no verified delta adapter to port. |
| 12 Persisted cached-first warm screens | Already present | `ScreenCache` private home/library/liked snapshots + ordered writes/clear; `HomeCache`, `ContextViewModel.recent` set cached state before first composition. Cached metadata is not offline audio. |
| 13 Workbox caches | Already present host policy; worker policy change N/A | `WebPlayerHost` DOM storage/LOAD_DEFAULT; no WorkerNeutralize injection. Preserve official page behavior. Without device enabled/disabled A/B, do not force unregister/register or claim actual active-worker coverage. |
| 14 Hashed assets / split boot | Already present | Official page uses default cache, `GqlOpsUpdater` caches by bundle/build identity; native screen cache paints independently of page readiness. No custom HTML shell/chunk-map loader to port. No prefetch of all 164 chunks or startup-budget claim. |
| 15 Preconnect / pooling / discovery | Already present preservation; new regional tuning N/A | Shared `SpotifyHttp` client and unchanged official page networking/hints. Native/WebView pools differ; no captured native apresolve contract or proven HTTP/3 negotiation justifies hard-coded endpoint/pool changes. |
| 16 Nearby/multi-CDN edge placement | N/A | Spotify-owned edge deployment/VCL is server infrastructure, not an app hook. Official playback URLs/services remain unchanged; disabled media proxy stays disabled. |
| 17 Range/chunks / BBR | N/A | Official DRM/audio pipeline owns streaming; client has no server congestion-control knob. Historical BBR research did not improve click-to-sound; no new range streamer or signed-URL rewriting. |
| 18 Queue-aware next preload | Already present | `player-bridge.js::player.preloadNext`; `SpotifyPlayer.preloadNextSong` gates/deduplicates, including completed Step 2 local ownership guards. No competing preload chain or gapless prediction claim. |
| 19 Stable/windowed lists / memoization | Ported remaining library slice; context already present | `bfd7005` `RootLibrary` derived assembly + remembered filtering; four `RootLibraryTest` cases retained/verified. `ContextScreen` LazyColumn/stable occurrence keys/derived filter-sort; native frame Canvas has no DOM read/write thrash. |
| 20 Integer-helper WASM | N/A | Official page runs its shipped integer helpers; no extraction evidence of UI/audio-decoder WASM or adoptable speed win. Native Compose needs no additional WASM runtime. |

## Additional native Step 3 candidates

Startup/pre-render: already present cached-first `ScreenCache`/Home state and
`ContextViewModel.recent` seeded before composition; native Compose has no DOM
pre-render tree to install. Boot guards are action 3. Coalesced updates are
actions 4–5. Search prefetch/cache: Step 0 read cache and cancellable search already
present; arbitrary query prediction N/A without user intent/workload evidence.
Context press hints and bounded `prefetchSoon` already warm likely Home/Library
pages. Palette/Canvas prefetch has one existing dock owner, so no duplicate work
is introduced. Lyrics prefetch is completed/out of scope and untouched.

The final ledger resolves code dispositions only. Real device visual/focus,
TalkBack, reduced-motion, background/audio continuity and latency acceptance
remain separate handoffs; none are claimed from fixture or Gradle success.

## Steps 3–4 final gate / local commits — 2026-10-24

Arrival `git log --oneline -8` proved the requested library leftovers were already
committed (`bfd7005`) and verified (`7369b64`, retained repo gate exit 0). Reviewed
and retained them rather than duplicating/reverting completed work. Steps 0–2
and the completed lyrics implementation were not reworked.

Native scrub correction is `879b911`; the preventive UTF8 guard is `8e2f942`.
The first fresh native gate failed because the new mandatory cancel callback had
two additional video/podcast call sites; both now explicitly discard previews.
This was a source integration error, not an environment/dependency failure:
`docs/port-evidence/steps34-scrub-gate.log` / `.exit`, exit **1**.
Frozen-source serial retry from `base/`:
`./gradlew -I /tmp/cadence-early-mirror.gradle testDebugUnitTest assembleDebug --no-configuration-cache`
**BUILD SUCCESSFUL in 3m**, exit **0**, `steps34-scrub-retry.log` / `.exit`.
Exact existing JDK 21 / SDK 37r2 / wrapper 9.7.1 / dependencies retained; explicit
GRADLE_USER_HOME, IPv4 and sandbox mirror/truststore. XML summary: **app 205,
host-android 8 tests; 0 failures/errors/skips** (`steps34-final-summary.json`);
new three-case scrub XML preserved. App tests actually executed in this retry;
host tests were up-to-date. APK rebuilt at
`base/app/build/outputs/apk/debug/app-debug.apk`.

Step 4 source + compiled checks exit **0** (`steps34-utf8.log` / `.exit`):
**5,502 literals / 255 Kotlin files**, largest literal **4,330 bytes**;
**285,700 UTF8 constants / 2,326 project classes, including 3 built project jars**,
largest compiled entry **13,448 bytes**. Both below the **60,000-byte** safety
budget. Six Python checker regressions exit **0** (`steps34-utf8-tests.log` /
`.exit`). Old checker template/raw delimiter failures were repaired, not ignored;
compiled audit was rerun only after Gradle finished rewriting its outputs.

Static fork/style gate exit **0** (`steps34-final-style.log` / `.exit`): product
naming, resource XML, merge policy and no runtime gradients. Actual page bridge
lesson mechanism test exit **0** (`steps34-bridge-mechanisms.log` / `.exit`):
scoped/coalesced boot lookup and async request-ID result. This supplemental JS
mechanism check is not the Android gate or device audio proof. Completed Step 0
JVM/account-gated suites were not rerun. No lyrics changes; no push.

All extraction and 20 speed-table rows now have a source disposition; no ledger
row is awaiting implementation/accounting. Supplied local brief files remain
unchanged/untracked inputs, not generated production work. All code, tools,
ledger updates and gate evidence from this chunk are committed locally on main.
