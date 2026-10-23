# Cadence / Spotilol parity — work checkpoint

2026-10-23, first supervised port chunk. **Not a completed parity report.** Read `port-brief.md` and the extraction `INDEX.md` in full; work is intentionally in order. Only Step 0's cache slice was implemented/reviewed here. `LESSONS.md` contains mechanisms, request-count evidence, exact test boundaries and the continuation command. No push.

## Gate / continuation

Do not begin Step 1 until the repo's own build/test gate passes. Exact wrapper 9.7.1 has been provisioned locally; required Gradle command is running, output `/tmp/cadence-port/verified-build.log`, exit receipt `/tmp/cadence-port/verified-build.exit`. Supplemental exact Kotlin/JUnit compilation of the JVM modules: **68 passed / 31 signed-in-CDP tests skipped / 0 failed**. Android compilation/device proof is not implied.

## Parity ledger (pending is deliberately not a final disposition)

| Source from extraction INDEX | Disposition | Evidence / remaining work |
|---|---|---|
| Stranded lessons / catalog-search caching | Implemented; required Gradle verification pending | `SpotifyApi.kt`, `gql/CachedGql.kt`, `CachedGqlTest.kt`, `SpotifyCacheLifecycleTest.kt`, `LiveCacheTest.kt`; bounded LRU, TTL/SWR, single-flight, failure backoff, session invalidation; `LESSONS.md` Step 0. Existing home/library screen caches retained, not duplicated. |
| Nav a163288 / b0908b9 / c197801 (including reduced motion) | Pending Step 1 | Must read design docs and both PNGs; rebuild in native Compose with real navigation/player state, Solar Bold assets, flat surfaces. No HTML/CSS port asserted. |
| Playback dcfb50e / 5a2dd1c | Pending Step 2 end-to-end audit | Existing `c4a4392` atomic metadata/order and fork pause/volume guards have passing supplemental JVM regressions; still must read integrity findings and audit remaining transport/service paths. |
| Uncommitted integrity leftovers (app.js, CustomUI.kt, AndroidAuto.kt, AndroidTracker.kt, PowerSave.kt + working-tree.diff) | Pending Step 2 review | Extraction includes unreviewed production leftovers; not silently treated as landed fixes or N/A. |
| Earlier UI/data 736227e | Pending detailed accounting | Compare extracted wiring against native API/UI; no mock data. |
| Perf 5b7aeb4 / PERF.md | Pending Step 3 | Step 0 supplies catalog/search cache; existing startup, screen caches, tokens, lyrics and palette prefetch documented in `LESSONS.md`. Must read findings before final dispositions. |
| Visual ad7e581 / 6842c73 | Pending detailed accounting | AMOLED / no gradients checks pass; native visual and icon parity still unaudited. Existing font is Figtree; do not invent Satoshi. |
| FEATURE-GAP.md / SPOTIFY-SPEED.md | Pending Step 3 action-table accounting | Research is not an instruction to implement every speculative feature; account for each relevant action with mechanism evidence or native/WebView N/A reason. |
| UTF-8 44d46ff + check-kotlin-string-size.py | Pending Step 4 | Do the analogous source/compiled string check after Steps 0–3, in order. |

Final completion must replace every pending row and expand the ledger to cover all extracted changes and the 20-row speed table. No completed port, visual acceptance, device latency, or playback continuity claim is made by this checkpoint.
