# Cadence fork changes — findings and handoff

Reviewed 2026-10-21 UTC. Repository: `~/workspace/myspoti`; branch **main**; fork remote `fork` → `zshadowultra/myspoti`. **Nothing pushed, dispatched remotely, or published.** Parent agent reviews and pushes.

## Evidence / verification boundary

Read `README.md`, `base/api-spotify/README.md`, and `.github/workflows/android.yml` first. The checkout was upstream `f25ddfe` (2.0.0-beta.9), plus existing fork commit `d4793bd`; a read-only `git ls-remote origin refs/heads/main` confirmed upstream was still `f25ddfe`.

Problem sources reviewed in the requested order:
1. `gh issue list --repo kota-zo/myspoti --state open --limit 100` with bodies, labels and comments: **23 open**; closed list sorted by update: **11 closed**. No issue labels in the returned set. Comments on #13 explicitly describe recurrence after the beta.3 fix; #10 asks for confirmation, not proof of a new beta.9 fault. #47's comment corrects its original login allegation.
2. `docs/audit-2026-10-18.md`, then `docs/api/feature-inventory.md` in full.
3. `CHANGELOG.md` and `rg -n 'TODO|FIXME|HACK' base/`: **no matches**. Recurring themes: stuck playback/reloads, Connect ownership/volume, background playback, login, downloads and repeated API/session work.

**No Java in this sandbox. No Gradle build, Kotlin unit-test execution, device test, or signed-in Spotify reproduction was possible. CI is the compilation/test check.** Code faults below have deterministic regression tests, not a claim that all related upstream symptoms are solved.

Local checks completed:
- `python3 -B tools/check_fork_changes.py`: generated vectors match source conversion, all app main/debug resource XML parses, all adaptive layer references exist, brand string sweep, no runtime UI gradient declarations.
- The same check executes the **actual workflow merge block** against temporary local Git repositories: preserves Cadence's commit, imports an upstream fix, creates a two-parent merge, pushes to a local bare fork, no-ops on the next run, and fails loudly on conflict without changing that fork. No real remote writes.
- PyYAML parsed all workflows; `bash -n` passed all workflow shell blocks. This is not actionlint or a GitHub-hosted workflow run.
- Tree-sitter Kotlin parsed the changed production player, Gradle script, new tests and most modified UI files. It rejects two **unchanged upstream syntax regions** in `BetaLogButton.kt` and `ContextScreen.kt`; comparing against `d4793bd` gives the same errors (line shift only). This parser is not the Kotlin compiler.
- Raster comparison at 1008px: original compound SVG vs converted subpaths differs only in Cairo edge antialiasing (1,495 edge pixels, max alpha difference 17/255; only 9 pixels differ at a 127 alpha threshold). Coordinates are preserved exactly, not redrawn. Preview generated at `/tmp/cadence-icon-preview.png`; attempted image Read, but this session's model cannot see images. Visual/device confirmation remains required.
- `git diff --check` clean; `git stash list` empty. Pre-existing untracked `fork-brief.md`, `lessons-brief.md`, `lyrics-brief.md` left untouched.

## 1. Upstream auto-sync — `834b7c8`

**Changed files**
- `.github/workflows/sync-upstream.yml` (new)
- `.github/workflows/android.yml`

Every six hours (`0 */6 * * *`) or manually: check out fork `main` with full history and `secrets.GITHUB_TOKEN`, fetch `https://github.com/kota-zo/myspoti.git`, test ancestry, and merge `upstream/main` with `--no-ff --no-edit`. Normal push only. An already-contained upstream is a successful no-op. Conflicts log a GitHub error and status then fail; no auto-resolution, reset, force push, or upstream overwrite. Concurrent sync runs are serialized; a concurrent human push fails safely rather than overwriting it.

**Important correction to requested build assumption:** GitHub suppresses other workflow triggers for pushes made with `GITHUB_TOKEN`. Merely adding a push would NOT automatically build an APK. Therefore the sync job also has `actions: write` and explicitly invokes `gh workflow run android.yml --repo "$GITHUB_REPOSITORY" --ref main` after a successful update. Android now has `workflow_dispatch`; human pushes still use the existing push trigger. No-op syncs do not dispatch builds.

**Needs deployment verification:** Actions must be enabled in the fork; scheduled workflows run from its default branch (main). Branch protection/rulesets can reject bot pushes. Repository/org policy can restrict token write permission. A merge containing protected workflow changes may be rejected by GitHub token restrictions; fail safely and review manually. Upstream conflicts necessarily need manual resolution. Schedule is polling, not instantaneous; GitHub may delay runs or disable inactive public-repo schedules. This syncs main only, not tags; the resulting build is the existing debug APK artifact, not an automatically signed release/update. After parent push: run Sync upstream manually once, inspect ancestry/result, and run Android manually if the first sync is a no-op.

## 2. Rebrand to Cadence — `01645a0`

**Changed files**
- `base/app/build.gradle.kts`: `Cadence`, `Cadence Beta`, and debug `Cadence dev` labels.
- `base/app/src/debug/res/values/strings.xml`.
- `base/app/src/main/res/values{,-de,-el,-es,-fr,-it,-pt}/myspoti_strings.xml`.
- `base/app/src/main/res/values{,-de,-el,-es,-fr,-in,-it,-pt,-ru,-tr}/strings.xml`.
- Under `base/app/src/main/java/com/music/spotui/`: `App.kt`, `data/BetaLogs.kt`, `ui/components/{AboutSheet,BetaLogButton,ShareCard}.kt`, `ui/screens/{SettingsScreen,WelcomeScreen}.kt`.
- `base/host-android/src/main/kotlin/app/myspoti/host/android/Logbook.kt` (export heading only).

Welcome, About, diagnostics/clipboard titles, update banner, notifications and translated product copy now say Cadence. About explicitly credits **“Based on Myspoti by kota-zo · GPL-3.0”** and links the original project. Existing donation links still support the original author, not an invented fork funding destination.

**Deliberately kept Myspoti**
- About attribution text, original-project row and URL.
- `README.md`, historical `CHANGELOG.md`, the upstream audit/API developer documentation, `LICENSE`, copyright headers, `THIRD_PARTY_NOTICES.md`, source-derivation comments. These are upstream history/attribution, not rewritten to claim fork authorship. Resource XML comments saying “Myspoti's own texts” are non-visible provenance.
- Packages/namespaces, `Theme.Myspoti`, classes/bridge names, resource file names, logcat tags and their filtering/test fixtures, preferences, storage/key aliases, MediaSession internal tag, user-agent and API identifiers.
- URLs, update repository/environment/signing plumbing, expected `myspoti-<version>.apk` asset names and CI artifact naming. Release CI still supplies the repository it builds; nothing redirected to an unrelated updater.
- **applicationId unchanged** (`app.myspoti`, `app.myspoti.stable`, `.dev` debug suffix). Changing it is not trivially safe: installation/update continuity, providers, intents and private data depend on it. A fork signed with a different key cannot update an upstream-signed installation despite the same ID; do not uninstall/delete user data automatically.

**Needs verification:** install CI APK, inspect launcher label, onboarding, About, update permission text and diagnostics in supported locales. New literal provenance credit/row is intentionally factual and English; it has not been localized.

## 3. Genuine Spotify launcher mark — `adbbd5e`

**Changed files**
- `base/app/src/main/res/drawable/ic_launcher_background.xml`.
- `base/app/src/main/res/drawable/ic_launcher_foreground{,_blue,_orange,_pink,_purple}.xml`.
- `base/app/src/main/res/drawable/ic_launcher_monochrome.xml` and `splash_icon.xml` comment.
- `docs/assets/spotify-logo-without-text.svg`, `docs/assets/README.md` (source/licensing/conversion notes).
- `tools/generate_launcher_icon.py` (reproducible offline conversion).

Genuine artwork downloaded from Wikimedia Commons `File:Spotify_logo_without_text.svg` (Spotify mark, not a hand-drawn approximation). Unmodified source checked in. Curve data preserved; only initial subpath moves converted to absolute coordinates. Requested classic **#1DB954 circle / black arcs**, no gradients. Commons metadata says public domain/simple logo; **trademark restrictions still apply** and no endorsement is implied.

Full-bleed green adaptive background; foreground includes the source circle as well as all three arcs, so it also renders correctly when used alone in About/onboarding. Source 168-unit mark uniformly scaled to 66dp and centred in the 108dp adaptive canvas. Platform masks supply the outer silhouette; circles, squares and squircles retain the arcs. The monochrome variant is the original three arcs in opaque white alpha for platform tinting. Dormant accent icons also use the same green/black artwork rather than recoloured Spotify marks.

All six existing `mipmap-anydpi-v26` definitions already reference these layers and remain valid; no legacy bitmap launcher assets ship. minSdk is 29, so no pre-adaptive fallback is necessary.

**Needs verification:** inspect CI APK on a launcher with round/squircle/square masks and Android 13+ themed icons, plus splash/About. Trademark/legal/product-confusion risk of using Spotify's official mark remains an owner decision.

## 4. Reported-problem fixes

### High-confidence fault A: recovery overwrites deliberate mute / remote volume — `0576d0f`

Related reports: [upstream #43](https://github.com/kota-zo/myspoti/issues/43), [#54](https://github.com/kota-zo/myspoti/issues/54) and recurring remote/background complaints #40/#57. **Fixes a demonstrated code fault; does not close the entire Connect reports.**

**Reproduction from code:** page-ready reads `volume()==0.0` then calls `setVolume(... ?: 1.0)`. A user-muted/remote device at zero is automatically unmuted to 100%. Periodic `guardVolume` has the same server-volume recovery even when `media.active == false`. This is distinct from an element left quiet by a local fade, and Connect volume commands can be refused on free accounts.

**Fix:** automatic repair touches only `media.volume {restore:true}`, only for positively confirmed local ownership, only below the local player's intended level; never sends `volume.set`. Unknown ownership, deliberate zero volume, and active ad mutes are not repair permission. Existing ad mute handling and user-controlled volume setter remain.

**Files:**
- `base/api-spotify/src/main/kotlin/app/myspoti/spotify/SpotifyPlayer.kt`
- `base/api-spotify/src/test/kotlin/app/myspoti/spotify/VolumeRecoveryTest.kt` (remote/unknown/no-element/local mute/lost fade/healthy element cases).

### High-confidence fault B: late refusal restarts playback after pause — `a6f9b18`

Related [upstream #13](https://github.com/kota-zo/myspoti/issues/13) recurrence (lost pause control), [#55](https://github.com/kota-zo/myspoti/issues/55) logs: `player.pause` returns a Connect 402 and emits `refused`. **Fixes unwanted retry, not server-side COMMAND_FAILED/refusal itself.**

**Reproduction from code:** `ControlRelay` posts `refused` for any `player/command` 402/403, including pause/seek. Within five seconds of a track-selected `play`, `SpotifyPlayer.onRefused` interprets that event as the earlier play's failure and sends another `player.play` of its context. No `pausedByUser` check existed. Sequence: play context+track → pause → refusal; context starts again despite the pause.

**Fix:** return from refusal handler when user has paused. Normal play-refused fallback is unchanged.

**Files:**
- Same `SpotifyPlayer.kt`.
- `base/api-spotify/src/test/kotlin/app/myspoti/spotify/RefusedPauseTest.kt` (pause→refusal sends no play; unpaused refusal still retries context).

### Already addressed upstream — do NOT re-fix

- Closed #2 search filters, #3 downloads, #4 player dismiss gesture, #5 radio, #6 new releases, #7 Account row, #9 widget/podcasts, #11 Italian, #12 Bluetooth disconnect pause. #39 is a lyrics question, not a reproduced fault.
- #10 download saturation watchdog coordination already in `OfflineDownloader`; needs a current repro before another change.
- #16 ascending/descending sort already in `TrackOrder` and playlist API tests.
- #36 podcast blank pages and #48 background autoplay/resume guards are addressed by beta.9; any recurrence needs current device evidence (video podcasts remain separate).
- #41 Android Auto cold-start browsing and liked-song support already addressed in beta.9 (`PlaybackService` waits for ready).
- #46 GrapheneOS exec startup recovery already in beta.7; not reapplying the older proposed patch.
- #47 login=false was a misread of login-page-required (confirmed by author's correction); beta.9 logs actual session status. Duplicate ready/library contains and MediaSession updates already fixed by audit; artist caches/shared liked loading already in beta.7/current `Api.ArtistCache` / `LikedCache.loadOnce`.
- #45 cache-clear workaround is not a diagnosis. #42 keeping a playing media service after task removal is deliberate (`onTaskRemoved` only stops when not playing); changing that is a playback policy change, not a proven bug.

### Needs live verification — deferred issues and exact capture

For all captures: use a current **CI-built Cadence APK**, record version/device/Android/WebView/account country and free/Premium plan; export app diagnostics (logcat tag remains `Myspoti`), timestamps and screen recording. **Redact tokens, cookies, grants and personal data before sharing.** No account/session parameters guessed.

| Source | Exact evidence required |
|---|---|
| #13/#40/#43/#54/#57 Connect/background, crossfade, short pause | Two Connect devices (phone + desktop): capture device list/isLocal/isActive, `player.state`, `media.state` (local element time, level, own, playerVolume, active), bridge commands/results and relay status during transfer out/back, 3 song transitions, deliberate remote mute, local pause, app switch and screen off. Repeat crossfade off/on. Include background audio continuity timestamps and Perfetto trace if a 1–2s pause persists. Prove both new guards do not regress local fade/ad recovery. |
| #53/#55 country/free-account refusals | Failing Brazilian/new-account session on reported Xiaomi (and control device if available). In the official Web Player capture a selected-track play and shuffle/context play, device registration, exact sanitized 402/403 error bodies and account country/plan. Then repeat in Cadence. Server `COMMAND_FAILED` is not evidence of a client implementation fix by itself. |
| #52 login, #46 recurrence | Force-stop→launch with GrapheneOS secure spawning on/off (if #46 recurs); record load/onPageStarted/ready timeline. For login record direct email, Google and Facebook redirect chain and signed-in/anonymous token classification after returning from email/browser. Never capture password/token values. |
| #10 + audit 25/80 offline failures | Download the same playlist while streaming: request IDs/ISRCs, resolver candidate/verifier results, retry/status codes, Wi-Fi/mobile state, redacted signed-session expiry/renewal events and per-file header/size validation. Identify failed tracks and whether failures are catalogue mismatch, verification, network, renewal or codec. Do not blanket retry/disable verification. |
| #36/#48 podcast page/background/video | Same show/episode IDs in stock/Web Player and Cadence; capture `queryShowMetadataV2`, `queryPodcastEpisodes`, `getEpisodeOrChapter` responses, episode media type, state/media.time before/after screen-off. Video requires the real video manifest/media capability shape; do not invent a video download path. |
| #41 Android Auto recurrence | Cold process start from car/head unit only, MediaBrowser root/children requests and completion/ready timestamps, liked playlist content and play callback; no prior phone launch. |
| #50 playlist removal recurrence | Owned/editable playlist with duplicate tracks and an episode: capture `fetchPlaylistContents` row uids and exact `removeFromPlaylist` variables/response, subsequent list read, permission state and undo. Current UI/API already supports removal; empty report lacks a reproducible new fault. |
| #47 remaining device identity/library 5xx/lyrics/system warnings | Two cold starts without data wipe: sanitized registration and device list IDs/names, localStorage persistence. For library/lyrics capture transient 5xx sequences and UI retained liked state. For AppOps/finalizer capture caller stack/Perfetto attribution rather than adding manifest tags blindly. Glide warning alone is not a demonstrated user fault. |
| Audit §5 library recomposition work | Perfetto/Compose recomposition counters plus allocations for a large library while track progress updates, switching sort/query/grid. Library assembly still runs inside composition, but no runtime measurement here justifies a state-model rewrite. Artist first-load full discography may still cost many calls; capture request counts before redesigning laziness/caches. |
| #49 status bar | Full screenshot/insets dump on reported Android device in gesture/3-button modes, system bar/edge-to-edge settings and current layout bounds. No status-bar API changes without device proof. |
| #42 task-swipe battery | Explicit desired policy (stop local only vs remote too), background duration and battery/Perfetto trace; foreground media service continuing playback is currently intentional. |

#8 TV, #44/#56 caching and #51 bulk playlist merge are **feature requests**, not surgical bug fixes. TV needs focus/remote/device QA; Spotify DRM streaming is owned by the hidden Web Player, not safe to replace with an invented cache/decryption path. No speculative implementations.

### Feature inventory capture backlog (not presumed current bugs)

The inventory predates some current implementations (e.g. public/collaborative now use base permissions, smart shuffle/cover upload/followers already have code). Its “needs capture” labels are not permission to reimplement them. For each item capture the **official Web Player action's request/service arguments, sanitized response and read-back** on a suitable account:
- Smart shuffle: ordinary→smart→off in a playlist, queue/state before/after, plan restrictions.
- Artist split discography/playlists/featuring/discovered-on/about/concerts/related videos: open each corresponding tab/modal and save the relevant named `queryArtist*`, `ArtistConcerts`/`concertFeed`/`concert`/`venue` operation shapes and pagination. Album merch: `queryAlbumMerch`; now-playing extras: `npvV2TrackSupplementalSections`/`queryNpvArtist` responses.
- Recents tracks: `recents` raw response distinct from verified context groups. Library text search: `libraryV3.textFilter` arguments/results. Drag custom library order: rootlist ops request and persisted order after reload.
- Playlist permission/cover/hide/delete: own a temporary playlist; change visibility/collaboration (current `getBasePermission/setBasePermission`), upload/remove cover, hide a row, delete, and capture mutation plus metadata/rootlist read-back. Do not change someone else's playlist.
- Client playlist sort: duplicate rows/date/duration order and actual playback sequence after ascending/descending; current client implementation already tested upstream at unit level.
- Save episode and artist ban: `CollectionPlatformAPI.add/remove` with `listenlater`/`artistban`, contains/read-back and stock-client state; toggle back after test.
- Followers/following, comments/reactions, country hubs: open official panels, capture endpoint/op, variables, response and pagination. Concert location: `saveLocation/deleteLocation/concertLocationsByLatLon`; promo code: `requestPromoCode` on a legitimately eligible account only (no guessing eligibility).
- Connect transfer: second real device required. Jam start/join/leave/end/remove/volume/queue-only: eligible Premium host + invitee, `SocialConnectAPI` arguments, response/error and session read-back. Free Jam `RESTRICTED`, private session `isSupported:false` and missing Web Player friend feed are known limits, not client bugs to bypass.

## Standing rule: no gradients — `d7445de`

Upstream already shipped gradients; none remain in runtime Kotlin/XML declarations. Flat colours replace only background brushes/shader, not navigation/playback/state logic. Existing cover art/media are not altered. Bottom-bar option now selects pure black vs flat dark grey, with accurate text in all seven product-copy locales.

**Changed files:** under `base/app/src/main/java/com/music/spotui/`:
- `ui/components/{MusicVideo,SavedInSheet,ShareCard}.kt`
- `ui/navigation/MainBottomNavigation.kt`
- `ui/screens/{ArtistScreen,ContextScreen,DownloadsScreen,EpisodeScreen,HomeScreen,LocalFilesScreen,PlayerScreen,PodcastPlayerScreen,RecentlyPlayedScreen,ShowScreen,WelcomeScreen}.kt`
- `base/app/src/main/res/values{,-de,-el,-es,-fr,-it,-pt}/myspoti_strings.xml`.

**Risk:** flat scrims/tints are unreviewed on device for contrast over bright art/Canvas/video; parent should inspect these screens. Future upstream merges may reintroduce gradients without a textual merge conflict; rerun `tools/check_fork_changes.py` during review. Broader docs/screenshots still describe historical upstream gradients accurately; not rewritten.

## Final handoff

Commits on main (not pushed):
- `834b7c8` auto-sync + explicit Android dispatch.
- `01645a0` Cadence labels/copy + upstream attribution.
- `adbbd5e` genuine Spotify launcher artwork.
- `0576d0f` local-only volume recovery regression fix.
- `d7445de` flat UI rule.
- `a6f9b18` no play retry after user pause.
- The final handoff commit adds this report and `tools/check_fork_changes.py`.

Parent: review diff → push `main` to **fork** (not origin) → inspect Android CI tests/debug artifact → manual Sync upstream check → device/capture checks above. No live issues are marked remotely closed, and no unverified symptom is claimed solved.
