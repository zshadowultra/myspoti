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

1. Bridge blocking: HTTP already uses OkHttp enqueue and request-ID replies to the captured WebView. One void `@JavascriptInterface` fallback; no String-return network method. Found lazy device-name Settings IO in relay request preparation on the message callback thread; fix pending.
2. CSP: privileged `evaluateJavascript` / document-start injection, no injected script tag. Ready timeout exists; syntax/blocked injection lacks a native assertion; fix pending.
3. Observers: no MutationObserver/interval full-DOM scan. Boot does `host.querySelectorAll('*')` every 250ms until services resolve; finding pending fix.
4. Artwork: app uses Glide, NOT Coil. `Api.cover/big` select 300/640; audit direct image-array consumers pending.
5. Latency: animation / optimistic transport audit pending.
6. Verification: checkpoint and CI tests will accompany changes; device-only list stays explicit.
7. Atomic state: bridge maps whole track synchronously, but poll/event application has no age guard; UI uses separate mutable metadata fields. Fix/test pending.
C. Startup/cache/prefetch/history audit pending.
