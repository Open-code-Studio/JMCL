#!/bin/bash
# =============================================================================
# JMCL Build & Package Script
# Builds JMCL from source and packages it as a macOS .dmg installer with
# multi-language license agreement (powered by dmgbuild).
#
# Usage:
#   ./build-jpackage.sh              — full build + package
#   ./build-jpackage.sh --skip-build — package only (reuse existing JAR)
#   ./build-jpackage.sh --help       — show usage
#
# Prerequisites:
#   - JDK 21+ with jpackage (JAVA_HOME or JDK21_HOME)
#   - Gradle (bundled wrapper)
#   - dmgbuild (pip3 install dmgbuild)
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ---- Config ----
APP_NAME="JMCL"
VENDOR="Open Code Studio"
IDENTIFIER="org.Open_code_Studio.jmcl"
ICON_PNG="$SCRIPT_DIR/JMCL/image/jmcl.png"
LICENSES_DIR="$SCRIPT_DIR/licenses"
BUILD_DIR="$SCRIPT_DIR/JMCL/build/libs"
DEST_DIR="$SCRIPT_DIR/dist"

# Note: FORK mechanism only needed for Gradle build, not for jpackage.
# Setting it globally can crash jpackage with SIGBUS on Apple Silicon.
# It is now set only in the build step.

# ---- JDK detection ----
resolve_jdk() {
    local dir="$1"
    # macOS .app-style JDK: Contents/Home/
    if [ -f "$dir/Contents/Home/bin/jpackage" ]; then
        echo "$dir/Contents/Home"
    elif [ -f "$dir/bin/jpackage" ]; then
        echo "$dir"
    else
        echo ""
    fi
}

# Priority: explicit env var > system java_home > project jdk21 (known stable)
# jdk21-full is used only for jmods (to create jlink runtime), NOT to run jpackage
# (JDK 21.0.11 has SIGBUS crash on macOS 26/Apple Silicon)
JAVA_HOME=""
if [ -n "${JDK21_HOME:-}" ]; then
    JAVA_HOME="$(resolve_jdk "$JDK21_HOME")"
fi
if [ -z "$JAVA_HOME" ]; then
    # Prefer jdk21 (Microsoft build, known stable) over jdk21-full (Adoptium, known SIGBUS)
    if [ -d "/Users/cangcang/Documents/jdk21" ]; then
        JAVA_HOME="$(resolve_jdk "/Users/cangcang/Documents/jdk21")"
    elif [ -d "/Users/cangcang/Documents/jdk21-full" ]; then
        JAVA_HOME="$(resolve_jdk "/Users/cangcang/Documents/jdk21-full")"
    fi
fi
if [ -z "$JAVA_HOME" ]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || echo "")"
fi

if [ -z "$JAVA_HOME" ] || [ ! -f "$JAVA_HOME/bin/jpackage" ]; then
    echo "ERROR: Cannot find JDK 21+ with jpackage."
    echo "  Set JDK21_HOME or JAVA_HOME to a JDK 21+ installation."
    exit 1
fi

JPACKAGE="$JAVA_HOME/bin/jpackage"
echo "Using JDK: $JAVA_HOME"

# ---- dmgbuild detection ----
DMGBUILD=""
for p in \
    "/Users/cangcang/Library/Python/3.9/bin/dmgbuild" \
    "$(which dmgbuild 2>/dev/null || echo "")" \
    "/opt/homebrew/bin/dmgbuild"; do
    if [ -x "$p" ]; then
        DMGBUILD="$p"
        break
    fi
done
if [ -z "$DMGBUILD" ]; then
    echo "ERROR: dmgbuild not found. Install with: pip3 install dmgbuild"
    exit 1
fi
echo "Using dmgbuild: $DMGBUILD"

# ---- Arg parsing ----
SKIP_BUILD=false
if [ "${1:-}" = "--skip-build" ]; then
    SKIP_BUILD=true
elif [ "${1:-}" = "--help" ]; then
    echo "Usage: $0 [--skip-build] [--help]"
    echo ""
    echo "  --skip-build  Skip Gradle build; reuse existing JAR from $BUILD_DIR"
    echo "  --help        Show this help"
    exit 0
fi

# ============================================================================
# Step 1: Build
# ============================================================================
if [ "$SKIP_BUILD" = false ]; then
    echo ""
    echo "=== Step 1: Building JMCL ==="
    export _JAVA_OPTIONS="-Djdk.lang.Process.launchMechanism=FORK"
    export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2g -Djava.awt.headless=true"

    if command -v gradle &>/dev/null; then
        GRADLE_CMD="gradle"
    else
        GRADLE_CMD="$SCRIPT_DIR/gradlew"
        if [ ! -f "$GRADLE_CMD" ]; then
            echo "ERROR: Gradle wrapper not found at $GRADLE_CMD"
            exit 1
        fi
        chmod +x "$GRADLE_CMD"
    fi

    "$GRADLE_CMD" \
        --no-daemon \
        -g "$SCRIPT_DIR/.gradle-user-home" \
        clean build -x test

    echo "Build complete."
else
    echo ""
    echo "=== Skipping build (--skip-build) ==="
fi

# ============================================================================
# Step 2: Find JAR & determine version
# ============================================================================
echo ""
echo "=== Step 2: Locating JAR ==="
JAR_FILE=$(ls -t "$BUILD_DIR"/*.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "ERROR: No JAR file found in $BUILD_DIR"
    exit 1
fi
echo "JAR: $JAR_FILE"

JAR_BASENAME=$(basename "$JAR_FILE" .jar)
RAW_VERSION="${JAR_BASENAME#JVM-MCL-}"
if [ -z "$RAW_VERSION" ] || [ "$RAW_VERSION" = "$JAR_BASENAME" ]; then
    RAW_VERSION=$(unzip -p "$JAR_FILE" "assets/jvmmcl.properties" 2>/dev/null \
        | grep "^jvmmcl.version=" | cut -d= -f2 || echo "1.0.0")
fi
APP_VERSION=$(echo "$RAW_VERSION" | sed 's/^[^0-9]*//')
echo "Version: $RAW_VERSION → $APP_VERSION"

# ============================================================================
# Step 3: Prepare clean input directory (JAR only)
# ============================================================================
echo ""
echo "=== Step 3: Preparing clean input directory ==="
INPUT_DIR="/tmp/jmcl-input-$$"
mkdir -p "$INPUT_DIR"
cp "$JAR_FILE" "$INPUT_DIR/"
echo "Input dir (JAR only): $INPUT_DIR"

# ============================================================================
# Step 4: Prepare ICNS icon
# ============================================================================
echo ""
echo "=== Step 4: Preparing app icon ==="
ICNS_FILE="/tmp/jmcl-$APP_VERSION.icns"

if [ -f "$ICON_PNG" ]; then
    echo "Converting PNG to ICNS..."
    ICONSET_DIR="/tmp/jmcl-iconset-$$.iconset"
    mkdir -p "$ICONSET_DIR"

    # Generate all required icon sizes for a proper multi-resolution ICNS
    for size in 16 32 128 256 512; do
        sips -z $size $size "$ICON_PNG" --out "$ICONSET_DIR/icon_${size}x${size}.png" &>/dev/null
        # Retina @2x sizes (except 512 which would be 1024)
        if [ "$size" -le 256 ]; then
            sips -z $((size * 2)) $((size * 2)) "$ICON_PNG" --out "$ICONSET_DIR/icon_${size}x${size}@2x.png" &>/dev/null
        fi
    done

    # Convert iconset to ICNS using iconutil
    if iconutil -c icns -o "$ICNS_FILE" "$ICONSET_DIR" &>/dev/null; then
        echo "  Multi-resolution ICNS created: $ICNS_FILE"
    else
        echo "WARNING: ICNS conversion via iconutil failed, falling back to sips..."
        ICNS_TMP_PNG="/tmp/jmcl-icon-256-$$.png"
        if sips -z 256 256 "$ICON_PNG" --out "$ICNS_TMP_PNG" &>/dev/null \
            && sips -s format icns "$ICNS_TMP_PNG" --out "$ICNS_FILE" &>/dev/null; then
            echo "  ICNS created (single size): $ICNS_FILE"
        else
            echo "WARNING: ICNS conversion failed. jpackage will use a default icon."
            ICNS_FILE=""
        fi
        rm -f "$ICNS_TMP_PNG"
    fi
    rm -rf "$ICONSET_DIR"
    echo "Icon: ${ICNS_FILE:-"(default)"}"
else
    echo "WARNING: Icon not found at $ICON_PNG, using default"
    ICNS_FILE=""
fi

# ============================================================================
# Step 4.5: Download JavaFX modules for the runtime (fixes dock bounce)
# ============================================================================
echo ""
echo "=== Step 4.5: Downloading JavaFX modules ==="

JAVAFX_VERSION="21.0.8"

# Detect platform classifier for JavaFX native jars
ARCH=$(uname -m)
if [ "$ARCH" = "arm64" ]; then
    JAVAFX_CLASSIFIER="mac-aarch64"
else
    JAVAFX_CLASSIFIER="mac"
fi

JAVAFX_MODULES=("javafx-base" "javafx-graphics" "javafx-controls" "javafx-web" "javafx-media")
JAVAFX_JARS=""

for module in "${JAVAFX_MODULES[@]}"; do
    JAR_NAME="${module}-${JAVAFX_VERSION}-${JAVAFX_CLASSIFIER}.jar"
    if [ ! -f "$INPUT_DIR/$JAR_NAME" ]; then
        echo "  Downloading $JAR_NAME..."
        curl -sL -o "$INPUT_DIR/$JAR_NAME" \
            "https://repo1.maven.org/maven2/org/openjfx/${module}/${JAVAFX_VERSION}/${JAR_NAME}"
    else
        echo "  $JAR_NAME already present"
    fi
    if [ -z "$JAVAFX_JARS" ]; then
        JAVAFX_JARS="\$APPDIR/$JAR_NAME"
    else
        JAVAFX_JARS="$JAVAFX_JARS:\$APPDIR/$JAR_NAME"
    fi
done

# ============================================================================
# Step 5: Create app-image via jpackage
# ============================================================================
echo ""
echo "=== Step 5: Creating .app bundle (app-image) ==="
mkdir -p "$DEST_DIR"

# Collect JVM options
ADD_OPENS=(
    "--add-opens=java.base/java.lang=ALL-UNNAMED"
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED"
    "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"
    "--add-opens=javafx.base/com.sun.javafx.binding=ALL-UNNAMED"
    "--add-opens=javafx.base/com.sun.javafx.event=ALL-UNNAMED"
    "--add-opens=javafx.base/com.sun.javafx.runtime=ALL-UNNAMED"
    "--add-opens=javafx.base/javafx.beans.property=ALL-UNNAMED"
    "--add-opens=javafx.graphics/javafx.css=ALL-UNNAMED"
    "--add-opens=javafx.graphics/javafx.stage=ALL-UNNAMED"
    "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED"
    "--add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED"
    "--add-opens=javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED"
    "--add-opens=javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED"
    "--add-opens=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED"
    "--add-opens=javafx.graphics/com.sun.prism=ALL-UNNAMED"
    "--add-opens=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED"
    "--add-opens=javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED"
    "--add-opens=javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED"
    "--add-opens=javafx.controls/javafx.scene.control.skin=ALL-UNNAMED"
    "--add-opens=jdk.attach/sun.tools.attach=ALL-UNNAMED"
    "-Djdk.lang.Process.launchMechanism=FORK"
    "-Djvmmcl.offline.auth.restricted=false"
    "-Djvmmcl.dir=$HOME/.jvm-mcl"
    "-Xmx1g"
)

# ============================================================================
# Step 5: Build .app manually (avoid jpackage NoSuchElementException + SIGBUS)
# ============================================================================
echo ""
echo "=== Step 5: Building .app bundle manually ==="

# Use Microsoft JDK 21 as runtime (stable, no SIGBUS on macOS 26/Apple Silicon).
# rsync -aL follows symlinks, which is required for macOS .app-style JDK.
RUNTIME_SRC=""
if [ -f "/Users/cangcang/Documents/jdk21/Contents/Home/bin/java" ]; then
    RUNTIME_SRC="/Users/cangcang/Documents/jdk21/Contents/Home"
elif [ -f "/Users/cangcang/Documents/jdk21/bin/java" ]; then
    RUNTIME_SRC="/Users/cangcang/Documents/jdk21"
else
    echo "ERROR: No stable JDK found for runtime"
    exit 1
fi
echo "  Runtime source: $RUNTIME_SRC"

APP_BUNDLE="$DEST_DIR/$APP_NAME.app"
rm -rf "$APP_BUNDLE"
mkdir -p "$APP_BUNDLE/Contents"/{MacOS,Resources,app}

# Copy runtime (follow symlinks — critical for macOS JDK bundles)
echo "  Copying Java runtime..."
rsync -aL --no-perms "$RUNTIME_SRC/" "$APP_BUNDLE/Contents/runtime/"
echo "  Runtime copied: $(du -sh "$APP_BUNDLE/Contents/runtime" | cut -f1)"

# Copy JAR
cp "$JAR_FILE" "$APP_BUNDLE/Contents/app/"

# Copy JavaFX jars if available
for jfx in "$INPUT_DIR"/javafx-*.jar; do
    [ -f "$jfx" ] && cp "$jfx" "$APP_BUNDLE/Contents/app/"
done

# Add all JVM options to launcher
ADD_OPENS_STR=""
for opt in "${ADD_OPENS[@]}"; do
    ADD_OPENS_STR="$ADD_OPENS_STR  $opt"
done

# Create native launcher script (jpackage's launcher has issues, use shell)
cat > "$APP_BUNDLE/Contents/MacOS/$APP_NAME" << LAUNCHER
#!/bin/bash
DIR="\$(cd "\$(dirname "\$0")" && pwd)"
RUNTIME="\$DIR/../runtime"
APP_DIR="\$DIR/../app"
JAR=\$(ls "\$APP_DIR"/JVM-MCL-*.jar 2>/dev/null | head -1)
if [ -z "\$JAR" ]; then
    osascript -e 'display dialog "JMCL JAR not found!" buttons {"OK"} default button 1 with icon stop'
    exit 1
fi
exec "\$RUNTIME/bin/java" \\
  -Djavafx.preloader=org.Open_code_Studio.jmcl.ui.JMCLPreloader \\
  -Xdock:icon="\$DIR/../Resources/$APP_NAME.icns" \\
  -Xdock:name="$APP_NAME" \\
  -Dprism.order=es2 \\
  -Dsun.java2d.metal=true \\
  -Xmx1g \\
  -Djdk.lang.Process.launchMechanism=FORK \\
  -Djvmmcl.offline.auth.restricted=false \\
  -Djvmmcl.dir="\$HOME/.jvm-mcl" \\
  $ADD_OPENS_STR \\
  -jar "\$JAR"
LAUNCHER
chmod +x "$APP_BUNDLE/Contents/MacOS/$APP_NAME"

# Copy compiled window tracker binary (CGWindowList, no permissions needed)
TRACKER_SRC="$SCRIPT_DIR/JMCL/src/main/resources/assets/macos/jmcl_window_tracker"
if [ -f "$TRACKER_SRC" ]; then
    cp "$TRACKER_SRC" "$APP_BUNDLE/Contents/MacOS/jmcl_window_tracker"
    chmod +x "$APP_BUNDLE/Contents/MacOS/jmcl_window_tracker"
    echo "  Window tracker: OK"
else
    echo "  Window tracker: not found (CGWindowList fallback)"
fi

# Create Info.plist
cat > "$APP_BUNDLE/Contents/Info.plist" << PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>$APP_NAME</string>
    <key>CFBundleIdentifier</key>
    <string>$IDENTIFIER</string>
    <key>CFBundleName</key>
    <string>$APP_NAME</string>
    <key>CFBundleIconFile</key>
    <string>$APP_NAME</string>
    <key>CFBundleVersion</key>
    <string>$APP_VERSION</string>
    <key>CFBundleShortVersionString</key>
    <string>$APP_VERSION</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>LSMinimumSystemVersion</key>
    <string>11.0</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSSupportsAutomaticTermination</key>
    <false/>
    <key>NSQuitAlwaysKeepsWindows</key>
    <false/>
    <key>NSPersistentStateRestorationEnabled</key>
    <false/>
</dict>
</plist>
PLIST

# Copy icon
if [ -n "${ICNS_FILE:-}" ] && [ -f "$ICNS_FILE" ]; then
    cp "$ICNS_FILE" "$APP_BUNDLE/Contents/Resources/$APP_NAME.icns"
fi

# Copy splash screen image (JVM -splash: shows it immediately on JVM init)
if [ -f "$ICON_PNG" ]; then
    cp "$ICON_PNG" "$APP_BUNDLE/Contents/Resources/splash.png"
fi

# Create PkgInfo
echo -n "APPL????" > "$APP_BUNDLE/Contents/PkgInfo"

# Code sign (ad-hoc) to avoid Gatekeeper issues
echo "  Signing .app (ad-hoc)..."
codesign --force --deep --sign - "$APP_BUNDLE" 2>/dev/null || echo "  WARNING: codesign failed (non-fatal)"

echo ""
echo ".app bundle created: $APP_BUNDLE"

# Skip to DMG step directly (no jpackage retry needed)
echo ""
echo "=== Step 6: Generating DMG background ==="

BG_SCRIPT="$LICENSES_DIR/generate_dmg_background.py"
BG_OUTPUT="$LICENSES_DIR/dmg_background.png"
if [ -f "$BG_SCRIPT" ]; then
    python3 "$BG_SCRIPT" "$BG_OUTPUT"
else
    echo "  WARNING: Background generator not found, using default"
fi

# ============================================================================
# Step 7: Create DMG with dmgbuild (includes multi-language license)
# ============================================================================
echo ""
echo "=== Step 7: Creating DMG with dmgbuild ==="

DMG_FINAL="$DEST_DIR/${APP_NAME}-${APP_VERSION}.dmg"
DMGBUILD_SETTINGS="$LICENSES_DIR/dmgbuild_settings.py"

# Remove previous DMG
rm -f "$DMG_FINAL"

# Run dmgbuild
"$DMGBUILD" \
    -D app="$APP_BUNDLE" \
    -D version="$APP_VERSION" \
    -D licenses_dir="$LICENSES_DIR" \
    -s "$DMGBUILD_SETTINGS" \
    "$APP_NAME" \
    "$DMG_FINAL"

echo ""
echo "=== BUILD SUCCESSFUL ==="
echo "Package: $DMG_FINAL"
echo ""

# Show output
ls -lh "$DMG_FINAL"

# Clean up temp files
rm -f "$ICNS_FILE" 2>/dev/null
rm -rf "$INPUT_DIR" 2>/dev/null