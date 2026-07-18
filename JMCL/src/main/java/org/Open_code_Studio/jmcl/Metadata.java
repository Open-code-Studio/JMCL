/*
 * JMCL
 * Copyright (C) 2021  Open Code Studio and contributors
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
package org.Open_code_Studio.jmcl;

import org.Open_code_Studio.jmcl.util.StringUtils;
import org.Open_code_Studio.jmcl.util.io.JarUtils;
import org.Open_code_Studio.jmcl.util.platform.Architecture;
import org.Open_code_Studio.jmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.EnumSet;

/**
 * Stores metadata about this application.
 */
public final class Metadata {
    private Metadata() {
    }

    public static final String NAME = "JVM-MCL";
    public static final String FULL_NAME = "JVM Minecraft! Launcher";
    public static final String VERSION = System.getProperty("jvmmcl.version.override", JarUtils.getAttribute("jvmmcl.version", "@develop@"));

    public static final String TITLE = NAME + " " + VERSION;
    public static final String FULL_TITLE = FULL_NAME + " v" + VERSION;

    public static final int MINIMUM_REQUIRED_JAVA_VERSION = 17;
    public static final int MINIMUM_SUPPORTED_JAVA_VERSION = 17;
    public static final int RECOMMENDED_JAVA_VERSION = 21;

    public static final String NEW_REPO_URL = "https://github.com/Open-code-Studio/JMCL";
    public static final String ORIGINAL_REPO_URL = "https://github.com/Open-code-Studio/JMCL";
    public static final String PUBLISH_URL = NEW_REPO_URL;
    public static final String DOWNLOAD_URL = NEW_REPO_URL + "/releases";
    public static final String UPDATE_URL = System.getProperty("jvmmcl.update_source.override", NEW_REPO_URL + "/releases");
    public static final String MANUAL_UPDATE_URL = ORIGINAL_REPO_URL + "/releases";

    public static final String DOCS_URL = "https://docs.jmcl.net";
    public static final String CONTACT_URL = DOCS_URL + "/help.html";
    public static final String CHANGELOG_URL = ORIGINAL_REPO_URL + "/releases";
    public static final String EULA_URL = DOCS_URL + "/eula/jmcl.html";

    public static final String BUILD_CHANNEL = JarUtils.getAttribute("jvmmcl.version.type", "nightly");
    public static final String GITHUB_SHA = JarUtils.getAttribute("jvmmcl.version.hash", null);

    public static final Path CURRENT_DIRECTORY = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    public static final Path MINECRAFT_DIRECTORY = OperatingSystem.getWorkingDirectory("minecraft");
    public static final Path GLOBAL_DIRECTORY;
    public static final Path LOCAL_DIRECTORY;
    public static final Path DEPENDENCIES_DIRECTORY;

    public static final String OLD_HMCL_HOME_SYSTEM_PROPERTY = "jmcl.home";

    static {
        String home = System.getProperty("jvmmcl.home", System.getenv("JVM-MCL_USER_HOME"));
        if (StringUtils.isBlank(home)) {
            home = System.getProperty(OLD_HMCL_HOME_SYSTEM_PROPERTY);
        }
        if (StringUtils.isBlank(home)) {
            if (OperatingSystem.CURRENT_OS.isLinuxOrBSD()) {
                String xdgData = System.getenv("XDG_DATA_HOME");
                if (StringUtils.isNotBlank(xdgData)) {
                    GLOBAL_DIRECTORY = Path.of(xdgData, "jvm-mcl").toAbsolutePath().normalize();
                } else {
                    GLOBAL_DIRECTORY = Path.of(System.getProperty("user.home"), ".local", "share", "jvm-mcl").toAbsolutePath().normalize();
                }
            } else {
                GLOBAL_DIRECTORY = OperatingSystem.getWorkingDirectory("jvm-mcl");
            }
        } else {
            GLOBAL_DIRECTORY = Path.of(home).toAbsolutePath().normalize();
        }

        String localDir = System.getProperty("jvmmcl.dir", System.getenv("JVM-MCL_LOCAL_HOME"));
        LOCAL_DIRECTORY = StringUtils.isNotBlank(localDir)
                ? Path.of(localDir).toAbsolutePath().normalize()
                : CURRENT_DIRECTORY.resolve(".jvm-mcl");

        String deps = System.getProperty("jvmmcl.dependencies.dir", System.getenv("JVM-MCL_DEPENDENCIES_DIR"));
        DEPENDENCIES_DIRECTORY = StringUtils.isNotBlank(deps)
                ? Path.of(deps).toAbsolutePath().normalize()
                : LOCAL_DIRECTORY.resolve("dependencies");
    }

    public static boolean isStable() {
        return "stable".equals(BUILD_CHANNEL);
    }

    public static boolean isDev() {
        return "dev".equals(BUILD_CHANNEL);
    }

    public static boolean isNightly() {
        return !isStable() && !isDev();
    }

    public static @Nullable String getSuggestedJavaDownloadLink() {
        if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX && Architecture.SYSTEM_ARCH == Architecture.LOONGARCH64_OW)
            return "https://www.loongnix.cn/zh/api/java/downloads-jdk21/index.html";
        else {
            EnumSet<Architecture> supportedArchitectures;
            if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS)
                supportedArchitectures = EnumSet.of(Architecture.X86_64, Architecture.X86, Architecture.ARM64);
            else if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX)
                supportedArchitectures = EnumSet.of(
                        Architecture.X86_64, Architecture.X86,
                        Architecture.ARM64, Architecture.ARM32,
                        Architecture.RISCV64, Architecture.LOONGARCH64
                );
            else if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS)
                supportedArchitectures = EnumSet.of(Architecture.X86_64, Architecture.ARM64);
            else
                supportedArchitectures = EnumSet.noneOf(Architecture.class);
            if (supportedArchitectures.contains(Architecture.SYSTEM_ARCH))
                return String.format("https://adoptium.net/temurin/releases/?os=%s&arch=%s",
                        OperatingSystem.CURRENT_OS.getCheckedName(),
                        Architecture.SYSTEM_ARCH.getCheckedName()
                );
            else
                return null;
        }
    }
}