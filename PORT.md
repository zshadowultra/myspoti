# Cadence / Spotilol parity — work checkpoint

2026-10-23, supervised port chunks. **Not a completed parity report.** Read `port-brief.md` and the extraction `INDEX.md` in full; work is intentionally in order. Only Step 0's cache slice was implemented/reviewed here. `LESSONS.md` contains mechanisms, request-count evidence, exact test boundaries and the continuation command. No push.

## Gate / continuation

Do not begin Step 1 until the repo's own build/test gate passes. The first gate (`/tmp/cadence-port/verified-build.log`) **failed** before tasks: Android SDK Platform 37.0 was missing and its license unaccepted. The second chunk installed the exact declared `platforms;android-37.0` (revision 2) using SDK Manager; no compileSdk/dependency downgrade. SDK Manager needed the refreshed egress truststore passed explicitly via `_JAVA_OPTIONS` (Gradle's properties do not apply to SDK Manager).

The first fresh JVM gate (`chunk2-jvm.log`) ran **68 tests: 67 passed / 1 failed**; CDP separately skipped 31 account-gated tests. Its failure was an invalid `assertSame` exception assertion (coroutine stack-trace recovery copies exceptions); corrected to assert propagated class/message while retaining owner-survival/retry checks. The first Android run timed out on Gradle's shared journal lock, not source compilation.

Current repo evidence: corrected JVM `:api:test :api-spotify:test :host-cdp:test` **BUILD SUCCESSFUL in 47s**, exit **0**, `/tmp/cadence-port/chunk2-jvm-retry.log` + `.exit`. XML totals: **68 passed / 31 account-gated skipped / 0 failures / 0 errors**. This verifies all cache regressions and the earlier JVM player/order/refusal/volume checks under the actual Gradle test runtime.

Android `testDebugUnitTest assembleDebug` is now running alone in `/tmp/cadence-port/chunk2-android-final.log` + `.exit`. Its earlier retry was deliberately interrupted during first-build dependency resolution to prioritize verifying the corrected JVM regression within the chunk; that daemon-disappeared result is **not** a crash claim or a passing build. **Android gate still pending; Step 0 is not complete; Steps 1–4 remain untouched.** Exact wrapper 9.7.1 is provisioned. No supplemental compiler result is substituted for the Android gate.

## Parity ledger (pending is deliberately not a final disposition)

| Source from extraction INDEX | Disposition | Evidence / remaining work |
|---|---|---|
| Stranded lessons / catalog-search caching | Implemented; JVM Gradle tests verified; Android gate pending | `b6e25e1` implementation; `87d1dd1` recovery-compatible regression. Actual wrapper tests: 68 passed / 31 account-gated skipped / 0 failed (`chunk2-jvm-retry.log`, exit 0). `SpotifyApi.kt`, `gql/CachedGql.kt`, `CachedGqlTest.kt`, `SpotifyCacheLifecycleTest.kt`, `LiveCacheTest.kt`; bounded LRU, TTL/SWR, single-flight, failure backoff, session invalidation. Existing home/library screen caches retained, not duplicated. |
| Nav a163288 / b0908b9 / c197801 (including reduced motion) | Pending Step 1 | Must read design docs and both PNGs; rebuild in native Compose with real navigation/player state, Solar Bold assets, flat surfaces. No HTML/CSS port asserted. |
| Playback dcfb50e / 5a2dd1c | Pending Step 2 end-to-end audit | Existing `c4a4392` atomic metadata/order and fork pause/volume guards have passing supplemental JVM regressions; still must read integrity findings and audit remaining transport/service paths. |
| Uncommitted integrity leftovers (app.js, CustomUI.kt, AndroidAuto.kt, AndroidTracker.kt, PowerSave.kt + working-tree.diff) | Pending Step 2 review | Extraction includes unreviewed production leftovers; not silently treated as landed fixes or N/A. |
| Earlier UI/data 736227e | Pending detailed accounting | Compare extracted wiring against native API/UI; no mock data. |
| Perf 5b7aeb4 / PERF.md | Pending Step 3 | Step 0 supplies catalog/search cache; existing startup, screen caches, tokens, lyrics and palette prefetch documented in `LESSONS.md`. Must read findings before final dispositions. |
| Visual ad7e581 / 6842c73 | Pending detailed accounting | AMOLED / no gradients checks pass; native visual and icon parity still unaudited. Existing font is Figtree; do not invent Satoshi. |
| FEATURE-GAP.md / SPOTIFY-SPEED.md | Pending Step 3 action-table accounting | Research is not an instruction to implement every speculative feature; account for each relevant action with mechanism evidence or native/WebView N/A reason. |
| UTF-8 44d46ff + check-kotlin-string-size.py | Pending Step 4 | Do the analogous source/compiled string check after Steps 0–3, in order. |

Final completion must replace every pending row and expand the ledger to cover all extracted changes and the 20-row speed table. No completed port, visual acceptance, device latency, or playback continuity claim is made by this checkpoint.
