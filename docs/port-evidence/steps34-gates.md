# Steps 3–4 gate receipts (2026-10-24)

The requested RootLibrary.kt / RootLibraryTest.kt / LibraryScreeen.kt leftovers
were already committed on arrival as `bfd7005`. Reviewed the production diff
and four snapshot invalidation/pinning tests: retained, no duplicate commit or
reversion. Unrelated recomposition reuses root assembly; sort/custom revision,
local playlist edits and downloads still invalidate it.

Fresh serial repo gate from `base/`:
`./gradlew -I /tmp/cadence-early-mirror.gradle testDebugUnitTest assembleDebug --no-configuration-cache`
with exact JDK 21, SDK 37r2, wrapper/dependencies unchanged, explicit
`GRADLE_USER_HOME=$HOME/.gradle`, IPv4, refreshed sandbox truststore/mirrors.
`steps34-retained-gate.log` / `.exit`: BUILD SUCCESSFUL in 1m 8s, exit 0.
Tests are up-to-date from the committed outputs; this is the actual repo gate,
not a fresh execution claim. Prior four regression cases are preserved in
`steps34-RootLibraryTest.xml`. `steps34-style.log` / `.exit`: static fork/style
check exit 0. No device/UI/audio timing claim; no lyrics edits; no push.
