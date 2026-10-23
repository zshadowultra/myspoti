# Native playback-integrity port audit — 2026-10-23

Scope: extraction `findings/PLAYBACK-INTEGRITY.md`, committed atomic snapshot /
transport fixes, and `uncommitted/working-tree.diff`. This is a source and repo
regression audit, **not** an authenticated device/audio reproduction.

## 1. Wrong title at a track transition

Already present (`c4a4392`): `player-bridge.js::pushState` maps **one** synchronous
`PlayerAPI.getState()` value, not independently scraped DOM title/art/artist.
`Bridge` passes one JSON event to `Mappers.playerState`; `SpotifyPlayer.onState`
rejects older timestamps through `StateOrder`. `SongPlayer.applyState` maps the
whole item into one `CurrentSongState.updateSongState` publication;
`DockMiniPlayer` reads that immutable `snapshot`. Native notification publication
uses one `NowPlayingInfo` object, and `PlaybackService.loadCover` checks its URL
before applying an asynchronously fetched bitmap. No Spotify DOM scraper exists
in this path. Equal timestamps may legitimately carry pause/restriction changes.

Added `PlaybackSnapshotTest`: send A(100), B(200), late A(150) through the **real
Bridge**; B's URI/title/artist/album/art stay together and the captured A remains
immutable. Existing `SongSnapshotTest` covers the Compose atomic publication.
These two repo test boundaries cover the protocol-to-model and model-to-Compose
slices; they do not assert on-phone frame or notification ordering.

## 2. Ghost pauses / desired-state transport

Ported native retry protection: a monotonically increasing explicit transport
revision invalidates suspended bridge retries of play/pause/resume/next/previous/
queue jumps. Recovery play and resume keep the initiating revision rather than
pretending to be fresh user intent. Refused-play follow-ups check both pause and
revision. Pause clears held skip and reload intent. Cancellation must not be
classified as an ad-held skip (`heldError`). Stale buffering/playing states after
a deliberate pause cannot trigger stall self-advance; paused watcher ticks do
not perform ad/transport recovery.

App-side `SongPlayer.transport` cancels the older job in addition to its existing
serialized mutex/latest-generation check. Pause drops the delayed skip burst,
clears cold-start `resumeWhenReady`, and supersedes episode-resume follow-ups;
those follow-ups now go through the transport queue. Logout cancels that job.
Pause sources are `user`, `system-media`, `autoplay-off`, `sleep-timer` and
`sleep-end-of-track`, logged without adding UI copy. Existing local-player
handoff pauses have explicit log/context but still need the same queue audit.

New tests exercise real bridge transient failure interleavings (play/next/
previous/queue jump → pause, pause → resume), held skip → pause → ready, and a
new resume during delayed page recovery. Existing refusal and volume regressions
are retained. A command already accepted by Spotify cannot be recalled; newer
intent prevents **additional** host retries, not remote server rollback.

## 3. Stutter / self-advance / interception

`MediaProxy.ENABLED == false`: there is no active native full-body CDN type
probe or double-download path to replace with Spotilol's HEAD fix. The dormant
proxy already forwards the request's method and Range headers and streams a
response closed by `ClosingStream`. Do not turn it on without a device/network
reproduction. Active `WebPlayerHost.shouldInterceptRequest` checks known ad URLs,
then falls through for music; it does not fetch a track just to classify it.

Album/playlist native callers register and submit real context/order;
`SongPlayer.playSong` keeps standalone search results context-free. Spotilol's
uncommitted search `albumData.uri` fallback is **not adopted**: an arbitrary
search track is not a user-selected album playback context. The existing queue
and context/order tests remain the mechanism evidence. No new fabricated queue,
blind toggle or ad auto-resume is introduced. Remaining automatic reload/stall
handoff generations and Connect-ownership guards need review before Step 2 can
be called complete; source regressions do not prove latency or audible continuity.

## 4. Background sound / foreground service / focus

Already present, source-audited: WebView uses application context; renderer
priority is `IMPORTANT` with waiver=false. Ordinary `MainActivity.onStop` does
not detach/unload the Spotify engine. Only `onDestroy` detaches to avoid an
activity leak. `PlaybackService` declares `mediaPlayback` foreground type and
permissions, cancels foreground-exit callbacks while playing/buffering and uses
a 60-second pause grace (not an immediate stop between tracks). Task removal
keeps a playing service. Native local audio delegates focus and noisy-device
handling to Media3 (`setAudioAttributes(..., true)` /
`setHandleAudioBecomingNoisy(true)`). Chromium owns streaming audio focus;
adding a second native streaming focus owner would be unjustified.

Spotify's hidden page is parked on `/preferences`, not the custom DOM UI.
Canvas is a separate muted native player: the player-screen lifecycle observer
pauses it below STARTED, and its preloader starts with `playWhenReady=false`.
`MusicVideoController.onBackground` stops the separate clip player and cancels
lookups/sync jobs. No global Spotify `setInterval`/RAF throttle is installed.
Thus Spotilol's PowerSave global-clock removal is already satisfied; its DOM
Canvas selector and activity-owned visibility patch are not copied verbatim.
Task-dismissal/detached lifetime and screen-off renderer visibility remain device
validation risks, not proven audible fixes. A foreground notification does not
itself prove sound is heard.

## Uncommitted extraction dispositions

- `app.js`: context propagation already native via `playSong`; don't render
  errors into artist metadata (native `command`/`transport` logs them instead).
  The extra WebView status overlay is N/A and would violate the no-extra-copy
  rule if transplanted merely for visual parity.
- `CustomUI.kt`: its DOM bridge is N/A; direct native API context transport exists.
- `AndroidAuto.kt`: native `PlaybackService` / `MediaLibrary` replaces DOM media
  browsing; keep standalone search context-free as above, not an album fallback.
- `AndroidTracker.kt`: no document-wide metadata fallback; one PlayerAPI model
  feeds native media session/widget/Compose. Protocol interleaving regression added.
- `PowerSave.kt`: no global Spotify interval replacement; separate native clip /
  Canvas lifecycle is already present. No sweeping mute/loop/cover-fit selection
  is copied into the streaming engine.

Verification receipts and remaining slice are in root `PORT.md`.
