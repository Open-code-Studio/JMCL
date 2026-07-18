/*
 * JMCL
 * Copyright (C) 2026  OCS
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
package org.Open_code_Studio.jmcl.util;

import org.Open_code_Studio.jmcl.EntryPoint;
import org.Open_code_Studio.jmcl.Metadata;
import org.Open_code_Studio.jmcl.util.gson.JsonUtils;
import org.Open_code_Studio.jmcl.util.io.ChecksumMismatchException;
import org.Open_code_Studio.jmcl.util.io.JarUtils;
import org.Open_code_Studio.jmcl.java.JavaRuntime;
import org.Open_code_Studio.jmcl.util.platform.Architecture;
import org.Open_code_Studio.jmcl.util.platform.OperatingSystem;
import org.Open_code_Studio.jmcl.util.platform.Platform;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.Open_code_Studio.jmcl.util.gson.JsonUtils.listTypeOf;
import static org.Open_code_Studio.jmcl.util.gson.JsonUtils.mapTypeOf;
import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;
import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

/// Detects architecture mismatch (emulation) at startup and coordinates
/// downloading native-architecture JavaFX libraries with a proper mirror infrastructure.
///
/// When the JVM's architecture differs from the real hardware architecture
/// (e.g. x86-64 JDK on ARM64 Windows), the emulated rendering may fail.
/// This class discovers a native JDK, downloads matching JavaFX modules,
/// and relaunches the application correctly.
///
/// @author OCS contributors
public final class NativeLibraryManager {

    private NativeLibraryManager() {
    }

    /// Represents a single JavaFX dependency descriptor
    public static final class DepDescriptor {
        public String module;
        public String groupId;
        public String artifactId;
        public String version;
        public String classifier;
        public String sha1;

        public String filename() {
            return artifactId + "-" + version + "-" + classifier + ".jar";
        }

        public String sha1() {
            return sha1;
        }
    }

    /// A download source (repository mirror)
    public record Mirror(String name, String url) {
        public static final Mirror MAVEN_CENTRAL = new Mirror(
                i18n("repositories.maven_central"),
                "https://repo1.maven.org/maven2");
        public static final Mirror TENCENTCLOUD_MIRROR = new Mirror(
                i18n("repositories.tencentcloud_mirror"),
                "https://mirrors.cloud.tencent.com/nexus/repository/maven-public");
        public static final Mirror ALIYUN_MIRROR = new Mirror(
                i18n("repositories.aliyun_mirror"),
                "https://maven.aliyun.com/repository/public");
        public static final Mirror HUAWEICLOUD_MIRROR = new Mirror(
                i18n("repositories.huaweicloud_mirror"),
                "https://repo.huaweicloud.com/repository/maven");

        public String resolveUrl(DepDescriptor d) {
            return String.format("%s/%s/%s/%s/%s",
                    url,
                    d.groupId.replace('.', '/'),
                    d.artifactId, d.version,
                    d.filename());
        }
    }

    ///
    /// Entry point called from {@link EntryPoint#main(String[])}.
    ///
    /// If an architecture mismatch is detected, this method attempts to
    /// resolve it by finding a native JDK and downloading native JavaFX.
    /// On success it will relaunch (and thus never return).
    /// On failure it will return `false` so the caller falls through
    /// to the normal (emulated) path.
    ///
    /// @return {@code true} if handled (caller should not proceed further),
    ///         {@code false} if no action was taken (caller continues normally).
    ///
    public static boolean handleArchMismatchIfNeeded() {
        Architecture sysArch = Architecture.SYSTEM_ARCH;
        Architecture curArch = Architecture.CURRENT_ARCH;

        // No mismatch — nothing to do
        if (sysArch == curArch || sysArch == Architecture.UNKNOWN) {
            return false;
        }

        Platform sysPlatform = Platform.getPlatform(OperatingSystem.CURRENT_OS, sysArch);
        LOG.info("Architecture mismatch detected: JVM=" + curArch.getDisplayName()
                + " System=" + sysArch.getDisplayName()
                + " → will attempt to use native libraries for " + sysPlatform);

        // Show a dialog asking the user what to do
        SwingUtils.initLookAndFeel();
        int choice = showArchMismatchDialog(sysArch, curArch);
        if (choice != JOptionPane.YES_OPTION) {
            LOG.info("User chose to continue with emulated JavaFX");
            return false;
        }

        // Find a native JDK
        Path nativeJava = findNativeJDK(sysArch);
        if (nativeJava == null) {
            SwingUtils.showErrorDialog(
                    i18n("native.arch.no_jdk_found", sysArch.getDisplayName()));
            return false;
        }

        LOG.info("Found native JDK: " + nativeJava);

        // Download native-arch JavaFX
        try {
            List<DepDescriptor> deps = loadDependencies(sysPlatform);
            if (deps == null || deps.isEmpty()) {
                LOG.warning("No JavaFX dependencies defined for " + sysPlatform);
                SwingUtils.showErrorDialog(
                        i18n("native.arch.unsupported_platform", sysPlatform.toString()));
                return false;
            }

            boolean downloaded = downloadDependencies(deps, sysPlatform);
            if (!downloaded) {
                return false;
            }

            // Relaunch with native JDK + downloaded JavaFX
            relaunch(nativeJava, deps, sysPlatform);
            // relaunch exits or returns
            return true;
        } catch (CancellationException e) {
            LOG.info("User cancelled native arch download");
            return false;
        } catch (Exception e) {
            LOG.error("Failed to set up native architecture", e);
            SwingUtils.showErrorDialog(
                    i18n("native.arch.download_failed", e.getMessage()));
            return false;
        }
    }

    // --- Architecture mismatch dialog ---

    private static int showArchMismatchDialog(Architecture sysArch, Architecture curArch) {
        String message = i18n("native.arch.mismatch.desc",
                sysArch.getDisplayName(), curArch.getDisplayName());
        return JOptionPane.showConfirmDialog(null,
                message,
                i18n("native.arch.mismatch.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
    }

    // --- Native JDK discovery ---

    ///
    /// Searches common locations for a JDK matching the target architecture.
    ///
    static Path findNativeJDK(Architecture targetArch) {
        if (OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS) {
            // On macOS with Rosetta 2, native JDK is in /usr/libexec/java_home
            if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS) {
                return findMacNativeJDK(targetArch);
            }
            return null;
        }

        // Windows: scan common paths
        List<String> searchRoots = new ArrayList<>();

        // Program Files
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) {
            searchRoots.add(programFiles + "\\Microsoft");
            searchRoots.add(programFiles + "\\Eclipse Adoptium");
            searchRoots.add(programFiles + "\\Java");
            searchRoots.add(programFiles + "\\Eclipse Foundation");
            searchRoots.add(programFiles + "\\RedHat");
            searchRoots.add(programFiles + "\\BellSoft");
        }
        // Also check user home
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            searchRoots.add(userHome + "\\.jdks");
            searchRoots.add(userHome + "\\sdkman\\candidates\\java");
        }

        for (String root : searchRoots) {
            Path rootPath = Path.of(root);
            if (!Files.isDirectory(rootPath)) continue;
            Path result = scanJdkDir(rootPath, targetArch);
            if (result != null) return result;
        }

        return null;
    }

    private static Path scanJdkDir(Path dir, Architecture targetArch) {
        try {
            File[] children = dir.toFile().listFiles();
            if (children == null) return null;

            for (File child : children) {
                if (!child.isDirectory()) continue;
                Path javaExe = child.toPath().resolve("bin/java.exe");
                Path javawExe = child.toPath().resolve("bin/javaw.exe");
                Path javaBin = child.toPath().resolve("bin/java");

                Path exe = null;
                if (Files.isRegularFile(javaExe)) exe = javaExe;
                else if (Files.isRegularFile(javawExe)) exe = javawExe;
                else if (Files.isRegularFile(javaBin)) exe = javaBin;
                else {
                    // Check if child itself has bin/java.exe (flat structure)
                    Path nested = scanJdkDir(child.toPath(), targetArch);
                    if (nested != null) return nested;
                    continue;
                }

                if (checkJavaArch(exe, targetArch)) {
                    return exe;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Path findMacNativeJDK(Architecture targetArch) {
        try {
            Process process = Runtime.getRuntime().exec(
                    new String[]{"/usr/bin/arch", "-arm64", "/usr/libexec/java_home", "-v", "21"});
            process.waitFor();
            if (process.exitValue() == 0) {
                String home = new String(process.getInputStream().readAllBytes(), UTF_8).trim();
                Path javaBin = Path.of(home, "bin/java");
                if (Files.isRegularFile(javaBin)) {
                    return javaBin;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    ///
    /// Verifies the binary at {@code exe} matches {@code targetArch}.
    ///
    private static boolean checkJavaArch(Path exe, Architecture targetArch) {
        try {
            if (!Files.isRegularFile(exe)) return false;

            ProcessBuilder pb = new ProcessBuilder(exe.toString(), "-XshowSettings:properties", "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes(), UTF_8).toLowerCase(Locale.ROOT);
            p.waitFor();

            String expectedArch = switch (targetArch) {
                case ARM64 -> "aarch64";
                case X86_64 -> "amd64";
                case X86 -> "x86";
                default -> targetArch.getCheckedName();
            };

            return output.contains("os.arch = " + expectedArch)
                    || output.contains("os.arch=" + expectedArch);
        } catch (Exception e) {
            return false;
        }
    }

    // --- Dependency loading ---

    private static final String DEPENDENCIES_LIST_FILE = "/assets/openjfx-dependencies.json";

    static List<DepDescriptor> loadDependencies(Platform sysPlatform) {
        try (Reader reader = new InputStreamReader(
                NativeLibraryManager.class.getResourceAsStream(DEPENDENCIES_LIST_FILE), UTF_8)) {
            Map<String, Map<String, List<DepDescriptor>>> allDeps =
                    JsonUtils.GSON.fromJson(reader,
                            mapTypeOf(String.class,
                                    mapTypeOf(String.class,
                                            listTypeOf(DepDescriptor.class))));
            Map<String, List<DepDescriptor>> plat = allDeps.get(sysPlatform.toString());
            if (plat == null) return null;

            if (JavaRuntime.CURRENT_VERSION >= 23) {
                List<DepDescriptor> modern = plat.get("modern");
                if (modern != null) return modern;
            }
            return plat.get("classic");
        } catch (IOException e) {
            LOG.warning("Failed to read dependencies list", e);
            return null;
        }
    }

    // --- Download ---

    static boolean downloadDependencies(List<DepDescriptor> deps, Platform sysPlatform) throws CancellationException, IOException {
        SwingUtils.initLookAndFeel();

        Path depsDir = Metadata.DEPENDENCIES_DIRECTORY
                .resolve(sysPlatform.toString())
                .resolve("openjfx");

        // Determine default mirror
        Mirror defaultMirror;
        List<Mirror> mirrors;
        String customUrl = System.getProperty("jvmmcl.openjfx.repo");
        if (customUrl != null) {
            defaultMirror = new Mirror(i18n("repositories.custom", customUrl), customUrl);
            mirrors = List.of(Mirror.TENCENTCLOUD_MIRROR, Mirror.ALIYUN_MIRROR,
                    Mirror.HUAWEICLOUD_MIRROR, Mirror.MAVEN_CENTRAL, defaultMirror);
        } else if (System.getProperty("user.country", "").equalsIgnoreCase("CN")) {
            defaultMirror = Mirror.TENCENTCLOUD_MIRROR;
            mirrors = List.of(Mirror.TENCENTCLOUD_MIRROR, Mirror.ALIYUN_MIRROR,
                    Mirror.HUAWEICLOUD_MIRROR, Mirror.MAVEN_CENTRAL);
        } else {
            defaultMirror = Mirror.MAVEN_CENTRAL;
            mirrors = List.of(Mirror.MAVEN_CENTRAL, Mirror.TENCENTCLOUD_MIRROR,
                    Mirror.ALIYUN_MIRROR, Mirror.HUAWEICLOUD_MIRROR);
        }

        byte[] buffer = new byte[64 * 1024];
        MessageDigest digest = DigestUtils.getDigest("SHA-1");

        // Check what's already cached
        List<DepDescriptor> toDownload = new ArrayList<>();
        try {
            Files.createDirectories(depsDir);
        } catch (IOException e) {
            LOG.warning("Cannot create deps dir", e);
            return false;
        }

        for (DepDescriptor dep : deps) {
            Path local = depsDir.resolve(dep.filename());
            if (Files.exists(local)) {
                try {
                    verifyChecksum(dep, local, digest, buffer);
                    continue; // already good
                } catch (ChecksumMismatchException e) {
                    LOG.warning("Corrupted: " + dep.filename());
                } catch (IOException e) {
                    LOG.warning("Cannot verify: " + dep.filename());
                }
            }
            toDownload.add(dep);
        }

        if (toDownload.isEmpty()) {
            LOG.info("All native JavaFX modules already cached for " + sysPlatform);
            return true;
        }

        // Download with progress window
        Mirror repo = defaultMirror;
        AtomicBoolean isCancelled = new AtomicBoolean();
        AtomicBoolean changeSource = new AtomicBoolean();
        AtomicLong totalBytes = new AtomicLong();

        while (true) {
            DownloadProgressWindow dialog;
            try {
                String header = i18n("native.arch.download.header",
                        sysPlatform.getArchitecture().getDisplayName(),
                        Metadata.TITLE);
                dialog = new DownloadProgressWindow(
                        i18n("native.arch.download.title"),
                        "<html>" + header.replace("\n", "<br>") + "</html>",
                        toDownload.size(),
                        isCancelled);
            } catch (HeadlessException e) {
                LOG.warning("Headless — downloading without UI");
                downloadSilently(toDownload, repo, depsDir, buffer, digest);
                return true;
            }

            dialog.setOnChangeSource(() -> {
                isCancelled.set(true);
                changeSource.set(true);
            });

            dialog.setVisible(true);

            try {
                try {
                    // Pause briefly so the user can read the window
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }

                for (int i = 0; i < toDownload.size(); i++) {
                    if (isCancelled.get()) {
                        throw new CancellationException();
                    }

                    DepDescriptor dep = toDownload.get(i);
                    String url = repo.resolveUrl(dep);
                    Path localPath = depsDir.resolve(dep.filename());

                    dialog.setCurrent(i18n("download.javafx.component", dep.module));
                    dialog.appendDetail("→ " + dep.filename());

                    LOG.info("Downloading " + url);

                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setRequestProperty("User-Agent", "JMCL/" + Metadata.VERSION);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(60000);
                    conn.setInstanceFollowRedirects(true);

                    try (InputStream is = conn.getInputStream()) {
                        Path tmpFile = localPath.resolveSibling(localPath.getFileName() + ".tmp");
                        long fileBytes = 0;
                        try (OutputStream os = Files.newOutputStream(tmpFile)) {
                            int read;
                            while ((read = is.read(buffer, 0, buffer.length)) >= 0) {
                                if (isCancelled.get()) {
                                    os.close();
                                    Files.deleteIfExists(tmpFile);
                                    throw new CancellationException();
                                }
                                os.write(buffer, 0, read);
                                fileBytes += read;
                            }
                        }
                        Files.move(tmpFile, localPath,
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        totalBytes.addAndGet(fileBytes);
                    }

                    verifyChecksum(dep, localPath, digest, buffer);
                    dialog.advanceProgress(totalBytes.get());

                    if (isCancelled.get()) {
                        throw new CancellationException();
                    }
                }
            } catch (CancellationException e) {
                dialog.dispose();
                if (changeSource.get()) {
                    repo = showMirrorSelector(mirrors, defaultMirror);
                    isCancelled.set(false);
                    changeSource.set(false);
                    totalBytes.set(0);
                    continue;
                }
                throw e;
            }

            dialog.dispose();
            return true;
        }
    }

    private static void downloadSilently(List<DepDescriptor> deps, Mirror repo,
                                          Path depsDir, byte[] buffer, MessageDigest digest)
            throws CancellationException {
        try {
            Files.createDirectories(depsDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        for (DepDescriptor dep : deps) {
            Path localPath = depsDir.resolve(dep.filename());
            String url = repo.resolveUrl(dep);
            LOG.info("Downloading " + url);

            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("User-Agent", "JMCL/" + Metadata.VERSION);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                conn.setInstanceFollowRedirects(true);

                try (InputStream is = conn.getInputStream();
                     OutputStream os = Files.newOutputStream(localPath)) {
                    int read;
                    while ((read = is.read(buffer, 0, buffer.length)) >= 0) {
                        os.write(buffer, 0, read);
                    }
                }
                verifyChecksum(dep, localPath, digest, buffer);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static Mirror showMirrorSelector(List<Mirror> mirrors, Mirror defaultMirror) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        for (String line : i18n("repositories.chooser").split("\n")) {
            panel.add(new JLabel(line));
        }

        ButtonGroup group = new ButtonGroup();
        for (Mirror m : mirrors) {
            JRadioButton btn = new JRadioButton(m.name);
            btn.putClientProperty("mirror", m);
            group.add(btn);
            panel.add(btn);
            if (m == defaultMirror) {
                btn.setSelected(true);
            }
        }

        int res = JOptionPane.showConfirmDialog(null, panel,
                i18n("repositories.chooser.title"),
                JOptionPane.OK_CANCEL_OPTION);

        if (res == JOptionPane.OK_OPTION) {
            for (AbstractButton btn : Collections.list(group.getElements())) {
                if (btn.isSelected()) {
                    return (Mirror) btn.getClientProperty("mirror");
                }
            }
        }
        return defaultMirror;
    }

    private static void verifyChecksum(DepDescriptor dep, Path file,
                                        MessageDigest digest, byte[] buffer)
            throws IOException, ChecksumMismatchException {
        digest.reset();
        try (InputStream is = Files.newInputStream(file)) {
            int read;
            while ((read = is.read(buffer, 0, buffer.length)) > -1) {
                digest.update(buffer, 0, read);
            }
        }
        String actual = HexFormat.of().formatHex(digest.digest());
        if (!dep.sha1().equalsIgnoreCase(actual)) {
            throw new ChecksumMismatchException("SHA-1", dep.sha1(), actual);
        }
    }

    // --- Relaunch ---

    static void relaunch(Path nativeJavaExe, List<DepDescriptor> deps, Platform sysPlatform) {
        Path depsDir = Metadata.DEPENDENCIES_DIRECTORY
                .resolve(sysPlatform.toString())
                .resolve("openjfx");

        Path thisJar = JarUtils.thisJarPath();
        if (thisJar == null) {
            LOG.error("Cannot locate this JAR, unable to relaunch");
            return;
        }

        // Build classpath: deps JARs + this JAR
        StringBuilder cp = new StringBuilder();
        for (DepDescriptor dep : deps) {
            Path jarPath = depsDir.resolve(dep.filename());
            if (!cp.isEmpty()) cp.append(File.pathSeparator);
            cp.append(jarPath.toAbsolutePath());
        }
        cp.append(File.pathSeparator).append(thisJar.toAbsolutePath());

        List<String> cmd = new ArrayList<>();
        cmd.add(nativeJavaExe.toAbsolutePath().toString());
        cmd.add("-cp");
        cmd.add(cp.toString());
        cmd.add("-Xmx1g");

        // Pass through relevant system properties
        for (String key : new String[]{
                "jvmmcl.offline.auth.restricted",
                "jvmmcl.openjfx.repo",
                "jvmmcl.home",
                "jvmmcl.dir",
                "jvmmcl.dependencies.dir",
                "jvmmcl.uiScale",
                "jvmmcl.update_source.override",
                "jvmmcl.version.override"
        }) {
            String val = System.getProperty(key);
            if (val != null) {
                cmd.add("-D" + key + "=" + val);
            }
        }

        cmd.add("org.Open_code_Studio.jmcl.Main");

        LOG.info("Relaunching with native JDK: " + String.join(" ", cmd));

        try {
            new ProcessBuilder(cmd)
                    .directory(thisJar.getParent().toFile())
                    .inheritIO()
                    .start();
            EntryPoint.exit(0);
        } catch (IOException e) {
            LOG.error("Failed to relaunch", e);
            SwingUtils.showErrorDialog(
                    i18n("native.arch.relaunch_failed", e.getMessage()));
        }
    }
}
