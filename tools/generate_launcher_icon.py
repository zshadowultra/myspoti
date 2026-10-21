#!/usr/bin/env python3
"""Convert the checked-in Wikimedia Spotify mark to Android launcher vectors.

No redrawing: retain each SVG subpath, changing only the colour and placement.
Run from any directory: python3 tools/generate_launcher_icon.py
"""
from decimal import Decimal
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs/assets/spotify-logo-without-text.svg"
RES = ROOT / "base/app/src/main/res/drawable"
ANDROID = "http://schemas.android.com/apk/res/android"


def artwork_paths():
    svg = ET.parse(SOURCE).getroot()
    assert svg.attrib["viewBox"] == "0 0 168 168"
    compound = svg.find("{http://www.w3.org/2000/svg}path").attrib["d"]
    # Each closed relative subpath starts from the preceding subpath's start.
    # Make only that first move absolute; all original curve coordinates stay intact.
    starts = [Decimal(0), Decimal(0)]
    paths = []
    for subpath in compound.split("z"):
        if not subpath.strip():
            continue
        match = re.fullmatch(r"m(-?\d+(?:\.\d+)?)[\s,]*(-?\d+(?:\.\d+)?)(.*)", subpath)
        assert match, subpath
        starts = [starts[i] + Decimal(match.group(i + 1)) for i in range(2)]
        paths.append(f"M{starts[0]},{starts[1]}{match.group(3)}z")
    assert len(paths) == 4  # circle, then three sound-wave arcs
    return paths


def vector(paths, colours):
    # 66dp diameter inside the 108dp adaptive canvas; no essential artwork clips
    # under circular, squircle or square masks. Green background is full bleed.
    elements = "\n".join(f'        <path android:fillColor="{colour}" android:pathData="{p}" />' for p, colour in zip(paths, colours))
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Generated from Wikimedia's Spotify logo; see docs/assets/README.md. -->
<vector xmlns:android="{ANDROID}"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <group android:scaleX="0.392857142857" android:scaleY="0.392857142857"
        android:translateX="21" android:translateY="21">
{elements}
    </group>
</vector>
'''


def main():
    paths = artwork_paths()
    foreground = vector(paths, ["#1DB954", "#000000", "#000000", "#000000"])
    for suffix in ("", "_blue", "_purple", "_pink", "_orange"):
        (RES / f"ic_launcher_foreground{suffix}.xml").write_text(foreground)
    (RES / "ic_launcher_monochrome.xml").write_text(vector(paths[1:], ["#FFFFFFFF"] * 3))
    (RES / "ic_launcher_background.xml").write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="{ANDROID}"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#1DB954" android:pathData="M0,0h108v108h-108z" />
</vector>
''')


if __name__ == "__main__":
    main()
