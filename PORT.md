# Cadence / Spotilol parity — work checkpoint

2026-10-23, supervised port chunks. **Not a completed parity report.** `port-brief.md` and the extraction `INDEX.md` were read in full; work is intentionally in order. Step 0's JVM cache slice is committed and retained, and its Android gate is now green. Step 1's native dock implementation and ten new regressions are Android-Gradle verified; device visual/accessibility acceptance remains open. Step 2's transport/snapshot regression slice is verified via the repo JVM/Android gates; the full playback audit is not complete. `LESSONS.md` retains cache mechanisms and the earlier JVM test boundaries. No push. **Next: Step 2 playback-integrity audit, then Steps 3–4 and the complete final ledger.**

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

## Parity ledger (pending is deliberately not a final disposition)

| Source from extraction INDEX | Disposition | Evidence / remaining work |
|---|---|---|
| Stranded lessons / catalog-search caching | Ported; repo JVM and Android gates verified | `b6e25e1` implementation; `87d1dd1` recovery-compatible regression. JVM: 68 passed / 31 account-gated skipped (`chunk2-jvm-retry.log`, exit 0). Android: `chunk5-android-root.log`, exit 0. `SpotifyApi.kt`, `gql/CachedGql.kt`, cache lifecycle/live tests; bounded LRU, TTL/SWR, single-flight, failure backoff, session invalidation. Existing home/library caches retained. |
| Nav a163288 / b0908b9 / c197801 (including reduced motion) | Ported natively; Android gate verified; device visual/accessibility proof pending | `MainBottomNavigation.kt`, `DockMiniPlayer.kt`, `DockScrollController.kt`, policy + ten regressions; `docs/native-dock.md`. Both source docs and reference files read. Actual repo Android gates exit 0, 197 native tests passed. Native system animator-scale support replaces CSS media query; Figtree replaces absent native Satoshi. No HTML/CSS copied or screenshot acceptance asserted. |
| Playback dcfb50e / 5a2dd1c | Step 2 snapshot / desired-state retry slice ported; full audit pending | `docs/playback-integrity-port.md`; immutable PlayerAPI/Compose metadata already present, new real-bridge interleaving + transport-generation regressions. Repo gate exit **0** (`chunk6-playback-retry.log`); 275 tests passed, including ten new regressions. Remaining watchdog/reload/ownership and local handoff audit next. |
| Uncommitted integrity leftovers (app.js, CustomUI.kt, AndroidAuto.kt, AndroidTracker.kt, PowerSave.kt + working-tree.diff) | Reviewed; per-file native/already-present/N/A dispositions recorded | `docs/playback-integrity-port.md`: native context transport, atomic metadata/media session, no global timer throttling; DOM bridge/overlay N/A. Arbitrary search album-context fallback intentionally not adopted. No extra UI copy; device audio proof not claimed. |
| Earlier UI/data 736227e | Pending detailed accounting | Compare extracted wiring against native API/UI; no mock data. |
| Perf 5b7aeb4 / PERF.md | Pending Step 3 | Step 0 supplies catalog/search cache; existing startup, screen caches, tokens, lyrics and palette prefetch documented in `LESSONS.md`. Must read findings before final dispositions. |
| Visual ad7e581 / 6842c73 | Pending detailed accounting | AMOLED / no gradients checks pass; native visual and icon parity still unaudited. Existing font is Figtree; do not invent Satoshi. |
| FEATURE-GAP.md / SPOTIFY-SPEED.md | Pending Step 3 action-table accounting | Research is not an instruction to implement every speculative feature; account for each relevant action with mechanism evidence or native/WebView N/A reason. |
| UTF-8 44d46ff + check-kotlin-string-size.py | Pending Step 4 | Do the analogous source/compiled string check after Steps 0–3, in order. |

Final completion must replace every pending row and expand the ledger to cover all extracted changes and the 20-row speed table. No completed port, visual acceptance, device latency, or playback continuity claim is made by this checkpoint.
