#!/usr/bin/env python3
"""
Generate a DMG background image for JMCL.
Creates a distinctive gradient PNG - visible enough to confirm it's working.
"""
import struct
import zlib
import sys
import os


def create_gradient_png(width, height):
    """Create a vertical gradient from blue-purple to teal."""
    raw_data = bytearray()
    for y in range(height):
        ratio = y / max(height - 1, 1)
        # Blue-purple to teal gradient
        r = int(70 + (40 - 70) * ratio)   # 70→40
        g = int(60 + (120 - 60) * ratio)  # 60→120
        b = int(140 + (180 - 140) * ratio) # 140→180
        raw_data.append(0)  # filter byte
        raw_data.extend(bytes([r, g, b]) * width)

    def chunk(chunk_type, data):
        c = chunk_type + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)

    ihdr = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)
    idat = zlib.compress(bytes(raw_data))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b'')


def main():
    if len(sys.argv) < 2:
        print("Usage: generate_dmg_background.py <output_path>")
        sys.exit(1)

    output_path = sys.argv[1]
    os.makedirs(os.path.dirname(output_path) or '.', exist_ok=True)

    bg = create_gradient_png(660, 400)

    with open(output_path, 'wb') as f:
        f.write(bg)
    print(f"DMG background created: {output_path} ({len(bg)} bytes)")


if __name__ == '__main__':
    main()