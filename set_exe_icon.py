#!/usr/bin/env python3
"""Replace icon AND version info in a Windows PE executable."""

import sys
import os
import struct
import pefile


def utf16le(s):
    """Encode a string as UTF-16LE with null terminator."""
    return s.encode('utf-16-le') + b'\x00\x00'


def pad4(data):
    """Pad data to 4-byte alignment."""
    pad = (4 - len(data) % 4) % 4
    return data + b'\x00' * pad


def build_string_entry(key, value):
    """Build a String entry (wLength, wValueLength, wType, szKey, padding, Value)."""
    key_enc = utf16le(key)
    key_enc_padded = pad4(key_enc)
    value_enc = value.encode('utf-16-le') + b'\x00\x00'
    wValueLength = len(value)
    entry_data = key_enc_padded + value_enc
    total_len = 6 + len(key_enc_padded) + len(value_enc)
    total_len_padded = total_len + (4 - total_len % 4) % 4
    padding_needed = total_len_padded - total_len
    return struct.pack('<HHH', total_len_padded, wValueLength, 1) + key_enc_padded + value_enc + b'\x00' * padding_needed


def build_string_table(lang_charset, strings):
    """Build a StringTable containing multiple String entries."""
    children_data = b''
    for key, value in strings:
        children_data += build_string_entry(key, value)

    key_enc = utf16le(lang_charset)
    key_enc_padded = pad4(key_enc)
    total_len = 6 + len(key_enc_padded) + len(children_data)
    total_len_padded = total_len + (4 - total_len % 4) % 4
    padding_needed = total_len_padded - total_len
    return struct.pack('<HHH', total_len_padded, 0, 1) + key_enc_padded + children_data + b'\x00' * padding_needed


def build_string_file_info(strings):
    """Build StringFileInfo containing one StringTable."""
    lang_charset = '040804b0'
    table = build_string_table(lang_charset, strings)
    key_enc = utf16le('StringFileInfo')
    key_enc_padded = pad4(key_enc)
    total_len = 6 + len(key_enc_padded) + len(table)
    total_len_padded = total_len + (4 - total_len % 4) % 4
    padding_needed = total_len_padded - total_len
    return struct.pack('<HHH', total_len_padded, 0, 1) + key_enc_padded + table + b'\x00' * padding_needed


def build_var_file_info(translation_bytes):
    """Build VarFileInfo with the given translation bytes."""
    key_enc = utf16le('VarFileInfo')
    key_enc_padded = pad4(key_enc)
    # Var entry
    var_key = utf16le('Translation')
    var_key_padded = pad4(var_key)
    var_data = var_key_padded + translation_bytes
    var_total = 6 + len(var_key_padded) + len(translation_bytes)
    var_total_padded = var_total + (4 - var_total % 4) % 4
    var_padding = var_total_padded - var_total
    var_entry = struct.pack('<HHH', var_total_padded, len(translation_bytes) // 2, 0) + var_data + b'\x00' * var_padding

    key_enc_vfi = utf16le('VarFileInfo')
    key_enc_vfi_padded = pad4(key_enc_vfi)
    total_len = 6 + len(key_enc_vfi_padded) + len(var_entry)
    total_len_padded = total_len + (4 - total_len % 4) % 4
    padding_needed = total_len_padded - total_len
    return struct.pack('<HHH', total_len_padded, 0, 1) + key_enc_vfi_padded + var_entry + b'\x00' * padding_needed


def build_version_info(fixed_file_info, strings, translation_bytes):
    """Build complete VS_VERSIONINFO resource."""
    vffi_key = utf16le('VS_VERSION_INFO')
    vffi_key_padded = pad4(vffi_key)
    sfi = build_string_file_info(strings)
    vfi = build_var_file_info(translation_bytes)
    children = sfi + vfi
    value_length = len(fixed_file_info)
    total_len = 6 + len(vffi_key_padded) + value_length + len(children)
    total_len_padded = total_len + (4 - total_len % 4) % 4
    padding_needed = total_len_padded - total_len
    return struct.pack('<HHH', total_len_padded, value_length // 2, 0) + vffi_key_padded + fixed_file_info + children + b'\x00' * padding_needed


def replace_exe_icon_and_info(exe_path, ico_path, version):
    """Replace icon and version info in the EXE."""

    with open(exe_path, "rb") as f:
        exe_bytes = bytearray(f.read())

    pe = pefile.PE(exe_path, fast_load=True)
    pe.full_load()

    # ---- Part 1: Replace Icons ----
    print("=== Replacing Icons ===")
    with open(ico_path, "rb") as f:
        ico_data = f.read()

    ico_count = struct.unpack_from("<H", ico_data, 4)[0]
    print(f"ICO contains {ico_count} images")

    ico_entries = []
    for i in range(ico_count):
        entry_offset = 6 + i * 16
        width = ico_data[entry_offset]
        height = ico_data[entry_offset + 1]
        bpp = struct.unpack_from("<H", ico_data, entry_offset + 6)[0]
        size = struct.unpack_from("<I", ico_data, entry_offset + 8)[0]
        offset = struct.unpack_from("<I", ico_data, entry_offset + 12)[0]
        image_data = ico_data[offset:offset + size]
        ico_entries.append({
            "width": width, "height": height, "bpp": bpp, "size": size, "data": image_data
        })
        print(f"  Image {i}: {width}x{height}, {bpp}bpp, {size} bytes")

    for rsrc in pe.DIRECTORY_ENTRY_RESOURCE.entries:
        if rsrc.id == pefile.RESOURCE_TYPE["RT_GROUP_ICON"]:
            for sub in rsrc.directory.entries:
                for lang in sub.directory.entries:
                    raw_rva = lang.data.struct.OffsetToData
                    raw_size = lang.data.struct.Size
                    raw_offset = pe.get_offset_from_rva(raw_rva)

                    group_data = struct.pack("<HHH", 0, 1, ico_count)
                    for i, entry in enumerate(ico_entries):
                        group_data += struct.pack(
                            "<BBBBHHIH",
                            entry["width"], entry["height"],
                            0, 0, 1, entry["bpp"],
                            entry["size"], i + 1
                        )

                    if len(group_data) > raw_size:
                        print(f"ERROR: Group data ({len(group_data)}B) > slot ({raw_size}B)")
                        return False
                    padded = group_data + b'\x00' * (raw_size - len(group_data))
                    exe_bytes[raw_offset:raw_offset + raw_size] = padded
                    print(f"RT_GROUP_ICON: offset=0x{raw_offset:x}, size={raw_size}")

        elif rsrc.id == pefile.RESOURCE_TYPE["RT_ICON"]:
            icon_idx = 0
            for sub in rsrc.directory.entries:
                for lang in sub.directory.entries:
                    raw_rva = lang.data.struct.OffsetToData
                    raw_size = lang.data.struct.Size
                    raw_offset = pe.get_offset_from_rva(raw_rva)

                    if icon_idx < len(ico_entries):
                        img_data = ico_entries[icon_idx]["data"]
                        if len(img_data) <= raw_size:
                            padded = img_data + b'\x00' * (raw_size - len(img_data))
                            exe_bytes[raw_offset:raw_offset + raw_size] = padded
                            print(f"RT_ICON[{icon_idx}]: offset=0x{raw_offset:x}, size={raw_size}, data={len(img_data)}B")
                        else:
                            print(f"ERROR: Icon {icon_idx} data ({len(img_data)}B) > slot ({raw_size}B)")
                            return False
                    icon_idx += 1

    # ---- Part 2: Replace Version Info ----
    print("=== Replacing Version Info ===")
    new_strings = [
        ("CompanyName", "OCS"),
        ("FileDescription", "JMCL - Java Minecraft Launcher"),
        ("FileVersion", version),
        ("LegalCopyright", "Copyright (C) 2013-2026 Open Code Studio"),
        ("OriginalFilename", "JMCL.exe"),
        ("ProductName", "JMCL Launcher"),
        ("ProductVersion", version),
    ]

    for rsrc in pe.DIRECTORY_ENTRY_RESOURCE.entries:
        if rsrc.id == pefile.RESOURCE_TYPE["RT_VERSION"]:
            for sub in rsrc.directory.entries:
                for lang in sub.directory.entries:
                    raw_rva = lang.data.struct.OffsetToData
                    raw_size = lang.data.struct.Size
                    raw_offset = pe.get_offset_from_rva(raw_rva)

                    old_data = pe.get_data(raw_rva, raw_size)

                    # The structure starts with:
                    #   wLength(2) + wValueLength(2) + wType(2) = 6 bytes header
                    #   szKey = "VS_VERSION_INFO\0" in UTF-16LE = 34 bytes
                    #   Total so far: 40 bytes (which is 4-byte aligned)
                    #   VS_FIXEDFILEINFO: starts at offset 40, 52 bytes

                    if raw_size < 92:
                        print(f"ERROR: RT_VERSION too small: {raw_size}")
                        return False

                    fixed_info = old_data[40:40 + 52]
                    if len(fixed_info) != 52:
                        print(f"ERROR: VS_FIXEDFILEINFO size mismatch: got {len(fixed_info)} bytes")
                        return False

                    # Find Translation value bytes (last 4-16 bytes of the VarFileInfo section)
                    # The Var entry's value contains the language/charset pair at the end
                    var_key = b'V\x00a\x00r\x00F\x00i\x00l\x00e\x00I\x00n\x00f\x00o\x00\x00\x00'
                    var_off = old_data.find(var_key)
                    if var_off >= 0:
                        search_off = var_off + len(var_key)
                        search_off += (4 - search_off % 4) % 4
                        remaining = old_data[search_off:]
                        trans_idx = remaining.rfind(struct.pack('<HH', 0x04b0, 0x0408))
                        if trans_idx >= 0:
                            trans_data = remaining[trans_idx:trans_idx + 4]
                        else:
                            trans_data = struct.pack('<HH', 0x0408, 0x04b0)
                    else:
                        trans_data = struct.pack('<HH', 0x0408, 0x04b0)

                    new_version_data = build_version_info(fixed_info, new_strings, trans_data)

                    # Pad or truncate to fit the existing slot
                    if len(new_version_data) > raw_size:
                        print(f"ERROR: Version data ({len(new_version_data)}B) > slot ({raw_size}B)")
                        return False

                    padded = new_version_data + b'\x00' * (raw_size - len(new_version_data))
                    exe_bytes[raw_offset:raw_offset + raw_size] = padded
                    print(f"RT_VERSION: offset=0x{raw_offset:x}, slot={raw_size}, new_data={len(new_version_data)}B")

    # ---- Write output ----
    output_path = exe_path.replace(".exe", "_new.exe")
    with open(output_path, "wb") as f:
        f.write(exe_bytes)
    print(f"Written to: {output_path}")

    pe.close()
    return True


if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: set_exe_icon.py <exe_path> <ico_path> <version>")
        sys.exit(1)

    exe_path = sys.argv[1]
    ico_path = sys.argv[2]
    version = sys.argv[3]

    if not os.path.exists(exe_path):
        print(f"EXE not found: {exe_path}")
        sys.exit(1)
    if not os.path.exists(ico_path):
        print(f"ICO not found: {ico_path}")
        sys.exit(1)

    success = replace_exe_icon_and_info(exe_path, ico_path, version)
    sys.exit(0 if success else 1)