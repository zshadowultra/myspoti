# Sister-project lessons and speed pass

Session 2026-10-21. Branch **main**. No push, publication, account mutation or device access. Pre-existing untracked briefs left untouched. GPL attribution retained.

## Evidence boundary

This sandbox has Node/Python but no Java, Android SDK or signed-in test browser. Executed JavaScript mechanism tests are evidence of the shipped bridge's logic, NOT Android timing or account playback proof. Kotlin tests are added to the existing CI unit-test homes; compilation/execution remains pending CI. Device/account-only claims are explicitly pending below. No speculative performance percentages.

## Part A — Canvas default off (fixed)

- Fault: `EnergySettings.kt:52` read absent `canvas` as true, and `EnergyPolicy.kt:7,19,39` initial/resolved state also enabled it before preferences loaded. All four now default false. Explicit saved opt-in remains true; no destructive preference migration.
- Existing Settings switch: `SettingsScreen.kt:135–140`. Fetch/preload gate: `AppComponents.kt:169–175`; player fetch/render gate: `PlayerScreen.kt:247–258`. Disabled policy passes an empty track ID, so no Canvas API fetch/preload occurs. Capability remains intact.
- Official music videos use `videoMaxHeight` / background lookup policy, not `allowCanvas`; unchanged. Policy-matrix regression covers all saver/AMOLED/Canvas combinations and proves Canvas changes no other field.
- Pending: Kotlin CI; installed fresh/default-off, existing opt-in, toggle off/on during playback, official-video playback with Canvas off.

## Audit in progress

1. Bridge blocking (found + fixed): `PlayerChannel.kt:33–34` is void, not a synchronous network-return method; `ControlRelay.send` already uses OkHttp enqueue (default bounded dispatcher), and `reply` posts request-ID responses to its captured originating view. However `send` lazily reads `Settings.Global/Secure` for device registration (`ControlRelay.kt:104–113`) on the message callback thread. `dispatchMessage` now submits parsing/header construction/registration rewriting/Settings access to one shared worker with a 128-entry bounded queue; rejection returns fetch failure rather than running IO on the caller. No per-rebuild pool leak. Legacy player-state message callback is only `tryEmit`, retained. JS `fetch` is a real Promise completed by native callback, not await around sync HTTP. No new synchronous result channel. **Device-only proof pending**: callback duration and tap-to-command under slow Settings/network, saturation, renderer rebuild. No timing improvement asserted without Android.
2. CSP (injection audited clean; silent-failure hardening fixed): `ControlRelay.install/injectNow` use privileged document-start/evaluateJavascript, and `Bridge.inject` uses host evaluation. No script elements or inline DOM handlers. A separate post-injection evaluation now asserts the detectable `Myspoti.version` heartbeat and sends/logs `bridge bootstrap missing` even when the preceding source has a syntax error; existing 60s service-boot timeout and `Bridge.ping` cover services/frozen page separately. `WebPlayerHost.evaluate` no longer silently swallows dispatch exceptions. Console errors already reach WebChromeClient diagnostics. **Device-only proof pending**: restrictive Spotify CSP + older WebView fallback, renderer failure. The assertion is a diagnostic, not an automatic reload loop.
3. DOM scanning (found + fixed): `player-bridge.js:43–60` eagerly materialized all descendants before inspecting React's root on **every** 250ms boot attempt. Now inspect #main first; fallback descendant-root snapshots are cached and dirtied by one scoped boot-only childList observer. Mutation bursts coalesce in the existing 250ms retry; observer and cache are released at ready/timeout. No body scan, recurring whole-document poll, or runtime DOM state sampling. `media.mute`'s `querySelectorAll('audio, video')` is a command-only recovery path, not a per-tick scan. **Executed mechanism count** (same actual old/new script, 22 boot attempts): root path **22 → 1** descendant queries; nested fallback with 100-mutation burst **22 → 2**. Initial test expected 0 but correctly measured 1 before the provider mounted; corrected assertion, not measurement. `bridge-tests/lesson_mechanisms.cjs` runs in Android CI. Real Spotify React shape/Android CPU timing **device-only proof pending**; no elapsed-time claim.
4. Artwork: app uses Glide, NOT Coil. `Api.cover/big` select 300/640; audit direct image-array consumers pending.
5. Latency: animation / optimistic transport audit pending.
6. Verification: checkpoint and CI tests will accompany changes; device-only list stays explicit.
7. Atomic state: bridge maps whole track synchronously, but poll/event application has no age guard; UI uses separate mutable metadata fields. Fix/test pending.
C. Startup/cache/prefetch/history audit pending.
