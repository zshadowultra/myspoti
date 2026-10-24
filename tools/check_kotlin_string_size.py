#!/usr/bin/env python3
"""Audit Kotlin literals and (with --compiled) project class-file UTF8 constants.

JVM constants use modified UTF-8, not ordinary UTF-8. The 60,000-byte safety
budget catches literals before the JVM's 65,535-byte limit. Template expressions
are counted conservatively as source, not evaluated. Compiled mode also catches
constant-folded concatenations; run it after the repo Gradle build.
"""
import argparse
import pathlib
import struct
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
LIMIT = 60_000
ESCAPES = {"t": "\t", "b": "\b", "n": "\n", "r": "\r", "'": "'", '"': '"', "\\": "\\", "$": "$"}


def quoted(source, start):
    raw = source.startswith('"""', start)
    quote = '"""' if raw else source[start]
    index = start + len(quote)
    value = []
    while index < len(source):
        if source.startswith(quote, index):
            end = index + len(quote)
            if raw:
                # Kotlin uses the last three quotes in a closing run; preceding
                # quotes belong to the raw string (common in embedded regex/JSON).
                while end < len(source) and source[end] == '"':
                    end += 1
                value.append('"' * (end - index - 3))
            return end, "".join(value)
        if quote != "`" and source.startswith("${", index):
            end = expression_end(source, index + 2)
            value.append(source[index:end])
            index = end
        elif not raw and quote != "`" and source[index] == "\\":
            index += 1
            escape = source[index]
            index += 1
            if escape == "u":
                value.append(chr(int(source[index:index + 4], 16)))
                index += 4
            else:
                value.append(ESCAPES[escape])
        else:
            value.append(source[index])
            index += 1
    raise ValueError(f"Unclosed quoted literal at offset {start}")


def comment_end(source, index):
    if source.startswith("//", index):
        end = source.find("\n", index + 2)
        return len(source) if end < 0 else end + 1
    depth = 1
    index += 2
    while depth and index < len(source):
        if source.startswith("/*", index):
            depth += 1
            index += 2
        elif source.startswith("*/", index):
            depth -= 1
            index += 2
        else:
            index += 1
    if depth:
        raise ValueError("Unclosed block comment")
    return index


def expression_end(source, index):
    depth = 1
    while index < len(source):
        if source.startswith(("//", "/*"), index):
            index = comment_end(source, index)
        elif source[index] in ('"', "'", "`"):
            index, _ = quoted(source, index)
        else:
            if source[index] == "{":
                depth += 1
            elif source[index] == "}":
                depth -= 1
                if depth == 0:
                    return index + 1
            index += 1
    raise ValueError("Unclosed template expression")


def string_literals(source):
    """Yield decoded strings; correctly skip nested comments and templates."""
    index = 0
    while index < len(source):
        if source.startswith(("//", "/*"), index):
            index = comment_end(source, index)
        elif source[index] in ('"', "'", "`"):
            start = index
            index, value = quoted(source, index)
            if source[start] == '"':
                yield start, value
        else:
            index += 1


def byte_sizes(value):
    normalized = value.encode("utf-16-le", errors="surrogatepass").decode("utf-16-le", errors="surrogatepass")
    utf8 = len(normalized.encode("utf-8", errors="surrogatepass"))
    modified = 0
    for char in value:
        code = ord(char)
        if code == 0:
            modified += 2
        elif code < 0x80:
            modified += 1
        elif code < 0x800:
            modified += 2
        elif code <= 0xFFFF:
            modified += 3
        else:
            modified += 6
    return utf8, modified


def class_utf8_sizes(data):
    """Read the actual CONSTANT_Utf8 byte lengths, including folded strings."""
    if data[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("Not a class file")
    count = struct.unpack_from(">H", data, 8)[0]
    offset, index = 10, 1
    sizes = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4,
             11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}
    while index < count:
        tag = data[offset]
        offset += 1
        if tag == 1:
            length = struct.unpack_from(">H", data, offset)[0]
            yield index, length
            offset += 2 + length
        else:
            offset += sizes[tag]
            if tag in (5, 6):
                index += 1
        if offset > len(data):
            raise ValueError("Truncated constant pool")
        index += 1


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--compiled", action="store_true", help="also inspect built project .class files; fail if absent")
    args = parser.parse_args()
    largest, failures = [], []
    count = 0
    paths = sorted(p for p in (ROOT / "base").rglob("*.kt") if "build" not in p.parts and ".gradle" not in p.parts)
    for path in paths:
        try:
            source = path.read_text(encoding="utf-8")
            for offset, value in string_literals(source):
                utf8, modified = byte_sizes(value)
                location = f"{path.relative_to(ROOT)}:{source.count(chr(10), 0, offset) + 1}"
                count += 1
                largest.append((max(utf8, modified), location))
                if max(utf8, modified) > LIMIT:
                    failures.append(f"{location}: UTF-8={utf8}, modified UTF-8={modified} exceeds {LIMIT}")
        except (ValueError, IndexError, KeyError) as error:
            failures.append(f"{path.relative_to(ROOT)}: {error}")
    for size, location in sorted(largest, reverse=True)[:5]:
        print(f"{location}: max UTF-8/modified UTF-8={size}")
    print(f"Source: {count} literals in {len(paths)} Kotlin files")
    if args.compiled:
        classes = sorted(p for p in (ROOT / "base").rglob("*.class") if "build" in p.parts)
        jars = sorted((ROOT / "base").glob("*/build/libs/*.jar"))
        units = [(str(path.relative_to(ROOT)), path.read_bytes()) for path in classes]
        for path in jars:
            with zipfile.ZipFile(path) as archive:
                units.extend((f"{path.relative_to(ROOT)}!{name}", archive.read(name))
                             for name in archive.namelist() if name.endswith(".class"))
        constants = []
        for location, data in units:
            try:
                for index, size in class_utf8_sizes(data):
                    constants.append((size, f"{location}:constant#{index}"))
                    if size > LIMIT:
                        failures.append(f"{location}:constant#{index}={size} exceeds {LIMIT}")
            except (ValueError, IndexError, KeyError, struct.error) as error:
                failures.append(f"{location}: {error}")
        if not units:
            failures.append("No compiled classes; run the repo Gradle gate first")
        for size, location in sorted(constants, reverse=True)[:3]:
            print(f"{location}: modified UTF-8={size}")
        print(f"Compiled: {len(constants)} UTF8 constants in {len(units)} project classes ({len(jars)} jars)")
    if failures:
        print("\n".join(failures), file=sys.stderr)
        return 1
    print(f"PASS: all checked constants <= {LIMIT} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
