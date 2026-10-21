# Launcher artwork provenance

- Original artwork: [Spotify logo without text.svg on Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Spotify_logo_without_text.svg).
- Download: https://upload.wikimedia.org/wikipedia/commons/1/19/Spotify_logo_without_text.svg
- The checked-in `spotify-logo-without-text.svg` is the unmodified download. Commons' image metadata classifies it as **Public domain** (simple logo); Spotify's trademark rights still apply. This is not permission to imply affiliation or endorsement.
- `python3 tools/generate_launcher_icon.py` retains the exact SVG curve paths, splits its compound path into the circle and three arcs, and converts only the subpaths' initial relative moves to absolute moves. No artwork is hand-drawn.
- The fork requests the classic **#1DB954** green with **black** arcs, rather than the source's newer #1ED760. The coloured vector includes the green circle so it also renders correctly in About/onboarding; the adaptive background is full-bleed #1DB954 (launcher masks supply the outer silhouette). The 168-unit mark is uniformly scaled to 66dp, centred in the 108dp canvas. Essential artwork remains in the adaptive safe zone.
- The themed icon contains only the original three arcs, opaque white alpha for the platform to tint. Dormant accent icons use the same green/black mark, not recoloured logos. No legacy bitmap mipmaps ship (minSdk 29); all six adaptive mipmap definitions use these vectors.

Cadence is not affiliated with or endorsed by Spotify. Spotify is a trademark of Spotify AB.
