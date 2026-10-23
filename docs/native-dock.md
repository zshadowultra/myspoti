# Native dock — Step 1 native adaptation

2026-10-23. Native Compose adaptation, not injected HTML/CSS.

## Reference / implementation

Read the extraction's `NAV-BAR.md`, `nav-design-brief.md`, `NAV-ICONS.md`, and opened both `nav-ref-*.png` files. They are JPEGs; this model cannot receive their image attachments. **No screenshot or visual acceptance claim.** The extraction's measured pixels take precedence over its approximate prose: common surface `#313131`, selected `#646464`, AMOLED `#000000`.

`MainBottomNavigation.kt`: expanded player over a Home / Library / Settings pill, detached Search circle. At 412dp, the 15dp side margins, 9dp gap and 64dp Search give a 309dp tab pill (81% of the row; the reference's measured 309/382 rather than estimated 78%). Upper player is 54dp, vertical gap 6dp, bottom clearance 12dp + navigation inset. `BottomBarsSpace.kt` already reserves 160dp + inset; unchanged. No animated sizes or cover-derived dock tint. `AppBackground` is always black, including when the energy policy would otherwise choose grey.

`DockMiniPlayer.kt`: one `CurrentSongState.snapshot` read supplies art/title/artist/playing. Text ellipsizes in the remaining width. 32dp rounded art, Figtree via existing `AppSans` (Satoshi is not a native asset here). Separate 48dp play/pause and next targets use existing `SongPlayer` methods; body opens `PlayerSheet`. Ad state is explicit. Existing full-player palette/Canvas prefetch is retained. Removed mini-player-only decorative progress, like and device controls remain available in the full player; the dock follows the reference instead of inventing extra UI. No optimistic guessed metadata is published by its buttons.

Real navigation uses existing routes and retained tab state/reselect behavior. Settings is the app's existing native Settings screen, not a duplicated WebView account dialog. The dock is absent under Player, Queue, SpotiFLAC verification and the full-player overlay. Home's existing profile/avatar entry is not a top-right settings icon and is not removed.

`ic_dock_{home,library,search,settings,play,pause,next}.xml` retain the extraction's Solar Bold SVG path data, including even-odd fills; converted to Android vectors at a 24-unit viewport, tinted by the caller. Icon Set / Solar, MIT (source recorded in extraction via Iconify). No web font/CDN or Material stand-in glyphs.

## Scroll / motion

Collapsed geometry at 412dp matches the extraction's measurements: Home 54dp, 14dp gap, flexible 246dp compact player (art and play/pause only), 14dp gap, Search 54dp. Expanded and collapsed layers are permanently allocated; `graphicsLayer` alone animates alpha, translation and scale with 280ms `(0.6,0,0.1,1)`. No animated measurement, size, padding, color or content clearance. Hidden-layer controls are disabled and its semantics cleared. Compose's animation runtime honors the Android animator duration scale, including zero; no independent hand-rolled clock bypasses reduced motion. Device validation remains open.

`App.kt` owns `DockScrollController` in the composition's coroutine scope. Home/Search vertical lists, both Library list/grid modes, and the Settings scroll column use `dockScroll`; sheets, horizontal shelves and full Player are not connected. Nested scroll is observation-only (`Offset.Zero`), never an interceptor. Consumed vertical movement is accumulated and one frame makes one top/offset decision. Top is within 4dp at item zero; collapse threshold is 8dp downward with reversal/jitter hysteresis. A cancel/rearm settle expands 120ms after the last event, including zero-net coalesced bursts. Route changes and opening/closing Player cancel stale frame/timer work and reset to expanded. Palette/Canvas prefetch belongs to the expanded player only, preventing duplicate API work from the two allocated layers.

`DockScrollPolicyTest` covers threshold, jitter, settle rearm, top/navigation reset and zero-net activity. `DockScrollControllerTest` uses the same production controller with an injected clock/frame boundary: 120 events → one frame / one top read, zero/horizontal events → no work, route reset cancels an old view read, top immediately expands. These are repo unit tests, not device frame-time measurements.

## Still pending

- Device screenshots of expanded/collapsed states, TalkBack/focus/touch geometry (including focus transfer from outgoing controls), system motion-scale-zero behavior and background playback continuity. Source dimensions and a Gradle pass do not prove these.

## Gate

Results are checkpointed in root `PORT.md`; Android dependency repair logs are not a passing UI test by themselves.
