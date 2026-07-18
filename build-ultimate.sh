#!/bin/bash
set -euo pipefail

# Ultimate build script - cross-platform build with FORK mechanism

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ---- Auto-detect JAVA_HOME ----
if [ -z "${JAVA_HOME:-}" ]; then
    for candidate in \
        "/Users/cangcang/Documents/jdk21/Contents/Home" \
        "/Users/cangcang/Documents/jdk21" \
        "/usr/lib/jvm/java-21-openjdk" \
        "/usr/lib/jvm/java-21-openjdk-amd64"; do
        if [ -f "$candidate/bin/java" ]; then
            JAVA_HOME="$candidate"
            break
        fi
    done
fi
if [ -z "${JAVA_HOME:-}" ]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || dirname "$(dirname "$(readlink -f "$(which java 2>/dev/null || echo /usr/bin/java)")")" || echo "")"
fi
if [ -z "${JAVA_HOME:-}" ] || [ ! -f "$JAVA_HOME/bin/java" ]; then
    echo "ERROR: JAVA_HOME not found. Set JAVA_HOME to JDK 21+."
    exit 1
fi
export JAVA_HOME
echo "JAVA_HOME: $JAVA_HOME"

# ---- Gradle ----
GRADLE_USER_HOME="$SCRIPT_DIR/.gradle-user-home"
export GRADLE_USER_HOME
GRADLE_CMD="$SCRIPT_DIR/gradlew"
chmod +x "$GRADLE_CMD"

# Force FORK on macOS
if [ "$(uname)" = "Darwin" ]; then
    export _JAVA_OPTIONS="-Djdk.lang.Process.launchMechanism=FORK"
fi
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2g -Djava.awt.headless=true"

echo ""
echo "=== Building JAR + EXE ==="
"$GRADLE_CMD" \
    --no-daemon \
    -g "$GRADLE_USER_HOME" \
    --no-build-cache \
    --no-parallel \
    --max-workers=1 \
    clean build -x test

# ---- Post-build: EXE icon & version (required when EXE exists, FAILS if tools missing) ----
echo ""
echo "=== Post-build: EXE icon & version ==="
BUILD_DIR="$SCRIPT_DIR/JMCL/build/libs"
JAR_FILE=$(ls -t "$BUILD_DIR"/*.jar 2>/dev/null | head -1)
EXE_FILE=$(ls -t "$BUILD_DIR"/*.exe 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ] || [ -z "$EXE_FILE" ]; then
    echo "No JAR/EXE found in $BUILD_DIR, skipping EXE post-processing."
else
    echo "JAR: $JAR_FILE"
    echo "EXE: $EXE_FILE"

    # Extract HMCLauncher.exe
    unzip -p "$JAR_FILE" "assets/HMCLauncher.exe" > /tmp/HMCLauncher_original.exe
    if [ ! -f /tmp/HMCLauncher_original.exe ]; then
        echo "ERROR: Could not extract HMCLauncher.exe from JAR"
        exit 1
    fi
    STUB_SIZE=$(wc -c < /tmp/HMCLauncher_original.exe | tr -d ' ')
    echo "Extracted HMCLauncher.exe ($STUB_SIZE bytes)"

    # Version
    RAW_VERSION=$(unzip -p "$JAR_FILE" "assets/jvmmcl.properties" 2>/dev/null \
        | grep "^jvmmcl.version=" | cut -d= -f2 || echo "2026.1.0")
    echo "Version: $RAW_VERSION"

    # Generate .ico from IMG_0132.JPG (use CreateIcon.java)
    ICON_JPG="$SCRIPT_DIR/IMG_0132.JPG"
    if [ ! -f "$ICON_JPG" ]; then
        echo "ERROR: IMG_0132.JPG not found, cannot create EXE icon"
        exit 1
    fi
    "$JAVA_HOME/bin/javac" -cp "$SCRIPT_DIR" "$SCRIPT_DIR/CreateIcon.java" || exit 1
    "$JAVA_HOME/bin/java" -cp "$SCRIPT_DIR" CreateIcon "$ICON_JPG" /tmp/icon.ico || exit 1
    ICO_SIZE=$(wc -c < /tmp/icon.ico | tr -d ' ')
    echo "ICO created ($ICO_SIZE bytes)"

    # Find Python
    PYTHON_CMD=""
    for py in python3 python; do
        if command -v "$py" >/dev/null 2>&1; then PYTHON_CMD="$py"; break; fi
    done
    if [ -z "$PYTHON_CMD" ]; then
        echo "ERROR: Python not found (python3 or python required)"
        exit 1
    fi

    # Verify pefile is installed
    if ! "$PYTHON_CMD" -c "import pefile" 2>/dev/null; then
        echo "ERROR: pefile not installed. Run: pip3 install pefile"
        exit 1
    fi

    # Run set_exe_icon.py
    if [ ! -f "$SCRIPT_DIR/set_exe_icon.py" ]; then
        echo "ERROR: set_exe_icon.py not found"
        exit 1
    fi
    "$PYTHON_CMD" "$SCRIPT_DIR/set_exe_icon.py" /tmp/HMCLauncher_original.exe /tmp/icon.ico "$RAW_VERSION" || exit 1
    if [ ! -f /tmp/HMCLauncher_original_new.exe ]; then
        echo "ERROR: set_exe_icon.py did not produce output"
        exit 1
    fi

    # Concatenate
    cat /tmp/HMCLauncher_original_new.exe "$JAR_FILE" > "$EXE_FILE"
    echo "EXE icon & version updated: $EXE_FILE"
fi

echo ""
echo "=== Build complete ==="