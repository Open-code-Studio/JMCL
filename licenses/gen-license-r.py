#!/usr/bin/env python3
"""
Generate a Rez .r resource file with multi-language SLIC (Software License) resources.
These resources are embedded in a DMG's resource fork to show a macOS license dialog
with a language selector popup.

Usage:
    python3 gen-license-r.py output.r

The script reads license text from stdin or from files specified via environment variables:
    LICENSE_EN  — path to English license text file
    LICENSE_ZH  — path to Chinese license text file
"""

import os
import sys
import struct


def pstring(text: str) -> bytes:
    """Return a Pascal string (length-prefixed). Strings longer than 255 bytes
    are truncated to 255 bytes to comply with the pstring format."""
    encoded = text.encode("utf-8")
    if len(encoded) > 255:
        encoded = encoded[:255]
    return struct.pack("B", len(encoded)) + encoded


def escape_rez_string(text: str) -> str:
    """Escape a string for inclusion as a literal in Rez source.
    Rez uses C-style string escaping with \" for quotes."""
    return text.replace("\\", "\\\\").replace('"', '\\"')


def generate_r_file(en_text: str, zh_text: str) -> str:
    """
    Generate the complete Rez .r source file with:
    - TMPL resource (ID 128) defining the SLIC structure
    - SLIC resource (ID 0) for English
    - SLIC resource (ID 1) for Simplified Chinese
    """

    en_text_escaped = escape_rez_string(en_text)
    zh_text_escaped = escape_rez_string(zh_text)

    r_source = """// Auto-generated DMG License Resource File
// Contains multi-language SLIC (Software License) resources for macOS DMG.
// Both 'TMPL' and 'SLIC' resources are generated programmatically.

// ── TMPL template for SLIC resources ──────────────────────────────────
// Defines the binary layout of SLIC resources so that macOS Security Agent
// can parse the language code, language name, and license text.
// The "castlongs" values define fields for:
//   - LANG (pstring): language code, e.g. "en", "zh"
//   - TEXT (pstring): the license text itself
data 'TMPL' (128, "SLIC") {
    castlongs(0, 0, 1, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 4, 0,
              0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 4, 0,
              0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 4);
};

// ── English License ──────────────────────────────────────────────────
data 'SLIC' (0, "English") {
    $"0000 0000 0000 0000"
    $"0000 0000 0000 0000"
    "en" 0
    "English" 0
    $"0000 0000 00"
    """ + '"' + en_text_escaped.replace('\n', '\\\n') + '"' + """
};

// ── Simplified Chinese License ────────────────────────────────────────
data 'SLIC' (1, "\\x7C\\xCC\\xD6\\xCE\\xC4") {
    $"0000 0000 0000 0000"
    $"0000 0000 0000 0000"
    "zh" 0
    "\\x7C\\xCC\\xD6\\xCE\\xC4" 0
    $"0000 0000 00"
    """ + '"' + zh_text_escaped.replace('\n', '\\\n') + '"' + """
};
"""

    return r_source


def main():
    output_path = sys.argv[1] if len(sys.argv) > 1 else "/dev/stdout"

    # Read license texts from files or stdin
    en_path = os.environ.get("LICENSE_EN", "")
    zh_path = os.environ.get("LICENSE_ZH", "")

    if en_path and os.path.isfile(en_path):
        with open(en_path, "r", encoding="utf-8") as f:
            en_text = f.read()
    else:
        en_text = sys.stdin.read()
        zh_text = en_text

    if zh_path and os.path.isfile(zh_path):
        with open(zh_path, "r", encoding="utf-8") as f:
            zh_text = f.read()
    elif not en_path:
        # If both from stdin, assume stdin was the Chinese text
        pass

    r_source = generate_r_file(en_text, zh_text)

    with open(output_path, "w", encoding="utf-8") as f:
        f.write(r_source)

    print(f"Generated: {output_path}", file=sys.stderr)


if __name__ == "__main__":
    main()