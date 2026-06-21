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
package org.Open_code_Studio.jmcl.ui;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.Open_code_Studio.jmcl.util.platform.NativeUtils;
import org.jetbrains.annotations.Nullable;

/// JNA binding for macOS CoreGraphics CGWindowList API.
///
/// Uses {@code CGWindowListCopyWindowInfo} to enumerate all on-screen windows
/// without requiring Accessibility permissions (unlike AppleScript/osascript).
///
/// @see <a href="https://developer.apple.com/documentation/coregraphics/1455137-cgwindowlistcopywindowinfo">CGWindowListCopyWindowInfo</a>
public interface CoreGraphicsAPI extends Library {

    CoreGraphicsAPI INSTANCE = NativeUtils.USE_JNA && com.sun.jna.Platform.isMac()
            ? Native.load("CoreGraphics", CoreGraphicsAPI.class)
            : null;

    /// kCGWindowListOptionAll = 0
    int kCGWindowListOptionAll = 0;
    /// kCGNullWindowID = 0
    int kCGNullWindowID = 0;

    /// CGWindowListCopyWindowInfo(CGWindowListOption option, CGWindowID relativeToWindow)
    /// Returns a CFArray of CFDictionary objects, each describing a window.
    Pointer CGWindowListCopyWindowInfo(int option, int relativeToWindow);

    /// CFArrayGetCount
    long CFArrayGetCount(Pointer array);

    /// CFArrayGetValueAtIndex
    Pointer CFArrayGetValueAtIndex(Pointer array, long index);

    /// CFDictionaryGetValue
    Pointer CFDictionaryGetValue(Pointer dict, Pointer key);

    /// CFNumberGetValue
    boolean CFNumberGetValue(Pointer number, int type, Pointer valuePtr);

    /// CFRelease
    void CFRelease(Pointer obj);

    /// CFStringGetCString
    boolean CFStringGetCString(Pointer string, byte[] buffer, long bufferSize, int encoding);

    /// kCFStringEncodingUTF8 = 0x08000100
    int kCFStringEncodingUTF8 = 0x08000100;

    /// kCFNumberSInt32Type = 3
    int kCFNumberSInt32Type = 3;

    /// CFSTR - create a CFString from a C string (static inline)
    /// We use toll-free bridging: CFStringRef constants are accessible via their address
    static Pointer cfstr(String s) {
        // Create via CFStringCreateWithCString
        // But for known keys we can use predefined CFSTR constants
        // Actually, for the window info dictionary keys, we use JNA Pointer-based lookup
        return null; // replaced by CGWindowProperty
    }

    /// Known CGWindow dictionary keys (as C string pointers for CFDictionaryGetValue)
    static final class CGWindowProperty {
        static String kCGWindowOwnerPID = "kCGWindowOwnerPID";
        static String kCGWindowBounds = "kCGWindowBounds";
        static String kCGWindowLayer = "kCGWindowLayer";
    }
}
