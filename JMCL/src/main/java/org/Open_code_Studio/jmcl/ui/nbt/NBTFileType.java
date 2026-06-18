/*
 * JMCL
 * Copyright (C) 2026 OCS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.Open_code_Studio.jmcl.ui.nbt;

import org.glavo.nbt.NBTElement;
import org.glavo.nbt.chunk.ChunkRegion;
import org.glavo.nbt.io.NBTCodec;
import org.glavo.nbt.tag.Tag;
import org.Open_code_Studio.jmcl.util.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

/// @author Glavo
public enum NBTFileType {
    COMPRESSED("dat", "dat_old") {
        @Override
        public Tag read(Path file) throws IOException {
            return NBTCodec.of().readTag(file);
        }
    },
    ANVIL("mca") {
        @Override
        public NBTElement read(Path file) throws IOException {
            return NBTCodec.of().readRegion(file);
        }
    },
    REGION("mcr") {
        @Override
        public ChunkRegion read(Path file) throws IOException {
            return NBTCodec.of().readRegion(file);
        }
    };

    static final NBTFileType[] types = values();

    public static boolean isNBTFileByExtension(Path file) {
        return NBTFileType.ofFile(file) != null;
    }

    public static @Nullable NBTFileType ofFile(Path file) {
        String ext = FileUtils.getExtension(file);
        for (NBTFileType type : types) {
            for (String extension : type.extensions) {
                if (extension.equals(ext))
                    return type;
            }
        }

        return null;
    }

    private final String[] extensions;

    NBTFileType(String... extensions) {
        this.extensions = extensions;
    }

    public abstract NBTElement read(Path file) throws IOException;

    /// Writes the NBT element back to the specified file path.
    ///
    /// @param file    the target file path.
    /// @param element the NBT element to write.
    public void write(Path file, NBTElement element) throws IOException {
        switch (this) {
            case COMPRESSED -> {
                if (element instanceof Tag tag) {
                    writeCompressed(file, tag);
                } else {
                    throw new IOException("Cannot write " + element.getClass().getSimpleName() + " as compressed NBT");
                }
            }
            case ANVIL, REGION -> {
                if (element instanceof ChunkRegion region) {
                    NBTCodec.of().writeRegion(file, region);
                } else {
                    throw new IOException("Cannot write " + element.getClass().getSimpleName() + " as region NBT");
                }
            }
        }
    }

    /// Writes a Tag to a GZIP-compressed NBT file, saved safely via atomic rename.
    private void writeCompressed(Path file, Tag tag) throws IOException {
        FileUtils.saveSafely(file, os -> {
            try (OutputStream gos = new GZIPOutputStream(os)) {
                NBTCodec.of().writeTag(gos, tag);
            }
        });
    }

}
