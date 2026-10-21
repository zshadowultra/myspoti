#!/usr/bin/env python3
"""Offline checks for fork workflows and launcher paths (no Java/Spotify account).

Run: python3 -B tools/check_fork_changes.py
"""
import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"


def check_icon():
    spec = importlib.util.spec_from_file_location("launcher", ROOT / "tools/generate_launcher_icon.py")
    launcher = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(launcher)
    paths = launcher.artwork_paths()
    expected = launcher.vector(paths, ["#1DB954", "#000000", "#000000", "#000000"])
    for suffix in ("", "_blue", "_purple", "_pink", "_orange"):
        assert (launcher.RES / f"ic_launcher_foreground{suffix}.xml").read_text() == expected
    mono = launcher.vector(paths[1:], ["#FFFFFFFF"] * 3)
    assert (launcher.RES / "ic_launcher_monochrome.xml").read_text() == mono
    background = ET.parse(launcher.RES / "ic_launcher_background.xml").getroot()
    assert background.find("path").attrib[ANDROID + "fillColor"] == "#1DB954"
    for root in (ROOT / "base/app/src/main/res", ROOT / "base/app/src/debug/res"):
        for path in root.rglob("*.xml"):
            ET.parse(path)
    for adaptive in (ROOT / "base/app/src/main/res/mipmap-anydpi-v26").glob("*.xml"):
        icon = ET.parse(adaptive).getroot()
        assert icon.tag == "adaptive-icon"
        for layer in ("background", "foreground", "monochrome"):
            drawable = icon.find(layer).attrib[ANDROID + "drawable"].removeprefix("@drawable/")
            assert (launcher.RES / f"{drawable}.xml").is_file()
    print("PASS: generated icon paths, palette, adaptive references, all resource XML")


def git(repo, *args):
    return subprocess.run(["git", "-C", str(repo), *args], check=True, capture_output=True, text=True).stdout.strip()


def check_merge_script():
    workflow = (ROOT / ".github/workflows/sync-upstream.yml").read_text()
    script = workflow.split("        run: |\n", 1)[1].split("\n      # Explicit", 1)[0]
    script = "\n".join(line.removeprefix("          ") for line in script.splitlines())
    assert "git merge --no-ff --no-edit upstream/main" in script
    assert "reset --hard" not in script and "--force" not in script
    assert "0 */6 * * *" in workflow
    assert "gh workflow run android.yml" in workflow
    assert "workflow_dispatch:" in (ROOT / ".github/workflows/android.yml").read_text()
    with tempfile.TemporaryDirectory(prefix="cadence-merge-") as temp:
        temp = Path(temp)
        upstream, fork = temp / "upstream.git", temp / "fork.git"
        git(temp, "init", "--bare", "--initial-branch=main", str(upstream))
        git(temp, "init", "--bare", "--initial-branch=main", str(fork))
        seed = temp / "seed"
        git(temp, "init", "--initial-branch=main", str(seed))
        git(seed, "config", "user.name", "Test")
        git(seed, "config", "user.email", "test@example.invalid")
        (seed / "app.txt").write_text("Myspoti\n")
        git(seed, "add", "app.txt")
        git(seed, "commit", "-m", "base")
        git(seed, "remote", "add", "origin", str(upstream))
        git(seed, "push", "origin", "main")
        git(seed, "push", str(fork), "main")
        work = temp / "work"
        git(temp, "clone", str(fork), str(work))
        git(work, "config", "user.name", "Test")
        git(work, "config", "user.email", "test@example.invalid")
        (work / "app.txt").write_text("Cadence\n")
        git(work, "commit", "-am", "fork rebrand")
        fork_commit = git(work, "rev-parse", "HEAD")
        git(work, "push", "origin", "main")
        (seed / "fix.txt").write_text("upstream fix\n")
        git(seed, "add", "fix.txt")
        git(seed, "commit", "-m", "upstream fix")
        git(seed, "push", "origin", "main")
        upstream_commit = git(seed, "rev-parse", "HEAD")
        # Execute the actual workflow block, substituting only the remote URL.
        local_script = script.replace("https://github.com/kota-zo/myspoti.git", str(upstream))
        output = temp / "output"
        def run():
            env = dict(os.environ, GITHUB_OUTPUT=str(output))
            return subprocess.run(["bash", "-e", "-o", "pipefail", "-c", local_script], cwd=work, env=env, capture_output=True, text=True)
        result = run()
        assert result.returncode == 0, result.stderr
        merge = git(work, "rev-parse", "HEAD")
        assert git(work, "show", "-s", "--format=%P", "HEAD").split() == [fork_commit, upstream_commit]
        assert (work / "app.txt").read_text() == "Cadence\n"
        assert (work / "fix.txt").is_file()
        assert git(fork, "rev-parse", "main") == merge
        assert output.read_text().strip() == "updated=true"
        # A fresh checkout, like the next scheduled run: no-op writes no output.
        git(work, "remote", "remove", "upstream")
        output.unlink()
        assert run().returncode == 0
        assert git(work, "rev-parse", "HEAD") == merge and not output.exists()
        # An upstream edit colliding with the rebrand must not update the fork.
        (seed / "app.txt").write_text("New upstream brand\n")
        git(seed, "commit", "-am", "conflicting change")
        git(seed, "push", "origin", "main")
        git(work, "remote", "remove", "upstream")
        result = run()
        assert result.returncode != 0
        assert "::error::Upstream merge failed" in result.stdout
        assert git(fork, "rev-parse", "main") == merge and not output.exists()
    print("PASS: actual merge script preserves fork, creates merge commit, no-ops, fails conflicts without push")


def check_brand_and_flat_ui():
    app = ROOT / "base/app/src"
    for path in app.rglob("*strings.xml"):
        for string in ET.parse(path).getroot().iter("string"):
            assert "Myspoti" not in (string.text or ""), path
    for path in (app / "main").rglob("*"):
        if path.suffix in (".kt", ".xml"):
            assert "gradient" not in path.read_text().lower(), path
    print("PASS: product strings say Cadence; no runtime UI gradient declarations")


if __name__ == "__main__":
    check_icon()
    check_merge_script()
    check_brand_and_flat_ui()
