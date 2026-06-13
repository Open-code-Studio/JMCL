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
package org.Open_code_Studio.jmcl.ui.skin;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;

/**
 * A WebView-based Minecraft skin viewer using the skinview3d library.
 * <p>
 * The HTML page and skinview3d JS bundle are extracted from classpath resources
 * to a temporary directory, then loaded via {@code engine.load(file://...)}.
 * This avoids both network dependency and JAR URL / loadContent issues in WebView.
 */
public class SkinViewerFX extends StackPane {

    private static final int READY_POLL_INTERVAL_MS = 200;
    private static final int READY_POLL_MAX_RETRIES = 75; // 15s total

    /// Static temp dir shared across all SkinViewerFX instances.
    private static volatile Path TEMP_DIR;

    private final WebView webView;
    private final WebEngine engine;
    private volatile boolean initialized = false;
    private int readyPollRetries = 0;
    private volatile Image pendingSkin = null;
    private volatile boolean pendingSlim = false;
    private volatile Image pendingCape = null;

    private final BooleanProperty autoRotate = new SimpleBooleanProperty(true);

    /**
     * Creates a SkinViewerFX with the specified preferred dimensions.
     *
     * @param defaultSkin the default skin image to show
     * @param prefWidth   preferred width
     * @param prefHeight  preferred height
     */
    public SkinViewerFX(Image defaultSkin, int prefWidth, int prefHeight) {
        setPrefWidth(prefWidth);
        setPrefHeight(prefHeight);

        webView = new WebView();
        webView.setStyle("-fx-background-color: transparent;");

        // Make WebView fill the pane
        webView.prefWidthProperty().bind(widthProperty());
        webView.prefHeightProperty().bind(heightProperty());

        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        // After page load, start polling for _skinViewerReady
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                startReadyPolling(defaultSkin);
            }
        });

        // Load from temp directory (file:// URL) — the JS script tag uses a relative path
        String pageUrl = prepareTempResources();
        engine.load(pageUrl);

        getChildren().add(webView);

        // Re-trigger resize on size changes
        ChangeListener<Number> resizeListener = (obs, old, val) -> {
            if (initialized) {
                Platform.runLater(() -> engine.executeScript("resizeCanvas()"));
            }
        };
        widthProperty().addListener(resizeListener);
        heightProperty().addListener(resizeListener);
    }

    /**
     * Extracts {@code skin_viewer.html} and {@code skinview3d.js} from classpath
     * resources to a static temporary directory, then returns the {@code file://}
     * URL of the HTML file.
     * <p>
     * Uses raw byte-level I/O to avoid any charset encoding corruption of the
     * minified JS bundle.
     */
    private static String prepareTempResources() {
        try {
            if (TEMP_DIR == null) {
                synchronized (SkinViewerFX.class) {
                    if (TEMP_DIR == null) {
                        TEMP_DIR = Files.createTempDirectory("jmcl-skinview3d-");
                        TEMP_DIR.toFile().deleteOnExit();

                        // Write skinview3d.js (raw bytes — MUST NOT go through String)
                        copyResourceRaw("/assets/skinview3d/skinview3d.js",
                                TEMP_DIR.resolve("skinview3d.js"));

                        // Write skin_viewer.html (raw bytes)
                        copyResourceRaw("/assets/skinview3d/skin_viewer.html",
                                TEMP_DIR.resolve("skin_viewer.html"));

                        LOG.info("SkinViewerFX resources extracted to " + TEMP_DIR);
                    }
                }
            }
            return TEMP_DIR.resolve("skin_viewer.html").toUri().toString();
        } catch (Exception e) {
            LOG.warning("Failed to extract skin viewer resources", e);
            return "data:text/html,<html><body><p>Skin preview unavailable</p></body></html>";
        }
    }

    /// Copies a classpath resource to a file using raw byte streams.
    private static void copyResourceRaw(String resourcePath, Path dest) throws IOException {
        try (InputStream is = SkinViewerFX.class.getResourceAsStream(resourcePath)) {
            Objects.requireNonNull(is, "Resource not found: " + resourcePath);
            Files.copy(is, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        dest.toFile().deleteOnExit();
    }

    /**
     * Polls the page for skinview3d readiness on the JavaFX thread.
     * Also polls for JS errors reported via {@code window._skinViewerLastError}.
     */
    private void startReadyPolling(Image defaultSkin) {
        PauseTransition poll = new PauseTransition(Duration.millis(READY_POLL_INTERVAL_MS));
        poll.setOnFinished(e -> {
            if (++readyPollRetries > READY_POLL_MAX_RETRIES) {
                LOG.warning("skinview3d failed to become ready within timeout");
                // Report last known JS error if available
                try {
                    Object jsErr = engine.executeScript("window._skinViewerLastError || ''");
                    if (jsErr != null && !jsErr.toString().isEmpty()) {
                        LOG.warning("Last JS error: " + jsErr);
                    }
                } catch (Exception ignored) {
                }
                return;
            }
            try {
                // Check for JS errors
                Object jsErr = engine.executeScript("window._skinViewerLastError || ''");
                if (jsErr != null && !jsErr.toString().isEmpty()) {
                    LOG.warning("JS error (attempt " + readyPollRetries + "): " + jsErr);
                    // Don't give up yet — keep polling in case it resolves
                    poll.playFromStart();
                    return;
                }

                Object ready = engine.executeScript("window._skinViewerReady");
                if (Boolean.TRUE.equals(ready)) {
                    initialized = true;
                    // Apply pending skin/cape if any was queued before ready
                    if (pendingSkin != null) {
                        doUpdateSkin(pendingSkin, pendingSlim, pendingCape);
                        pendingSkin = null;
                        pendingCape = null;
                    } else {
                        // Load the default skin
                        String dataURL = imageToDataURL(defaultSkin);
                        if (dataURL != null && !dataURL.isEmpty()) {
                            engine.executeScript("loadSkin('" + escapeJS(dataURL) + "')");
                        }
                    }
                } else {
                    poll.playFromStart();
                }
            } catch (Exception ex) {
                LOG.warning("skinview3d not ready yet, retrying...", ex);
                poll.playFromStart();
            }
        });
        poll.play();
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Update the skin texture.
     *
     * @param skin   the skin image
     * @param isSlim whether the skin uses slim (Alex) arm model
     * @param cape   optional cape image, null to hide
     */
    public void updateSkin(Image skin, boolean isSlim, @Nullable Image cape) {
        if (!initialized) {
            pendingSkin = skin;
            pendingSlim = isSlim;
            pendingCape = cape;
            return;
        }
        doUpdateSkin(skin, isSlim, cape);
    }

    private void doUpdateSkin(Image skin, boolean isSlim, @Nullable Image cape) {
        String skinDataURL = imageToDataURL(skin);
        Platform.runLater(() -> {
            engine.executeScript("loadSkin('" + escapeJS(skinDataURL) + "')");
            if (cape != null) {
                String capeDataURL = imageToDataURL(cape);
                engine.executeScript("loadCape('" + escapeJS(capeDataURL) + "')");
            } else {
                engine.executeScript("loadCape(null)");
            }
        });
    }

    /**
     * Enable or disable auto-rotation of the model.
     */
    public void setAutoRotate(boolean enabled) {
        autoRotate.set(enabled);
    }

    /**
     * Returns the auto-rotate property.
     */
    public BooleanProperty autoRotateProperty() {
        return autoRotate;
    }

    /**
     * Force a resize of the canvas to match the current container dimensions.
     */
    public void forceResize() {
        if (initialized) {
            Platform.runLater(() -> engine.executeScript("resizeCanvas()"));
        }
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    /**
     * Converts a JavaFX {@link Image} to a base64 data URL (PNG format).
     */
    private static String imageToDataURL(Image fxImage) {
        if (fxImage == null) return "";
        try {
            int w = (int) fxImage.getWidth();
            int h = (int) fxImage.getHeight();
            if (w <= 0 || h <= 0) return "";

            PixelReader reader = fxImage.getPixelReader();
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    bi.setRGB(x, y, reader.getArgb(x, y));
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            LOG.warning("Failed to convert skin image to data URL", e);
            return "";
        }
    }

    /**
     * Escapes special characters in a JavaScript string literal.
     */
    private static String escapeJS(String s) {
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}