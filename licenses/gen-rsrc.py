#!/usr/bin/env python3
"""
Generate a compiled macOS resource file (.rsrc) containing SLIC (Software License)
resources for a DMG. This bypasses the need for Apple's Rez compiler and its headers.

The .rsrc file can be embedded into a DMG using:
    hdiutil unflatten input.dmg
    Rez -a license.rsrc -o input.dmg   # OR use cp + xattr to add resource fork
    hdiutil flatten input.dmg
"""

import os
import struct
import sys


def pad16(data: bytes) -> bytes:
    """Pad to 16-byte boundary (resources are typically stored on 4-byte boundaries)"""
    return data


def write_resource_file(path: str, slic_tmpl_data: bytes, slic_entries: list[tuple[int, str, str, str, bytes]]):
    """
    Write a compiled .rsrc file.
    
    Args:
        path: Output file path
        slic_tmpl_data: Binary data for the TMPL resource (ID 128)
        slic_entries: List of (id, lang_code, lang_name, display_name, license_text_bytes)
    """
    
    # ── Build resource data ───────────────────────────────────────────
    # Each resource's data is stored in the resource data area.
    # We'll build the data blobs first, then compute offsets.
    
    data_chunks = []
    data_offsets = []
    
    # TMPL resource data (ID 128)
    data_chunks.append(slic_tmpl_data)
    data_offsets.append(0)
    
    # SLIC resource data (one per language)
    slic_data_list = []
    for res_id, lang_code, lang_name, display_name, lic_text in slic_entries:
        # Build SLIC data: 16 bytes reserved, then lang code, lang name, 5 bytes reserved, license text
        lang_code_b = lang_code.encode('ascii') + b'\x00'
        lang_name_b = lang_name.encode('utf-8') + b'\x00'
        
        sl_data = (
            b'\x00\x00\x00\x00\x00\x00\x00\x00'
            b'\x00\x00\x00\x00\x00\x00\x00\x00'
            + lang_code_b
            + lang_name_b
            + b'\x00\x00\x00\x00\x00'
            + lic_text
            + b'\x00'
        )
        slic_data_list.append(sl_data)
        data_chunks.append(sl_data)
        data_offsets.append(0)  # will compute
    
    # Compute data offsets (all from the start of resource data area)
    current_offset = 0
    for i in range(len(data_chunks)):
        data_offsets[i] = current_offset
        current_offset += len(data_chunks[i])
        # Align to 4 bytes
        while current_offset % 4 != 0:
            current_offset += 1
    
    resource_data_length = current_offset
    
    # ── Build Resource Map ────────────────────────────────────────────
    # Resource map layout (offsets from map_start):
    #   0-15:  system copy area (16 bytes, zeros)
    #   16-17: type list offset (from map start)
    #   18-19: reference list offset (from map start)
    #   20-21: (#types - 1) | 0x8000  (16-bit IDs)
    #   22-23: min resource ID
    #   24-25: max resource ID
    #   26+:   type list entries
    #   then:  reference list entries
    #   then:  name list (empty)
    
    map_header = b'\x00' * 16
    
    # Type list
    num_types = 2  # TMPL + SLIC
    type_list_entries = []
    
    # Type 'TMPL', 1 resource (ID 128), references at offset N
    # Type 'SLIC', 2 resources (IDs 0, 1), references at offset M
    
    # Reference list offsets (relative to reference list start)
    ref_base = 0
    
    # First, compute reference list layout
    # TMPL has 1 resource, SLIC has len(slic_entries) resources
    num_tmpl_refs = 1
    num_slic_refs = len(slic_entries)
    
    # Reference list entries (3 bytes per reference + 4 bytes handle = 7 bytes reserved for modern .rsrc)
    # Actually, reference list entry format:
    #   2 bytes: resource ID
    #   2 bytes: name offset (0xFFFF = no name)
    #   1 byte:  attributes
    #   3 bytes: data offset (from resource data start)
    # Followed by 4 bytes handle (zero, no longer used)
    # But in the old format, the handle is not present. In the "modern" .rsrc format used by Rez,
    # each reference is 12 bytes: 
    #   ID(2) + name_offset(2) + attr(1) + data_offset(3) + handle(4)
    
    # Let me use the format that Rez generates:
    # References: ID(2) + name_ofs(2) + attr(1) + data_ofs(3) = 8 bytes
    
    ref_list = b''
    
    # TMPL ref
    ref_list += struct.pack('>h', 128)      # ID = 128
    ref_list += struct.pack('>H', 0xFFFF)   # No name
    ref_list += struct.pack('>B', 0)        # Attributes
    # 3-byte data offset (big-endian)
    ref_list += struct.pack('>I', data_offsets[0])[1:4]  # Last 3 bytes
    
    # SLIC refs
    for i in range(len(slic_entries)):
        ref_list += struct.pack('>h', slic_entries[i][0])  # ID
        ref_list += struct.pack('>H', 0xFFFF)              # No name
        ref_list += struct.pack('>B', 0)                   # Attributes
        ref_list += struct.pack('>I', data_offsets[i + 1])[1:4]  # data offset
    
    # Now compute offsets
    type_list_offset = 26  # After header (16) + 10 bytes of type list info
    ref_list_offset = type_list_offset + num_types * 8  # Each type entry = 8 bytes
    
    # Type list
    type_list = b''
    
    # TMPL type entry
    type_list += b'TMPL'
    type_list += struct.pack('>H', num_tmpl_refs - 1)  # count-1
    type_list += struct.pack('>H', 0)  # ref offset from ref_list start (first)
    
    # SLIC type entry
    type_list += b'SLIC'
    type_list += struct.pack('>H', num_slic_refs - 1)   # count-1
    type_list += struct.pack('>H', num_tmpl_refs * 8)   # ref offset (after TMPL refs)
    
    # Now compute offsets precisely
    actual_type_list_offset = 26
    actual_ref_list_offset = actual_type_list_offset + len(type_list)
    name_list_offset = actual_ref_list_offset + len(ref_list)
    map_length = name_list_offset  # Name list is empty
    
    # Build the map header
    map_header = struct.pack('>H', actual_type_list_offset)
    map_header += struct.pack('>H', actual_ref_list_offset)
    map_header += struct.pack('>H', (num_types - 1) | 0x8000)  # 16-bit resource IDs
    # Min and max IDs
    all_ids = [128] + [e[0] for e in slic_entries]
    map_header += struct.pack('>H', min(all_ids))
    map_header += struct.pack('>H', max(all_ids))
    # Remaining header bytes (16 total)
    map_header += b'\x00' * 6  # filler to make 16 bytes
    
    # Build complete map
    resource_map = map_header + type_list + ref_list  # No name list
    
    # ── Build .rsrc file header ──────────────────────────────────────
    # File header:
    #   4 bytes: resource data offset (usually at file start: 0)
    #   4 bytes: resource map offset (after data)
    #   4 bytes: resource data length
    #   4 bytes: resource map length
    
    resource_map_offset = resource_data_length
    file_header = struct.pack('>I', 0)  # Data at start
    file_header += struct.pack('>I', resource_map_offset)
    file_header += struct.pack('>I', resource_data_length)
    file_header += struct.pack('>I', len(resource_map))
    
    # ── Write file ───────────────────────────────────────────────────
    with open(path, 'wb') as f:
        f.write(file_header)
        f.write(resource_data_length * b'\x00')  # placeholder for data
        f.write(resource_map)
    
    # Now write data at the correct offset
    with open(path, 'r+b') as f:
        f.write(file_header)
        for i, chunk in enumerate(data_chunks):
            f.seek(16 + data_offsets[i])  # 16 is file header size
            f.write(chunk)
    
    print(f"Written: {path} ({os.path.getsize(path)} bytes)", file=sys.stderr)
    print(f"  Resources: TMPL(128) + SLIC({[e[0] for e in slic_entries]})", file=sys.stderr)


def make_tmpl_data() -> bytes:
    """
    Generate TMPL resource data for SLIC type.
    This tells the Security Agent how to parse SLIC resources.
    The format is an array of type descriptors.
    
    Using the working byte sequence from create-dmg project's Rez castlongs:
        0,0,1,0,0,0,0,0, 0,0,0,0,0,0,4,0,
        0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,4,0, 0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,4
    
    This is a byte-level template: each long (4 bytes) describes one field.
    The fields with value 4 are string-type fields, the rest are padding/long fields.
    """
    words = [
        0, 0, 1, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 4, 0,
        0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 4, 0,
        0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 4,
    ]
    return struct.pack('>' + 'I' * len(words), *words)


def main():
    licenses_dir = os.path.dirname(os.path.abspath(__file__))
    output_path = os.path.join(licenses_dir, "license.rsrc")
    
    # Read license texts
    en_path = os.path.join(licenses_dir, "license_en.txt")
    zh_path = os.path.join(licenses_dir, "license_zh.txt")
    
    with open(en_path, "r", encoding="utf-8") as f:
        en_text = f.read()
    with open(zh_path, "r", encoding="utf-8") as f:
        zh_text = f.read()
    
    en_text_bytes = en_text.encode("utf-8")
    zh_text_bytes = zh_text.encode("utf-8")
    
    tmpl_data = make_tmpl_data()
    
    slic_entries = [
        (0, "en", "English", "English", en_text_bytes),
        (1, "zh", "简体中文", "\\x7C\\xCC\\xD6\\xCE\\xC4", zh_text_bytes),
    ]
    
    write_resource_file(output_path, tmpl_data, slic_entries)
    
    # Generate companion Rez .r file for reference (not used for compilation)
    out_path_r = os.path.join(licenses_dir, "license.r")
    print(f"  Skip Rez .r generation (using binary .rsrc instead)", file=sys.stderr)


if __name__ == "__main__":
    main()