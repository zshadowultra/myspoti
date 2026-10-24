#!/usr/bin/env python3
"""Convert the preserved Solar Bold paths to Android vectors, without redrawing."""
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
NAMES = {
    "alt-arrow-down-bold": "close", "menu-dots-bold": "menu",
    "maximize-bold": "expand", "share-bold": "share",
    "translation-bold": "translate", "play-bold": "play",
    "pause-bold": "pause", "check-circle-bold": "check",
}

def generate():
    source = json.loads((ROOT / "docs/assets/lyrics-solar-icons.json").read_text())
    for name, suffix in NAMES.items():
        svg = ET.fromstring("<svg>" + source["icons"][name]["body"] + "</svg>")
        paths = []
        for node in svg.iter():
            if node.tag == "path":
                assert set(node.attrib) <= {"d", "fill", "fill-rule", "clip-rule"}, node.attrib
                fill_type = "evenOdd" if node.attrib.get("fill-rule") == "evenodd" else "nonZero"
                paths.append(f'    <path android:fillColor="#FFFFFFFF" android:fillType="{fill_type}" android:pathData="{node.attrib["d"]}" />')
            else:
                assert node.tag in {"svg", "g"} and set(node.attrib) <= {"fill"}, node.attrib
        content = ('<?xml version="1.0" encoding="utf-8"?>\n'
                   f'<!-- Solar {name}, 480 Design, CC BY 4.0; original paths preserved. -->\n'
                   '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
                   '    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n'
                   + "\n".join(paths) + "\n</vector>\n")
        (ROOT / f"base/app/src/main/res/drawable/ic_lyrics_{suffix}.xml").write_text(content)

if __name__ == "__main__":
    generate()
