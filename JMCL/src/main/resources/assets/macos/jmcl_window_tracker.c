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
    double bestX = 0, bestY = 0, bestW = 0, bestH = 0;
    double bestScore = -1;
    int found = 0;

    for (CFIndex i = 0; i < count; i++) {
        CFDictionaryRef window = (CFDictionaryRef)CFArrayGetValueAtIndex(windowList, i);
        if (!window) continue;

        CFNumberRef pidRef = CFDictionaryGetValue(window, kCGWindowOwnerPID);
        if (!pidRef) continue;
        long pid = 0;
        CFNumberGetValue(pidRef, kCFNumberSInt32Type, &pid);
        if (pid != targetPid) continue;

        long layer = 0;
        CFNumberRef layerRef = CFDictionaryGetValue(window, kCGWindowLayer);
        if (layerRef) CFNumberGetValue(layerRef, kCFNumberSInt32Type, &layer);
        if (layer > 1000) continue;

        CFDictionaryRef boundsRef = CFDictionaryGetValue(window, kCGWindowBounds);
        if (!boundsRef) continue;

        double x = 0, y = 0, w = 0, h = 0;
        CFNumberRef nx = CFDictionaryGetValue(boundsRef, CFSTR("X"));
        CFNumberRef ny = CFDictionaryGetValue(boundsRef, CFSTR("Y"));
        CFNumberRef nw = CFDictionaryGetValue(boundsRef, CFSTR("Width"));
        CFNumberRef nh = CFDictionaryGetValue(boundsRef, CFSTR("Height"));
        if (nx && ny && nw && nh) {
            CFNumberGetValue(nx, kCFNumberDoubleType, &x);
            CFNumberGetValue(ny, kCFNumberDoubleType, &y);
            CFNumberGetValue(nw, kCFNumberDoubleType, &w);
            CFNumberGetValue(nh, kCFNumberDoubleType, &h);

            // Score: prefer layer-0 windows with large area (main game window)
            double area = w * h;
            double score = area;
            if (layer == 0) score += 10000000;  // heavy bias for normal windows
            if (score > bestScore) {
                bestScore = score;
                bestX = x; bestY = y; bestW = w; bestH = h;
                found = 1;
            }
        }
    }

    if (found) {
        printf("%.0f,%.0f,%.0f,%.0f\n", bestX, bestY, bestW, bestH);
    }

    CFRelease(windowList);
    return found ? 0 : 1;
}
