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
/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2021 Matthew Coley
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.Open_code_Studio.jmcl.util;

import org.Open_code_Studio.jmcl.EntryPoint;
import org.Open_code_Studio.jmcl.Metadata;
import org.Open_code_Studio.jmcl.util.gson.JsonUtils;
import org.Open_code_Studio.jmcl.util.io.ChecksumMismatchException;
import org.Open_code_Studio.jmcl.java.JavaRuntime;
import org.Open_code_Studio.jmcl.util.io.JarUtils;
import org.Open_code_Studio.jmcl.util.platform.Platform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.List;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;
import static org.Open_code_Studio.jmcl.util.gson.JsonUtils.listTypeOf;
import static org.Open_code_Studio.jmcl.util.gson.JsonUtils.mapTypeOf;
import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;
import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

// From: https://github.com/Col-E/Recaf/blob/7378b397cee664ae81b7963b0355ef8ff013c3a7/src/main/java/me/coley/recaf/util/self/SelfDependencyPatcher.java
public final class SelfDependencyPatcher {
    private final List<DependencyDescriptor> dependencies = DependencyDescriptor.readDependencies();
    private final List<Repository> repositories;
    private final Repository defaultRepository;
    private final byte[] buffer = new byte[64 * 1024];
    private final MessageDigest digest = DigestUtils.getDigest("SHA-1");

    private SelfDependencyPatcher() throws PatchException {
        // We can only self-patch JavaFX on specific platform.
        if (dependencies == null) {
            throw new PatchException("Unsupported platform: operating system %s, architecture %s".formatted(
                    System.getProperty("os.name"), System.getProperty("os.arch")));
        }

        final String customUrl = System.getProperty("jvmmcl.openjfx.repo");
        if (customUrl == null) {
            if (System.getProperty("user.country", "").equalsIgnoreCase("CN")) {
                defaultRepository = Repository.TENCENTCLOUD_MIRROR;
            } else {
                defaultRepository = Repository.MAVEN_CENTRAL;
            }
            repositories = List.of(Repository.MAVEN_CENTRAL, Repository.TENCENTCLOUD_MIRROR);
        } else {
            defaultRepository = new Repository(String.format(i18n("repositories.custom"), customUrl), customUrl);
            repositories = List.of(Repository.MAVEN_CENTRAL, Repository.TENCENTCLOUD_MIRROR, defaultRepository);
        }
    }

    private static final class DependencyDescriptor {
        private static final String DEPENDENCIES_LIST_FILE = "/assets/openjfx-dependencies.json";
        private static final Path DEPENDENCIES_DIR_PATH = Metadata.DEPENDENCIES_DIRECTORY.resolve(Platform.CURRENT_PLATFORM.toString()).resolve("openjfx");

        static List<DependencyDescriptor> readDependencies() {
            //noinspection ConstantConditions
            try (Reader reader = new InputStreamReader(SelfDependencyPatcher.class.getResourceAsStream(DEPENDENCIES_LIST_FILE), UTF_8)) {
                Map<String, Map<String, List<DependencyDescriptor>>> allDependencies =
                        JsonUtils.GSON.fromJson(reader, mapTypeOf(String.class, mapTypeOf(String.class, listTypeOf(DependencyDescriptor.class))));
                Map<String, List<DependencyDescriptor>> platform = allDependencies.get(Platform.CURRENT_PLATFORM.toString());
                if (platform == null)
                    return null;

                if (JavaRuntime.CURRENT_VERSION >= 23) {
                    List<DependencyDescriptor> modernDependencies = platform.get("modern");
                    if (modernDependencies != null)
                        return modernDependencies;
                }
                return platform.get("classic");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

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

        public Path localPath() {
            return DEPENDENCIES_DIR_PATH.resolve(filename());
        }
    }

    private record Repository(String name, String url) {
        public static final Repository MAVEN_CENTRAL = new Repository(i18n("repositories.maven_central"), "https://repo1.maven.org/maven2");
        public static final Repository TENCENTCLOUD_MIRROR = new Repository(i18n("repositories.tencentcloud_mirror"), "https://mirrors.cloud.tencent.com/nexus/repository/maven-public");

        public String resolveDependencyURL(DependencyDescriptor descriptor) {
            return String.format("%s/%s/%s/%s/%s",
                    url,
                    descriptor.groupId.replace('.', '/'),
                    descriptor.artifactId, descriptor.version,
                    descriptor.filename());
        }
    }

    /**
     * Patch in any missing dependencies, if any.
     */
    public static void patch() throws PatchException, CancellationException {
        boolean hasBase = classExists("javafx.application.Application");
        boolean hasWeb = classExists("javafx.scene.web.WebView");

        // Everything is fine
        if (hasBase && hasWeb) return;

        SelfDependencyPatcher patcher = new SelfDependencyPatcher();
        boolean needAll = !hasBase;

        LOG.info(needAll
                ? "Missing JavaFX dependencies, attempting to patch in missing classes"
                : "Missing javafx.web module, attempting to patch it in");

        if (needAll) {
            // Download all missing dependencies (original behaviour)
            List<DependencyDescriptor> missing = patcher.checkMissingDependencies();
            if (!missing.isEmpty()) {
                // Try to copy from application bundle first (avoids network download)
                missing = patcher.copyFromBundle(missing);
            }
            if (!missing.isEmpty()) {
                try {
                    patcher.fetchDependencies(missing);
                } catch (IOException e) {
                    throw new PatchException("Failed to download dependencies", e);
                }
            }
            try {
                patcher.loadFromCache(patcher.dependencies);
            } catch (IOException ex) {
                throw new PatchException("Failed to load JavaFX cache", ex);
            } catch (ReflectiveOperationException | NoClassDefFoundError ex) {
                throw new PatchException("Failed to add dependencies to classpath!", ex);
            }
        } else {
            // Only download and load javafx.web
            DependencyDescriptor webDep = patcher.dependencies.stream()
                    .filter(d -> "javafx.web".equals(d.module))
                    .findFirst()
                    .orElseThrow(() -> new PatchException("javafx.web is not declared in openjfx-dependencies.json"));

            boolean needsDownload = true;
            if (java.nio.file.Files.exists(webDep.localPath())) {
                try {
                    patcher.verifyChecksum(webDep);
                    needsDownload = false;
                } catch (ChecksumMismatchException e) {
                    LOG.warning("Corrupted javafx.web: " + e.getMessage());
                } catch (IOException e) {
                    // fall through to download
                }
            }
            if (needsDownload) {
                // Try to copy from application bundle first
                needsDownload = !patcher.copyFromBundle(List.of(webDep)).isEmpty();
            }
            if (needsDownload) {
                try {
                    patcher.fetchDependencies(List.of(webDep));
                } catch (IOException e) {
                    throw new PatchException("Failed to download javafx.web", e);
                }
            }
            try {
                patcher.loadFromCache(List.of(webDep));
            } catch (IOException ex) {
                throw new PatchException("Failed to load javafx.web cache", ex);
            } catch (ReflectiveOperationException | NoClassDefFoundError ex) {
                throw new PatchException("Failed to add javafx.web to classpath!", ex);
            }
        }

        LOG.info(" - Done!");
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Repository showChooseRepositoryDialog() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        for (String line : i18n("repositories.chooser").split("\n")) {
            panel.add(new JLabel(line));
        }

        final ButtonGroup buttonGroup = new ButtonGroup();

        for (Repository repository : repositories) {
            final JRadioButton button = new JRadioButton(repository.name);
            button.putClientProperty("repository", repository);
            buttonGroup.add(button);
            panel.add(button);
            if (repository == defaultRepository) {
                button.setSelected(true);
            }
        }

        int res = JOptionPane.showConfirmDialog(null, panel, i18n("repositories.chooser.title"), JOptionPane.OK_CANCEL_OPTION);

        if (res == JOptionPane.OK_OPTION) {
            final Enumeration<AbstractButton> buttons = buttonGroup.getElements();
            while (buttons.hasMoreElements()) {
                final AbstractButton button = buttons.nextElement();
                if (button.isSelected()) {
                    return (Repository) button.getClientProperty("repository");
                }
            }
        } else {
            LOG.info("User choose not to download JavaFX");
            EntryPoint.exit(0);
        }
        throw new AssertionError();
    }

    /**
     * Inject them into the current classpath.
     *
     * @throws IOException                  When the locally cached dependency urls cannot be resolved.
     * @throws ReflectiveOperationException When the call to add these urls to the system classpath failed.
     */
    private void loadFromCache() throws IOException, ReflectiveOperationException {
        loadFromCache(dependencies);
    }

    private void loadFromCache(Collection<DependencyDescriptor> deps) throws IOException, ReflectiveOperationException {
        LOG.info(" - Loading dependencies...");

        Set<String> modules = deps.stream()
                .map(it -> it.module)
                .collect(toSet());

        Path[] jars = deps.stream()
                .map(DependencyDescriptor::localPath)
                .toArray(Path[]::new);

        String addOpens = JarUtils.getAttribute("jvmmcl.add-opens", null);
        JavaFXPatcher.patch(modules, jars, addOpens != null ? addOpens.split(" ") : new String[0]);
    }

    /**
     * Try to copy missing JavaFX JARs from the application bundle directory
     * (e.g. JMCL.app/Contents/app/). This avoids network downloads when the app
     * was packaged with JavaFX modules included.
     *
     * @param missing the list of still-missing dependencies
     * @return the dependencies that are still missing after the bundle copy attempt
     */
    private List<DependencyDescriptor> copyFromBundle(List<DependencyDescriptor> missing) {
        Path bundleDir = getBundleJarDirectory();
        if (bundleDir == null) {
            return missing; // not in a bundle structure
        }

        List<DependencyDescriptor> stillMissing = new ArrayList<>();
        try {
            Files.createDirectories(DependencyDescriptor.DEPENDENCIES_DIR_PATH);
        } catch (IOException e) {
            LOG.warning("Cannot create dependencies directory", e);
            return missing;
        }

        for (DependencyDescriptor dep : missing) {
            Path bundleJar = bundleDir.resolve(dep.filename());
            if (Files.exists(bundleJar)) {
                try {
                    Files.copy(bundleJar, dep.localPath(), StandardCopyOption.REPLACE_EXISTING);
                    verifyChecksum(dep);
                    LOG.info(" - Copied " + dep.filename() + " from application bundle");
                } catch (ChecksumMismatchException e) {
                    LOG.warning("Bundle checksum mismatch for " + dep.filename() + ": " + e.getMessage());
                    stillMissing.add(dep);
                } catch (IOException e) {
                    LOG.warning("Failed to copy " + dep.filename() + " from bundle: " + e.getMessage());
                    stillMissing.add(dep);
                }
            } else {
                stillMissing.add(dep);
            }
        }

        return stillMissing;
    }

    /**
     * Detect whether the application is running from within a packaged .app bundle.
     * Returns the parent directory of the code-source JAR (Contents/app/), or
     * {@code null} if not in a bundle structure.
     */
    private static Path getBundleJarDirectory() {
        try {
            Path codeSource = Path.of(
                    SelfDependencyPatcher.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI());
            Path parent = codeSource.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                return parent;
            }
        } catch (Exception e) {
            // Not in a bundle — fall through
        }
        return null;
    }

    /**
     * Download dependencies.
     *
     * @throws IOException When the files cannot be fetched or saved.
     */
    private void fetchDependencies(List<DependencyDescriptor> dependencies) throws IOException {
        SwingUtils.initLookAndFeel();

        boolean isFirstTime = true;

        Repository repository = defaultRepository;

        int count = 0;
        while (true) {
            AtomicBoolean isCancelled = new AtomicBoolean();
            AtomicBoolean showDetails = new AtomicBoolean();

            ProgressFrame dialog;
            try {
                dialog = new SwingProgressFrame(i18n("download.javafx"));
            } catch (HeadlessException e) {
                LOG.warning("Failed to open dialog", e);
                dialog = new FakeProgressFrame();
            }

            dialog.setProgressMaximum(dependencies.size() + 1);
            dialog.setProgress(count);
            dialog.setOnCancel(() -> isCancelled.set(true));
            dialog.setOnChangeSource(() -> {
                isCancelled.set(true);
                showDetails.set(true);
            });
            dialog.setVisible(true);
            try {
                if (isFirstTime) {
                    isFirstTime = false;
                    try {
                        //noinspection BusyWait
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                Files.createDirectories(DependencyDescriptor.DEPENDENCIES_DIR_PATH);
                for (int i = count; i < dependencies.size(); i++) {
                    if (isCancelled.get()) {
                        throw new CancellationException();
                    }

                    DependencyDescriptor dependency = dependencies.get(i);

                    final String url = repository.resolveDependencyURL(dependency);
                    ProgressFrame finalDialog = dialog;
                    SwingUtilities.invokeLater(() -> {
                        finalDialog.setCurrent(dependency.module);
                        finalDialog.incrementProgress();
                    });

                    LOG.info("Downloading " + url);

                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setRequestProperty("User-Agent", "JMCL/" + Metadata.VERSION);
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(30000);
                    connection.setInstanceFollowRedirects(true);
                    //noinspection MagicConstant
                    try (InputStream is = connection.getInputStream();
                         OutputStream os = Files.newOutputStream(dependency.localPath())) {

                        int read;
                        while ((read = is.read(buffer, 0, buffer.length)) >= 0) {
                            if (isCancelled.get()) {
                                try {
                                    os.close();
                                } finally {
                                    Files.deleteIfExists(dependency.localPath());
                                }
                                throw new CancellationException();
                            }
                            os.write(buffer, 0, read);
                        }
                    }
                    verifyChecksum(dependency);
                    count++;
                }
            } catch (CancellationException e) {
                dialog.dispose();
                if (showDetails.get()) {
                    repository = showChooseRepositoryDialog();
                    continue;
                } else {
                    throw e;
                }
            }
            dialog.dispose();
            return;
        }
    }

    private List<DependencyDescriptor> checkMissingDependencies() {
        List<DependencyDescriptor> missing = new ArrayList<>();

        for (DependencyDescriptor dependency : dependencies) {
            if (!Files.exists(dependency.localPath())) {
                missing.add(dependency);
                continue;
            }

            try {
                verifyChecksum(dependency);
            } catch (ChecksumMismatchException e) {
                LOG.warning("Corrupted dependency " + dependency.filename() + ": " + e.getMessage());
                missing.add(dependency);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return missing;
    }

    private void verifyChecksum(DependencyDescriptor dependency) throws IOException, ChecksumMismatchException {
        digest.reset();
        try (InputStream is = Files.newInputStream(dependency.localPath())) {
            int read;
            while ((read = is.read(buffer, 0, buffer.length)) > -1) {
                digest.update(buffer, 0, read);
            }
        }

        String sha1 = HexFormat.of().formatHex(digest.digest());
        if (!dependency.sha1().equalsIgnoreCase(sha1))
            throw new ChecksumMismatchException("SHA-1", dependency.sha1(), sha1);
    }

    public static class PatchException extends Exception {
        PatchException(String message) {
            super(message);
        }

        PatchException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public sealed interface ProgressFrame {
        void setCurrent(String component);

        void setProgressMaximum(int total);

        void setProgress(int n);

        void incrementProgress();

        void setOnCancel(Runnable action);

        void setOnChangeSource(Runnable action);

        void setVisible(boolean visible);

        void dispose();
    }

    public static final class SwingProgressFrame extends JDialog implements ProgressFrame {

        private final JProgressBar progressBar;
        private final JLabel progressText;
        private final JButton btnChangeSource;
        private final JButton btnCancel;

        public SwingProgressFrame(String title) {
            JPanel panel = new JPanel();

            setResizable(false);
            setTitle(title);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setBounds(100, 100, 500, 200);
            setContentPane(panel);
            setLocationRelativeTo(null);

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

            for (String note : i18n("download.javafx.notes").split("\n")) {
                content.add(new JLabel(note));
            }
            content.add(new JLabel("<html><br/></html>"));
            progressText = new JLabel(i18n("download.javafx.prepare"));
            content.add(progressText);
            progressBar = new JProgressBar();
            content.add(progressBar);

            final JPanel buttonBar = new JPanel();
            btnChangeSource = new JButton(i18n("button.change_source"));
            btnCancel = new JButton(i18n("button.cancel"));
            buttonBar.add(btnChangeSource);
            buttonBar.add(btnCancel);

            panel.setLayout(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 5));
            panel.add(content, BorderLayout.CENTER);
            panel.add(buttonBar, BorderLayout.SOUTH);
        }

        public void setCurrent(String component) {
            progressText.setText(i18n("download.javafx.component", component));
        }

        public void setProgressMaximum(int total) {
            progressBar.setMaximum(total);
        }

        public void setProgress(int n) {
            progressBar.setValue(n);
        }

        public void incrementProgress() {
            progressBar.setValue(progressBar.getValue() + 1);
        }

        public void setOnCancel(Runnable action) {
            btnCancel.addActionListener(e -> action.run());
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    action.run();
                }
            });
        }

        public void setOnChangeSource(Runnable action) {
            btnChangeSource.addActionListener(e -> action.run());
        }
    }

    public static final class FakeProgressFrame implements ProgressFrame {

        @Override
        public void setCurrent(String component) {
        }

        @Override
        public void setProgressMaximum(int total) {
        }

        @Override
        public void setProgress(int n) {
        }

        @Override
        public void incrementProgress() {
        }

        @Override
        public void setOnCancel(Runnable action) {
        }

        @Override
        public void setOnChangeSource(Runnable action) {
        }

        @Override
        public void setVisible(boolean visible) {
        }

        @Override
        public void dispose() {
        }
    }
}
