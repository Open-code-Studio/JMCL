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

import com.sun.jna.Pointer;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.Open_code_Studio.jmcl.util.platform.macos.ObjectiveCRuntime;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;

public final class MacOSNativeUtils {

    private static final Pointer nsApp = init();

    private static @Nullable Pointer init() {
        if (ObjectiveCRuntime.INSTANCE == null) {
            return null;
        }

        try {
            var objc = ObjectiveCRuntime.INSTANCE;

            Pointer nsApplication = objc.objc_getClass("NSApplication");
            if (!isNull(nsApplication)) {
                Pointer sharedSel = objc.sel_registerName("sharedApplication");
                if (!isNull(sharedSel))
                    return objc.objc_msgSend(nsApplication, sharedSel);
            }
        } catch (Throwable e) {
            LOG.warning("Failed to initialize macOS appearance support", e);
        }

        return null;
    }

    public static boolean isSupported() {
        return nsApp != null;
    }

    private static boolean isNull(Pointer pointer) {
        return pointer == null || Pointer.nativeValue(pointer) == 0;
    }

    public static void setAppearance(boolean dark) {
        setAppearance(dark, false);
    }

    public static void setAppearance(boolean dark, boolean highContrast) {
        if (nsApp == null) return;

        try {
            var objc = ObjectiveCRuntime.INSTANCE;

            Pointer nsAppearance = objc.objc_getClass("NSAppearance");
            if (isNull(nsAppearance))
                return;

            Pointer namedSel = objc.sel_registerName("appearanceNamed:");
            Pointer nsString = objc.objc_getClass("NSString");
            if (isNull(nsString)) return;

            Pointer sel = objc.sel_registerName("stringWithUTF8String:");

            String appearanceName;
            if (highContrast) {
                appearanceName = dark ? "NSAppearanceNameAccessibilityHighContrastDarkAqua" : "NSAppearanceNameAccessibilityHighContrastAqua";
            } else {
                appearanceName = dark ? "NSAppearanceNameDarkAqua" : "NSAppearanceNameAqua";
            }

            Pointer appearanceNamePtr = objc.objc_msgSend(nsString, sel, appearanceName);
            if (isNull(appearanceNamePtr)) return;

            Pointer appearance = objc.objc_msgSend(nsAppearance, namedSel, appearanceNamePtr);
            if (isNull(appearance)) return;

            Pointer setSel = objc.sel_registerName("setAppearance:");
            objc.objc_msgSend(nsApp, setSel, appearance);
        } catch (Throwable t) {
            LOG.warning("Failed to set macOS appearance", t);
        }
    }

    /// Applies rounded corners on the given Stage's NSWindow content view layer.
    ///
    /// This must be called after the stage is shown (e.g. in a {@code WINDOW_SHOWN} event).
    ///
    /// @param stage the JavaFX stage (must be a macOS NSWindow-backed stage)
    /// @param radius the corner radius in pixels
    public static void applyRoundedCorners(Stage stage, double radius) {
        if (nsApp == null) return;

        try {
            var objc = ObjectiveCRuntime.INSTANCE;

            long windowPtr = getNSWindowPointer(stage);
            if (windowPtr == 0) return;
            Pointer nsWindow = new Pointer(windowPtr);

            // [NSColor clearColor]
            Pointer nsColor = objc.objc_getClass("NSColor");
            if (isNull(nsColor)) return;
            Pointer clearColorSel = objc.sel_registerName("clearColor");
            Pointer clearColor = objc.objc_msgSend(nsColor, clearColorSel);
            if (isNull(clearColor)) return;

            // [window setBackgroundColor: [NSColor clearColor]]
            Pointer setBgSel = objc.sel_registerName("setBackgroundColor:");
            objc.objc_msgSend(nsWindow, setBgSel, clearColor);

            // [window setOpaque: NO]
            Pointer setOpaqueSel = objc.sel_registerName("setOpaque:");
            objc.objc_msgSend(nsWindow, setOpaqueSel, false);

            // [window contentView]
            Pointer contentViewSel = objc.sel_registerName("contentView");
            Pointer contentView = objc.objc_msgSend(nsWindow, contentViewSel);
            if (isNull(contentView)) return;

            // [contentView setWantsLayer: YES]
            Pointer setWantsLayerSel = objc.sel_registerName("setWantsLayer:");
            objc.objc_msgSend(contentView, setWantsLayerSel, true);

            // [[contentView layer] setCornerRadius: radius]
            Pointer layerSel = objc.sel_registerName("layer");
            Pointer layer = objc.objc_msgSend(contentView, layerSel);
            if (!isNull(layer)) {
                Pointer setCornerRadiusSel = objc.sel_registerName("setCornerRadius:");
                objc.objc_msgSend(layer, setCornerRadiusSel, radius);

                // [[contentView layer] setMasksToBounds: YES]
                Pointer setMasksToBoundsSel = objc.sel_registerName("setMasksToBounds:");
                objc.objc_msgSend(layer, setMasksToBoundsSel, true);
            }
        } catch (Throwable t) {
            LOG.warning("Failed to apply macOS rounded corners", t);
        }
    }

    /// Retrieves the native NSWindow pointer from a JavaFX Stage using reflection.
    private static long getNSWindowPointer(Stage stage) {
        try {
            Class<?> windowStageClass = Class.forName("com.sun.javafx.tk.quantum.WindowStage");
            Class<?> glassWindowClass = Class.forName("com.sun.glass.ui.Window");
            Class<?> tkStageClass = Class.forName("com.sun.javafx.tk.TKStage");

            Object tkStage = MethodHandles.privateLookupIn(Window.class, MethodHandles.lookup())
                    .findVirtual(Window.class, "getPeer", MethodType.methodType(tkStageClass))
                    .invoke(stage);

            MethodHandles.Lookup windowStageLookup = MethodHandles.privateLookupIn(windowStageClass, MethodHandles.lookup());
            MethodHandle getPlatformWindow = windowStageLookup.findVirtual(windowStageClass, "getPlatformWindow", MethodType.methodType(glassWindowClass));
            Object platformWindow = getPlatformWindow.invoke(tkStage);

            return (long) MethodHandles.privateLookupIn(glassWindowClass, MethodHandles.lookup())
                    .findVirtual(glassWindowClass, "getNativeWindow", MethodType.methodType(long.class))
                    .invoke(platformWindow);
        } catch (Throwable ex) {
            LOG.warning("Failed to get NSWindow handle", ex);
            return 0;
        }
    }

    private MacOSNativeUtils() {
    }
}
