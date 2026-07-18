#!/usr/bin/env python3
"""
JMCL EXE Icon & Version Setter (Cross-Platform, pefile-based)
Replaces the EXE icon and sets version metadata in HMCLauncher.exe stub.
No Resource Hacker or Windows dependency needed.

Usage:
    python3 set_exe_icon.py <input_exe> <icon_file> <version_string>

Output:
    Produces <input_exe_base>_new.exe in the same directory.

Prerequisites:
    pip3 install pefile
"""

import struct
import sys
import os
import io
from typing import Optional, List


# ===========================================================================
# ICO parser
# ===========================================================================

class ICOEntry:
    __slots__ = ("width", "height", "colors", "planes", "bpp", "size", "offset", "data")

    def __init__(self):
        self.width = 0
        self.height = 0
        self.colors = 0
        self.planes = 0
        self.bpp = 0
        self.size = 0
        self.offset = 0
        self.data = b""


def _parse_ico(filepath: str) -> List[ICOEntry]:
    """Parse an .ico file."""
    with open(filepath, "rb") as f:
        data = f.read()
    if data[:4] != b"\x00\x00\x01\x00":
        raise ValueError("Not a valid .ico file")
    count = struct.unpack_from("<H", data, 4)[0]
    entries = []
    for i in range(count):
        off = 6 + i * 16
        w, h, colors, _, planes, bpp, size, img_off = struct.unpack_from(
            "<BBBBHHII", data, off
        )
        e = ICOEntry()
        e.width = w if w != 0 else 256
        e.height = h if h != 0 else 256
        e.colors = colors
        e.planes = planes
        e.bpp = bpp
        e.size = size
        e.offset = img_off
        e.data = data[img_off : img_off + size]
        entries.append(e)
    return entries


def _build_icon_group(entries: List[ICOEntry], icon_ids: List[int]) -> bytes:
    """Build RT_GROUP_ICON data, referencing specific RT_ICON ids."""
    buf = io.BytesIO()
    buf.write(b"\x00\x00")       # reserved
    buf.write(b"\x01\x00")       # type = ICO
    buf.write(struct.pack("<H", len(entries)))
    for i, e in enumerate(entries):
        buf.write(struct.pack("<B", e.width if e.width < 256 else 0))
        buf.write(struct.pack("<B", e.height if e.height < 256 else 0))
        buf.write(struct.pack("<B", e.colors))
        buf.write(b"\x00")       # reserved
        buf.write(struct.pack("<H", e.planes))
        buf.write(struct.pack("<H", e.bpp))
        buf.write(struct.pack("<I", e.size))
        # nID: references RT_ICON id
        nid = icon_ids[i] if i < len(icon_ids) else (i + 1)
        buf.write(struct.pack("<H", nid))
    return buf.getvalue()


# ===========================================================================
# PE resource manipulation (file-offset-level, safe in-place replacement)
# ===========================================================================

def _find_resource_entry(pe, type_id: int):
    """Yield (name_id, lang_entry) tuples for all entries of given type."""
    if not hasattr(pe, "DIRECTORY_ENTRY_RESOURCE"):
        return
    import pefile as pf
    for t in pe.DIRECTORY_ENTRY_RESOURCE.entries:
        if t.id != type_id:
            continue
        for n in t.directory.entries:
            for l in n.directory.entries:
                yield (n.id if n.name is None else n.name, l)


def _resolve_type_id(pe, rt_name: str) -> int:
    """Resolve a resource type name to its numeric ID."""
    import pefile as pf
    return pf.RESOURCE_TYPE[rt_name]


def _rva_to_offset(pe, rva: int) -> int:
    """Convert RVA to file offset."""
    for section in pe.sections:
        start = section.VirtualAddress
        end = start + max(section.Misc_VirtualSize, section.SizeOfRawData)
        if start <= rva < end:
            return rva - start + section.PointerToRawData
    return 0


def _set_bytes_at_offset(data: bytearray, offset: int, new_bytes: bytes) -> int:
    """Replace bytes at file offset. Pads with zeros if shorter, or truncates.
    Returns number of bytes written (original length preserved)."""
    orig_len = len(new_bytes)   # will update caller to adjust
    return orig_len


def replace_icon(pe, ico_path: str) -> None:
    """Replace all icons in the PE. Detects the existing icon group automatically."""
    import pefile as pf

    RT_ICON = _resolve_type_id(pe, "RT_ICON")
    RT_GROUP_ICON = _resolve_type_id(pe, "RT_GROUP_ICON")

    ico_entries = _parse_ico(ico_path)
    if not ico_entries:
        raise ValueError("No icons found in .ico file")

    # ----- Find existing RT_GROUP_ICON and its icon id list -----
    group_name_id: Optional[int] = None
    old_icon_ids: List[int] = []

    for nid, lang_entry in _find_resource_entry(pe, RT_GROUP_ICON):
        group_name_id = nid
        group_rva = lang_entry.data.struct.OffsetToData
        group_size = lang_entry.data.struct.Size
        group_data = pe.get_data(group_rva, group_size)
        # Parse GRPICONDIR
        cnt = struct.unpack_from("<H", group_data, 4)[0]
        for i in range(cnt):
            off = 6 + i * 14
            icon_id = struct.unpack_from("<H", group_data, off + 12)[0]
            old_icon_ids.append(icon_id)

    if group_name_id is None:
        raise RuntimeError("No RT_GROUP_ICON found in PE")

    # ----- Match new icons to old icon IDs -----
    if len(old_icon_ids) < len(ico_entries):
        print(f"  Warning: .ico has {len(ico_entries)} entries but PE only has {len(old_icon_ids)} icon slots.")
        print(f"  Only the first {len(old_icon_ids)} icons will be used.")
        ico_entries = ico_entries[:len(old_icon_ids)]
    elif len(old_icon_ids) > len(ico_entries):
        while len(old_icon_ids) > len(ico_entries):
            old_icon_ids.pop()

    # ----- Replace RT_ICON data -----
    replaced_count = 0
    for nid, lang_entry in _find_resource_entry(pe, RT_ICON):
        if nid not in old_icon_ids:
            continue
        idx = old_icon_ids.index(nid)
        if idx >= len(ico_entries):
            continue

        new_data = ico_entries[idx].data
        rva = lang_entry.data.struct.OffsetToData
        old_size = lang_entry.data.struct.Size
        file_off = _rva_to_offset(pe, rva)

        # In-place replacement: pad or truncate
        section_data = pe.__data__
        if len(new_data) > old_size:
            new_data = new_data[:old_size]
        elif len(new_data) < old_size:
            new_data = new_data + b"\x00" * (old_size - len(new_data))

        pe.__data__ = section_data[:file_off] + new_data + section_data[file_off + len(new_data):]
        lang_entry.data.struct.Size = len(ico_entries[idx].data)  # actual size
        replaced_count += 1

    # ----- Replace RT_GROUP_ICON data -----
    new_group = _build_icon_group(ico_entries, old_icon_ids)
    for nid, lang_entry in _find_resource_entry(pe, RT_GROUP_ICON):
        if nid != group_name_id:
            continue
        rva = lang_entry.data.struct.OffsetToData
        old_size = lang_entry.data.struct.Size
        file_off = _rva_to_offset(pe, rva)

        padded = new_group
        if len(padded) > old_size:
            padded = padded[:old_size]
        elif len(padded) < old_size:
            padded = padded + b"\x00" * (old_size - len(padded))

        pe.__data__ = pe.__data__[:file_off] + padded + pe.__data__[file_off + len(padded):]
        lang_entry.data.struct.Size = len(new_group)
        break

    print(f"  Replaced {replaced_count} RT_ICON entries + RT_GROUP_ICON (id={group_name_id})")


def set_version_info(pe, version_string: str) -> None:
    """Set version strings in RT_VERSION resource."""
    import pefile as pf

    RT_VERSION = _resolve_type_id(pe, "RT_VERSION")

    # Parse version numbers
    clean = version_string.removeprefix("DEV").strip()
    parts = [int(p) if p.isdigit() else 0 for p in clean.split(".")]
    while len(parts) < 4:
        parts.append(0)

    fv_ms = (parts[0] << 16) | parts[1]
    fv_ls = (parts[2] << 16) | parts[3]

    found = False
    for nid, lang_entry in _find_resource_entry(pe, RT_VERSION):
        if nid != 1:
            continue
        rva = lang_entry.data.struct.OffsetToData
        file_off = _rva_to_offset(pe, rva)
        data = bytearray(pe.__data__[file_off : file_off + lang_entry.data.struct.Size])

        # Find VS_FIXEDFILEINFO position (after key "VS_VERSION_INFO\0" + padding)
        key_end = 0
        for i in range(0, len(data) - 2, 2):
            if data[i] == 0 and data[i + 1] == 0:
                key_end = i + 2
                break
        ff_offset = (key_end + 3) & ~3

        if ff_offset + 52 <= len(data):
            struct.pack_into("<I", data, ff_offset + 8, fv_ms)
            struct.pack_into("<I", data, ff_offset + 12, fv_ls)
            struct.pack_into("<I", data, ff_offset + 16, fv_ms)
            struct.pack_into("<I", data, ff_offset + 20, fv_ls)

        # Replace string values in StringFileInfo
        old_str = bytes(data)
        for key_pattern, replacement in (
            (b"FileVersion\x00", version_string),
            (b"ProductVersion\x00", version_string),
        ):
            idx = 0
            while True:
                idx = old_str.find(key_pattern, idx)
                if idx < 0:
                    break
                val_start = idx + len(key_pattern)
                # Align to word boundary
                val_start = (val_start + 3) & ~3 if val_start % 4 != 0 else val_start

                # Find end of wide-string (double null)
                val_end = val_start
                while val_end < len(data) - 1:
                    if data[val_end] == 0 and data[val_end + 1] == 0:
                        break
                    val_end += 1
                val_end = min(val_end, len(data))

                new_val_wide = replacement.encode("utf-16-le")
                old_len = val_end - val_start
                if len(new_val_wide) > old_len:
                    new_val_wide = new_val_wide[:old_len]
                else:
                    new_val_wide = new_val_wide + b"\x00" * (old_len - len(new_val_wide))
                data[val_start:val_end] = new_val_wide
                old_str = bytes(data)
                idx = val_end

        # Write back
        pe.__data__ = pe.__data__[:file_off] + bytes(data) + pe.__data__[file_off + len(data):]
        found = True
        break

    if not found:
        print("  WARNING: No RT_VERSION resource found")
    else:
        print(f"  Version info set to: {version_string}")


# ===========================================================================
# Main
# ===========================================================================

def main() -> None:
    try:
        import pefile as _  # noqa: F401
    except ImportError:
        print("ERROR: pefile not installed. Run: pip3 install pefile", file=sys.stderr)
        sys.exit(1)

    if len(sys.argv) != 4:
        print(f"Usage: {sys.argv[0]} <input_exe> <icon_file> <version_string>", file=sys.stderr)
        print(f"Example: {sys.argv[0]} HMCLauncher.exe icon.ico DEV2026.3.0", file=sys.stderr)
        sys.exit(1)

    input_exe = sys.argv[1]
    icon_file = sys.argv[2]
    version_string = sys.argv[3]

    if not os.path.isfile(input_exe):
        print(f"ERROR: Input EXE not found: {input_exe}", file=sys.stderr)
        sys.exit(1)
    if not os.path.isfile(icon_file):
        print(f"ERROR: Icon file not found: {icon_file}", file=sys.stderr)
        sys.exit(1)

    # Output path
    base = os.path.splitext(os.path.basename(input_exe))[0]
    ext = os.path.splitext(input_exe)[1]
    dir_name = os.path.dirname(os.path.abspath(input_exe))
    output_exe = os.path.join(dir_name, f"{base}_new{ext}")

    # Load PE
    print(f"Loading: {input_exe}")
    import pefile
    pe = pefile.PE(input_exe)

    # Step 1: Replace icon
    print(f"\n[1/2] Replacing icon from: {icon_file}")
    replace_icon(pe, icon_file)

    # Step 2: Set version info
    print(f"\n[2/2] Setting version info: {version_string}")
    set_version_info(pe, version_string)

    # Write output
    print(f"\nWriting: {output_exe}")
    pe.write(output_exe)
    pe.close()

    print(f"\nSUCCESS: {output_exe}")


if __name__ == "__main__":
    main()
