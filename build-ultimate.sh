#!/bin/bash

# Ultimate build script - use FORK mechanism to bypass posix_spawn issues

JAVA_HOME="/Users/cangcang/Documents/jdk21"
export JAVA_HOME

GRADLE_USER_HOME="/Users/cangcang/Documents/开发/JMCL/.gradle-user-home"
export GRADLE_USER_HOME

GRADLE_HOME="/Users/cangcang/.gradle/wrapper/dists/gradle-9.4.0-bin/lcvyxq3t37f6mx9miaydrrgs/gradle-9.4.0"

# Force use of traditional process spawning (FORK), not posix_spawn
export _JAVA_OPTIONS="-Djdk.lang.Process.launchMechanism=FORK"
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2g -Djava.awt.headless=true"

chmod +x "$GRADLE_HOME/bin/gradle"

"$GRADLE_HOME/bin/gradle" \
  --no-daemon \
  --project-dir "$PWD" \
  --gradle-user-home "$GRADLE_USER_HOME" \
  --no-build-cache \
  --no-parallel \
  --max-workers=1 \
  clean build

# Post-build: Replace EXE icon
echo ""
echo "=== Replacing EXE icon ==="
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/JMCL/build/libs"
JAR_FILE=$(ls -t "$BUILD_DIR"/*.jar 2>/dev/null | head -1)
EXE_FILE=$(ls -t "$BUILD_DIR"/*.exe 2>/dev/null | head -1)

if [ -n "$JAR_FILE" ] && [ -n "$EXE_FILE" ]; then
    echo "JAR_FILE: $JAR_FILE"
    echo "EXE_FILE: $EXE_FILE"

    # Extract HMCLauncher.exe from JAR
    unzip -p "$JAR_FILE" "assets/HMCLauncher.exe" > /tmp/HMCLauncher_original.exe

    if [ -f /tmp/HMCLauncher_original.exe ]; then
        echo "Extracted HMCLauncher.exe ($(stat -f%z /tmp/HMCLauncher_original.exe) bytes)"

        # Extract version from JAR
        RAW_VERSION=$(unzip -p "$JAR_FILE" "assets/jvmmcl.properties" 2>/dev/null \
            | grep "^jvmmcl.version=" | cut -d= -f2 || echo "2026.1.0")
        echo "Version: $RAW_VERSION"

        # Compile and run CreateIcon using IMG_0132.JPG
        ICON_JPG="$SCRIPT_DIR/IMG_0132.JPG"
        if [ -f "$ICON_JPG" ]; then
            "$JAVA_HOME/bin/javac" -cp "$SCRIPT_DIR" "$SCRIPT_DIR/CreateIcon.java" && \
            "$JAVA_HOME/bin/java" -cp "$SCRIPT_DIR" CreateIcon "$ICON_JPG" /tmp/icon.ico
        else
            echo "ERROR: IMG_0132.JPG not found"
            exit 1
        fi

        if [ -f /tmp/icon.ico ]; then
            echo "ICO created ($(stat -f%z /tmp/icon.ico) bytes)"

            python3 "$SCRIPT_DIR/set_exe_icon.py" /tmp/HMCLauncher_original.exe /tmp/icon.ico "$RAW_VERSION"

            if [ -f /tmp/HMCLauncher_original_new.exe ]; then
                cat /tmp/HMCLauncher_original_new.exe "$JAR_FILE" > "$EXE_FILE"
                echo "EXE icon replaced successfully: $EXE_FILE"
            else
                echo "ERROR: set_exe_icon.py did not produce output file"
            fi
        else
            echo "ERROR: Failed to create ICO file"
        fi
    else
        echo "ERROR: Could not extract HMCLauncher.exe from JAR"
    fi
else
    echo "ERROR: Could not find JAR or EXE files in $BUILD_DIR"
fi

echo ""
echo "=== Build complete ==="