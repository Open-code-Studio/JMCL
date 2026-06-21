// JMCL macOS Window Position Tracker
// Compiles to a tiny binary that calls CGWindowListCopyWindowInfo
// to find the bounds of a window by PID. No permissions required.
//
// Compile: clang -O2 -framework CoreGraphics -framework CoreFoundation -o jmcl_window_tracker jmcl_window_tracker.c

#include <CoreGraphics/CoreGraphics.h>
#include <CoreFoundation/CoreFoundation.h>
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char *argv[]) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <pid>\n", argv[0]);
        return 1;
    }

    long targetPid = atol(argv[1]);
    if (targetPid <= 0) return 1;

    CFArrayRef windowList = CGWindowListCopyWindowInfo(
        kCGWindowListOptionAll, kCGNullWindowID);
    if (!windowList) return 1;

    CFIndex count = CFArrayGetCount(windowList);
    int found = 0;

    for (CFIndex i = 0; i < count; i++) {
        CFDictionaryRef window = (CFDictionaryRef)CFArrayGetValueAtIndex(windowList, i);
        if (!window) continue;

        // Get PID
        CFNumberRef pidRef = CFDictionaryGetValue(window, kCGWindowOwnerPID);
        if (!pidRef) continue;
        long pid = 0;
        CFNumberGetValue(pidRef, kCFNumberSInt32Type, &pid);
        if (pid != targetPid) continue;

        // Skip off-screen windows (layer < 0)
        CFNumberRef layerRef = CFDictionaryGetValue(window, kCGWindowLayer);
        if (layerRef) {
            long layer = 0;
            CFNumberGetValue(layerRef, kCFNumberSInt32Type, &layer);
            if (layer > 1000) continue; // skip menus, tooltips, dock
        }

        // Get bounds
        CFDictionaryRef boundsRef = CFDictionaryGetValue(window, kCGWindowBounds);
        if (!boundsRef) continue;

        double x = 0, y = 0, w = 0, h = 0;
        CFNumberRef nx = CFDictionaryGetValue(boundsRef, CFSTR("X"));
        CFNumberRef ny = CFDictionaryGetValue(boundsRef, CFSTR("Y"));
        CFNumberRef nw = CFDictionaryGetValue(boundsRef, CFSTR("Width"));
        CFNumberRef nh = CFDictionaryGetValue(boundsRef, CFSTR("Height"));
        if (nx && ny && nw) {
            CFNumberGetValue(nx, kCFNumberDoubleType, &x);
            CFNumberGetValue(ny, kCFNumberDoubleType, &y);
            CFNumberGetValue(nw, kCFNumberDoubleType, &w);

            // Prefer main window or the largest visible one
            printf("%.0f,%.0f,%.0f\n", x, y, w);
            found = 1;
            break; // first visible window is usually the game window
        }
    }

    CFRelease(windowList);
    return found ? 0 : 1;
}
